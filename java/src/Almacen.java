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
import java.util.ArrayList;
import java.util.List;

/**
 * Se encarga de guardar y leer los datos en archivos de texto (CSV con
 * separador ';') dentro de la carpeta "datos". Así la información se
 * conserva entre ejecuciones del programa.
 */
public class Almacen {

    private final Path carpeta;

    public Almacen(String nombreCarpeta) {
        this.carpeta = Paths.get(nombreCarpeta);
    }

    public void guardar(FilmMagic sistema) throws IOException {
        Files.createDirectories(carpeta);
        List<String> lineas = new ArrayList<>();

        for (Cliente c : sistema.getClientes()) {
            lineas.add(c.toCsv());
        }
        escribir("clientes.csv", lineas);

        lineas.clear();
        for (Articulo a : sistema.getArticulos()) {
            lineas.add(a.toCsv());
        }
        escribir("articulos.csv", lineas);

        lineas.clear();
        for (Renta r : sistema.getRentas()) {
            lineas.add(r.toCsv());
        }
        escribir("rentas.csv", lineas);
    }

    public void cargar(FilmMagic sistema) throws IOException {
        for (String l : leer("clientes.csv")) {
            sistema.agregarClienteCargado(Cliente.desdeCsv(l));
        }
        for (String l : leer("articulos.csv")) {
            sistema.agregarArticuloCargado(Articulo.desdeCsv(l));
        }
        for (String l : leer("rentas.csv")) {
            sistema.agregarRentaCargada(Renta.desdeCsv(l));
        }
    }

    private void escribir(String archivo, List<String> lineas) throws IOException {
        Files.write(carpeta.resolve(archivo), lineas, StandardCharsets.UTF_8);
    }

    private List<String> leer(String archivo) throws IOException {
        Path ruta = carpeta.resolve(archivo);
        List<String> resultado = new ArrayList<>();
        if (Files.exists(ruta)) {
            for (String l : Files.readAllLines(ruta, StandardCharsets.UTF_8)) {
                if (!l.isBlank()) {
                    resultado.add(l);
                }
            }
        }
        return resultado;
    }
}
