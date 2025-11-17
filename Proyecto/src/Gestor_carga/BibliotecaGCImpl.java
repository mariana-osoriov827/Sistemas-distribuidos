/**************************************************************************************
 * Fecha: 10/10/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema:
 * - Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Implementación del Gestor de Carga (GC).
 * - Gestiona colas separadas por tópico (Devolución, Renovación, Préstamo) usando
 *   estructuras concurrentes (ConcurrentLinkedQueue, ConcurrentHashMap).
 * - Mantiene el estado de los mensajes para permitir polling por parte de los clientes.
 ***************************************************************************************/
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * BibliotecaGCImpl
 *
 * - Publica mensajes asíncronos: cada publicación devuelve inmediatamente un UUID.
 * - Los Actores consumen los mensajes usando fetchNextMessage(topic).
 * - Cuando un Actor confirma el procesamiento, se actualiza messageStatus.
 * - messageStatus usa valores: "PENDING", "OK", "FAILED", "UNKNOWN".
 */
public class BibliotecaGCImpl extends UnicastRemoteObject implements BibliotecaGC {

    private static final long serialVersionUID = 1L;

    private final ConcurrentLinkedQueue<Message> colaDevolucion = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Message> colaRenovacion = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Message> colaPrestamo = new ConcurrentLinkedQueue<>();

    // Mapas para seguimiento del mensaje y su estado
    private final Map<String, Message> messagesMap = new ConcurrentHashMap<>();
    private final Map<String, String> messageStatus = new ConcurrentHashMap<>();

    private final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

    protected BibliotecaGCImpl() throws RemoteException {
        super();
    }

    /**
     * Publica una devolución en la cola correspondiente.
     * - Crea un Message con ID único, lo encola y marca su estado como "PENDING".
     */
    @Override
    public String devolverLibroAsync(String codigoLibro, String usuarioId) throws RemoteException {
        String id = UUID.randomUUID().toString();
        String fecha = LocalDate.now().format(fmt);
        Message m = new Message(id, "Devolucion", codigoLibro, usuarioId, fecha, null);
        colaDevolucion.add(m);
        messagesMap.put(id, m);
        messageStatus.put(id, "PENDING");
        System.out.println("GC publicó devolución: " + m);
        return id;
    }

    /**
     * Publica una renovación en la cola correspondiente.
     * - Calcula una nuevaFecha (ejemplo: +1 semana) y la añade al Message.
     */
    @Override
    public String renovarLibroAsync(String codigoLibro, String usuarioId) throws RemoteException {
        String id = UUID.randomUUID().toString();
        String fecha = LocalDate.now().format(fmt);
        String nuevaFecha = LocalDate.now().plusWeeks(1).format(fmt);
        Message m = new Message(id, "Renovacion", codigoLibro, usuarioId, fecha, nuevaFecha);
        colaRenovacion.add(m);
        messagesMap.put(id, m);
        messageStatus.put(id, "PENDING");
        System.out.println("GC publicó renovación: " + m);
        return id;
    }

    /**
     * Publica un préstamo en la cola correspondiente.
     */
    @Override
    public String prestarLibroAsync(String codigoLibro, String usuarioId) throws RemoteException {
        String id = UUID.randomUUID().toString();
        String fecha = LocalDate.now().format(fmt);
        Message m = new Message(id, "Prestamo", codigoLibro, usuarioId, fecha, null);
        colaPrestamo.add(m);
        messagesMap.put(id, m);
        messageStatus.put(id, "PENDING");
        System.out.println("GC publicó préstamo: " + m);
        return id;
    }

    /**
     * Entrega el siguiente mensaje disponible para un tópico.
     * - Si no hay mensajes en la cola devuelve null.
     * - Observación: fetchNextMessage realiza poll() sin bloqueo
     */
    @Override
    public Message fetchNextMessage(String topic) throws RemoteException {
        Message m = null;
        if ("Devolucion".equalsIgnoreCase(topic)) {
            m = colaDevolucion.poll();
        } else if ("Renovacion".equalsIgnoreCase(topic)) {
            m = colaRenovacion.poll();
        } else if ("Prestamo".equalsIgnoreCase(topic)) {
            m = colaPrestamo.poll();
        }
        if (m != null) System.out.println("GC envía mensaje al actor (" + topic + "): " + m);
          return m;
    }

    /**
     * Actualiza el estado del mensaje cuando el Actor responde (ACK).
     * - success = true -> "OK"
     * - success = false -> "FAILED"
     */
    @Override
    public void ackMessage(String messageId, boolean success) throws RemoteException {
        messageStatus.put(messageId, success ? "OK" : "FAILED");
        System.out.println("GC recibió confirmación: " + messageId + " -> " + messageStatus.get(messageId));
    }

    /**
     * Permite a un cliente consultar el estado actual de un mensaje por su ID.
     */
    @Override
    public String getMessageStatus(String messageId) throws RemoteException {
        return messageStatus.getOrDefault(messageId, "UNKNOWN");
    }

    /**
     * Método auxiliar para indicar el endpoint del Gestor de Almacenamiento 
     */
    @Override
    public void setGestorAlmacenamientoEndpoint(String host) throws RemoteException {
        System.out.println("Gestor de almacenamiento configurado: " + host);
    }
}
