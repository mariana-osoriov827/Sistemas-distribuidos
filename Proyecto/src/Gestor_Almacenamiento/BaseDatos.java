/**************************************************************************************
* Fecha: 10/10/2025
* Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
* Tema: 
* - Proyecto préstamo de libros (Sistema Distribuido)
* Descripción:
* - Clase de Persistencia Local (BaseDatos):
* - Implementa la gestión de los libros y sus ejemplares en memoria (ConcurrentHashMap).
* - Proporciona métodos para cargar y guardar el estado de la BD desde/hacia archivos.
* - Contiene las operaciones transaccionales (devolver, renovar, prestar) que son
* invocadas por los Actores de Devolución/Renovación.
* - Utiliza bloques synchronized (l) para asegurar la **consistencia** y el manejo
* de **concurrencia** a nivel de libro.
***************************************************************************************/

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BaseDatos {

    // Aquí guardamos todos los libros, usando un mapa para buscarlos rápido por código
    private final Map<String, Libro> libros = new ConcurrentHashMap<>();

    public BaseDatos() { }

    // Este método carga los libros desde un archivo de texto
    // Primero lee cada línea, si la línea comienza con un número significa que es un ejemplar
    // Si la línea comienza con texto, significa que es la cabecera de un libro
    public void cargarDesdeArchivo(String ruta) throws IOException {
        libros.clear();
        BufferedReader br = new BufferedReader(new FileReader(ruta));
        String linea;
        Libro libroActual = null;

        while ((linea = br.readLine()) != null) {
            if (linea.trim().isEmpty()) continue;
            String[] partes = linea.split(",");
            String first = partes[0].trim();

            // Si la línea empieza con un número, se asume que describe un ejemplar
            if (Character.isDigit(first.charAt(0))) {
                if (libroActual != null && partes.length >= 3) {
                    int id = Integer.parseInt(partes[0].trim());
                    char estado = partes[1].trim().charAt(0);
                    String fecha = partes[2].trim();
                    libroActual.addEjemplar(new Ejemplar(id, estado, fecha));
                }
            } else {
                // Si la línea no empieza con número, es la cabecera de un libro
                if (partes.length >= 3) {
                    String titulo = partes[0].trim();
                    String codigo = partes[1].trim();
                    int ejemplares = Integer.parseInt(partes[2].trim());
                    libroActual = new Libro(codigo, titulo, 0);
                    libros.put(codigo, libroActual);

                    // Guardamos cuántos ejemplares debería tener este libro
                    libroActual.setCantidadEsperada(ejemplares);
                }
            }
        }
        br.close();

        // Si un libro no tenía ejemplares listados en el archivo,
        // aquí creamos los ejemplares en estado disponible
        for (Libro l : libros.values()) {
            if (l.getEjemplares().isEmpty()) {
                for (int i = 1; i <= l.getCantidadEsperada(); i++) {
                    l.addEjemplar(new Ejemplar(i, 'D', ""));
                }
            }
        }

        System.out.println("BaseDatos cargada desde archivo: " + libros.size() + " libros");
    }

    // Este método guarda el estado actual de los libros en el archivo
    // Escribe primero la cabecera de cada libro, luego sus ejemplares
    public void guardarEnArchivo(String ruta) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(ruta));
        for (Libro libro : libros.values()) {
            bw.write(libro.getNombre() + ", " + libro.getCodigo() + ", " + libro.getEjemplares().size());
            bw.newLine();
            for (Ejemplar ej : libro.getEjemplares()) {
                bw.write(ej.getId() + ", " + ej.getEstado() + ", " + ej.getFecha());
                bw.newLine();
            }
        }
        bw.close();
        System.out.println("BaseDatos guardada en archivo: " + ruta);
    }

    // Aquí comienzan las operaciones sobre los libros
    // Cada operación busca el libro por su código y luego hace la acción
    // El bloque synchronized garantiza que dos actores no modifiquen el mismo libro al mismo tiempo

    // Devolver un ejemplar prestado
    public boolean devolverEjemplar(String codigo) {
        Libro l = libros.get(codigo);
        if (l == null) return false;
        synchronized (l) {
            return l.devolver();
        }
    }

    // Renovar un ejemplar prestado
    public boolean renovarPrestamo(String codigo, String usuarioId, String nuevaFecha) {
        Libro l = libros.get(codigo);
        if (l == null) return false;
        synchronized (l) {
            return l.renovar();
        }
    }

    // Prestar un ejemplar disponible
    public boolean prestarEjemplar(String codigo) {
        Libro l = libros.get(codigo);
        if (l == null) return false;
        synchronized (l) {
            return l.prestar();
        }
    }

    // Este método devuelve un resumen en texto del estado de la base de datos
    // Muestra cuántos ejemplares están disponibles y cuántos prestados por cada libro
    public String dumpResumen() {
        StringBuilder sb = new StringBuilder();
        for (Libro l : libros.values()) {
            long disponibles = l.getEjemplares().stream().filter(e -> e.getEstado() == 'D').count();
            long prestados = l.getEjemplares().stream().filter(e -> e.getEstado() == 'P').count();
            sb.append(l.getCodigo())
              .append(" - ").append(l.getNombre())
              .append(": disponibles=").append(disponibles)
              .append(", prestados=").append(prestados)
              .append("\n");
        }
        return sb.toString();
    }
}
