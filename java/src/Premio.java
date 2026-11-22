// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

/**
 * Resultado de la revisión mensual o anual de un cliente premiado:
 * total rentado, cantidad de cupones de renta gratis y si recibe
 * certificado para un video gratis (solo en la revisión anual).
 */
public class Premio {

    private final Cliente cliente;
    private final double totalRentado;
    private final int cupones;
    private final boolean certificado;

    public Premio(Cliente cliente, double totalRentado, int cupones, boolean certificado) {
        this.cliente = cliente;
        this.totalRentado = totalRentado;
        this.cupones = cupones;
        this.certificado = certificado;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public double getTotalRentado() {
        return totalRentado;
    }

    public int getCupones() {
        return cupones;
    }

    public boolean tieneCertificado() {
        return certificado;
    }
}
