/**************************************************************************************
* Fecha: 05/09/2025
* Autores: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema:
* - Cliente RMI en Java
* Descripción:
* - Implementa la aplicación cliente que se conecta al servidor RMI de la biblioteca.
* - Establece la conexión con el servicio remoto "BibliotecaService" a través del Registry
*   en el puerto 1099.
* - Permite al usuario seleccionar operaciones disponibles:
*   - Prestar, Devolver, Renovar un libro.
*   - Mostrar un reporte del estado de los ejemplares.
*   - Salir y desconectarse del servidor.
* - Envía las solicitudes al servidor, recibe la respuesta y la imprime en consola.
**************************************************************************************/

import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import java.net.InetAddress;

public class ClienteRMI {

    public static void main(String[] args) {

        try {

            // Se obtiene una referencia al registro RMI con la IP del servidor (VM de la Universidad)y puerto 1099
            Registry registry = LocateRegistry.getRegistry("10.43.103.236", 1099);
            // Se busca en el registro el objeto remoto (stub) con nombre "BibliotecaService" y se castea a la interfaz Biblioteca
            Biblioteca bibliotecaRemota = (Biblioteca) registry.lookup("BibliotecaService");

            // Se obtiene el nombre de usuario del sistema para usarlo como identificador del cliente (puede ser el usuario del SO o un nombre fijo)
            String clienteId = System.getProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostAddress());
            // Se conecta el cliente al servidor usando su identificador
            bibliotecaRemota.conectar(clienteId);

            // Objeto Scanner para leer las operaciones del usuario desde consola
            Scanner sc = new Scanner(System.in);

            while (true) {

                System.out.println("\nSeleccione operación:");
                System.out.println("P - Prestar\nR - Renovar\nD - Devolver\nM - Mostrar reporte\nQ - Salir");
                System.out.print("> ");

                //Se lee el primer caracter de la entrada del usuario y se convierte en mayúscula.
                char op = sc.next().toUpperCase().charAt(0);
                sc.nextLine(); // Se limpia el buffer de lectura

                // Si el usuario elige "Q" se desconecta del servidor y se cierra el cliente
                if (op == 'Q') {
                    bibliotecaRemota.desconectar(clienteId);
                    System.out.println("Cerrando cliente...");
                    break;
                }

                // Si el usuario elige "M" se solicita al servidor el reporte de libros y se imprime
                if (op == 'M') {
                    List<String> rep = bibliotecaRemota.reporte();
                    rep.forEach(System.out::println);
                    continue;
                }

                // Si se llega hasta acá se pide al usuario información del libro para las operaciones P, D y R.
                System.out.print("Nombre del libro: ");
                String nombre = sc.nextLine();
                System.out.print("ISBN: ");
                int isbn = sc.nextInt();
                sc.nextLine(); // Se limpia el buffer de lectura

                // Dependiendo de la operación elegida, se llama al método correspondiente en el servidor
                String resp;
                switch (op) {
                    case 'P':
                      resp = bibliotecaRemota.prestar(nombre, isbn);
                      break;                  
                    case 'D':
                      resp = bibliotecaRemota.devolver(nombre, isbn);
                      break;
                    case 'R':
                      resp = bibliotecaRemota.renovar(nombre, isbn);
                      break;
                    default:
                      resp = "Opción inválida";
                      break;
                };

                // Se imprime la respuesta que devuelve el servidor
                System.out.println("[Respuesta] " + resp);

            }

            // Se cierra la lectura del buffer
            sc.close();

        } catch (Exception e) {
            e.printStackTrace(); // Si ocurre algún error en la comunicación RMI se imprime el stacktrace
        }

    }

}
