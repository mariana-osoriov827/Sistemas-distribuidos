/**************************************************************************************
* Fecha: 05/09/2025
* Autores: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Gestión de datos de la biblioteca en Java
* Descripción:
* - Contiene la lógica para manejar la base de datos de libros y ejemplares.
* - Permite cargar y guardar la información desde y en archivos de texto.
* - Implementa las operaciones de negocio: procesar préstamos, devoluciones y renovaciones.
* - Se usa el método synchronized para evitar condiciones de carrera en las operaciones de préstamo,
    devolución y renovación.
* - Provee reportes del estado actual de la biblioteca (disponibilidad y fechas).
* - Utiliza clases auxiliares (Libro, Ejemplar) para estructurar la información.
**************************************************************************************/
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Clase BaseDatos.
 * Gestiona el almacenamiento y las operaciones sobre los libros y sus ejemplares.
 * Actúa como la "base de datos" de la biblioteca, cargando y guardando información en archivos de texto.
 */
public class BaseDatos {

    private List<Libro> biblioteca; // Lista principal que contiene todos los libros de la biblioteca

    /**
     * Constructor de BaseDatos.
     * Inicializa la lista de libros vacía.
     */
    public BaseDatos() {
        biblioteca = new ArrayList<>();
    }

     /**
      * Carga la base de datos desde un archivo de texto.
      * El formato del archivo es:
      *   - Línea con: nombre, isbn, cantidad_ejemplares
      *   - Luego N líneas (ejemplares): numero, estado, fecha
      * @param filename Ruta del archivo de entrada.
      * @throws IOException Si ocurre un error en la lectura del archivo.
      */
    public void cargarBD(String filename) throws IOException {

      try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

        String linea;

        while ((linea = br.readLine()) != null) {

          // Si la línea no tiene comas, no se considera válida
          if (!linea.contains(",")) 
            continue;
          
          // Parseo de los datos principales del libro
          String[] parts = linea.split(",");
          String nombre = parts[0].trim();
          int isbn = Integer.parseInt(parts[1].trim());
          int total = Integer.parseInt(parts[2].trim());

          // Se crea un nuevo objeto Libro
          Libro libro = new Libro(nombre, isbn);

          // Se leen N ejemplares (tantos como el total indicado en la cabecera)
          for (int i = 0; i < total; i++) {
            linea = br.readLine();
            if (linea == null)
              break;

            String[] datos = linea.split(",");
            int num = Integer.parseInt(datos[0].trim());
            char status = datos[1].trim().charAt(0); // Puede ser 'D' (disponible) o 'P' (prestado)
            String fecha = datos[2].trim();

            // Se agrega el ejemplar a la lista de ejemplares del libro
            libro.agregarEjemplar(new Ejemplar(num, status, fecha));

          }

          // Se agrega el libro completo a la biblioteca
          biblioteca.add(libro);

        }
      }
    }

    /**
     * Guarda la base de datos en un archivo de texto con el mismo formato de cargarBD().
     * @param filename Ruta del archivo de salida.
     * @throws IOException Si ocurre un error en la escritura del archivo.
     */
    public void guardarBD(String filename) throws IOException {

      try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
        for (Libro l : biblioteca) {
          // Cabecera del libro
          pw.printf("%s, %d, %d%n", l.getNombre(), l.getIsbn(), l.getEjemplares().size());

          // Datos de cada ejemplar
          for (Ejemplar e : l.getEjemplares())
            pw.printf("%d, %c, %s%n", e.getNumero(), e.getStatus(), e.getFecha());
        }
      }
      
    }

    /**
     * Busca un libro en la biblioteca por nombre e ISBN.
     * @param nombre Nombre del libro.
     * @param isbn Código ISBN del libro.
     * @return El objeto Libro encontrado o null si no existe.
     */
    private Libro buscarLibro(String nombre, int isbn) {

      for (Libro l : biblioteca)
          if (l.getIsbn() == isbn && l.getNombre().equals(nombre))
              return l;

      return null;

    }

  // ============= MÉTODOS DE NEGOCIO =============

    /**
     * Procesa el préstamo de un libro.
     * Cambia el estado de un ejemplar disponible ('D') a prestado ('P')
     * y asigna una fecha de devolución 7 días en el futuro.
     */
    public String procesarPrestamo(String nombre, int isbn) {

      Libro l = buscarLibro(nombre, isbn);
      if (l == null) 
        return "LIBRO NO ENCONTRADO";

      // Sincronización para evitar condiciones de carrera al acceder a los ejemplares
      synchronized (l) { 
        for (Ejemplar e : l.getEjemplares())
            if (e.getStatus() == 'D') {
                e.setStatus('P');
                e.setFecha(obtenerFechaFutura(7));
              return "OK";
          }
      }
      
      return "NO DISPONIBLE";

    }

    /**
     * Procesa la devolución de un libro.
     * Cambia el estado de un ejemplar prestado ('P') a disponible ('D')
     * y actualiza la fecha a la fecha actual.
     */
    public String procesarDevolucion(String nombre, int isbn) {

      Libro l = buscarLibro(nombre, isbn);
      if (l == null) 
        return "LIBRO NO ENCONTRADO";

      synchronized (l) { // Sincronización para evitar condiciones de carrera al acceder a los ejemplares
        for (Ejemplar e : l.getEjemplares())
            if (e.getStatus() == 'P') {
                e.setStatus('D');
                e.setFecha(obtenerFechaActual());
              return "OK";
          }
      }

      return "NO APLICABLE";

    }

    /**
     * Procesa la renovación de un libro prestado.
     * Extiende la fecha de devolución por 7 días adicionales.
     */
    public String procesarRenovacion(String nombre, int isbn) {

      Libro l = buscarLibro(nombre, isbn);
      if (l == null) 
        return "LIBRO NO ENCONTRADO";

      synchronized (l) {
        for (Ejemplar e : l.getEjemplares())
            if (e.getStatus() == 'P') {
                e.setFecha(obtenerFechaFutura(7));
                return "OK";
            }
      }
       
      return "NO APLICABLE";

    }

    /**
     * Genera un reporte de todos los ejemplares de la biblioteca.
     * Cada línea contiene: estado, nombre, isbn, número de ejemplar, fecha.
     */
    public List<String> mostrarReporte() {

      List<String> reporte = new ArrayList<>();

      for (Libro l : biblioteca)
          for (Ejemplar e : l.getEjemplares())
              reporte.add(String.format("%c, %s, %d, %d, %s", e.getStatus(), l.getNombre(), l.getIsbn(), e.getNumero(), e.getFecha()));
        
      return reporte;

    }

  // ============= MÉTODOS AUXILIARES =============

    /**
     * Obtiene la fecha actual en formato dd-MM-yyyy.
     */
    private String obtenerFechaActual() {

      SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
      return sdf.format(new Date());

    }

    /**
     * Calcula la fecha futura sumando un número de días a la fecha actual.
     * @param dias Número de días a sumar.
     * @return Fecha futura en formato dd-MM-yyyy.
     */
    private String obtenerFechaFutura(int dias) {

      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_YEAR, dias);
      SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

      return sdf.format(cal.getTime());

    }

}
