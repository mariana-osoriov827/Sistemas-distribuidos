/**************************************************************************************
 * Fecha: 17/11/2025
 * Autor: Gabriel Jaramillo, Roberth Méndez, Mariana Osorio Vasquez, Juan Esteban Vera
 * Tema: Proyecto préstamo de libros (Sistema Distribuido)
 * Descripción:
 * - Cliente usando ZeroMQ para enviar peticiones al GC
 * - Usa REQ para préstamos (síncronos)
 * - Usa REQ también para devoluciones/renovaciones (recibe ACK inmediato)
 ***************************************************************************************/
import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import java.io.*;
import java.time.LocalDate;

public class ClienteBatch_ZMQ {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java ClienteBatch_ZMQ <archivo_peticiones> <gcHost:repPort>");
            System.out.println("Ejemplo: java ClienteBatch_ZMQ peticiones.txt localhost:5556");
            System.exit(1);
        }
        
        String archivo = args[0];
        String[] gcParts = args[1].split(":");
        String gcHost = gcParts[0];
        int gcRepPort = Integer.parseInt(gcParts[1]);
        
        try (ZContext context = new ZContext()) {
            
            // Socket REQ para enviar solicitudes al GC
            ZMQ.Socket requester = context.createSocket(ZMQ.REQ);
            requester.connect("tcp://" + gcHost + ":" + gcRepPort);
            System.out.println("Cliente conectado al GC en " + gcHost + ":" + gcRepPort);
            
            BufferedReader br = new BufferedReader(new FileReader(archivo));
            String linea;
            int contador = 0;
            
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                
                String[] parts = linea.split("\\|");
                if (parts.length < 3) continue;
                
                String tipo = parts[0].trim();
                String codigo = parts[1].trim();
                String usuario = parts[2].trim();
                
                // Enviar solicitud al GC
                String request = tipo.toUpperCase() + "|" + codigo + "|" + usuario;
                requester.send(request);
                System.out.println(LocalDate.now() + " -> " + tipo + " enviada (" + codigo + ")");
                
                // Recibir respuesta del GC
                String response = requester.recvStr();
                
                if (response.startsWith("OK")) {
                    System.out.println("[OK] Operación " + tipo + " sobre " + codigo + " procesada con éxito.");
                } else if (response.startsWith("ERROR") || response.startsWith("FAILED")) {
                    System.out.println("[ERROR] Operación " + tipo + " sobre " + codigo + " falló.");
                } else {
                    System.out.println("[INFO] Respuesta: " + response);
                }
                
                contador++;
                
                // Pequeña pausa entre envíos
                Thread.sleep(200);
            }
            
            br.close();
            System.out.println("\nClienteBatch_ZMQ finalizado. Total operaciones: " + contador);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
