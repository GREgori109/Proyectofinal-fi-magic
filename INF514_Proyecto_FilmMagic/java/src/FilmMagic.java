// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica del negocio de Film Magic: clientes, artículos, rentas,
 * devoluciones con cuota por atraso y revisiones de premios.
 */
public class FilmMagic {

    // ----- Reglas del negocio (constantes) -----
    public static final double NIVEL_PREMIO_MENSUAL = 1000.0;
    public static final double NIVEL_PREMIO_ANUAL = 10000.0;
    /** Un cupón adicional por cada RD$500 rentados por encima del nivel mensual. */
    public static final double PASO_CUPON_MENSUAL = 500.0;
    /** Un cupón adicional por cada RD$2,000 rentados por encima del nivel anual. */
    public static final double PASO_CUPON_ANUAL = 2000.0;

    // ----- Almacenamiento en memoria -----
    // LinkedHashMap: búsqueda rápida por código y conserva el orden de registro
    private final Map<String, Cliente> clientes = new LinkedHashMap<>();
    private final Map<String, Articulo> articulos = new LinkedHashMap<>();
    private final List<Renta> rentas = new ArrayList<>();

    private LocalDate fechaSistema = LocalDate.now();

    // ---------- Fecha del sistema (permite simular el paso del tiempo) ----------

    public LocalDate getFechaSistema() {
        return fechaSistema;
    }

    public void setFechaSistema(LocalDate fecha) {
        this.fechaSistema = fecha;
    }

    // ---------- Consultas ----------

    public List<Cliente> getClientes() {
        return new ArrayList<>(clientes.values());
    }

    public List<Articulo> getArticulos() {
        return new ArrayList<>(articulos.values());
    }

    public List<Renta> getRentas() {
        return new ArrayList<>(rentas);
    }

    public Cliente buscarCliente(String codigo) {
        return clientes.get(codigo.trim().toUpperCase());
    }

    public Articulo buscarArticulo(String codigo) {
        return articulos.get(codigo.trim().toUpperCase());
    }

    // ---------- Registro ----------

    public boolean registrarCliente(Cliente c) {
        if (clientes.containsKey(c.getCodigo())) {
            return false;
        }
        clientes.put(c.getCodigo(), c);
        return true;
    }

    public boolean registrarArticulo(Articulo a) {
        if (articulos.containsKey(a.getCodigo())) {
            return false;
        }
        articulos.put(a.getCodigo(), a);
        return true;
    }

    // Usados por Almacen al leer los archivos
    void agregarClienteCargado(Cliente c) {
        clientes.put(c.getCodigo(), c);
    }

    void agregarArticuloCargado(Articulo a) {
        articulos.put(a.getCodigo(), a);
    }

    void agregarRentaCargada(Renta r) {
        rentas.add(r);
    }

    // ---------- Rentas ----------

    /**
     * Renta un artículo a un cliente usando la fecha del sistema.
     * Antes de llamar, el empleado debe haber cobrado la cuota por atraso (si existía).
     */
    public Renta rentar(String codigoCliente, String codigoArticulo) {
        Cliente c = buscarCliente(codigoCliente);
        Articulo a = buscarArticulo(codigoArticulo);
        if (c == null) {
            throw new IllegalArgumentException("No existe un cliente con el código " + codigoCliente);
        }
        if (a == null) {
            throw new IllegalArgumentException("No existe un artículo con el código " + codigoArticulo);
        }
        if (c.tieneDeuda()) {
            throw new IllegalStateException("El cliente tiene una cuota por atraso pendiente de RD$"
                    + String.format("%,.2f", c.getDeudaAtraso()) + ". Debe pagarla antes de rentar.");
        }
        if (!a.isDisponible()) {
            throw new IllegalStateException("El artículo '" + a.getTitulo() + "' ya está rentado.");
        }
        Renta r = new Renta(siguienteIdRenta(), c.getCodigo(), a.getCodigo(), fechaSistema,
                fechaSistema.plusDays(a.getDiasRenta()), a.getPrecioRenta());
        a.setDisponible(false);
        rentas.add(r);
        return r;
    }

