// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Programa principal: menú de consola del sistema de renta de Film Magic.
 */
public class Main {

    private static final Scanner entrada = new Scanner(System.in, "UTF-8");
    private static final FilmMagic sistema = new FilmMagic();
    private static final Almacen almacen = new Almacen("datos");
    // Con -Deco=true se muestra en pantalla lo que se lee de un archivo de pruebas
    private static final boolean ECO = Boolean.getBoolean("eco");

    public static void main(String[] args) throws Exception {
        // Salida en UTF-8 para que se vean bien los acentos
        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        Locale.setDefault(Locale.US);

        try {
            almacen.cargar(sistema);
        } catch (IOException | RuntimeException e) {
            System.out.println("Aviso: no se pudieron cargar los datos guardados (" + e.getMessage() + ").");
        }

        boolean salir = false;
        try {
            while (!salir) {
                mostrarMenu();
                int opcion = leerEntero("Seleccione una opción: ");
                System.out.println();
                try {
                    switch (opcion) {
                        case 1: registrarCliente(); break;
                        case 2: registrarArticulo(); break;
                        case 3: rentarArticulo(); break;
                        case 4: devolverArticulo(); break;
                        case 5: consultarCliente(); break;
                        case 6: listarArticulos(); break;
                        case 7: revisionMensual(); break;
                        case 8: revisionAnual(); break;
                        case 9: cambiarFecha(); break;
                        case 10: cargarDemostracion(); break;
                        case 0: salir = true; break;
                        default: System.out.println("Opción no válida.");
                    }
                    almacen.guardar(sistema); // se guarda después de cada operación
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("ERROR: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("ERROR al guardar los datos: " + e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            // Fin de la entrada de datos: se cierra el programa con normalidad
        }
        System.out.println("Gracias por usar el sistema de Film Magic. ¡Hasta pronto!");
    }

    // ================= Pantallas =================

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("        FILM MAGIC - SISTEMA DE RENTAS");
        System.out.println("   Fecha del sistema: " + sistema.getFechaSistema());
        System.out.println("==============================================");
        System.out.println(" 1. Registrar cliente");
        System.out.println(" 2. Registrar artículo");
        System.out.println(" 3. Rentar artículo");
        System.out.println(" 4. Devolver artículo");
        System.out.println(" 5. Consultar cliente por código");
        System.out.println(" 6. Listar artículos");
        System.out.println(" 7. Revisión mensual de premios");
        System.out.println(" 8. Revisión anual de premios");
        System.out.println(" 9. Cambiar fecha del sistema (simulación)");
        System.out.println("10. Cargar datos de demostración");
        System.out.println(" 0. Salir");
        System.out.println("----------------------------------------------");
    }

    // ================= Opciones del menú =================

    private static void registrarCliente() {
        System.out.println("--- REGISTRO DE CLIENTE ---");
        String cedula = leerTexto("Cédula: ");
        String nombre = leerTexto("Nombre: ");
        String telefono = leerTexto("Teléfono: ");
        String direccion = leerTexto("Dirección: ");
        String codigo = leerTexto("Código de la empresa: ").toUpperCase();
        Cliente c = new Cliente(cedula, nombre, telefono, direccion, codigo);
        if (sistema.registrarCliente(c)) {
            System.out.println("Cliente registrado correctamente.");
        } else {
            System.out.println("ERROR: ya existe un cliente con el código " + codigo + ".");
        }
    }

    private static void registrarArticulo() {
        System.out.println("--- REGISTRO DE ARTÍCULO ---");
        System.out.println("Tipo: 1) Video   2) Video juego   3) Cinta de video juego");
        int tipo = leerEntero("Tipo: ");
        String clave;
        switch (tipo) {
            case 1: clave = "VIDEO"; break;
            case 2: clave = "VIDEOJUEGO"; break;
            case 3: clave = "CINTA"; break;
            default:
                System.out.println("ERROR: tipo no válido.");
                return;
        }
        String codigo = leerTexto("Código del artículo: ").toUpperCase();
        String titulo = leerTexto("Título: ");
        double precio = leerDecimal("Precio de renta (RD$): ");
        Articulo a = Articulo.crear(clave, codigo, titulo, precio);
        if (sistema.registrarArticulo(a)) {
            System.out.println("Artículo registrado. Días de renta: " + a.getDiasRenta()
                    + " | Cuota por atraso: RD$" + a.getTarifaAtrasoDiaria() + " por día.");
        } else {
            System.out.println("ERROR: ya existe un artículo con el código " + codigo + ".");
        }
    }

    private static void rentarArticulo() {
        System.out.println("--- RENTA DE ARTÍCULO ---");
        String codCliente = leerTexto("Código del cliente: ");
        Cliente c = sistema.buscarCliente(codCliente);
        if (c == null) {
            System.out.println("ERROR: no existe un cliente con ese código.");
            return;
        }
        System.out.println("Datos del cliente:");
        System.out.println("  " + c.toString().replace("\n", "\n"));

        // Regla: si tiene cuota por atraso, debe pagarla al rentar otro artículo
        if (c.tieneDeuda()) {
            System.out.printf("El cliente tiene una cuota por atraso de RD$%,.2f.%n", c.getDeudaAtraso());
            String r = leerTexto("¿El cliente paga la cuota ahora? (S/N): ");
            if (r.equalsIgnoreCase("S")) {
                double pagado = c.pagarDeuda();
                System.out.printf("Pago recibido: RD$%,.2f. Cuota saldada.%n", pagado);
            } else {
                System.out.println("No se puede rentar mientras exista cuota pendiente.");
                return;
            }
        }
        String codArticulo = leerTexto("Código del artículo: ");
        Renta renta = sistema.rentar(c.getCodigo(), codArticulo);
        Articulo a = sistema.buscarArticulo(codArticulo);
        System.out.println("Renta #" + renta.getId() + " registrada: " + a.getTitulo() + " (" + a.getTipo() + ")");
        System.out.printf("  Monto: RD$%,.2f | Fecha de renta: %s | Debe devolver el: %s%n",
                renta.getMonto(), renta.getFechaRenta(), renta.getFechaVencimiento());
    }

    private static void devolverArticulo() {
        System.out.println("--- DEVOLUCIÓN DE ARTÍCULO ---");
        String codArticulo = leerTexto("Código del artículo: ");
        Renta r = sistema.devolver(codArticulo);
        Articulo a = sistema.buscarArticulo(codArticulo);
        Cliente c = sistema.buscarCliente(r.getCodigoCliente());
        System.out.println("Devuelto: " + a.getTitulo() + " (cliente " + c.getNombre() + ")");
        if (r.getCuotaAtraso() > 0) {
            System.out.println("NOTA: " + r.getNota());
            System.out.printf("Cuota por atraso registrada: RD$%,.2f (el cliente deberá pagarla en su próxima renta).%n",
                    r.getCuotaAtraso());
        } else {
            System.out.println("Devolución a tiempo. Sin cuota por atraso.");
        }
    }

    private static void consultarCliente() {
        System.out.println("--- CONSULTA DE CLIENTE ---");
        Cliente c = sistema.buscarCliente(leerTexto("Código del cliente: "));
        if (c == null) {
            System.out.println("ERROR: no existe un cliente con ese código.");
            return;
        }
        System.out.println(c);
        System.out.println("  Rentas registradas:");
        int abiertas = 0;
        int total = 0;
        for (Renta r : sistema.getRentas()) {
            if (r.getCodigoCliente().equals(c.getCodigo())) {
                total++;
                if (!r.estaDevuelta()) {
                    abiertas++;
                    Articulo a = sistema.buscarArticulo(r.getCodigoArticulo());
                    System.out.printf("   - #%d %s, vence el %s%n", r.getId(), a.getTitulo(), r.getFechaVencimiento());
                }
            }
        }
        System.out.println("   Total de rentas: " + total + " | Pendientes de devolver: " + abiertas);
    }

    private static void listarArticulos() {
        System.out.println("--- INVENTARIO DE ARTÍCULOS ---");
        List<Articulo> lista = sistema.getArticulos();
        if (lista.isEmpty()) {
            System.out.println("No hay artículos registrados.");
            return;
        }
        System.out.println("Código | Tipo         | Título                       | Precio     | Estado");
        for (Articulo a : lista) {
            System.out.println(a);
        }
    }

    private static void revisionMensual() throws IOException {
        System.out.println("--- REVISIÓN MENSUAL (premio: más de RD$1,000 en el mes) ---");
        int anio = leerEntero("Año: ");
        int mes = leerEntero("Mes (1-12): ");
        if (mes < 1 || mes > 12) {
            System.out.println("ERROR: mes no válido.");
            return;
        }
        List<Premio> premiados = sistema.revisionMensual(anio, mes);
        mostrarPremios(premiados, String.format("%02d/%d", mes, anio), false);
    }

    private static void revisionAnual() throws IOException {
        System.out.println("--- REVISIÓN ANUAL (premio: más de RD$10,000 en el año) ---");
        int anio = leerEntero("Año: ");
        List<Premio> premiados = sistema.revisionAnual(anio);
        mostrarPremios(premiados, String.valueOf(anio), true);
    }

    private static void mostrarPremios(List<Premio> premiados, String periodo, boolean anual) throws IOException {
        if (premiados.isEmpty()) {
            System.out.println("Ningún cliente superó el nivel de premio en " + periodo + ".");
            return;
        }
        System.out.println("Clientes premiados en " + periodo + ":");
        for (Premio p : premiados) {
            Path carta = sistema.generarCarta(p, periodo, anual);
            System.out.printf(" * %s (%s) - Rentó RD$%,.2f | Cupones: %d%s%n",
                    p.getCliente().getNombre(), p.getCliente().getCodigo(), p.getTotalRentado(),
                    p.getCupones(), p.tieneCertificado() ? " | CERTIFICADO de video gratis" : "");
            System.out.println("     Carta generada: " + carta);
        }
    }

    private static void cambiarFecha() {
        LocalDate f = leerFecha("Nueva fecha del sistema (AAAA-MM-DD): ");
        sistema.setFechaSistema(f);
        System.out.println("Fecha del sistema cambiada a " + f + ".");
    }

    private static void cargarDemostracion() {
        sistema.cargarDemostracion();
        System.out.println("Datos de demostración cargados (5 clientes, 8 artículos y rentas del año "
                + sistema.getFechaSistema().getYear() + ").");
    }

    // ================= Lectura segura de datos =================

    /** Lee un texto no vacío; el ';' se elimina porque es el separador de los archivos. */
    private static String leerTexto(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String s = entrada.nextLine().trim().replace(";", ",");
            if (ECO) {
                System.out.println(s); // eco de la entrada (solo al ejecutar con un archivo de pruebas)
            }
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("El campo no puede estar vacío.");
        }
    }

    private static int leerEntero(String mensaje) {
        while (true) {
            try {
                return Integer.parseInt(leerTexto(mensaje));
            } catch (NumberFormatException e) {
                System.out.println("Debe escribir un número entero.");
            }
        }
    }

    private static double leerDecimal(String mensaje) {
        while (true) {
            try {
                double d = Double.parseDouble(leerTexto(mensaje));
                if (d > 0) {
                    return d;
                }
                System.out.println("El valor debe ser mayor que cero.");
            } catch (NumberFormatException e) {
                System.out.println("Debe escribir un número válido.");
            }
        }
    }

    private static LocalDate leerFecha(String mensaje) {
        while (true) {
            try {
                return LocalDate.parse(leerTexto(mensaje));
            } catch (DateTimeParseException e) {
                System.out.println("Formato de fecha no válido. Use AAAA-MM-DD.");
            }
        }
    }
}
