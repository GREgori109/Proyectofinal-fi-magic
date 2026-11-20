// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: PENDIENTE
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

"use strict";

/* =====================================================================
   MODELO DE DATOS - Film Magic
   Clases: Articulo (abstracta), Video, VideoJuego, CintaVideoJuego,
           Cliente, Renta y Premio.
   Aplica ENCAPSULAMIENTO (campos privados #), CONSTRUCTORES, HERENCIA
   (extends), ABSTRACCIÓN y POLIMORFISMO.
   ===================================================================== */

/* ---------- Utilidades de fecha (las fechas se manejan como "AAAA-MM-DD") ---------- */
const Fecha = {
    parsear(texto) {
        const [a, m, d] = texto.split("-").map(Number);
        return new Date(Date.UTC(a, m - 1, d));
    },
    formato(date) {
        return date.toISOString().slice(0, 10);
    },
    /** Devuelve la fecha resultante de sumar n días. */
    sumar(texto, n) {
        const d = Fecha.parsear(texto);
        d.setUTCDate(d.getUTCDate() + n);
        return Fecha.formato(d);
    },
    /** Días transcurridos entre a y b (b - a). */
    diferencia(a, b) {
        return Math.round((Fecha.parsear(b) - Fecha.parsear(a)) / 86400000);
    },
    hoy() {
        const d = new Date();
        const mm = String(d.getMonth() + 1).padStart(2, "0");
        const dd = String(d.getDate()).padStart(2, "0");
        return `${d.getFullYear()}-${mm}-${dd}`;
    },
    valida(texto) {
        return /^\d{4}-\d{2}-\d{2}$/.test(texto) && Fecha.formato(Fecha.parsear(texto)) === texto;
    }
};