    /**
     * Registra la devolución de un artículo. Si hay atraso, calcula la cuota
     * y la anota en el registro del cliente.
     */
    public Renta devolver(String codigoArticulo) {
        Articulo a = buscarArticulo(codigoArticulo);
        if (a == null) {
            throw new IllegalArgumentException("No existe un artículo con el código " + codigoArticulo);
        }
        Renta abierta = null;
        for (Renta r : rentas) {
            if (r.getCodigoArticulo().equals(a.getCodigo()) && !r.estaDevuelta()) {
                abierta = r;
            }
        }
        if (abierta == null) {
            throw new IllegalStateException("El artículo '" + a.getTitulo() + "' no está rentado.");
        }
        int diasAtraso = (int) Math.max(0, ChronoUnit.DAYS.between(abierta.getFechaVencimiento(), fechaSistema));
        double cuota = a.calcularCuotaAtraso(diasAtraso); // polimorfismo: depende del tipo
        abierta.registrarDevolucion(fechaSistema, diasAtraso, cuota);
        a.setDisponible(true);
        if (cuota > 0) {
            buscarCliente(abierta.getCodigoCliente()).agregarDeuda(cuota);
        }
        return abierta;
    }

    private int siguienteIdRenta() {
        int max = 0;
        for (Renta r : rentas) {
            max = Math.max(max, r.getId());
        }
        return max + 1;
    }

    /** Agrega una renta ya finalizada (usado para cargar datos de demostración). */
    public void agregarRentaHistorica(String codCliente, String codArticulo, LocalDate fecha) {
        Articulo a = buscarArticulo(codArticulo);
        Renta r = new Renta(siguienteIdRenta(), codCliente, codArticulo, fecha,
                fecha.plusDays(a.getDiasRenta()), a.getPrecioRenta());
        r.registrarDevolucion(fecha.plusDays(a.getDiasRenta()), 0, 0);
        rentas.add(r);
    }

    // ---------- Revisiones de premios ----------

    /** Revisión mensual: clientes que rentaron MÁS de RD$1,000 en el mes indicado. */
    public List<Premio> revisionMensual(int anio, int mes) {
        List<Premio> premiados = new ArrayList<>();
        for (Cliente c : clientes.values()) {
            double total = 0;
            for (Renta r : rentas) {
                if (r.getCodigoCliente().equals(c.getCodigo())
                        && r.getFechaRenta().getYear() == anio
                        && r.getFechaRenta().getMonthValue() == mes) {
                    total += r.getMonto();
                }
            }
            if (total > NIVEL_PREMIO_MENSUAL) {
                int cupones = 1 + (int) ((total - NIVEL_PREMIO_MENSUAL) / PASO_CUPON_MENSUAL);
                premiados.add(new Premio(c, total, cupones, false));
            }
        }
        return premiados;
    }

    /**
     * Revisión anual: clientes que rentaron MÁS de RD$10,000 en el año.
     * Reciben certificado de video gratis si superan dos veces el nivel (RD$20,000).
     */
    public List<Premio> revisionAnual(int anio) {
        List<Premio> premiados = new ArrayList<>();
        for (Cliente c : clientes.values()) {
            double total = 0;
            for (Renta r : rentas) {
                if (r.getCodigoCliente().equals(c.getCodigo()) && r.getFechaRenta().getYear() == anio) {
                    total += r.getMonto();
                }
            }
            if (total > NIVEL_PREMIO_ANUAL) {
                int cupones = 1 + (int) ((total - NIVEL_PREMIO_ANUAL) / PASO_CUPON_ANUAL);
                boolean certificado = total > 2 * NIVEL_PREMIO_ANUAL;
                premiados.add(new Premio(c, total, cupones, certificado));
            }
        }
        return premiados;
    }

    // ---------- Cartas ----------

