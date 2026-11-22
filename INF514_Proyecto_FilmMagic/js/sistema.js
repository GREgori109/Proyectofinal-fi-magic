// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

"use strict";

/* =====================================================================
   ALMACEN
   Guarda y lee los datos en el navegador (localStorage) para que se
   conserven al cerrar la página. Si el navegador no permite localStorage,
   el sistema sigue funcionando con los datos en memoria.
   ===================================================================== */
class Almacen {
    #clave;
    #respaldo = null; // copia en memoria si localStorage no está disponible

    constructor(clave = "filmmagic.datos.v1") {
        this.#clave = clave;
    }

    guardar(sistema) {
        const texto = JSON.stringify(sistema.aObjeto());
        try {
            localStorage.setItem(this.#clave, texto);
        } catch (e) {
            this.#respaldo = texto;
        }
    }

    /** Devuelve el objeto guardado o null si no hay datos. */
    leer() {
        let texto = this.#respaldo;
        try {
            texto = localStorage.getItem(this.#clave) ?? texto;
        } catch (e) {
            // sin acceso a localStorage: se usa el respaldo en memoria
        }
        if (!texto) {
            return null;
        }
        try {
            return JSON.parse(texto);
        } catch (e) {
            return null; // datos dañados: se empieza vacío
        }
    }

    borrar() {
        this.#respaldo = null;
        try {
            localStorage.removeItem(this.#clave);
        } catch (e) {
            // nada que borrar
        }
    }
}

/* =====================================================================
   FILMMAGIC - Lógica del negocio
   Clientes, artículos, rentas, devoluciones con cuota por atraso,
   revisiones mensual/anual de premios y cartas.
   ===================================================================== */
class FilmMagic {
    // ----- Reglas del negocio (constantes) -----
    static NIVEL_PREMIO_MENSUAL = 1000;
    static NIVEL_PREMIO_ANUAL = 10000;
    /** Un cupón adicional por cada RD$500 rentados por encima del nivel mensual. */
    static PASO_CUPON_MENSUAL = 500;
    /** Un cupón adicional por cada RD$2,000 rentados por encima del nivel anual. */
    static PASO_CUPON_ANUAL = 2000;

    // ----- Almacenamiento en memoria -----
    // Map: búsqueda rápida por código y conserva el orden de registro
    #clientes = new Map();
    #articulos = new Map();
    #rentas = [];
    #fechaSistema = Fecha.hoy();

    // ---------- Fecha del sistema (permite simular el paso del tiempo) ----------
    get fechaSistema() { return this.#fechaSistema; }
    set fechaSistema(fecha) { this.#fechaSistema = fecha; }

    // ---------- Consultas ----------
    get clientes() { return [...this.#clientes.values()]; }
    get articulos() { return [...this.#articulos.values()]; }
    get rentas() { return [...this.#rentas]; }

    buscarCliente(codigo) {
        return this.#clientes.get(String(codigo).trim().toUpperCase());
    }

    buscarArticulo(codigo) {
        return this.#articulos.get(String(codigo).trim().toUpperCase());
    }

    // ---------- Registro ----------
    registrarCliente(c) {
        if (this.#clientes.has(c.codigo)) {
            return false;
        }
        this.#clientes.set(c.codigo, c);
        return true;
    }

    registrarArticulo(a) {
        if (this.#articulos.has(a.codigo)) {
            return false;
        }
        this.#articulos.set(a.codigo, a);
        return true;
    }

    // ---------- Rentas ----------
    /**
     * Renta un artículo a un cliente usando la fecha del sistema.
     * Lanza un Error con el motivo si la renta no se puede hacer.
     */
    rentar(codigoCliente, codigoArticulo) {
        const c = this.buscarCliente(codigoCliente);
        const a = this.buscarArticulo(codigoArticulo);
        if (!c) {
            throw new Error(`No existe un cliente con el código ${codigoCliente}`);
        }
        if (!a) {
            throw new Error(`No existe un artículo con el código ${codigoArticulo}`);
        }
        if (c.tieneDeuda) {
            throw new Error(`El cliente tiene una cuota por atraso pendiente de ${dinero(c.deudaAtraso)}. Debe pagarla antes de rentar.`);
        }
        if (!a.disponible) {
            throw new Error(`El artículo '${a.titulo}' ya está rentado.`);
        }
        const r = new Renta(this.#siguienteIdRenta(), c.codigo, a.codigo, this.#fechaSistema,
            Fecha.sumar(this.#fechaSistema, a.diasRenta), a.precioRenta);
        a.disponible = false;
        this.#rentas.push(r);
        return r;
    }

    /**
     * Registra la devolución de un artículo. Si hay atraso, calcula la cuota
     * y la anota en el registro del cliente.
     */
    devolver(codigoArticulo) {
        const a = this.buscarArticulo(codigoArticulo);
        if (!a) {
            throw new Error(`No existe un artículo con el código ${codigoArticulo}`);
        }
        const abierta = this.#rentas.find(r => r.codigoArticulo === a.codigo && !r.estaDevuelta);
        if (!abierta) {
            throw new Error(`El artículo '${a.titulo}' no está rentado.`);
        }
        const diasAtraso = Math.max(0, Fecha.diferencia(abierta.fechaVencimiento, this.#fechaSistema));
        const cuota = a.calcularCuotaAtraso(diasAtraso); // polimorfismo: depende del tipo
        abierta.registrarDevolucion(this.#fechaSistema, diasAtraso, cuota);
        a.disponible = true;
        if (cuota > 0) {
            this.buscarCliente(abierta.codigoCliente).agregarDeuda(cuota);
        }
        return abierta;
    }

    #siguienteIdRenta() {
        return this.#rentas.reduce((max, r) => Math.max(max, r.id), 0) + 1;
    }

    /** Agrega una renta ya finalizada (usado para cargar datos de demostración). */
    agregarRentaHistorica(codCliente, codArticulo, fecha) {
        const a = this.buscarArticulo(codArticulo);
        const r = new Renta(this.#siguienteIdRenta(), codCliente, codArticulo, fecha,
            Fecha.sumar(fecha, a.diasRenta), a.precioRenta);
        r.registrarDevolucion(Fecha.sumar(fecha, a.diasRenta), 0, 0);
        this.#rentas.push(r);
    }

    // ---------- Revisiones de premios ----------
    /** Revisión mensual: clientes que rentaron MÁS de RD$1,000 en el mes indicado. */
    revisionMensual(anio, mes) {
        const premiados = [];
        const prefijo = `${anio}-${String(mes).padStart(2, "0")}`;
        for (const c of this.#clientes.values()) {
            const total = this.#rentas
                .filter(r => r.codigoCliente === c.codigo && r.fechaRenta.startsWith(prefijo))
                .reduce((suma, r) => suma + r.monto, 0);
            if (total > FilmMagic.NIVEL_PREMIO_MENSUAL) {
                const cupones = 1 + Math.floor((total - FilmMagic.NIVEL_PREMIO_MENSUAL) / FilmMagic.PASO_CUPON_MENSUAL);
                premiados.push(new Premio(c, total, cupones, false));
            }
        }
        return premiados;
    }

    /**
     * Revisión anual: clientes que rentaron MÁS de RD$10,000 en el año.
     * Reciben certificado de video gratis si superan dos veces el nivel (RD$20,000).
     */
    revisionAnual(anio) {
        const premiados = [];
        for (const c of this.#clientes.values()) {
            const total = this.#rentas
                .filter(r => r.codigoCliente === c.codigo && r.fechaRenta.startsWith(`${anio}-`))
                .reduce((suma, r) => suma + r.monto, 0);
            if (total > FilmMagic.NIVEL_PREMIO_ANUAL) {
                const cupones = 1 + Math.floor((total - FilmMagic.NIVEL_PREMIO_ANUAL) / FilmMagic.PASO_CUPON_ANUAL);
                const certificado = total > 2 * FilmMagic.NIVEL_PREMIO_ANUAL;
                premiados.push(new Premio(c, total, cupones, certificado));
            }
        }
        return premiados;
    }

    // ---------- Cartas ----------
    /** Redacta el texto de la carta para un cliente premiado. */
    redactarCarta(premio, periodo, anual) {
        const c = premio.cliente;
        let t = "FILM MAGIC\n";
        t += `Santo Domingo, ${this.#fechaSistema}\n\n`;
        t += `Estimado(a) ${c.nombre}\n${c.direccion}\n\n`;
        t += "Gracias por ser parte de la familia Film Magic. ";
        t += `${anual ? "Durante el año " : "Durante el mes "}${periodo} usted rentó ${dinero(premio.totalRentado)}, `;
        t += `superando nuestro nivel de premio ${anual ? "anual" : "mensual"}.\n\n`;
        t += `Como muestra de agradecimiento le enviamos ${premio.cupones} ${premio.cupones === 1 ? "cupón" : "cupones"} de renta gratis.\n`;
        if (premio.tieneCertificado) {
            t += "Además, incluimos un CERTIFICADO PARA UN VIDEO GRATIS.\n";
        }
        t += "\nAtentamente,\nLa Gerencia de Film Magic\n";
        return t;
    }

    // ---------- Persistencia ----------
    aObjeto() {
        return {
            fecha: this.#fechaSistema,
            clientes: this.clientes.map(c => c.toJSON()),
            articulos: this.articulos.map(a => a.toJSON()),
            rentas: this.#rentas.map(r => r.toJSON())
        };
    }

    cargarDesdeObjeto(o) {
        this.#clientes.clear();
        this.#articulos.clear();
        this.#rentas = [];
        this.#fechaSistema = Fecha.valida(o.fecha) ? o.fecha : Fecha.hoy();
        (o.clientes || []).forEach(x => { const c = Cliente.desdeObjeto(x); this.#clientes.set(c.codigo, c); });
        (o.articulos || []).forEach(x => { const a = Articulo.desdeObjeto(x); this.#articulos.set(a.codigo, a); });
        (o.rentas || []).forEach(x => this.#rentas.push(Renta.desdeObjeto(x)));
    }

    vaciar() {
        this.cargarDesdeObjeto({ fecha: Fecha.hoy() });
    }

    // ---------- Datos de demostración ----------
    /**
     * Carga clientes, artículos y un historial de rentas del año de la fecha del
     * sistema, para poder probar las revisiones mensual y anual.
     */
    cargarDemostracion() {
        this.registrarCliente(new Cliente("001-0000001-1", "María Fernández", "809-555-0101", "C/ Duarte #12, Santo Domingo", "C001"));
        this.registrarCliente(new Cliente("001-0000002-2", "Pedro Martínez", "809-555-0102", "Av. Duarte #45, Santiago", "C002"));
        this.registrarCliente(new Cliente("001-0000003-3", "Ana Rodríguez", "829-555-0103", "C/ Sánchez #7, La Vega", "C003"));
        this.registrarCliente(new Cliente("001-0000004-4", "Luis Peña", "849-555-0104", "Av. Lincoln #88, Santo Domingo", "C004"));
        this.registrarCliente(new Cliente("001-0000005-5", "Carla Núñez", "809-555-0105", "C/ Mella #3, San Cristóbal", "C005"));

        this.registrarArticulo(new Video("V001", "El Padrino", 150, true, "https://i.ebayimg.com/00/s/MTYwMFgxMDY2/z/ioUAAOSw6Btj8lJA/$_57.JPG?set_id=880000500F"));
        this.registrarArticulo(new Video("V002", "Titanic", 150, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRErvzi-28e-p5B_jrBUhoStBmYgUg2OliDZzZ8BmC_yA&s=10"));
        this.registrarArticulo(new Video("V003", "Matrix", 150, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSPFAKeaPH-V-tjN-Uxj3vh0fYQOdeGcIUvabX66GumhA&s=10"));
        this.registrarArticulo(new VideoJuego("J001", "FIFA 24", 300, true, "https://i.pinimg.com/736x/b1/fa/a2/b1faa25f8499f1edb5353e3109b91ad3.jpg"));
        this.registrarArticulo(new VideoJuego("J002", "Zelda Breath of the Wild", 300, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSuaikpqT1ZniAQSbSwTgvM_6ggog2X3D_xso32n0N20Q&s"));
        this.registrarArticulo(new CintaVideoJuego("T001", "Super Mario Bros", 250, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ7utyn-0UlzzlqJShzZi1wtg58UiZ0_xy-TXeTH_iKaw&s=10"));
        this.registrarArticulo(new CintaVideoJuego("T002", "Sonic 2", 250, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTSI7DlVXMrSyUyHnW-UBvGOEQHLLb-9fPRT4xN54GWbg&s=10"));
        this.registrarArticulo(new CintaVideoJuego("T003", "Mortal Kombat", 250, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTq_EznQNEolMOCZ-T-FBlV70kem2Bph7zvq24qbIh5OA&s=10"));
        // Más títulos para el catálogo (sin historial de rentas)
        this.registrarArticulo(new VideoJuego("J003", "Spider-Man: Miles Morales", 150, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRwo6Kc0DVop0GGlfOz7jwsBwF54cYLfKvkh_m0Tfe91Q&s=10"));
        this.registrarArticulo(new VideoJuego("J004", "The Last of Us Part II", 150, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSyEN6ARlwy8k5xqH8Lz5LyqA_Q6mxot_dF7GuyK_cKQQ&s=10"));
        this.registrarArticulo(new VideoJuego("J005", "God of War Ragnarök", 175, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcROx7RH4DFS2dI3UHIoIwXBQI2FpzWpBbA9Z3Gm-PJFcA&s"));
        this.registrarArticulo(new Video("V004", "Avatar: El camino del agua", 100, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQZsAyAbQElUKFSpFEh4GAjrPKcfzjid7FnLv_-bORH9A&s=10"));
        this.registrarArticulo(new Video("V005", "Joker", 100, true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRonyllm4tHPRwvQqkIrjS0caHK5tJKK9I20hHw8mL0RAkrhJRjBhc0nGk&s=10"));
        this.registrarArticulo(new Video("V006", "Interstelar", 100,true, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT2hN-xE6GehHYlWwHMvvylYXQTyAq54hImVhDzlVjlYQ&s=10"));

        const anio = this.#fechaSistema.slice(0, 4);
        const inicio = `${anio}-01-02`;
        const repetir = (codCliente, codArticulo, desde, cadaDias) => {
            for (let f = desde; f <= this.#fechaSistema; f = Fecha.sumar(f, cadaDias)) {
                this.agregarRentaHistorica(codCliente, codArticulo, f);
            }
        };
        repetir("C001", "J001", inicio, 4);                  // supera el premio mensual y el doble del anual
        repetir("C003", "T001", inicio, 6);                  // supera el premio mensual y el anual (sin certificado)
        repetir("C002", "V001", inicio, 9);                  // no alcanza los niveles
        repetir("C004", "V002", Fecha.sumar(inicio, 10), 45); // pocas rentas
    }
}
