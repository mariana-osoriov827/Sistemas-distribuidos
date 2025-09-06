/**************************************************************************************
* Fecha: 06/09/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Programación de Sockets
* Descripción:
* - Este programa es un cliente UDP que se conecta a un servidor en el puerto 6000 para enviar
* mensajes como datagramas. La comunicación es sin conexión y se envía un paquete por cada mensaje.
***************************************************************************************/

import java.net.*;
import java.io.*;

// La clase principal del cliente UDP.
public class socketudpcli {
   
    // El método principal, punto de entrada del programa.
    public static void main(String argv[]) {
      
      // Verifica si se ha proporcionado un argumento (la dirección del servidor).
      if (argv.length == 0) {
         System.err.println("Java socketudpcli servidor");
         System.exit(1);
      }

      // Objeto para leer la entrada de texto desde la consola.
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

      System.out.println("Prueba de sockets UDP (cliente)");
      
      // Se declaran las variables para el socket, la dirección, los datos y el paquete.
      DatagramSocket socket;
      InetAddress address;
      byte[] mensaje_bytes = new byte[256];
      String mensaje = "";
      DatagramPacket paquete;

      // Inicialización redundante, ya que la variable se sobrescribe más adelante.
      mensaje_bytes = mensaje.getBytes();
      
      // Bloque try-catch para manejar posibles excepciones.
      try {
         // Crea un DatagramSocket para enviar y recibir datos.
         System.out.print("Creando socket... ");
         socket = new DatagramSocket();
         System.out.println("ok");

         // Convierte la dirección del servidor (proporcionada como argumento) a un objeto InetAddress.
         System.out.print("Capturando direccion de host... ");
         address = InetAddress.getByName(argv[0]);
         System.out.println("ok");

         System.out.println("Introduce mensajes a enviar:");

         // Bucle para leer mensajes del usuario y enviarlos al servidor.
         do {
            // Lee la entrada del usuario.
            mensaje = in.readLine();
            
            // Convierte la cadena del mensaje en un arreglo de bytes.
            mensaje_bytes = mensaje.getBytes();
            
            // Crea un paquete de datos con el mensaje, su longitud, la dirección del destino y el puerto (6000).
            paquete = new DatagramPacket(mensaje_bytes, mensaje.length(), address, 6000);
            
            // Envía el paquete a través del socket.
            socket.send(paquete);
         } while (!mensaje.startsWith("fin")); // El bucle se detiene cuando el mensaje comienza con "fin".
      }
      // Captura y maneja cualquier excepción que ocurra.
      catch (Exception e) {
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }
}