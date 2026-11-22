// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Versión WEB del sistema de Film Magic.
 *
 * Usa el servidor HTTP incluido en el JDK (com.sun.net.httpserver), por lo que no
 * necesita librerías externas. Reutiliza las clases de negocio (Cliente, Articulo,
 * Renta, FilmMagic, Almacen); esta clase solo se encarga de recibir las peticiones
 * del navegador y de generar las páginas HTML.
 *
 * Uso: java -jar FilmMagic.jar [puerto]   (por defecto 8080) y abrir http://localhost:8080
 */
public class ServidorWeb {

    private static final FilmMagic sistema = new FilmMagic();
    private static final Almacen almacen = new Almacen("datos");
    private static final Object CERROJO = new Object(); // evita que dos peticiones modifiquen los datos a la vez

    /** Cada ruta de la aplicación se implementa con un manejador de este tipo. */
    private interface Manejador {
        void atender(HttpExchange ex) throws Exception;
    }

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        try {
            almacen.cargar(sistema);
        } catch (IOException | RuntimeException e) {
            System.out.println("Aviso: no se pudieron cargar los datos guardados (" + e.getMessage() + ").");
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        ruta(servidor, "/", ServidorWeb::inicio);
        ruta(servidor, "/clientes", ServidorWeb::clientes);
        ruta(servidor, "/cliente", ServidorWeb::consultarCliente);
        ruta(servidor, "/articulos", ServidorWeb::articulos);
        ruta(servidor, "/rentar", ServidorWeb::rentar);
        ruta(servidor, "/pagar", ServidorWeb::pagar);
        ruta(servidor, "/devolver", ServidorWeb::devolver);
        ruta(servidor, "/premios", ServidorWeb::premios);
        ruta(servidor, "/carta", ServidorWeb::carta);
        ruta(servidor, "/sistema", ServidorWeb::sistemaAdmin);
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Film Magic (web) en marcha: abra http://localhost:" + puerto + " en el navegador.");
        System.out.println("Presione Ctrl+C para detener el servidor.");
    }

    /** Registra una ruta; envuelve el manejador para sincronizar y atender errores. */
    private static void ruta(HttpServer servidor, String camino, Manejador m) {
        servidor.createContext(camino, ex -> {
            try {
                synchronized (CERROJO) {
                    if (camino.equals("/") && !ex.getRequestURI().getPath().equals("/")) {
                        enviar(ex, 404, pagina("No encontrado", "", null, "<p>La página no existe.</p>"));
                    } else {
                        m.atender(ex);
                    }
                }
            } catch (Exception e) {
                try {
                    enviar(ex, 500, pagina("Error", "", null, "<div class='msg error'>Error interno: " + esc(String.valueOf(e.getMessage())) + "</div>"));
                } catch (IOException ignorado) {
                    // la conexión ya se cerró
                }
            } finally {
                ex.close();
            }
        });
    }

    // =====================================================================
    //  PÁGINAS
    // =====================================================================

    private static void inicio(HttpExchange ex) throws IOException {
        int disponibles = 0;
        for (Articulo a : sistema.getArticulos()) {
            if (a.isDisponible()) {
                disponibles++;
            }
        }
        int abiertas = 0;
        int atrasadas = 0;
        for (Renta r : sistema.getRentas()) {
            if (!r.estaDevuelta()) {
                abiertas++;
                if (sistema.getFechaSistema().isAfter(r.getFechaVencimiento())) {
                    atrasadas++;
                }
            }
        }
        StringBuilder h = new StringBuilder();
        h.append("<div class='tarjetas'>");
        h.append(tarjeta(String.valueOf(sistema.getClientes().size()), "Clientes"));
        h.append(tarjeta(String.valueOf(sistema.getArticulos().size()), "Artículos"));
        h.append(tarjeta(String.valueOf(disponibles), "Disponibles"));
        h.append(tarjeta(String.valueOf(abiertas), "Rentados ahora"));
        h.append(tarjeta(String.valueOf(atrasadas), "Con atraso"));
        h.append("</div>");
        h.append("<div class='panel'><h2>¿Qué desea hacer?</h2><div class='accesos'>");
        h.append("<a class='btn' href='/rentar'>Rentar un artículo</a>");
        h.append("<a class='btn' href='/devolver'>Devolver un artículo</a>");
        h.append("<a class='btn sec' href='/clientes'>Clientes</a>");
        h.append("<a class='btn sec' href='/articulos'>Artículos</a>");
        h.append("<a class='btn sec' href='/premios'>Revisión de premios</a>");
        h.append("<a class='btn sec' href='/sistema'>Fecha y datos de demostración</a>");
        h.append("</div></div>");
        h.append("<div class='panel'><h2>Reglas del negocio</h2><ul>");
        h.append("<li>Al rentar se muestran los datos del cliente a partir de su código.</li>");
        h.append("<li>Si devuelve con atraso se anota la cuota; debe pagarla en su próxima renta.</li>");
        h.append("<li>Revisión mensual: más de RD$1,000 → carta y cupones de renta gratis.</li>");
        h.append("<li>Revisión anual: más de RD$10,000 → carta y cupones; más de RD$20,000 → certificado de video gratis.</li>");
        h.append("</ul></div>");
        enviar(ex, 200, pagina("Inicio", "/", mensaje(ex), h.toString()));
    }

