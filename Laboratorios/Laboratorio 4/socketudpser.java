/**************************************************************************************
* Fecha: 06/09/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Programación de Sockets
* Descripción:
* - Este programa es un servidor UDP que se enlaza al puerto 6000 para recibir datagramas
* de clientes y mostrar el contenido de los mensajes en la consola. La recepción de
* mensajes continúa hasta que se recibe un mensaje que comienza con "fin".
***************************************************************************************/

import java.net.*;
import java.io.*;

// Clase principal del servidor UDP.
public class socketudpser {
   
    // Método principal, punto de entrada del programa.
    public static void main(String argv[]) {
      
      // Muestra un mensaje para indicar que el servidor está en funcionamiento.
      System.out.println("Prueba de sockets UDP (servidor)");
      
      // Se declaran las variables para el socket y la bandera de finalización.
      DatagramSocket socket;
      boolean fin = false;

      // El bloque try-catch se utiliza para manejar excepciones.
      try {
         // Se crea un DatagramSocket y se enlaza al puerto 6000 para escuchar.
         System.out.print("Creando socket... ");
         socket = new DatagramSocket(6000);
         System.out.println("ok");

         System.out.println("Recibiendo mensajes... ");
         
         // Inicia un bucle para recibir continuamente paquetes.
         do {
            // Se crea un array de bytes para almacenar los datos del datagrama recibido.
            byte[] mensaje_bytes = new byte[256];
            
            // Se crea un DatagramPacket vacío para recibir el paquete del cliente.
            DatagramPacket paquete = new DatagramPacket(mensaje_bytes, 256);
            
            // Espera (se bloquea) hasta que reciba un datagrama. Los datos se copian en 'paquete'.
            socket.receive(paquete);
            
            // Se declara una variable para el mensaje.
            String mensaje = "";
            
            // Se crea una nueva cadena a partir de los bytes recibidos.
            mensaje = new String(mensaje_bytes);
            
            // Muestra el mensaje recibido en la consola.
            System.out.println(mensaje);
            
            // Se comprueba si el mensaje recibido comienza con "fin". Si es así, se activa la bandera de finalización.
            if (mensaje.startsWith("fin")) fin = true;
         } while (!fin); // El bucle se detiene cuando 'fin' es verdadero.
      }
      // Este bloque captura y maneja cualquier excepción que ocurra.
      catch (Exception e) {
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }
}