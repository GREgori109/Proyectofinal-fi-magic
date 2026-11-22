// Nombre del Autor: Jose Gregorio Molina Ozuna
// Matrícula: 100706992
// Asignatura: Programación
// Sección: 7 (INF514-7)
// Fecha de Entrega: 27 de noviembre de 2026
// Hora de Entrega: 2:17 p. m. (hora límite)
// Profesor: Julio Castro Ortiz

/** Video juego rentable (disco o cartucho). Hereda de Articulo. */
public class VideoJuego extends Articulo {

    public VideoJuego(String codigo, String titulo, double precioRenta) {
        super(codigo, titulo, precioRenta);
    }

    @Override
    public String getTipo() {
        return "Video juego";
    }

    @Override
    public String getClaveTipo() {
        return "VIDEOJUEGO";
    }

    @Override
    public int getDiasRenta() {
        return 5;
    }

    @Override
    public double getTarifaAtrasoDiaria() {
        return 50.0;
    }
}
