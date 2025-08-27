import java.rmi.*; // Permite ejecutar métodos de objetos ubicados en otra máquina con Remote Method Invocation
import java.rmi.registry.LocateRegistry; //Permite crear o obtener una referencia al registro RMI.

public class SumadorServer {

    public static void main (String args[]) {

        try {

            // Crear instancia del objeto remoto
            SumadorImpl obj = new SumadorImpl();

            /*
             * Inicia el registro RMI en el puerto 1099 (el puerto por defecto de RMI).
             * Esto hace que no sea necesario arrancar "rmiregistry" manualmente desde la terminal.
             */ 
            LocateRegistry.createRegistry(1099);

            /*
             * Registra el objeto remoto "obj" en el registro RMI bajo el nombre "MiSumador".
             * "rebind" significa: si ya existe un objeto con ese nombre, lo reemplaza.
             * rmi://localhost/MiSumador indica:
             * - protocolo RMI (rmi://)
             * - host local (localhost)
             * - nombre lógico del objeto (MiSumador)
             */ 
            Naming.rebind("rmi://localhost/MiSumador", obj);

            // Mensaje para confirmar que el servidor está funcionando
            System.out.println("Servidor RMI listo con Naming." );

        } catch (Exception e) {
            // Captura cualquier excepción
            e.printStackTrace();
        }

    }

}