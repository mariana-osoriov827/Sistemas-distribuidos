package Gestor_carga;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Actor genérico para manejar DEVOLUCION y RENOVACION.
 * - Patrón ZMQ: SUB para recibir peticiones del GC.
 * - Patrón ZMQ: REQ para enviar peticiones al GA (con failover).
 * - Patrón ZMQ: PUSH para enviar el resultado de vuelta al GC.
 */
public class ActorClient_ZMQ {

    private final String gcPubAddress;
    private final String operationType;
    private final String[] gaHosts;
    private final int[] gaPorts;
    private final String gcResultIp;
    private int currentGaIndex = 0;

    public ActorClient_ZMQ(String gcPubAddress, String gaList, String operationType, String gcResultIp) {
        this.gcPubAddress = gcPubAddress;
        this.operationType = operationType;
        this.gcResultIp = gcResultIp;
        // Parsear la lista de GAs para failover
        String[] gaServers = gaList.split(",");
        this.gaHosts = new String[gaServers.length];
        this.gaPorts = new int[gaServers.length];
        for (int i = 0; i < gaServers.length; i++) {
            String[] parts = gaServers[i].trim().split(":");
            this.gaHosts[i] = parts[0];
            this.gaPorts[i] = Integer.parseInt(parts[1]);
        }
        System.out.println("Actor configurado con " + gaServers.length + " GA(s):");
        for (int i = 0; i < gaServers.length; i++) {
            System.out.println("  GA" + (i + 1) + ": " + gaServers[i]);
        }
        System.out.println("Actor reportará resultados en tcp://" + gcResultIp + ":5557");
    }

    public void iniciar() {
        try (ZContext context = new ZContext()) {
            // 1. Socket SUB para recibir peticiones del GC
            ZMQ.Socket subscriber = context.createSocket(ZMQ.SUB);
            subscriber.connect("tcp://" + gcPubAddress);
            subscriber.subscribe(operationType.getBytes(ZMQ.CHARSET));
            System.out.println("Actor suscrito a tópico " + operationType + " en " + gcPubAddress);

            // 2. Socket PUSH para enviar resultados de vuelta al GC (Puerto 5557)
            ZMQ.Socket pusher = context.createSocket(ZMQ.PUSH);
            pusher.connect("tcp://" + gcResultIp + ":5557");
            System.out.println("Actor reportará resultados en tcp://" + gcResultIp + ":5557");

            System.out.println("Actor conectado a " + gaHosts.length + " GAs.");
            while (!Thread.currentThread().isInterrupted()) {
                // Recibir mensaje completo (topic y cuerpo)
                String receivedTopic = subscriber.recvStr(); 
                String request = subscriber.recvStr();
                if (request == null) continue;
                System.out.println("Actor " + operationType + " recibió: " + request);
                // Formato esperado: TOPIC|codigoLibro|userId|messageId
                String[] parts = request.split("\\|", 4);
                if (parts.length < 4) {
                    System.err.println("Mensaje incompleto: " + request);
                    continue;
                }
                String codigoLibro = parts[1];
                String userId = parts[2];
                String messageId = parts[3];

                String result;
                // --- 1. Validar con GA si el usuario tiene el libro prestado (para DEVOLUCION y RENOVACION) ---
                String validationRequest = "VALIDAR_PRESTAMO|" + codigoLibro + "|" + userId;
                String validationResponse = sendAndReceiveGA_TCP(validationRequest);
                if (validationResponse.startsWith("OK|true")) {
                    // --- 2. Si tiene el libro, realizar la operación real ---
                    String gaRequest = operationType + "|" + codigoLibro + "|" + userId;
                    String gaResponse = sendAndReceiveGA_TCP(gaRequest);
                    if (gaResponse.startsWith("OK")) {
                        result = "EXITO|" + (operationType.equals("DEVOLUCION") ? "Devolución" : "Renovación") + " de libro " + codigoLibro + " para usuario " + userId + ".";
                        System.out.println("Actor " + operationType + ": Exito! " + result);
                    } else {
                        result = gaResponse;
                        System.out.println("Actor " + operationType + ": Fallo en operación. " + result);
                    }
                } else if (validationResponse.startsWith("OK|false")) {
                    result = "FAILED|El usuario no tiene el libro prestado";
                    System.out.println("Actor " + operationType + ": Usuario no tiene el libro prestado.");
                } else if (validationResponse.startsWith("ERROR|Timeout")) {
                    result = "ERROR|Timeout de comunicación con GA. Intente de nuevo.";
                    System.err.println("Actor " + operationType + ": Fallo por timeout con GA.");
                } else {
                    result = validationResponse;
                    System.out.println("Actor " + operationType + ": Fallo por validación GA. " + result);
                }
                // --- 3. Procesar respuesta y enviar al GC (PUSH) ---
                String finalResult = receivedTopic + "|" + messageId + "|" + result;
                pusher.send(finalResult);
                System.out.println("Resultado enviado al GC: " + finalResult);
            }
        } catch (Exception e) {
            System.err.println("Error en el Actor " + operationType + ": " + e.getMessage());
            // e.printStackTrace(); // Eliminado para evitar advertencia de lint
        }
    }

    // Comunicación con GA usando sockets Java estándar (failover incluido)
    private String sendAndReceiveGA_TCP(String request) {
        int intentos = 0;
        int maxIntentos = gaHosts.length * 2;
        final int gaTimeoutMs = 3000;
        while (intentos < maxIntentos) {
            String gaHost = gaHosts[currentGaIndex];
            int gaPort = gaPorts[currentGaIndex];
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(gaHost, gaPort), gaTimeoutMs);
                socket.setSoTimeout(gaTimeoutMs);
                PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                out.println(request);
                String reply = in.readLine();
                if (reply != null) {
                    System.out.println("[INFO Actor " + operationType + "] Éxito con GA " + gaHost + ":" + gaPort);
                    return reply;
                }
                System.err.println("[FAILOVER Actor " + operationType + "] GA " + gaHost + ":" + gaPort + " no responde a tiempo. Rotando...");
            } catch (Exception e) {
                System.err.println("[FAILOVER Actor " + operationType + "] Error en GA " + gaHost + ":" + gaPort + ": " + e.getMessage());
            }
            currentGaIndex = (currentGaIndex + 1) % gaHosts.length;
            intentos++;
        }
        return "ERROR|Timeout de comunicación con GA (después de " + maxIntentos + " intentos)";
    }
    
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Uso: java ActorClient_ZMQ <gcHost:pubPort> <gaHost1:port1[,gaHost2:port2]> <topic> <gcResultIp>");
            System.out.println("Ejemplo: java ActorClient_ZMQ localhost:5555 localhost:5560,10.43.102.177:6560 DEVOLUCION 10.43.103.49");
            System.exit(1);
        }
        try {
            String gcPubAddress = args[0];
            String gaList = args[1];
            String operationType = args[2].toUpperCase();
            String gcResultIp = args[3];
            if (!operationType.equals("DEVOLUCION") && !operationType.equals("RENOVACION")) {
                System.out.println("Error: tópico debe ser DEVOLUCION o RENOVACION");
                System.exit(1);
            }
            ActorClient_ZMQ actor = new ActorClient_ZMQ(gcPubAddress, gaList, operationType, gcResultIp);
            actor.iniciar();
        } catch (Exception e) {
            System.err.println("Error fatal en Actor Client: " + e.getMessage());
            // e.printStackTrace(); // Eliminado para evitar advertencia de lint
        }
    }
}