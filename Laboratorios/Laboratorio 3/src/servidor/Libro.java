/**************************************************************************************
* Fecha: 05/09/2025
* Autores: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Modelo de libro en la biblioteca
* Descripción:
* - Representa un libro identificado por su nombre y su código ISBN.
* - Mantiene una lista de ejemplares asociados a ese libro.
* - Provee métodos para acceder a sus atributos y para añadir nuevos ejemplares.
**************************************************************************************/

import java.util.ArrayList;
import java.util.List;

public class Libro {

  // Nombre del libro
  private String nombre;

  // Código ISBN del libro
  private int isbn;

  // Lista de ejemplares asociados a este libro
  private List<Ejemplar> ejemplares;

  /**
   * Constructor de Libro.
   * Inicializa un libro con su nombre e ISBN y crea una lista vacía de ejemplares.
   * @param nombre Nombre del libro.
   * @param isbn Código ISBN del libro.
   */
  public Libro(String nombre, int isbn) {
    this.nombre = nombre;
    this.isbn = isbn;
    this.ejemplares = new ArrayList<>();
  }

  /**
   * Agrega un ejemplar a la lista de ejemplares del libro.
   * @param e Objeto de tipo Ejemplar a añadir.
   */
  public void agregarEjemplar(Ejemplar e) {
    ejemplares.add(e);
  }

  // ============== GETTERS ==============
  public String getNombre() { return nombre; }
  public int getIsbn() { return isbn; }
  public List<Ejemplar> getEjemplares() { return ejemplares; }

}
