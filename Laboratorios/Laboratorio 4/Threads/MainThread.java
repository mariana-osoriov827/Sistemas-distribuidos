package threadsJarroba;

/**************************************************************************************
* Fecha: 05/09/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema: 
* - Programación de Sockets
* Descripción:
* - // Programa que simula a dos cajeras atendiendo clientes en paralelo utilizando hilos. 
* En este caso se extiende la clase Thread, por lo que cada cajera es directamente un hilo. A diferencia de
* implementar Runnable, esta forma acopla la lógica de la tarea con la gestión del hilo, reduciendo la reutilización de la clase.
***************************************************************************************/
public class MainThread { // Clase principal para probar con hilos
	
	public static void main(String[] args) { // Método main, punto de entrada
		
		Cliente cliente1 = new Cliente("Cliente 1", new int[] { 2, 2, 1, 5, 2, 3 }); // Se crea cliente 1
		Cliente cliente2 = new Cliente("Cliente 2", new int[] { 1, 3, 5, 1, 1 }); // Se crea cliente 2
		
		// Tiempo inicial de referencia
		long initialTime = System.currentTimeMillis();
		
		// Se crean dos cajeras en hilos distintos
		CajeraThread cajera1 = new CajeraThread("Cajera 1", cliente1, initialTime);
		CajeraThread cajera2 = new CajeraThread("Cajera 2", cliente2, initialTime);
		
		// Se inician los hilos (cada cajera procesa al mismo tiempo)
		cajera1.start();
		cajera2.start();
	}
}
