// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

/** Película o video rentable. Hereda de Articulo (HERENCIA con extends). */
public class Video extends Articulo {

    public Video(String codigo, String titulo, double precioRenta) {
        super(codigo, titulo, precioRenta); // llama al constructor de la clase base
    }

    @Override
    public String getTipo() {
        return "Video";
    }

    @Override
    public String getClaveTipo() {
        return "VIDEO";
    }

    @Override
    public int getDiasRenta() {
        return 3;
    }

    @Override
    public double getTarifaAtrasoDiaria() {
        return 25.0;
    }
}
