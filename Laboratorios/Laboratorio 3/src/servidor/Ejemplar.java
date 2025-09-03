/**************************************************************************************
* Fecha: 05/09/2025
* Autores: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Modelo de ejemplar de libro en la biblioteca
* Descripción:
* - Representa un ejemplar específico de un libro.
* - Cada ejemplar tiene un número, un estado ('P' = prestado, 'D' = disponible) y una fecha
*   asociada (según el estado).
* - Provee métodos para acceder y modificar estos atributos.
**************************************************************************************/

public class Ejemplar {

  // Número único del ejemplar dentro de un libro
  private int numero;

  // Estado del ejemplar: 'P' = Prestado, 'D' = Disponible
  private char status;

    // Fecha asociada: puede ser fecha de devolución o de disponibilidad
  private String fecha;

  /**
   * Constructor de Ejemplar.
   * Inicializa un ejemplar con sus datos principales.
   * @param numero Número identificador del ejemplar.
   * @param status Estado inicial del ejemplar ('P' o 'D').
   * @param fecha Fecha asociada al estado del ejemplar.
   */
  public Ejemplar(int numero, char status, String fecha) {
    this.numero = numero;
    this.status = status;
    this.fecha = fecha;
  }

  // ============== GETTERS ==============
  public int getNumero() { return numero; }
  public char getStatus() { return status; }
  public String getFecha() { return fecha; }
  
  // ============== SETTERS ==============
  public void setStatus(char status) { this.status = status; }
  public void setFecha(String fecha) { this.fecha = fecha; }
    
}
