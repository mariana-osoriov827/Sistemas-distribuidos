package threadsJarroba;

/**
 * 
 * @author Richard
 */
public class CajeraThread extends Thread { // Clase que hereda de Thread, representa a una cajera trabajando en un hilo
	
	private String nombre;
	private Cliente cliente;
	private long initialTime;
	
	public CajeraThread(String nombre, Cliente cliente, long initialTime) {
		this.nombre = nombre; // Nombre de la cajera
		this.cliente = cliente; // Cliente que será atendido
		this.initialTime = initialTime; // Tiempo inicial de referencia
	}
	
	@Override
	public void run() { // Método que se ejecuta al iniciar el hilo
		System.out.println("La cajera " + this.nombre + 
				" comienza a procesar la compra del cliente " + cliente.getNombre() + 
				" en el tiempo: " + (System.currentTimeMillis() - this.initialTime) / 1000 + "seg"); // Inicio
		
		for (int i = 0; i < cliente.getCarroCompra().length; i++) { 
			this.esperarXsegundos(cliente.getCarroCompra()[i]); // Simula el escaneo del producto
			System.out.println("Procesado el producto " + (i + 1) + 
					" del cliente " + cliente.getNombre() + 
					" ->Tiempo: " + (System.currentTimeMillis() - this.initialTime) / 1000 + "seg"); // Progreso
		}
		
		System.out.println("La cajera " + this.nombre + 
				" ha terminado de procesar " + cliente.getNombre() + 
				" en el tiempo: " + (System.currentTimeMillis() - this.initialTime) / 1000 + "seg"); // Fin
	}
	
	private void esperarXsegundos(int segundos) { // Método que simula el tiempo de espera
		try {
			Thread.sleep(segundos * 1000); // Espera la cantidad de segundos indicada
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt(); // Maneja la interrupción
		}
	}
}
