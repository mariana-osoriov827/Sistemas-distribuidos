/**************************************************************************************
* Fecha: 10/10/2025
* Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
* Tema: 
* - Proyecto préstamo de libros (Sistema Distribuido)
* Descripción:
* - Clase Entidad (Libro):
* - Representa un tipo de libro por su código y nombre.
* - Contiene una lista de objetos Ejemplar, que son las copias físicas.
* - Implementa la lógica de negocio para las operaciones de préstamo, devolución y 
* renovación sobre sus ejemplares.
* - Todos los métodos de operación son sincronizados (`synchronized`) para 
* asegurar la integridad de los datos de sus ejemplares ante accesos concurrentes.
***************************************************************************************/

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Libro implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String codigo;
    private final String nombre;
    private final List<Ejemplar> ejemplares = new ArrayList<>();

    // cantidad esperada según libros.txt
    private int cantidadEsperada = 0;

    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Libro(String codigo, String nombre, int cantidad) {
        this.codigo = codigo;
        this.nombre = nombre;
        for (int i = 1; i <= cantidad; i++) {
            ejemplares.add(new Ejemplar(i, 'D', ""));
        }
        this.cantidadEsperada = cantidad;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public List<Ejemplar> getEjemplares() { return ejemplares; }

    public void addEjemplar(Ejemplar ej) {
        ejemplares.add(ej);
    }

    public void setCantidadEsperada(int n) { this.cantidadEsperada = n; }
    public int getCantidadEsperada() { return cantidadEsperada; }

    /**
     * - Busca el primer ejemplar disponible ('D'), lo marca como prestado ('P')
     *   y asigna una fecha de entrega (7 días a partir de hoy).
     */
    public synchronized boolean prestar() {
        for (Ejemplar ej : ejemplares) {
            if (ej.getEstado() == 'D') {
                ej.setEstado('P');
                String fechaEntrega = LocalDate.now().plusDays(7).format(fmt);
                ej.setFecha(fechaEntrega);
                return true;
            }
        }
        return false;
    }

    /**
     * - Busca un ejemplar marcado como prestado ('P'), lo marca como disponible ('D')
     *   y limpia la fecha.
     */
    public synchronized boolean devolver() {
        for (Ejemplar ej : ejemplares) {
            if (ej.getEstado() == 'P') {
                ej.setEstado('D');
                ej.setFecha("");
                return true;
            }
        }
        return false;
    }

    /**
     * - Extiende la fecha de un ejemplar prestado por 7 días.
     * - No hace seguimiento de contadores de renovación por usuario.
     */
    public synchronized boolean renovar() {
        for (Ejemplar ej : ejemplares) {
            if (ej.getEstado() == 'P') {
                String nuevaFecha = LocalDate.now().plusDays(7).format(fmt);
                ej.setFecha(nuevaFecha);
                return true;
            }
        }
        return false;
    }
}