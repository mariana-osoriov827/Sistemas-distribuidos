package threadsJarroba;

/**************************************************************************************
* Fecha: 05/09/2025
* Autor: Mariana Osorio Vasquez, Gabriel Jaramillo, Roberth Méndez, Juan Esteban Vera
* Tema: 
* - Programación de Sockets
* Descripción:
* - // * Programa que simula a dos cajeras atendiendo a clientes en paralelo usando hilos. 
* La clase implementa Runnable en lugar de extender Thread, lo que permite separar la lógica de la tarea (procesar compras)
* de la gestión del hilo y así poder reutilizar la clase en diferentes contextos de concurrencia.
***************************************************************************************/
public class MainRunnable implements Runnable{  // Esta clase implementa Runnable para ejecutar en hilos
	
	private Cliente cliente;
	private Cajera cajera;
	private long initialTime;
	
	public MainRunnable (Cliente cliente, Cajera cajera, long initialTime){
		this.cajera = cajera;
		this.cliente = cliente;
		this.initialTime = initialTime;
	}

	public static void main(String[] args) { // Método main, punto de entrada del programa
		
		Cliente cliente1 = new Cliente("Cliente 1", new int[] { 2, 2, 1, 5, 2, 3 }); // Se crea un nuevo objeto Cliente
		Cliente cliente2 = new Cliente("Cliente 2", new int[] { 1, 3, 5, 1, 1 }); // Se crea un nuevo objeto Cliente
		
		Cajera cajera1 = new Cajera("Cajera 1"); // Se crea una nueva Cajera
		Cajera cajera2 = new Cajera("Cajera 2"); // Se crea otra Cajera
		
		long initialTime = System.currentTimeMillis(); // Se guarda el tiempo inicial
		
		Runnable proceso1 = new MainRunnable(cliente1, cajera1, initialTime); // Se prepara el proceso 1
		Runnable proceso2 = new MainRunnable(cliente2, cajera2, initialTime); // Se prepara el proceso 2
		
		new Thread(proceso1).start(); // Se lanza un nuevo hilo
		new Thread(proceso2).start(); // Se lanza un nuevo hilo
	}

	@Override
	public void run() {  // Método que ejecuta la lógica del hilo
		this.cajera.procesarCompra(this.cliente, this.initialTime); // Cajera procesa al cliente
	}
}
