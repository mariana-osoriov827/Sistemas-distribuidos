/**************************************************************************************
* Fecha: 10/10/2025
* Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
* Tema: 
* - Proyecto préstamo de libros (Sistema Distribuido)
* Descripción:
* - Clase Entidad de Mensajería (Message):
* - Define la estructura de datos utilizada para encapsular una solicitud de operación dentro del sistema distribuido.
* - Funciona como un evento o un comando en el patrón de mensajería asíncrona.
* - Contiene campos esenciales como el ID (para seguimiento del estado), el Topic (Devolucion o Renovacion), y los datos de la operación (código de libro, ID de usuario).
* - Implementa Serializable para ser transferida por RMI entre el Gestor de Carga 
* y los Actores.
***************************************************************************************/

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String topic;
    private final String codigoLibro;
    private final String usuarioId;
    private final String fechaOperacion;
    private final String nuevaFechaEntrega;

    public Message(String id, String topic, String codigoLibro, String usuarioId, String fechaOperacion, String nuevaFechaEntrega) {
        this.id = id;
        this.topic = topic;
        this.codigoLibro = codigoLibro;
        this.usuarioId = usuarioId;
        this.fechaOperacion = fechaOperacion;
        this.nuevaFechaEntrega = nuevaFechaEntrega;
    }

    public String getId() { return id; }
    public String getTopic() { return topic; }
    public String getCodigoLibro() { return codigoLibro; }
    public String getUsuarioId() { return usuarioId; }
    public String getFechaOperacion() { return fechaOperacion; }
    public String getNuevaFechaEntrega() { return nuevaFechaEntrega; }

    public String toString() {
        return String.format("Message[id=%s,topic=%s,codigo=%s,user=%s,fecha=%s,nueva=%s]",
                id, topic, codigoLibro, usuarioId, fechaOperacion, nuevaFechaEntrega);
    }
}
