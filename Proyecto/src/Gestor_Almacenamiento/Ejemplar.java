/**************************************************************************************
* Fecha: 10/10/2025
* Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
* Tema: 
* - Proyecto préstamo de libros (Sistema Distribuido)
* Descripción:
* - Clase Entidad (Ejemplar):
* - Representa una copia física única de un libro.
* - Almacena el estado ('D' = Disponible, 'P' = Prestado) y la fecha de devolución si está prestado.
* - Implementa Serializable para permitir la transferencia de objetos a través de 
* RMI y para la persistencia en el Gestor de Almacenamiento (GA).
***************************************************************************************/
import java.io.Serializable;

public class Ejemplar implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private char estado; 
    private String fecha;
    private int contadorRenovaciones; // Máximo 2 renovaciones permitidas

    public Ejemplar(int id, char estado, String fecha) {
        this.id = id;
        this.estado = estado;
        this.fecha = fecha;
        this.contadorRenovaciones = 0;
    }

    public int getId() { return id; }
    public char getEstado() { return estado; }
    public String getFecha() { return fecha; }
    public int getContadorRenovaciones() { return contadorRenovaciones; }

    public void setEstado(char estado) { 
        this.estado = estado;
        // Resetear contador de renovaciones cuando se devuelve
        if (estado == 'D') {
            this.contadorRenovaciones = 0;
        }
    }
    
    public void setFecha(String fecha) { this.fecha = fecha; }
    
    public boolean puedeRenovar() {
        return contadorRenovaciones < 2;
    }
    
    public void incrementarRenovaciones() {
        this.contadorRenovaciones++;
    }
}