    /** Redacta la carta para un cliente premiado y la guarda en la carpeta "cartas". */
    public Path generarCarta(Premio p, String periodo, boolean anual) throws IOException {
        Cliente c = p.getCliente();
        StringBuilder sb = new StringBuilder();
        sb.append("FILM MAGIC\n");
        sb.append("Santo Domingo, ").append(fechaSistema).append("\n\n");
        sb.append("Estimado(a) ").append(c.getNombre()).append("\n");
        sb.append(c.getDireccion()).append("\n\n");
        sb.append("Gracias por ser parte de la familia Film Magic. ");
        sb.append(anual ? "Durante el año " : "Durante el mes ").append(periodo);
        sb.append(" usted rentó RD$").append(String.format("%,.2f", p.getTotalRentado()));
        sb.append(", superando nuestro nivel de premio ");
        sb.append(anual ? "anual" : "mensual").append(".\n\n");
        sb.append("Como muestra de agradecimiento le enviamos ").append(p.getCupones());
        sb.append(p.getCupones() == 1 ? " cupón" : " cupones").append(" de renta gratis.\n");
        if (p.tieneCertificado()) {
            sb.append("Además, incluimos un CERTIFICADO PARA UN VIDEO GRATIS.\n");
        }
        sb.append("\nAtentamente,\nLa Gerencia de Film Magic\n");

        Path carpeta = Paths.get("cartas");
        Files.createDirectories(carpeta);
        String nombre = "carta_" + (anual ? "anual_" : "mensual_") + periodo.replace("/", "-")
                + "_" + c.getCodigo() + ".txt";
        Path destino = carpeta.resolve(nombre);
        Files.write(destino, sb.toString().getBytes(StandardCharsets.UTF_8));
        return destino;
    }

    // ---------- Datos de demostración ----------

    /**
     * Carga clientes, artículos y un historial de rentas del año de la fecha del
     * sistema, para poder probar las revisiones mensual y anual.
     */
    public void cargarDemostracion() {
        registrarCliente(new Cliente("001-0000001-1", "María Fernández", "809-555-0101", "C/ Duarte #12, Santo Domingo", "C001"));
        registrarCliente(new Cliente("001-0000002-2", "Pedro Martínez", "809-555-0102", "Av. Duarte #45, Santiago", "C002"));
        registrarCliente(new Cliente("001-0000003-3", "Ana Rodríguez", "829-555-0103", "C/ Sánchez #7, La Vega", "C003"));
        registrarCliente(new Cliente("001-0000004-4", "Luis Peña", "849-555-0104", "Av. Lincoln #88, Santo Domingo", "C004"));
        registrarCliente(new Cliente("001-0000005-5", "Carla Núñez", "809-555-0105", "C/ Mella #3, San Cristóbal", "C005"));

        registrarArticulo(new Video("V001", "El Padrino", 150));
        registrarArticulo(new Video("V002", "Titanic", 150));
        registrarArticulo(new Video("V003", "Matrix", 150));
        registrarArticulo(new VideoJuego("J001", "FIFA 24", 300));
        registrarArticulo(new VideoJuego("J002", "Zelda Breath of the Wild", 300));
        registrarArticulo(new CintaVideoJuego("T001", "Super Mario Bros", 250));
        registrarArticulo(new CintaVideoJuego("T002", "Sonic 2", 250));
        registrarArticulo(new CintaVideoJuego("T003", "Mortal Kombat", 250));

        int anio = fechaSistema.getYear();
        LocalDate inicio = LocalDate.of(anio, 1, 2);
        // C001: video juego cada 4 días  -> supera el premio mensual y el doble del anual
        for (LocalDate f = inicio; !f.isAfter(fechaSistema); f = f.plusDays(4)) {
            agregarRentaHistorica("C001", "J001", f);
        }
        // C003: cinta cada 6 días -> supera el premio mensual y el anual (sin certificado)
        for (LocalDate f = inicio; !f.isAfter(fechaSistema); f = f.plusDays(6)) {
            agregarRentaHistorica("C003", "T001", f);
        }
        // C002: video cada 9 días -> no alcanza los niveles
        for (LocalDate f = inicio; !f.isAfter(fechaSistema); f = f.plusDays(9)) {
            agregarRentaHistorica("C002", "V001", f);
        }
        // C004: pocas rentas
        for (LocalDate f = inicio.plusDays(10); !f.isAfter(fechaSistema); f = f.plusDays(45)) {
            agregarRentaHistorica("C004", "V002", f);
        }
    }
}
