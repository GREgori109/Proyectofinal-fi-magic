// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

import java.time.LocalDate;

/**
 * Registro de una renta: qué cliente rentó qué artículo, cuándo debía
 * devolverlo, cuándo lo devolvió y la cuota por atraso (si la hubo).
 */
public class Renta {

    private final int id;
    private final String codigoCliente;
    private final String codigoArticulo;
    private final LocalDate fechaRenta;
    private final LocalDate fechaVencimiento;
    private LocalDate fechaDevolucion; // null mientras no se haya devuelto
    private final double monto;
    private double cuotaAtraso;
    private String nota;

    public Renta(int id, String codigoCliente, String codigoArticulo, LocalDate fechaRenta,
                 LocalDate fechaVencimiento, double monto) {
        this.id = id;
        this.codigoCliente = codigoCliente;
        this.codigoArticulo = codigoArticulo;
        this.fechaRenta = fechaRenta;
        this.fechaVencimiento = fechaVencimiento;
        this.monto = monto;
        this.cuotaAtraso = 0;
        this.nota = "";
    }

    /** Registra la devolución; si hubo atraso deja la nota y la cuota en el registro. */
    public void registrarDevolucion(LocalDate fecha, int diasAtraso, double cuota) {
        this.fechaDevolucion = fecha;
        this.cuotaAtraso = cuota;
        if (diasAtraso > 0) {
            this.nota = "Devuelto con " + diasAtraso + " día(s) de atraso";
        }
    }

    public boolean estaDevuelta() {
        return fechaDevolucion != null;
    }

    public int getId() {
        return id;
    }

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public String getCodigoArticulo() {
        return codigoArticulo;
    }

    public LocalDate getFechaRenta() {
        return fechaRenta;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public LocalDate getFechaDevolucion() {
        return fechaDevolucion;
    }

    public double getMonto() {
        return monto;
    }

    public double getCuotaAtraso() {
        return cuotaAtraso;
    }

    public String getNota() {
        return nota;
    }

    public String toCsv() {
        return id + ";" + codigoCliente + ";" + codigoArticulo + ";" + fechaRenta + ";" + fechaVencimiento
                + ";" + (fechaDevolucion == null ? "-" : fechaDevolucion.toString())
                + ";" + monto + ";" + cuotaAtraso + ";" + nota;
    }

    public static Renta desdeCsv(String linea) {
        String[] p = linea.split(";", -1);
        Renta r = new Renta(Integer.parseInt(p[0]), p[1], p[2], LocalDate.parse(p[3]),
                LocalDate.parse(p[4]), Double.parseDouble(p[6]));
        if (!p[5].equals("-")) {
            r.fechaDevolucion = LocalDate.parse(p[5]);
        }
        r.cuotaAtraso = Double.parseDouble(p[7]);
        r.nota = p[8];
        return r;
    }
}
