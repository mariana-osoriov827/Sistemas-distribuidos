/**************************************************************************************
 * Fecha: 10/10/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema:
 * - Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Interfaz RMI del Gestor de Carga (GC) con métodos asíncronos (devolver/renovar/prestar).
 ***************************************************************************************/
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BibliotecaGC extends Remote {

    /**
     * Publica una devolución en la cola correspondiente.
     * - Crea un Message con ID único, lo encola y marca su estado como "PENDING".
     */
    String devolverLibroAsync(String codigoLibro, String usuarioId) throws RemoteException;

    /**
     * Publica una renovación en la cola correspondiente.
     * - Calcula una nuevaFecha (ejemplo: +1 semana) y la añade al Message.
     */
    String renovarLibroAsync(String codigoLibro, String usuarioId) throws RemoteException;

    /**
     * Publica un préstamo en la cola correspondiente.
     */
    String prestarLibroAsync(String codigoLibro, String usuarioId) throws RemoteException; // nuevo

    /**
     * Entrega el siguiente mensaje disponible para un tópico.
     * - Si no hay mensajes en la cola devuelve null.
     * - Observación: fetchNextMessage realiza poll() sin bloqueo
     */
    Message fetchNextMessage(String topic) throws RemoteException;

    /**
     * Actualiza el estado del mensaje cuando el Actor responde (ACK).
     * - success = true -> "OK"
     * - success = false -> "FAILED"
     */
    void ackMessage(String messageId, boolean success) throws RemoteException;

    /**
     * Permite a un cliente consultar el estado actual de un mensaje por su ID.
     */
    String getMessageStatus(String messageId) throws RemoteException;

    /**
     * Método auxiliar para indicar el endpoint del Gestor de Almacenamiento 
     */
    void setGestorAlmacenamientoEndpoint(String host) throws RemoteException;

}
