// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

/**
 * Clase abstracta que representa cualquier artículo que se puede rentar en
 * Film Magic (video, video juego o cinta de video juego).
 *
 * Aplica ENCAPSULAMIENTO (atributos privados con getters/setters),
 * ABSTRACCIÓN (métodos abstractos) y sirve de clase base para la HERENCIA.
 */
public abstract class Articulo {

    // Atributos encapsulados: solo se acceden mediante métodos de la clase
    private final String codigo;
    private String titulo;
    private double precioRenta;
    private boolean disponible;

    /** Constructor que inicializa todos los datos del artículo. */
    protected Articulo(String codigo, String titulo, double precioRenta) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.precioRenta = precioRenta;
        this.disponible = true;
    }

    // ---------- Métodos abstractos: cada subclase los define a su manera ----------

    /** Nombre legible del tipo de artículo. */
    public abstract String getTipo();

    /** Clave corta usada para guardar el tipo en los archivos de datos. */
    public abstract String getClaveTipo();

    /** Cantidad de días que se permite tener el artículo rentado. */
    public abstract int getDiasRenta();

    /** Cuota (RD$) que se cobra por cada día de atraso. */
    public abstract double getTarifaAtrasoDiaria();

    // ---------- Comportamiento común (polimorfismo sobre la cuota) ----------

    /** Calcula la cuota por atraso según los días de retraso y el tipo de artículo. */
    public double calcularCuotaAtraso(int diasAtraso) {
        if (diasAtraso <= 0) {
            return 0;
        }
        return diasAtraso * getTarifaAtrasoDiaria();
    }

    // ---------- Getters y setters ----------

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public double getPrecioRenta() {
        return precioRenta;
    }

    public void setPrecioRenta(double precioRenta) {
        this.precioRenta = precioRenta;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    // ---------- Persistencia ----------

    /** Convierte el artículo a una línea CSV (separador ';'). */
    public String toCsv() {
        return getClaveTipo() + ";" + codigo + ";" + titulo + ";" + precioRenta + ";" + disponible;
    }

    /** Fábrica: crea el tipo de artículo correcto a partir de su clave. */
    public static Articulo crear(String claveTipo, String codigo, String titulo, double precio) {
        switch (claveTipo) {
            case "VIDEO":
                return new Video(codigo, titulo, precio);
            case "VIDEOJUEGO":
                return new VideoJuego(codigo, titulo, precio);
            case "CINTA":
                return new CintaVideoJuego(codigo, titulo, precio);
            default:
                throw new IllegalArgumentException("Tipo de artículo desconocido: " + claveTipo);
        }
    }

    /** Reconstruye un artículo a partir de una línea CSV. */
    public static Articulo desdeCsv(String linea) {
        String[] p = linea.split(";", -1);
        Articulo a = crear(p[0], p[1], p[2], Double.parseDouble(p[3]));
        a.setDisponible(Boolean.parseBoolean(p[4]));
        return a;
    }

    @Override
    public String toString() {
        return String.format("%-6s | %-12s | %-28s | RD$%7.2f | %s",
                codigo, getTipo(), titulo, precioRenta, disponible ? "Disponible" : "Rentado");
    }
}
