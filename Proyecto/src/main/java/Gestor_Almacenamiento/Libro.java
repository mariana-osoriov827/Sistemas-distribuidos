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
package Gestor_Almacenamiento;

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
     *   y asigna una fecha de entrega (14 días a partir de hoy según requisitos).
     */
    public synchronized boolean prestar() {
        for (Ejemplar ej : ejemplares) {
            if (ej.getEstado() == 'D') {
                ej.setEstado('P');
                // Préstamo por 2 semanas (14 días) según requisitos del proyecto
                String fechaEntrega = LocalDate.now().plusDays(14).format(fmt);
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
     * - Valida que no se superen las 2 renovaciones máximas permitidas.
     */
    public synchronized boolean renovar(String usuarioId) {
        for (Ejemplar ej : ejemplares) {
            if (ej.getEstado() == 'P' && usuarioId != null && usuarioId.equals(ej.getUsuarioActual())) {
                // Validar que no se hayan superado las 2 renovaciones
                if (!ej.puedeRenovar()) {
                    System.out.println("Renovación denegada: máximo de 2 renovaciones alcanzado");
                    return false;
                }
                String nuevaFecha = LocalDate.now().plusDays(7).format(fmt);
                ej.setFecha(nuevaFecha);
                ej.incrementarRenovaciones();
                System.out.println("Renovación exitosa. Contador: " + ej.getContadorRenovaciones() + "/2");
                return true;
            }
        }
        return false;
    }
}