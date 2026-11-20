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
    #imagen; // URL de la portada (opcional)

    /** Constructor: inicializa los datos del artículo. */
    constructor(codigo, titulo, precioRenta, disponible = true, imagen = "") {
        // Abstracción: no se puede crear un "Articulo" genérico, solo sus hijas
        if (new.target === Articulo) {
            throw new TypeError("Articulo es abstracta: use Video, VideoJuego o CintaVideoJuego.");
        }
        this.#codigo = codigo;
        this.#titulo = titulo;
        this.#precioRenta = precioRenta;
        this.#disponible = disponible;
        this.#imagen = Articulo.imagenValida(imagen) ? imagen : "";
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
    get imagen() { return this.#imagen; }
    set imagen(url) { this.#imagen = Articulo.imagenValida(url) ? url : ""; }

    /** Categoría para el catálogo: las películas por un lado y los videojuegos (disco o cinta) por otro. */
    get categoria() { return this.claveTipo === "VIDEO" ? "Película" : "Videojuego"; }

    /** Portada para las tarjetas: la imagen del artículo o una portada generada con el título (SVG, no pesa). */
    get portada() {
        return this.#imagen || Articulo.portadaGenerada(this.#titulo, this.categoria, this.claveTipo);
    }

    /** Imagen a mostrar: la del artículo si tiene, o una ilustración ligera según el tipo. */
    get imagenMostrar() {
        return this.#imagen || Articulo.ilustracion(this.claveTipo);
    }

    /** Solo se aceptan rutas relativas o direcciones http(s); así se evita código malicioso en el enlace. */
    static imagenValida(url) {
        return typeof url === "string" && /^(https?:\/\/|img\/)[^\s"'<>]+$/i.test(url);
    }

    /** Portada generada: fondo de color según el tipo, categoría y título en hasta 3 líneas. */
    static portadaGenerada(titulo, categoria, claveTipo) {
        const colores = { VIDEO: "#be4a5c", VIDEOJUEGO: "#4a8fbe", CINTA: "#be9a4a" };
        const color = colores[claveTipo] || "#64748b";
        const lineas = [];
        let actual = "";
        for (const palabra of String(titulo).split(/\s+/)) {
            if ((actual + " " + palabra).trim().length > 13 && actual) { lineas.push(actual); actual = palabra; }
            else { actual = (actual + " " + palabra).trim(); }
        }
        lineas.push(actual);
        const texto = lineas.slice(0, 3).map((l, i) =>
            `<text x="80" y="${150 - (Math.min(lineas.length, 3) - 1) * 12 + i * 24}" text-anchor="middle" font-family="Arial" font-weight="bold" font-size="${l.length > 11 ? 16 : 19}" fill="#fff">${esc(l.length > 15 ? l.slice(0, 14) + "…" : l)}</text>`).join("");
        const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 160 220"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0f172a"/><stop offset="1" stop-color="${color}" stop-opacity=".55"/></linearGradient></defs>`
            + `<rect width="160" height="220" fill="url(#g)"/><rect x="10" y="10" width="140" height="200" rx="10" fill="none" stroke="${color}" stroke-opacity=".7"/>`
            + `<circle cx="125" cy="45" r="34" fill="${color}" fill-opacity=".25"/>`
            + `<text x="80" y="38" text-anchor="middle" font-family="Arial" font-size="9" letter-spacing="2" fill="${color}">${esc(categoria.toUpperCase())}</text>`
            + texto
            + `<text x="80" y="188" text-anchor="middle" font-family="Arial" font-size="7" letter-spacing="1" fill="#cbd5e1">FILM MAGIC COLLECTION</text>`
            + `<rect x="60" y="196" width="40" height="3" rx="2" fill="${color}"/></svg>`;
        return "data:image/svg+xml," + encodeURIComponent(svg);
    }

    /** Ilustración por defecto (SVG dentro del código, no pesa nada). */
    static ilustracion(claveTipo) {
        const fondos = { VIDEO: "#1e3a8a", VIDEOJUEGO: "#7c3aed", CINTA: "#b45309" };
        const dibujos = {
            VIDEO: '<rect x="14" y="22" width="52" height="34" rx="4" fill="#fff"/><path d="M34 31l16 8-16 8z" fill="#1e3a8a"/><rect x="14" y="14" width="52" height="8" fill="#93c5fd"/>',
            VIDEOJUEGO: '<rect x="12" y="26" width="56" height="28" rx="14" fill="#fff"/><rect x="24" y="35" width="12" height="4" fill="#7c3aed"/><rect x="28" y="31" width="4" height="12" fill="#7c3aed"/><circle cx="50" cy="35" r="3" fill="#7c3aed"/><circle cx="57" cy="41" r="3" fill="#7c3aed"/>',
            CINTA: '<rect x="20" y="14" width="40" height="46" rx="4" fill="#fff"/><rect x="26" y="20" width="28" height="18" fill="#b45309"/><rect x="30" y="46" width="20" height="8" fill="#fcd34d"/>'
        };
        const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 72"><rect width="80" height="72" rx="8" fill="${fondos[claveTipo] || "#475569"}"/>${dibujos[claveTipo] || ""}</svg>`;
        return "data:image/svg+xml," + encodeURIComponent(svg);
    }

    // ---------- Persistencia ----------
    toJSON() {
        return { tipo: this.claveTipo, codigo: this.#codigo, titulo: this.#titulo,
                 precio: this.#precioRenta, disponible: this.#disponible, imagen: this.#imagen };
    }

    /** Método estático (fábrica): crea el tipo de artículo correcto a partir de su clave. */
    static crear(claveTipo, codigo, titulo, precio, disponible = true, imagen = "") {
        switch (claveTipo) {
            case "VIDEO": return new Video(codigo, titulo, precio, disponible, imagen);
            case "VIDEOJUEGO": return new VideoJuego(codigo, titulo, precio, disponible, imagen);
            case "CINTA": return new CintaVideoJuego(codigo, titulo, precio, disponible, imagen);
            default: throw new Error("Tipo de artículo desconocido: " + claveTipo);
        }
    }

    static desdeObjeto(o) {
        return Articulo.crear(o.tipo, o.codigo, o.titulo, o.precio, o.disponible, o.imagen);
    }
}

/* ---------- Clases hijas: HERENCIA con "extends" ---------- */

/** Película o video rentable. */
class Video extends Articulo {
    constructor(codigo, titulo, precioRenta, disponible = true, imagen = "") {
        super(codigo, titulo, precioRenta, disponible, imagen); // constructor de la clase base
    }
    get tipo() { return "Video"; }
    get claveTipo() { return "VIDEO"; }
    get diasRenta() { return 3; }
    get tarifaAtrasoDiaria() { return 25; }
}

/** Video juego rentable (disco o cartucho). */
class VideoJuego extends Articulo {
    constructor(codigo, titulo, precioRenta, disponible = true, imagen = "") {
        super(codigo, titulo, precioRenta, disponible, imagen);
    }
    get tipo() { return "Video juego"; }
    get claveTipo() { return "VIDEOJUEGO"; }
    get diasRenta() { return 5; }
    get tarifaAtrasoDiaria() { return 50; }
}

/** Cinta de video juego rentable. */
class CintaVideoJuego extends Articulo {
    constructor(codigo, titulo, precioRenta, disponible = true, imagen = "") {
        super(codigo, titulo, precioRenta, disponible, imagen);
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