    private static void clientes(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            Map<String, String> p = formulario(ex);
            String codigo = p.getOrDefault("codigo", "").trim().toUpperCase();
            String cedula = p.getOrDefault("cedula", "").trim();
            String nombre = p.getOrDefault("nombre", "").trim();
            String telefono = p.getOrDefault("telefono", "").trim();
            String direccion = p.getOrDefault("direccion", "").trim();
            if (codigo.isEmpty() || cedula.isEmpty() || nombre.isEmpty() || telefono.isEmpty() || direccion.isEmpty()) {
                redirigir(ex, "/clientes", "Todos los campos son obligatorios.", false);
            } else if (sistema.registrarCliente(new Cliente(limpiar(cedula), limpiar(nombre), limpiar(telefono),
                    limpiar(direccion), limpiar(codigo)))) {
                guardar();
                redirigir(ex, "/clientes", "Cliente " + codigo + " registrado correctamente.", true);
            } else {
                redirigir(ex, "/clientes", "Ya existe un cliente con el código " + codigo + ".", false);
            }
            return;
        }
        StringBuilder h = new StringBuilder();
        h.append("<div class='panel'><h2>Registrar cliente</h2><form method='post' action='/clientes' class='fila'>");
        h.append(campo("codigo", "Código de la empresa", "C007"));
        h.append(campo("cedula", "Cédula", "001-0000000-0"));
        h.append(campo("nombre", "Nombre", "Nombre y apellido"));
        h.append(campo("telefono", "Teléfono", "809-000-0000"));
        h.append(campo("direccion", "Dirección", "Calle, ciudad"));
        h.append("<button class='btn'>Guardar cliente</button></form></div>");
        h.append("<div class='panel'><h2>Clientes registrados</h2><table><tr><th>Código</th><th>Nombre</th><th>Cédula</th><th>Teléfono</th><th>Cuota pendiente</th><th></th></tr>");
        for (Cliente c : sistema.getClientes()) {
            h.append("<tr><td>").append(esc(c.getCodigo())).append("</td><td>").append(esc(c.getNombre()))
                    .append("</td><td>").append(esc(c.getCedula())).append("</td><td>").append(esc(c.getTelefono()))
                    .append("</td><td>").append(c.tieneDeuda() ? "<span class='pill rojo'>" + dinero(c.getDeudaAtraso()) + "</span>" : "—")
                    .append("</td><td><a href='/cliente?codigo=").append(url(c.getCodigo())).append("'>Ver ficha</a></td></tr>");
        }
        h.append("</table></div>");
        enviar(ex, 200, pagina("Clientes", "/clientes", mensaje(ex), h.toString()));
    }

    private static void consultarCliente(HttpExchange ex) throws IOException {
        String codigo = consulta(ex).getOrDefault("codigo", "");
        Cliente c = codigo.isEmpty() ? null : sistema.buscarCliente(codigo);
        StringBuilder h = new StringBuilder();
        h.append("<div class='panel'><h2>Consultar cliente por código</h2><form method='get' action='/cliente' class='fila'>")
                .append(campo("codigo", "Código del cliente", "C001")).append("<button class='btn'>Buscar</button></form></div>");
        if (!codigo.isEmpty() && c == null) {
            h.append("<div class='msg error'>No existe un cliente con el código ").append(esc(codigo)).append(".</div>");
        } else if (c != null) {
            h.append(fichaCliente(c));
            h.append("<div class='panel'><h2>Últimas rentas</h2><table><tr><th>#</th><th>Artículo</th><th>Rentado</th><th>Vence</th><th>Devuelto</th><th>Monto</th><th>Atraso</th></tr>");
            List<Renta> todas = sistema.getRentas();
            int mostradas = 0;
            for (int i = todas.size() - 1; i >= 0 && mostradas < 10; i--) {
                Renta r = todas.get(i);
                if (r.getCodigoCliente().equals(c.getCodigo())) {
                    Articulo a = sistema.buscarArticulo(r.getCodigoArticulo());
                    h.append("<tr><td>").append(r.getId()).append("</td><td>").append(esc(a == null ? r.getCodigoArticulo() : a.getTitulo()))
                            .append("</td><td>").append(r.getFechaRenta()).append("</td><td>").append(r.getFechaVencimiento())
                            .append("</td><td>").append(r.estaDevuelta() ? r.getFechaDevolucion().toString() : "<span class='pill amarillo'>Pendiente</span>")
                            .append("</td><td>").append(dinero(r.getMonto())).append("</td><td>")
                            .append(r.getCuotaAtraso() > 0 ? dinero(r.getCuotaAtraso()) : "—").append("</td></tr>");
                    mostradas++;
                }
            }
            h.append("</table></div>");
        }
        enviar(ex, 200, pagina("Cliente", "/clientes", mensaje(ex), h.toString()));
    }

    private static void articulos(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            Map<String, String> p = formulario(ex);
            String codigo = limpiar(p.getOrDefault("codigo", "").trim().toUpperCase());
            String titulo = limpiar(p.getOrDefault("titulo", "").trim());
            double precio;
            try {
                precio = Double.parseDouble(p.getOrDefault("precio", "").trim());
            } catch (NumberFormatException e) {
                precio = -1;
            }
            if (codigo.isEmpty() || titulo.isEmpty() || precio <= 0) {
                redirigir(ex, "/articulos", "Complete todos los campos; el precio debe ser mayor que cero.", false);
            } else if (sistema.registrarArticulo(Articulo.crear(p.getOrDefault("tipo", "VIDEO"), codigo, titulo, precio))) {
                guardar();
                redirigir(ex, "/articulos", "Artículo " + codigo + " registrado correctamente.", true);
            } else {
                redirigir(ex, "/articulos", "Ya existe un artículo con el código " + codigo + ".", false);
            }
            return;
        }
        StringBuilder h = new StringBuilder();
        h.append("<div class='panel'><h2>Registrar artículo</h2><form method='post' action='/articulos' class='fila'>");
        h.append("<label>Tipo<select name='tipo'><option value='VIDEO'>Video</option><option value='VIDEOJUEGO'>Video juego</option><option value='CINTA'>Cinta de video juego</option></select></label>");
        h.append(campo("codigo", "Código", "V010"));
        h.append(campo("titulo", "Título", "Título del artículo"));
        h.append("<label>Precio de renta (RD$)<input name='precio' type='number' step='0.01' min='1' placeholder='150' required></label>");
        h.append("<button class='btn'>Guardar artículo</button></form></div>");
        h.append("<div class='panel'><h2>Inventario</h2><table><tr><th>Código</th><th>Tipo</th><th>Título</th><th>Precio</th><th>Días</th><th>Atraso/día</th><th>Estado</th></tr>");
        for (Articulo a : sistema.getArticulos()) {
            h.append("<tr><td>").append(esc(a.getCodigo())).append("</td><td>").append(esc(a.getTipo())).append("</td><td>")
                    .append(esc(a.getTitulo())).append("</td><td>").append(dinero(a.getPrecioRenta())).append("</td><td>")
                    .append(a.getDiasRenta()).append("</td><td>").append(dinero(a.getTarifaAtrasoDiaria())).append("</td><td>")
                    .append(a.isDisponible() ? "<span class='pill verde'>Disponible</span>" : "<span class='pill amarillo'>Rentado</span>")
                    .append("</td></tr>");
        }
        h.append("</table></div>");
        enviar(ex, 200, pagina("Artículos", "/articulos", mensaje(ex), h.toString()));
    }

    /** Renta en dos pasos: 1) código del cliente (se muestran sus datos)  2) elegir el artículo. */
    private static void rentar(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            Map<String, String> p = formulario(ex);
            String cc = p.getOrDefault("cliente", "");
            try {
                Renta r = sistema.rentar(cc, p.getOrDefault("articulo", ""));
                Articulo a = sistema.buscarArticulo(r.getCodigoArticulo());
                guardar();
                redirigir(ex, "/rentar?cliente=" + url(cc), "Renta #" + r.getId() + " registrada: " + a.getTitulo() + " por "
                        + dinero(r.getMonto()) + ". Debe devolverlo el " + r.getFechaVencimiento() + ".", true);
            } catch (IllegalArgumentException | IllegalStateException e) {
                redirigir(ex, "/rentar?cliente=" + url(cc), e.getMessage(), false);
            }
            return;
        }
        String codigo = consulta(ex).getOrDefault("cliente", "").trim();
        Cliente c = codigo.isEmpty() ? null : sistema.buscarCliente(codigo);
        StringBuilder h = new StringBuilder();
        h.append("<div class='panel'><h2>Paso 1: código del cliente</h2><form method='get' action='/rentar' class='fila'>")
                .append(campo("cliente", "Código del cliente", "C001")).append("<button class='btn'>Mostrar datos</button></form></div>");
        if (!codigo.isEmpty() && c == null) {
            h.append("<div class='msg error'>No existe un cliente con el código ").append(esc(codigo)).append(".</div>");
        } else if (c != null) {
            h.append(fichaCliente(c));
            if (c.tieneDeuda()) {
                // Regla del negocio: la cuota por atraso se paga al rentar otro artículo
                h.append("<div class='panel aviso'><h2>Cuota por atraso pendiente: ").append(dinero(c.getDeudaAtraso())).append("</h2>")
                        .append("<p>El cliente debe pagar esta cuota antes de rentar otro artículo.</p>")
                        .append("<form method='post' action='/pagar'><input type='hidden' name='cliente' value='").append(esc(c.getCodigo()))
                        .append("'><button class='btn'>Cobrar cuota de ").append(dinero(c.getDeudaAtraso())).append("</button></form></div>");
            } else {
                h.append("<div class='panel'><h2>Paso 2: elegir el artículo</h2><form method='post' action='/rentar' class='fila'>")
                        .append("<input type='hidden' name='cliente' value='").append(esc(c.getCodigo())).append("'>")
                        .append("<label>Artículo disponible<select name='articulo'>");
                for (Articulo a : sistema.getArticulos()) {
                    if (a.isDisponible()) {
                        h.append("<option value='").append(esc(a.getCodigo())).append("'>").append(esc(a.getCodigo())).append(" – ")
                                .append(esc(a.getTitulo())).append(" (").append(esc(a.getTipo())).append(", ")
                                .append(dinero(a.getPrecioRenta())).append(", ").append(a.getDiasRenta()).append(" días)</option>");
                    }
                }
                h.append("</select></label><button class='btn'>Rentar</button></form></div>");
            }
        }
        enviar(ex, 200, pagina("Rentar", "/rentar", mensaje(ex), h.toString()));
    }

    private static void pagar(HttpExchange ex) throws IOException {
        Map<String, String> p = formulario(ex);
        String cc = p.getOrDefault("cliente", "");
        Cliente c = sistema.buscarCliente(cc);
        if (c == null) {
            redirigir(ex, "/rentar", "No existe un cliente con el código " + cc + ".", false);
            return;
        }
        double pagado = c.pagarDeuda();
        guardar();
        redirigir(ex, "/rentar?cliente=" + url(cc), "Pago recibido: " + dinero(pagado) + ". Cuota saldada; ya puede rentar.", true);
    }

    private static void devolver(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            String cod = formulario(ex).getOrDefault("articulo", "");
            try {
                Renta r = sistema.devolver(cod);
                Articulo a = sistema.buscarArticulo(cod);
                Cliente c = sistema.buscarCliente(r.getCodigoCliente());
                guardar();
                if (r.getCuotaAtraso() > 0) {
                    redirigir(ex, "/devolver", a.getTitulo() + " devuelto por " + c.getNombre() + ". NOTA: " + r.getNota()
                            + ". Cuota por atraso registrada: " + dinero(r.getCuotaAtraso()) + " (la pagará en su próxima renta).", false);
                } else {
                    redirigir(ex, "/devolver", a.getTitulo() + " devuelto a tiempo por " + c.getNombre() + ". Sin cuota por atraso.", true);
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                redirigir(ex, "/devolver", e.getMessage(), false);
            }
            return;
        }
        StringBuilder h = new StringBuilder();
        h.append("<div class='panel'><h2>Artículos rentados actualmente</h2>");
        h.append("<table><tr><th>Renta</th><th>Artículo</th><th>Cliente</th><th>Rentado</th><th>Vence</th><th>Estado</th><th></th></tr>");
        int n = 0;
        for (Renta r : sistema.getRentas()) {
            if (r.estaDevuelta()) {
                continue;
            }
            n++;
            Articulo a = sistema.buscarArticulo(r.getCodigoArticulo());
            Cliente c = sistema.buscarCliente(r.getCodigoCliente());
            long dias = ChronoUnit.DAYS.between(r.getFechaVencimiento(), sistema.getFechaSistema());
            h.append("<tr><td>#").append(r.getId()).append("</td><td>").append(esc(a.getTitulo())).append(" <small>(").append(esc(a.getCodigo()))
                    .append(")</small></td><td>").append(esc(c.getNombre())).append("</td><td>").append(r.getFechaRenta()).append("</td><td>")
                    .append(r.getFechaVencimiento()).append("</td><td>")
                    .append(dias > 0 ? "<span class='pill rojo'>" + dias + " día(s) de atraso</span>" : "<span class='pill verde'>A tiempo</span>")
                    .append("</td><td><form method='post' action='/devolver'><input type='hidden' name='articulo' value='")
                    .append(esc(a.getCodigo())).append("'><button class='btn peq'>Devolver</button></form></td></tr>");
        }
        if (n == 0) {
            h.append("<tr><td colspan='7'>No hay artículos rentados en este momento.</td></tr>");
        }
        h.append("</table><p class='nota'>Fecha del sistema: <b>").append(sistema.getFechaSistema()).append("</b>. El atraso se calcula contra esta fecha.</p></div>");
        enviar(ex, 200, pagina("Devolver", "/devolver", mensaje(ex), h.toString()));
    }

    private static void premios(HttpExchange ex) throws IOException {
        Map<String, String> q = consulta(ex);
        LocalDate hoy = sistema.getFechaSistema();
        StringBuilder h = new StringBuilder();
        h.append("<div class='dos'><div class='panel'><h2>Revisión mensual</h2><p class='nota'>Premio: más de RD$1,000 rentados en el mes.</p>")
                .append("<form method='get' action='/premios' class='fila'><input type='hidden' name='tipo' value='mensual'>")
                .append("<label>Año<input name='anio' type='number' value='").append(hoy.getYear()).append("' required></label>")
                .append("<label>Mes<input name='mes' type='number' min='1' max='12' value='").append(hoy.getMonthValue()).append("' required></label>")
                .append("<button class='btn'>Revisar mes</button></form></div>");
        h.append("<div class='panel'><h2>Revisión anual</h2><p class='nota'>Premio: más de RD$10,000 en el año; certificado si pasa de RD$20,000.</p>")
                .append("<form method='get' action='/premios' class='fila'><input type='hidden' name='tipo' value='anual'>")
                .append("<label>Año<input name='anio' type='number' value='").append(hoy.getYear()).append("' required></label>")
                .append("<button class='btn'>Revisar año</button></form></div></div>");
        String tipo = q.getOrDefault("tipo", "");
        if (tipo.equals("mensual") || tipo.equals("anual")) {
            try {
                boolean anual = tipo.equals("anual");
                int anio = Integer.parseInt(q.getOrDefault("anio", ""));
                int mes = anual ? 0 : Integer.parseInt(q.getOrDefault("mes", ""));
                if (!anual && (mes < 1 || mes > 12)) {
                    throw new NumberFormatException();
                }
                String periodo = anual ? String.valueOf(anio) : String.format("%02d/%d", mes, anio);
                List<Premio> lista = anual ? sistema.revisionAnual(anio) : sistema.revisionMensual(anio, mes);
                h.append("<div class='panel'><h2>Resultado de la revisión ").append(anual ? "anual " : "mensual ").append(esc(periodo)).append("</h2>");
                if (lista.isEmpty()) {
                    h.append("<p>Ningún cliente superó el nivel de premio en este período.</p>");
                } else {
                    h.append("<table><tr><th>Cliente</th><th>Total rentado</th><th>Cupones</th><th>Certificado</th><th>Carta</th></tr>");
                    for (Premio p : lista) {
                        Path carta = sistema.generarCarta(p, periodo, anual);
                        h.append("<tr><td>").append(esc(p.getCliente().getNombre())).append(" <small>(").append(esc(p.getCliente().getCodigo()))
                                .append(")</small></td><td>").append(dinero(p.getTotalRentado())).append("</td><td>").append(p.getCupones())
                                .append("</td><td>").append(p.tieneCertificado() ? "<span class='pill verde'>Video gratis</span>" : "—")
                                .append("</td><td><a href='/carta?archivo=").append(url(carta.getFileName().toString())).append("'>Ver carta</a></td></tr>");
                    }
                    h.append("</table>");
                }
                h.append("</div>");
            } catch (NumberFormatException e) {
                h.append("<div class='msg error'>Año o mes no válido.</div>");
            }
        }
        enviar(ex, 200, pagina("Premios", "/premios", mensaje(ex), h.toString()));
    }

    private static void carta(HttpExchange ex) throws IOException {
        String nombre = consulta(ex).getOrDefault("archivo", "");
        // seguridad: solo se permiten nombres simples de la carpeta "cartas"
        if (!nombre.matches("[A-Za-z0-9_\\-]+\\.txt")) {
            enviar(ex, 400, pagina("Carta", "/premios", null, "<div class='msg error'>Nombre de carta no válido.</div>"));
            return;
        }
        Path ruta = Paths.get("cartas").resolve(nombre);
        if (!Files.exists(ruta)) {
            enviar(ex, 404, pagina("Carta", "/premios", null, "<div class='msg error'>La carta no existe.</div>"));
            return;
        }
        String texto = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
        enviar(ex, 200, pagina("Carta", "/premios", null, "<div class='panel'><h2>" + esc(nombre) + "</h2><pre class='carta'>" + esc(texto)
                + "</pre><a class='btn sec' href='/premios'>Volver</a></div>"));
    }

    private static void sistemaAdmin(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            Map<String, String> p = formulario(ex);
            if (p.getOrDefault("accion", "").equals("fecha")) {
                try {
                    LocalDate f = LocalDate.parse(p.getOrDefault("fecha", ""));
                    sistema.setFechaSistema(f);
                    redirigir(ex, "/sistema", "Fecha del sistema cambiada a " + f + ".", true);
                } catch (DateTimeParseException e) {
                    redirigir(ex, "/sistema", "Fecha no válida.", false);
                }
            } else if (p.getOrDefault("accion", "").equals("demo")) {
                if (!sistema.getClientes().isEmpty()) {
                    redirigir(ex, "/sistema", "Los datos de demostración solo se cargan cuando el sistema está vacío.", false);
                } else {
                    sistema.cargarDemostracion();
                    guardar();
                    redirigir(ex, "/sistema", "Datos de demostración cargados para el año " + sistema.getFechaSistema().getYear() + ".", true);
                }
            }
            return;
        }
        StringBuilder h = new StringBuilder();
        h.append("<div class='dos'><div class='panel'><h2>Fecha del sistema</h2><p class='nota'>Permite simular el paso del tiempo (por ejemplo, para probar un atraso). Fecha actual: <b>")
                .append(sistema.getFechaSistema()).append("</b></p><form method='post' action='/sistema' class='fila'><input type='hidden' name='accion' value='fecha'>")
                .append("<label>Nueva fecha<input type='date' name='fecha' value='").append(sistema.getFechaSistema()).append("' required></label>")
                .append("<button class='btn'>Cambiar fecha</button></form></div>");
        h.append("<div class='panel'><h2>Datos de demostración</h2><p class='nota'>Carga 5 clientes, 8 artículos y un historial de rentas del año de la fecha del sistema, para probar las revisiones.</p>")
                .append("<form method='post' action='/sistema'><input type='hidden' name='accion' value='demo'><button class='btn'>Cargar datos de demostración</button></form></div></div>");
        enviar(ex, 200, pagina("Sistema", "/sistema", mensaje(ex), h.toString()));
    }

    // =====================================================================
    //  COMPONENTES HTML
    // =====================================================================

    private static String fichaCliente(Cliente c) {
        return "<div class='panel ficha'><h2>" + esc(c.getNombre()) + " <small>(" + esc(c.getCodigo()) + ")</small></h2>"
                + "<div class='datos'><div><span>Cédula</span><b>" + esc(c.getCedula()) + "</b></div>"
                + "<div><span>Teléfono</span><b>" + esc(c.getTelefono()) + "</b></div>"
                + "<div><span>Dirección</span><b>" + esc(c.getDireccion()) + "</b></div>"
                + "<div><span>Código de la empresa</span><b>" + esc(c.getCodigo()) + "</b></div>"
                + "<div><span>Cuota por atraso</span><b>" + (c.tieneDeuda() ? "<span class='pill rojo'>" + dinero(c.getDeudaAtraso()) + "</span>" : "RD$0.00") + "</b></div></div></div>";
    }

    private static String tarjeta(String numero, String texto) {
        return "<div class='tarjeta'><b>" + numero + "</b><span>" + texto + "</span></div>";
    }

    private static String campo(String nombre, String etiqueta, String ejemplo) {
        return "<label>" + etiqueta + "<input name='" + nombre + "' placeholder='" + esc(ejemplo) + "' required></label>";
    }

    /** Estructura común de todas las páginas: encabezado, menú, mensaje y pie. */
    private static String pagina(String titulo, String activo, String mensajeHtml, String contenido) {
        String[][] menu = {{"/", "Inicio"}, {"/clientes", "Clientes"}, {"/articulos", "Artículos"}, {"/rentar", "Rentar"},
                {"/devolver", "Devolver"}, {"/premios", "Premios"}, {"/sistema", "Fecha y datos"}};
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='es'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>");
        sb.append("<title>Film Magic – ").append(esc(titulo)).append("</title><style>").append(CSS).append("</style></head><body>");
        sb.append("<header><div class='marca'>🎬 FILM MAGIC <small>Sistema de rentas</small></div><div class='fecha'>Fecha del sistema: <b>")
                .append(sistema.getFechaSistema()).append("</b></div></header><nav>");
        for (String[] m : menu) {
            sb.append("<a href='").append(m[0]).append("'").append(m[0].equals(activo) ? " class='activo'" : "").append(">").append(m[1]).append("</a>");
        }
        sb.append("</nav><main><h1>").append(esc(titulo)).append("</h1>");
        if (mensajeHtml != null) {
            sb.append(mensajeHtml);
        }
        sb.append(contenido).append("</main><footer>Proyecto Final INF514 · Jose Gregorio Molina Ozuna · Prof. Julio Castro Ortiz</footer></body></html>");
        return sb.toString();
    }

    private static final String CSS =
            "*{box-sizing:border-box}body{margin:0;font-family:Segoe UI,Arial,sans-serif;background:#f1f5f9;color:#1f2937}"
            + "header{background:#1e3a8a;color:#fff;display:flex;justify-content:space-between;align-items:center;padding:14px 28px}"
            + ".marca{font-size:22px;font-weight:700}.marca small{font-weight:400;font-size:13px;opacity:.8;margin-left:8px}"
            + "nav{background:#172554;display:flex;flex-wrap:wrap;padding:0 20px}nav a{color:#cbd5e1;text-decoration:none;padding:12px 16px;font-size:15px}"
            + "nav a:hover{background:#1e3a8a;color:#fff}nav a.activo{background:#f1f5f9;color:#1e3a8a;font-weight:700}"
            + "main{max-width:1100px;margin:0 auto;padding:22px 20px 40px}h1{margin:0 0 16px;color:#1e3a8a;font-size:26px}h2{margin:0 0 12px;font-size:18px;color:#1e3a8a}"
            + ".panel{background:#fff;border-radius:10px;padding:18px 20px;margin-bottom:16px;box-shadow:0 1px 3px rgba(0,0,0,.1)}"
            + ".dos{display:grid;grid-template-columns:1fr 1fr;gap:16px}.dos .panel{margin-bottom:0}@media(max-width:800px){.dos{grid-template-columns:1fr}}"
            + ".tarjetas{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin-bottom:16px}"
            + ".tarjeta{background:#fff;border-radius:10px;padding:16px;text-align:center;box-shadow:0 1px 3px rgba(0,0,0,.1)}"
            + ".tarjeta b{display:block;font-size:30px;color:#1e3a8a}.tarjeta span{color:#6b7280;font-size:14px}"
            + ".accesos{display:flex;flex-wrap:wrap;gap:10px}.btn{display:inline-block;background:#1e3a8a;color:#fff;border:0;border-radius:7px;padding:10px 18px;font-size:15px;cursor:pointer;text-decoration:none}"
            + ".btn:hover{background:#1d4ed8}.btn.sec{background:#e0e7ff;color:#1e3a8a}.btn.peq{padding:6px 12px;font-size:13px}"
            + "table{width:100%;border-collapse:collapse;font-size:14px}th{background:#eff6ff;color:#1e3a8a;text-align:left}th,td{padding:9px 10px;border-bottom:1px solid #e5e7eb}"
            + "form.fila{display:flex;flex-wrap:wrap;gap:12px;align-items:flex-end}label{display:flex;flex-direction:column;font-size:13px;color:#4b5563;gap:4px}"
            + "input,select{padding:9px 10px;border:1px solid #cbd5e1;border-radius:6px;font-size:15px;min-width:170px}"
            + ".pill{padding:3px 10px;border-radius:99px;font-size:12px;font-weight:600}.verde{background:#dcfce7;color:#166534}.rojo{background:#fee2e2;color:#991b1b}.amarillo{background:#fef3c7;color:#92400e}"
            + ".msg{padding:12px 16px;border-radius:8px;margin-bottom:16px;font-size:15px}.msg.ok{background:#dcfce7;color:#166534;border:1px solid #86efac}.msg.error{background:#fee2e2;color:#991b1b;border:1px solid #fca5a5}"
            + ".datos{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:12px}.datos span{display:block;font-size:12px;color:#6b7280}.datos b{font-size:15px}"
            + ".aviso{border-left:6px solid #dc2626}.nota{color:#6b7280;font-size:13px;margin:0 0 10px}pre.carta{background:#fffbeb;border:1px solid #fde68a;padding:18px;border-radius:8px;white-space:pre-wrap;font-family:Georgia,serif;font-size:15px}"
            + "footer{text-align:center;color:#6b7280;font-size:12px;padding:16px}small{color:#6b7280;font-weight:400}";

    // =====================================================================
    //  UTILIDADES HTTP
    // =====================================================================

    private static void guardar() throws IOException {
        almacen.guardar(sistema);
    }

    private static void enviar(HttpExchange ex, int codigo, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        ex.getResponseBody().write(bytes);
    }

    /** Patrón Post/Redirect/Get: después de un cambio se redirige a una página con un mensaje. */
    private static void redirigir(HttpExchange ex, String ruta, String mensaje, boolean ok) throws IOException {
        String destino = ruta + (ruta.contains("?") ? "&" : "?") + "msg=" + url(mensaje) + "&ok=" + (ok ? "1" : "0");
        ex.getResponseHeaders().set("Location", destino);
        ex.sendResponseHeaders(303, -1);
    }

    private static String mensaje(HttpExchange ex) {
        Map<String, String> q = consulta(ex);
        String m = q.get("msg");
        if (m == null || m.isEmpty()) {
            return null;
        }
        return "<div class='msg " + (q.getOrDefault("ok", "0").equals("1") ? "ok" : "error") + "'>" + esc(m) + "</div>";
    }

    private static Map<String, String> consulta(HttpExchange ex) {
        return parsear(ex.getRequestURI().getRawQuery());
    }

    private static Map<String, String> formulario(HttpExchange ex) throws IOException {
        InputStream in = ex.getRequestBody();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = in.read(tmp)) > 0) {
            buf.write(tmp, 0, n);
        }
        return parsear(new String(buf.toByteArray(), StandardCharsets.UTF_8));
    }

    private static Map<String, String> parsear(String crudo) {
        Map<String, String> m = new HashMap<>();
        if (crudo == null || crudo.isEmpty()) {
            return m;
        }
        for (String par : crudo.split("&")) {
            int i = par.indexOf('=');
            String k = i < 0 ? par : par.substring(0, i);
            String v = i < 0 ? "" : par.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    /** Escapa texto para HTML (evita que lo escrito por el usuario altere la página). */
    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** El ';' es el separador de los archivos CSV, por eso se reemplaza. */
    private static String limpiar(String s) {
        return s.replace(";", ",");
    }

    private static String dinero(double v) {
        return String.format(Locale.US, "RD$%,.2f", v);
    }
}