/** Da formato de dinero dominicano: RD$1,234.50 */
function dinero(valor) {
    return "RD$" + Number(valor).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** Escapa texto para HTML: evita que lo escrito por el usuario altere la página. */
function esc(texto) {
    return String(texto)
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

/* =====================================================================
   ARTICULO (clase abstracta)
   ===================================================================== */
class Articulo {
    // Atributos encapsulados: solo se acceden mediante getters/setters
    #codigo;
    #titulo;
    #precioRenta;
    #disponible;

    /** Constructor: inicializa los datos del artículo. */
    constructor(codigo, titulo, precioRenta, disponible = true) {
        // Abstracción: no se puede crear un "Articulo" genérico, solo sus hijas
        if (new.target === Articulo) {
            throw new TypeError("Articulo es abstracta: use Video, VideoJuego o CintaVideoJuego.");
        }
        this.#codigo = codigo;
        this.#titulo = titulo;
        this.#precioRenta = precioRenta;
        this.#disponible = disponible;
    }

    // ---------- Métodos "abstractos": cada subclase los define a su manera ----------
    get tipo() { throw new Error("Debe implementarse en la subclase"); }
    get claveTipo() { throw new Error("Debe implementarse en la subclase"); }
    get diasRenta() { throw new Error("Debe implementarse en la subclase"); }
    get tarifaAtrasoDiaria() { throw new Error("Debe implementarse en la subclase"); }

    // ---------- Comportamiento común (polimorfismo sobre la cuota) ----------
    /** Calcula la cuota por atraso según los días de retraso y el tipo de artículo. */
    calcularCuotaAtraso(diasAtraso) {
        if (diasAtraso <= 0) {
            return 0;
        }
        return diasAtraso * this.tarifaAtrasoDiaria;
    }

    // ---------- Getters y setters ----------
    get codigo() { return this.#codigo; }
    get titulo() { return this.#titulo; }
    set titulo(valor) { this.#titulo = valor; }
    get precioRenta() { return this.#precioRenta; }
    set precioRenta(valor) { this.#precioRenta = valor; }
    get disponible() { return this.#disponible; }
    set disponible(valor) { this.#disponible = Boolean(valor); }

    // ---------- Persistencia ----------
    toJSON() {
        return { tipo: this.claveTipo, codigo: this.#codigo, titulo: this.#titulo,
                 precio: this.#precioRenta, disponible: this.#disponible };
    }

    /** Método estático (fábrica): crea el tipo de artículo correcto a partir de su clave. */
    static crear(claveTipo, codigo, titulo, precio, disponible = true) {
        switch (claveTipo) {
            case "VIDEO": return new Video(codigo, titulo, precio, disponible);
            case "VIDEOJUEGO": return new VideoJuego(codigo, titulo, precio, disponible);
            case "CINTA": return new CintaVideoJuego(codigo, titulo, precio, disponible);
            default: throw new Error("Tipo de artículo desconocido: " + claveTipo);
        }
    }

    static desdeObjeto(o) {
        return Articulo.crear(o.tipo, o.codigo, o.titulo, o.precio, o.disponible);
    }
}

/* ---------- Clases hijas: HERENCIA con "extends" ---------- */

/** Película o video rentable. */
class Video extends Articulo {
    constructor(codigo, titulo, precioRenta, disponible = true) {
        super(codigo, titulo, precioRenta, disponible); // constructor de la clase base
    }
    get tipo() { return "Video"; }
    get claveTipo() { return "VIDEO"; }
    get diasRenta() { return 3; }
    get tarifaAtrasoDiaria() { return 25; }
}

/** Video juego rentable (disco o cartucho). */
class VideoJuego extends Articulo {
    constructor(codigo, titulo, precioRenta, disponible = true) {
        super(codigo, titulo, precioRenta, disponible);
    }
    get tipo() { return "Video juego"; }
    get claveTipo() { return "VIDEOJUEGO"; }
    get diasRenta() { return 5; }
    get tarifaAtrasoDiaria() { return 50; }
}

/** Cinta de video juego rentable. */
class CintaVideoJuego extends Articulo {
    constructor(codigo, titulo, precioRenta, disponible = true) {
        super(codigo, titulo, precioRenta, disponible);
    }
    get tipo() { return "Cinta VJ"; }
    get claveTipo() { return "CINTA"; }
    get diasRenta() { return 5; }
    get tarifaAtrasoDiaria() { return 40; }
}

/* =====================================================================
   CLIENTE
   Datos de la tarjeta: {cédula, nombre, teléfono, dirección, código de la
   empresa} más la cuota por atraso pendiente.
   ===================================================================== */
class Cliente {
    #cedula;
    #nombre;
    #telefono;
    #direccion;
    #codigo;      // código de la empresa (identifica al cliente en el sistema)
    #deudaAtraso; // cuota por atraso pendiente

    constructor(cedula, nombre, telefono, direccion, codigo, deudaAtraso = 0) {
        this.#cedula = cedula;
        this.#nombre = nombre;
        this.#telefono = telefono;
        this.#direccion = direccion;
        this.#codigo = codigo;
        this.#deudaAtraso = deudaAtraso;
    }

    get cedula() { return this.#cedula; }
    get nombre() { return this.#nombre; }
    set nombre(valor) { this.#nombre = valor; }
    get telefono() { return this.#telefono; }
    set telefono(valor) { this.#telefono = valor; }
    get direccion() { return this.#direccion; }
    set direccion(valor) { this.#direccion = valor; }
    get codigo() { return this.#codigo; }
    get deudaAtraso() { return this.#deudaAtraso; }
    get tieneDeuda() { return this.#deudaAtraso > 0; }

    /** Suma una cuota por atraso al registro del cliente. */
    agregarDeuda(monto) {
        if (monto > 0) {
            this.#deudaAtraso += monto;
        }
    }

    /**
     * Paga la deuda. Sin argumento paga todo lo pendiente; con un monto paga
     * parcialmente (nunca más de lo que se debe). Equivale a la sobrecarga de
     * métodos de Java, hecha con un parámetro opcional.
     */
    pagarDeuda(monto) {
        const aPagar = monto === undefined ? this.#deudaAtraso : Math.min(Math.max(monto, 0), this.#deudaAtraso);
        this.#deudaAtraso -= aPagar;
        return aPagar;
    }

    toJSON() {
        return { cedula: this.#cedula, nombre: this.#nombre, telefono: this.#telefono,
                 direccion: this.#direccion, codigo: this.#codigo, deuda: this.#deudaAtraso };
    }

    static desdeObjeto(o) {
        return new Cliente(o.cedula, o.nombre, o.telefono, o.direccion, o.codigo, o.deuda);
    }
}

/* =====================================================================
   RENTA
   Registro de una renta: cliente, artículo, fechas, monto y cuota por atraso.
   ===================================================================== */
class Renta {
    #id;
    #codigoCliente;
    #codigoArticulo;
    #fechaRenta;
    #fechaVencimiento;
    #fechaDevolucion = null; // null mientras no se haya devuelto
    #monto;
    #cuotaAtraso = 0;
    #nota = "";

    constructor(id, codigoCliente, codigoArticulo, fechaRenta, fechaVencimiento, monto) {
        this.#id = id;
        this.#codigoCliente = codigoCliente;
        this.#codigoArticulo = codigoArticulo;
        this.#fechaRenta = fechaRenta;
        this.#fechaVencimiento = fechaVencimiento;
        this.#monto = monto;
    }

    /** Registra la devolución; si hubo atraso deja la nota y la cuota en el registro. */
    registrarDevolucion(fecha, diasAtraso, cuota) {
        this.#fechaDevolucion = fecha;
        this.#cuotaAtraso = cuota;
        if (diasAtraso > 0) {
            this.#nota = `Devuelto con ${diasAtraso} día(s) de atraso`;
        }
    }

    get estaDevuelta() { return this.#fechaDevolucion !== null; }
    get id() { return this.#id; }
    get codigoCliente() { return this.#codigoCliente; }
    get codigoArticulo() { return this.#codigoArticulo; }
    get fechaRenta() { return this.#fechaRenta; }
    get fechaVencimiento() { return this.#fechaVencimiento; }
    get fechaDevolucion() { return this.#fechaDevolucion; }
    get monto() { return this.#monto; }
    get cuotaAtraso() { return this.#cuotaAtraso; }
    get nota() { return this.#nota; }

    toJSON() {
        return { id: this.#id, cliente: this.#codigoCliente, articulo: this.#codigoArticulo,
                 renta: this.#fechaRenta, vence: this.#fechaVencimiento, devuelta: this.#fechaDevolucion,
                 monto: this.#monto, cuota: this.#cuotaAtraso, nota: this.#nota };
    }

    static desdeObjeto(o) {
        const r = new Renta(o.id, o.cliente, o.articulo, o.renta, o.vence, o.monto);
        if (o.devuelta) {
            r.registrarDevolucion(o.devuelta, 0, o.cuota);
            r.#nota = o.nota;
        }
        return r;
    }
}

/* =====================================================================
   PREMIO
   Resultado de la revisión mensual o anual de un cliente premiado.
   ===================================================================== */
class Premio {
    #cliente;
    #totalRentado;
    #cupones;
    #certificado;

    constructor(cliente, totalRentado, cupones, certificado) {
        this.#cliente = cliente;
        this.#totalRentado = totalRentado;
        this.#cupones = cupones;
        this.#certificado = certificado;
    }

    get cliente() { return this.#cliente; }
    get totalRentado() { return this.#totalRentado; }
    get cupones() { return this.#cupones; }
    get tieneCertificado() { return this.#certificado; }
}
