// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: PENDIENTE
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

"use strict";

/* =====================================================================
   INTERFAZ - Film Magic
   Dibuja las pantallas en el navegador y conecta los formularios con las
   clases del negocio (FilmMagic, Cliente, Articulo...).
   La navegación usa el "hash" de la dirección: #/inicio, #/clientes, etc.
   ===================================================================== */

const almacen = new Almacen();
const sistema = new FilmMagic();
let aviso = null; // mensaje que se muestra una sola vez: { texto, ok }

// Al abrir la página se cargan los datos guardados (si existen)
const guardado = almacen.leer();
if (guardado) {
    try {
        sistema.cargarDesdeObjeto(guardado);
    } catch (e) {
        sistema.vaciar(); // datos dañados: se empieza vacío
    }
}

function guardar() {
    almacen.guardar(sistema);
}

function avisar(texto, ok) {
    aviso = { texto, ok };
}

/* ---------- Navegación ---------- */

/** Va a una dirección; si ya está en ella, vuelve a dibujar la pantalla. */
function ir(destino) {
    if (location.hash === destino) {
        dibujar();
    } else {
        location.hash = destino;
    }
}

function leerRuta() {
    const [ruta, consulta = ""] = location.hash.replace(/^#/, "").split("?");
    const params = Object.fromEntries(new URLSearchParams(consulta));
    return { ruta: ruta || "/inicio", params };
}

const q = (valor) => encodeURIComponent(valor);

/* ---------- Componentes HTML ---------- */

function tarjeta(numero, texto) {
    return `<div class="tarjeta"><b>${numero}</b><span>${texto}</span></div>`;
}

function campo(nombre, etiqueta, ejemplo, valor = "") {
    return `<label>${etiqueta}<input name="${nombre}" placeholder="${esc(ejemplo)}" value="${esc(valor)}" required></label>`;
}

function fichaCliente(c) {
    return `<div class="panel ficha"><h2>${esc(c.nombre)} <small>(${esc(c.codigo)})</small></h2><div class="datos">
        <div><span>Cédula</span><b>${esc(c.cedula)}</b></div>
        <div><span>Teléfono</span><b>${esc(c.telefono)}</b></div>
        <div><span>Dirección</span><b>${esc(c.direccion)}</b></div>
        <div><span>Código de la empresa</span><b>${esc(c.codigo)}</b></div>
        <div><span>Cuota por atraso</span><b>${c.tieneDeuda ? `<span class="pill rojo">${dinero(c.deudaAtraso)}</span>` : "RD$0.00"}</b></div>
        </div></div>`;
}

function pagina(titulo, activo, contenido) {
    const menu = [["/inicio", "Inicio"], ["/clientes", "Clientes"], ["/articulos", "Artículos"], ["/catalogo", "Catálogo"], ["/rentar", "Rentar"],
                  ["/devolver", "Devolver"], ["/premios", "Premios"], ["/sistema", "Fecha y datos"]];
    const mensaje = aviso ? `<div class="msg ${aviso.ok ? "ok" : "error"}">${esc(aviso.texto)}</div>` : "";
    aviso = null; // el mensaje se muestra una sola vez
    return `<header><div class="marca">🎬 FILM MAGIC <small>Sistema de rentas</small></div>
        <div class="fecha">Fecha del sistema: <b>${sistema.fechaSistema}</b></div></header>
        <nav>${menu.map(([r, t]) => `<a href="#${r}"${r === activo ? ' class="activo"' : ""}>${t}</a>`).join("")}</nav>
        <main><h1>${esc(titulo)}</h1>${mensaje}${contenido}</main>
        <footer>Proyecto Final INF514 · Jose Gregorio Molina Ozuna · Prof. Julio Castro Ortiz</footer>`;
}

/* ---------- Pantallas ---------- */

function pantallaInicio() {
    const disponibles = sistema.articulos.filter(a => a.disponible).length;
    const abiertas = sistema.rentas.filter(r => !r.estaDevuelta);
    const atrasadas = abiertas.filter(r => sistema.fechaSistema > r.fechaVencimiento).length;
    return pagina("Inicio", "/inicio", `
        <div class="tarjetas">
            ${tarjeta(sistema.clientes.length, "Clientes")}${tarjeta(sistema.articulos.length, "Artículos")}
            ${tarjeta(disponibles, "Disponibles")}${tarjeta(abiertas.length, "Rentados ahora")}${tarjeta(atrasadas, "Con atraso")}
        </div>
        <div class="panel"><h2>¿Qué desea hacer?</h2><div class="accesos">
            <a class="btn" href="#/rentar">Rentar un artículo</a><a class="btn" href="#/devolver">Devolver un artículo</a>
            <a class="btn sec" href="#/clientes">Clientes</a><a class="btn sec" href="#/articulos">Artículos</a>
            <a class="btn sec" href="#/premios">Revisión de premios</a><a class="btn sec" href="#/sistema">Fecha y datos de demostración</a>
        </div></div>
        <div class="panel"><h2>Reglas del negocio</h2><ul>
            <li>Al rentar se muestran los datos del cliente a partir de su código.</li>
            <li>Si devuelve con atraso se anota la cuota; debe pagarla en su próxima renta.</li>
            <li>Revisión mensual: más de RD$1,000 → carta y cupones de renta gratis.</li>
            <li>Revisión anual: más de RD$10,000 → carta y cupones; más de RD$20,000 → certificado de video gratis.</li>
        </ul></div>`);
}

function pantallaClientes() {
    const filas = sistema.clientes.map(c => `<tr><td>${esc(c.codigo)}</td><td>${esc(c.nombre)}</td><td>${esc(c.cedula)}</td>
        <td>${esc(c.telefono)}</td><td>${c.tieneDeuda ? `<span class="pill rojo">${dinero(c.deudaAtraso)}</span>` : "—"}</td>
        <td><a href="#/cliente?codigo=${q(c.codigo)}">Ver ficha</a></td></tr>`).join("");
    return pagina("Clientes", "/clientes", `
        <div class="panel"><h2>Registrar cliente</h2><form data-accion="registrarCliente" class="fila">
            ${campo("codigo", "Código de la empresa", "C007")}${campo("cedula", "Cédula", "001-0000000-0")}
            ${campo("nombre", "Nombre", "Nombre y apellido")}${campo("telefono", "Teléfono", "809-000-0000")}
            ${campo("direccion", "Dirección", "Calle, ciudad")}<button class="btn">Guardar cliente</button></form></div>
        <div class="panel"><h2>Clientes registrados</h2><table><tr><th>Código</th><th>Nombre</th><th>Cédula</th><th>Teléfono</th><th>Cuota pendiente</th><th></th></tr>
        ${filas || '<tr><td colspan="6">No hay clientes registrados.</td></tr>'}</table></div>`);
}

function pantallaCliente(params) {
    const codigo = (params.codigo || "").trim();
    const c = codigo ? sistema.buscarCliente(codigo) : null;
    let h = `<div class="panel"><h2>Consultar cliente por código</h2><form data-accion="buscarCliente" class="fila">
        ${campo("codigo", "Código del cliente", "C001", codigo)}<button class="btn">Buscar</button></form></div>`;
    if (codigo && !c) {
        h += `<div class="msg error">No existe un cliente con el código ${esc(codigo)}.</div>`;
    } else if (c) {
        h += fichaCliente(c);
        const ultimas = sistema.rentas.filter(r => r.codigoCliente === c.codigo).slice(-10).reverse();
        h += `<div class="panel"><h2>Últimas rentas</h2><table><tr><th>#</th><th>Artículo</th><th>Rentado</th><th>Vence</th><th>Devuelto</th><th>Monto</th><th>Atraso</th></tr>
            ${ultimas.map(r => {
                const a = sistema.buscarArticulo(r.codigoArticulo);
                return `<tr><td>${r.id}</td><td>${esc(a ? a.titulo : r.codigoArticulo)}</td><td>${r.fechaRenta}</td><td>${r.fechaVencimiento}</td>
                    <td>${r.estaDevuelta ? r.fechaDevolucion : '<span class="pill amarillo">Pendiente</span>'}</td><td>${dinero(r.monto)}</td>
                    <td>${r.cuotaAtraso > 0 ? dinero(r.cuotaAtraso) : "—"}</td></tr>`;
            }).join("") || '<tr><td colspan="7">Sin rentas.</td></tr>'}</table></div>`;
    }
    return pagina("Cliente", "/clientes", h);
}

function pantallaArticulos() {
    const filas = sistema.articulos.map(a => `<tr><td><img class="portada" src="${esc(a.imagenMostrar)}" data-respaldo="${esc(Articulo.ilustracion(a.claveTipo))}" alt="Portada de ${esc(a.titulo)}" loading="lazy"></td><td>${esc(a.codigo)}</td><td>${esc(a.tipo)}</td><td>${esc(a.titulo)}</td>
        <td>${dinero(a.precioRenta)}</td><td>${a.diasRenta}</td><td>${dinero(a.tarifaAtrasoDiaria)}</td>
        <td>${a.disponible ? '<span class="pill verde">Disponible</span>' : '<span class="pill amarillo">Rentado</span>'}</td></tr>`).join("");
    return pagina("Artículos", "/articulos", `
        <div class="panel"><h2>Registrar artículo</h2><form data-accion="registrarArticulo" class="fila">
            <label>Tipo<select name="tipo"><option value="VIDEO">Video</option><option value="VIDEOJUEGO">Video juego</option><option value="CINTA">Cinta de video juego</option></select></label>
            ${campo("codigo", "Código", "V010")}${campo("titulo", "Título", "Título del artículo")}
            <label>Precio de renta (RD$)<input name="precio" type="number" step="0.01" min="1" placeholder="150" required></label>
            <label>Imagen (opcional: dirección https://… o img/archivo.jpg)<input name="imagen" type="text" placeholder="https://…/portada.jpg"></label>
            <button class="btn">Guardar artículo</button></form></div>
        <div class="panel"><h2>Poner o cambiar la imagen de un artículo</h2><form data-accion="imagenArticulo" class="fila">
            ${campo("codigo", "Código del artículo", "V001")}
            <label>Link de la imagen (https://… o img/archivo.jpg; vacío = portada automática)<input name="imagen" type="text" placeholder="https://…/portada.jpg"></label>
            <button class="btn">Guardar imagen</button></form></div>
        <div class="panel"><h2>Inventario</h2><table><tr><th>Imagen</th><th>Código</th><th>Tipo</th><th>Título</th><th>Precio</th><th>Días</th><th>Atraso/día</th><th>Estado</th></tr>
        ${filas || '<tr><td colspan="8">No hay artículos registrados.</td></tr>'}</table></div>`);
}

/** Catálogo con buscador y filtros (se filtra en la propia página, sin recargar). */
function pantallaCatalogo() {
    return pagina("Catálogo", "/catalogo", `
        <div class="panel"><input id="buscar" type="search" placeholder="Buscar título…" autocomplete="off">
        <div class="filtros"><button class="btn sec peq activo" data-filtro="">Todos</button>
        <button class="btn sec peq" data-filtro="Película">Películas</button>
        <button class="btn sec peq" data-filtro="Videojuego">Videojuegos</button></div></div>
        ${catalogo("")}`);
}

/** Muestra u oculta las tarjetas según el texto buscado y la categoría elegida. */
let filtroCategoria = "";
function aplicarFiltro() {
    const texto = (document.getElementById("buscar")?.value || "").trim().toLowerCase();
    document.querySelectorAll(".catalogo .item").forEach(t => {
        const coincide = t.dataset.titulo.includes(texto) && (!filtroCategoria || t.dataset.cat === filtroCategoria);
        t.classList.toggle("oculto", !coincide);
    });
}
/** Si la imagen de un link no carga, se muestra la portada automática en su lugar. */
document.addEventListener("error", (e) => {
    const img = e.target;
    if (img.tagName === "IMG" && img.dataset.respaldo && img.src !== img.dataset.respaldo) {
        img.src = img.dataset.respaldo;
    }
}, true);
document.addEventListener("input", (e) => { if (e.target.id === "buscar") { aplicarFiltro(); } });
document.addEventListener("click", (e) => {
    const b = e.target.closest("[data-filtro]");
    if (b) {
        filtroCategoria = b.dataset.filtro;
        document.querySelectorAll("[data-filtro]").forEach(x => x.classList.toggle("activo", x === b));
        aplicarFiltro();
    }
});

/** Renta en dos pasos: 1) código del cliente (se muestran sus datos)  2) elegir el artículo. */
/** Catálogo visual: tarjetas con imagen, título, precio y estado. Si hay cliente sin deuda, cada tarjeta permite elegirla. */
function catalogo(codigoCliente) {
    const tarjetas = sistema.articulos.map(a => `<div class="item${a.disponible ? "" : " ocupada"}" data-titulo="${esc(a.titulo.toLowerCase())}" data-cat="${esc(a.categoria)}">
        <img src="${esc(a.portada)}" data-respaldo="${esc(a.portadaAuto)}" alt="Portada de ${esc(a.titulo)}" loading="lazy">
        <strong>${esc(a.titulo)}</strong>
        <small>${esc(a.categoria)} · ${esc(a.codigo)}</small>
        <span>${dinero(a.precioRenta)} · ${a.diasRenta} días</span>
        ${a.disponible
            ? (codigoCliente
                ? `<form data-accion="rentar"><input type="hidden" name="cliente" value="${esc(codigoCliente)}"><input type="hidden" name="articulo" value="${esc(a.codigo)}"><button class="btn peq">Elegir este</button></form>`
                : '<span class="pill verde">Disponible</span>')
            : '<span class="pill amarillo">Rentado</span>'}
    </div>`).join("");
    return `<div class="panel"><h2>Catálogo de artículos</h2><div class="catalogo">${tarjetas || "No hay artículos registrados."}</div></div>`;
}

function pantallaRentar(params) {
    const codigo = (params.cliente || "").trim();
    const c = codigo ? sistema.buscarCliente(codigo) : null;
    let h = `<div class="panel"><h2>Paso 1: código del cliente</h2><form data-accion="buscarRentar" class="fila">
        ${campo("cliente", "Código del cliente", "C001", codigo)}<button class="btn">Mostrar datos</button></form></div>`;
    if (codigo && !c) {
        h += `<div class="msg error">No existe un cliente con el código ${esc(codigo)}.</div>`;
    } else if (c) {
        h += fichaCliente(c);
        if (c.tieneDeuda) {
            // Regla del negocio: la cuota por atraso se paga al rentar otro artículo
            h += `<div class="panel aviso"><h2>Cuota por atraso pendiente: ${dinero(c.deudaAtraso)}</h2>
                <p>El cliente debe pagar esta cuota antes de rentar otro artículo.</p>
                <form data-accion="pagar"><input type="hidden" name="cliente" value="${esc(c.codigo)}">
                <button class="btn">Cobrar cuota de ${dinero(c.deudaAtraso)}</button></form></div>`;
        } else {
            const opciones = sistema.articulos.filter(a => a.disponible).map(a =>
                `<option value="${esc(a.codigo)}">${esc(a.codigo)} – ${esc(a.titulo)} (${esc(a.tipo)}, ${dinero(a.precioRenta)}, ${a.diasRenta} días)</option>`).join("");
            h += `<div class="panel"><h2>Paso 2: elegir el artículo</h2><form data-accion="rentar" class="fila">
                <input type="hidden" name="cliente" value="${esc(c.codigo)}">
                <label>Artículo disponible<select name="articulo">${opciones}</select></label>
                <button class="btn">Rentar</button></form></div>`;
            h += catalogo(c.codigo);
        }
    }
    if (!c) {
        h += catalogo("");
    }
    return pagina("Rentar", "/rentar", h);
}

function pantallaDevolver() {
    const abiertas = sistema.rentas.filter(r => !r.estaDevuelta);
    const filas = abiertas.map(r => {
        const a = sistema.buscarArticulo(r.codigoArticulo);
        const c = sistema.buscarCliente(r.codigoCliente);
        const dias = Fecha.diferencia(r.fechaVencimiento, sistema.fechaSistema);
        return `<tr><td>#${r.id}</td><td>${esc(a.titulo)} <small>(${esc(a.codigo)})</small></td><td>${esc(c.nombre)}</td>
            <td>${r.fechaRenta}</td><td>${r.fechaVencimiento}</td>
            <td>${dias > 0 ? `<span class="pill rojo">${dias} día(s) de atraso</span>` : '<span class="pill verde">A tiempo</span>'}</td>
            <td><form data-accion="devolver"><input type="hidden" name="articulo" value="${esc(a.codigo)}"><button class="btn peq">Devolver</button></form></td></tr>`;
    }).join("");
    return pagina("Devolver", "/devolver", `<div class="panel"><h2>Artículos rentados actualmente</h2>
        <table><tr><th>Renta</th><th>Artículo</th><th>Cliente</th><th>Rentado</th><th>Vence</th><th>Estado</th><th></th></tr>
        ${filas || '<tr><td colspan="8">No hay artículos rentados en este momento.</td></tr>'}</table>
        <p class="nota">Fecha del sistema: <b>${sistema.fechaSistema}</b>. El atraso se calcula contra esta fecha.</p></div>`);
}

function pantallaPremios(params) {
    const hoy = sistema.fechaSistema;
    let h = `<div class="dos"><div class="panel"><h2>Revisión mensual</h2><p class="nota">Premio: más de RD$1,000 rentados en el mes.</p>
        <form data-accion="revisar" class="fila"><input type="hidden" name="tipo" value="mensual">
        <label>Año<input name="anio" type="number" value="${hoy.slice(0, 4)}" required></label>
        <label>Mes<input name="mes" type="number" min="1" max="12" value="${Number(hoy.slice(5, 7))}" required></label>
        <button class="btn">Revisar mes</button></form></div>
        <div class="panel"><h2>Revisión anual</h2><p class="nota">Premio: más de RD$10,000 en el año; certificado si pasa de RD$20,000.</p>
        <form data-accion="revisar" class="fila"><input type="hidden" name="tipo" value="anual">
        <label>Año<input name="anio" type="number" value="${hoy.slice(0, 4)}" required></label>
        <button class="btn">Revisar año</button></form></div></div>`;
    if (params.tipo === "mensual" || params.tipo === "anual") {
        const anual = params.tipo === "anual";
        const anio = Number(params.anio);
        const mes = Number(params.mes);
        if (!Number.isInteger(anio) || anio < 1900 || (!anual && (!Number.isInteger(mes) || mes < 1 || mes > 12))) {
            h += '<div class="msg error">Año o mes no válido.</div>';
        } else {
            const periodo = anual ? String(anio) : `${String(mes).padStart(2, "0")}/${anio}`;
            const lista = anual ? sistema.revisionAnual(anio) : sistema.revisionMensual(anio, mes);
            h += `<div class="panel"><h2>Resultado de la revisión ${anual ? "anual" : "mensual"} ${esc(periodo)}</h2>`;
            if (!lista.length) {
                h += "<p>Ningún cliente superó el nivel de premio en este período.</p>";
            } else {
                const base = `#/premios?tipo=${params.tipo}&anio=${anio}${anual ? "" : "&mes=" + mes}`;
                h += `<table><tr><th>Cliente</th><th>Total rentado</th><th>Cupones</th><th>Certificado</th><th>Carta</th></tr>
                    ${lista.map(p => `<tr><td>${esc(p.cliente.nombre)} <small>(${esc(p.cliente.codigo)})</small></td><td>${dinero(p.totalRentado)}</td>
                    <td>${p.cupones}</td><td>${p.tieneCertificado ? '<span class="pill verde">Video gratis</span>' : "—"}</td>
                    <td><a href="${base}&carta=${q(p.cliente.codigo)}">Ver carta</a></td></tr>`).join("")}</table>`;
                const elegido = lista.find(p => p.cliente.codigo === params.carta);
                if (elegido) {
                    const texto = sistema.redactarCarta(elegido, periodo, anual);
                    const archivo = `carta_${anual ? "anual" : "mensual"}_${periodo.replace("/", "-")}_${elegido.cliente.codigo}.txt`;
                    h += `<h2 style="margin-top:18px">Carta para ${esc(elegido.cliente.nombre)}</h2><pre class="carta">${esc(texto)}</pre>
                        <a class="btn sec" download="${esc(archivo)}" href="data:text/plain;charset=utf-8,${encodeURIComponent(texto)}">Descargar carta (.txt)</a>`;
                }
            }
            h += "</div>";
        }
    }
    return pagina("Premios", "/premios", h);
}

function pantallaSistema() {
    return pagina("Fecha y datos", "/sistema", `
        <div class="dos"><div class="panel"><h2>Fecha del sistema</h2>
            <p class="nota">Permite simular el paso del tiempo (por ejemplo, para probar un atraso). Fecha actual: <b>${sistema.fechaSistema}</b></p>
            <form data-accion="fecha" class="fila"><label>Nueva fecha<input type="date" name="fecha" value="${sistema.fechaSistema}" required></label>
            <button class="btn">Cambiar fecha</button></form></div>
        <div class="panel"><h2>Datos de demostración</h2>
            <p class="nota">Carga 5 clientes, 14 artículos y un historial de rentas del año de la fecha del sistema, para probar las revisiones.</p>
            <form data-accion="demo"><button class="btn">Cargar datos de demostración</button></form></div></div>
        <div class="panel"><h2>Borrar todos los datos</h2>
            <p class="nota">Elimina clientes, artículos y rentas guardados en este navegador.</p>
            <form data-accion="borrar"><button class="btn sec">Borrar todo</button></form></div>`);
}

/* ---------- Acciones (formularios) ---------- */

/** El ';' y los espacios sobrantes se limpian de lo que escribe el usuario. */
const limpiar = (texto) => String(texto ?? "").replace(/;/g, ",").trim();

const acciones = {
    registrarCliente(d) {
        const codigo = limpiar(d.codigo).toUpperCase();
        const cedula = limpiar(d.cedula), nombre = limpiar(d.nombre), telefono = limpiar(d.telefono), direccion = limpiar(d.direccion);
        if (!codigo || !cedula || !nombre || !telefono || !direccion) {
            avisar("Todos los campos son obligatorios.", false);
        } else if (sistema.registrarCliente(new Cliente(cedula, nombre, telefono, direccion, codigo))) {
            guardar();
            avisar(`Cliente ${codigo} registrado correctamente.`, true);
        } else {
            avisar(`Ya existe un cliente con el código ${codigo}.`, false);
        }
        ir("#/clientes");
    },

    registrarArticulo(d) {
        const codigo = limpiar(d.codigo).toUpperCase(), titulo = limpiar(d.titulo), precio = Number(d.precio);
        if (!codigo || !titulo || !(precio > 0)) {
            avisar("Complete todos los campos; el precio debe ser mayor que cero.", false);
        } else if (sistema.registrarArticulo(Articulo.crear(d.tipo, codigo, titulo, precio, true, (d.imagen || "").trim()))) {
            guardar();
            avisar(`Artículo ${codigo} registrado correctamente.`, true);
        } else {
            avisar(`Ya existe un artículo con el código ${codigo}.`, false);
        }
        ir("#/articulos");
    },

    imagenArticulo(d) {
        const art = sistema.buscarArticulo(limpiar(d.codigo));
        const url = (d.imagen || "").trim();
        if (!art) {
            avisar("No existe un artículo con ese código.", false);
        } else if (url && !Articulo.imagenValida(url)) {
            avisar("El link debe empezar con https:// o con img/ y no tener espacios.", false);
        } else {
            art.imagen = url;
            guardar();
            avisar(url ? `Imagen de ${art.codigo} guardada.` : `${art.codigo} vuelve a usar la portada automática.`, true);
        }
        ir("#/articulos");
    },

    buscarCliente(d) { ir(`#/cliente?codigo=${q(limpiar(d.codigo))}`); },
    buscarRentar(d) { ir(`#/rentar?cliente=${q(limpiar(d.cliente))}`); },

    rentar(d) {
        try {
            const r = sistema.rentar(d.cliente, d.articulo);
            const a = sistema.buscarArticulo(r.codigoArticulo);
            guardar();
            avisar(`Renta #${r.id} registrada: ${a.titulo} por ${dinero(r.monto)}. Debe devolverlo el ${r.fechaVencimiento}.`, true);
        } catch (e) {
            avisar(e.message, false);
        }
        ir(`#/rentar?cliente=${q(d.cliente)}`);
    },

    pagar(d) {
        const c = sistema.buscarCliente(d.cliente);
        if (!c) {
            avisar(`No existe un cliente con el código ${d.cliente}.`, false);
            return ir("#/rentar");
        }
        const pagado = c.pagarDeuda();
        guardar();
        avisar(`Pago recibido: ${dinero(pagado)}. Cuota saldada; ya puede rentar.`, true);
        ir(`#/rentar?cliente=${q(d.cliente)}`);
    },

    devolver(d) {
        try {
            const r = sistema.devolver(d.articulo);
            const a = sistema.buscarArticulo(d.articulo);
            const c = sistema.buscarCliente(r.codigoCliente);
            guardar();
            if (r.cuotaAtraso > 0) {
                avisar(`${a.titulo} devuelto por ${c.nombre}. NOTA: ${r.nota}. Cuota por atraso registrada: ${dinero(r.cuotaAtraso)} (la pagará en su próxima renta).`, false);
            } else {
                avisar(`${a.titulo} devuelto a tiempo por ${c.nombre}. Sin cuota por atraso.`, true);
            }
        } catch (e) {
            avisar(e.message, false);
        }
        ir("#/devolver");
    },

    revisar(d) {
        const extra = d.tipo === "mensual" ? `&mes=${q(d.mes)}` : "";
        ir(`#/premios?tipo=${d.tipo}&anio=${q(d.anio)}${extra}`);
    },

    fecha(d) {
        if (Fecha.valida(d.fecha)) {
            sistema.fechaSistema = d.fecha;
            guardar();
            avisar(`Fecha del sistema cambiada a ${d.fecha}.`, true);
        } else {
            avisar("Fecha no válida.", false);
        }
        ir("#/sistema");
    },

    demo() {
        if (sistema.clientes.length) {
            avisar("Los datos de demostración solo se cargan cuando el sistema está vacío.", false);
        } else {
            sistema.cargarDemostracion();
            guardar();
            avisar(`Datos de demostración cargados para el año ${sistema.fechaSistema.slice(0, 4)}.`, true);
        }
        ir("#/sistema");
    },

    borrar() {
        if (window.confirm("¿Seguro que desea borrar todos los datos?")) {
            sistema.vaciar();
            almacen.borrar();
            avisar("Todos los datos fueron borrados.", true);
        }
        ir("#/sistema");
    }
};

/* ---------- Arranque ---------- */

const pantallas = {
    "/inicio": pantallaInicio, "/clientes": pantallaClientes, "/cliente": pantallaCliente,
    "/articulos": pantallaArticulos, "/catalogo": pantallaCatalogo, "/rentar": pantallaRentar, "/devolver": pantallaDevolver,
    "/premios": pantallaPremios, "/sistema": pantallaSistema
};

function dibujar() {
    const { ruta, params } = leerRuta();
    const pantalla = pantallas[ruta] || pantallaInicio;
    document.getElementById("app").innerHTML = pantalla(params);
    window.scrollTo(0, 0);
}

document.addEventListener("submit", (evento) => {
    const accion = evento.target.dataset.accion;
    if (accion && acciones[accion]) {
        evento.preventDefault();
        acciones[accion](Object.fromEntries(new FormData(evento.target)));
    }
});

window.addEventListener("hashchange", dibujar);
dibujar();
