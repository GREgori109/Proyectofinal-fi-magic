// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

/**
 * Cliente de Film Magic. Representa los datos de la tarjeta de renta:
 * {cédula, nombre, teléfono, dirección, código de la empresa} más la
 * cuota por atraso pendiente de pago.
 *
 * Todos los atributos son privados (ENCAPSULAMIENTO).
 */
public class Cliente {

    private final String cedula;
    private String nombre;
    private String telefono;
    private String direccion;
    private final String codigo; // código de la empresa (identifica al cliente en el sistema)
    private double deudaAtraso;  // cuota por atraso pendiente

    /** Constructor completo: inicializa todos los datos del cliente. */
    public Cliente(String cedula, String nombre, String telefono, String direccion, String codigo) {
        this(cedula, nombre, telefono, direccion, codigo, 0.0);
    }

    /** Constructor sobrecargado: permite indicar una deuda inicial (usado al cargar datos). */
    public Cliente(String cedula, String nombre, String telefono, String direccion,
                   String codigo, double deudaAtraso) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.codigo = codigo;
        this.deudaAtraso = deudaAtraso;
    }

    // ---------- Getters y setters ----------

    public String getCedula() {
        return cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCodigo() {
        return codigo;
    }

    public double getDeudaAtraso() {
        return deudaAtraso;
    }

    public boolean tieneDeuda() {
        return deudaAtraso > 0;
    }

    // ---------- Operaciones sobre la cuota por atraso ----------

    /** Suma una cuota por atraso al registro del cliente. */
    public void agregarDeuda(double monto) {
        if (monto > 0) {
            deudaAtraso += monto;
        }
    }

    /** Método sobrecargado: paga toda la deuda pendiente. */
    public double pagarDeuda() {
        double pagado = deudaAtraso;
        deudaAtraso = 0;
        return pagado;
    }

    /** Método sobrecargado: paga un monto parcial (nunca más de lo que se debe). */
    public double pagarDeuda(double monto) {
        double pagado = Math.min(Math.max(monto, 0), deudaAtraso);
        deudaAtraso -= pagado;
        return pagado;
    }

    // ---------- Persistencia ----------

    public String toCsv() {
        return cedula + ";" + nombre + ";" + telefono + ";" + direccion + ";" + codigo + ";" + deudaAtraso;
    }

    public static Cliente desdeCsv(String linea) {
        String[] p = linea.split(";", -1);
        return new Cliente(p[0], p[1], p[2], p[3], p[4], Double.parseDouble(p[5]));
    }

    @Override
    public String toString() {
        return String.format("Código: %s%n  Cédula: %s%n  Nombre: %s%n  Teléfono: %s%n  Dirección: %s%n  Cuota por atraso pendiente: RD$%,.2f",
                codigo, cedula, nombre, telefono, direccion, deudaAtraso);
    }
}
