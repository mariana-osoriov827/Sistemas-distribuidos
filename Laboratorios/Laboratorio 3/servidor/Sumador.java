/*
 * Definición de la interfaz pública "Sumador".
 * Al extender "java.rmi.Remote", se convierte en una interfaz remota válida para RMI.
 */ 
public interface Sumador extends java.rmi.Remote {

  // Recibe dos enteros como parámetros y devuelve un entero con el resultado de la suma.
  public int sumar(int a, int b) throws java.rmi.RemoteException; 

  // Recibe dos enteros como parámetros y devuelve un entero con el resultado de la resta.
  public int restar(int a, int b) throws java.rmi.RemoteException;

  /*
   * La cláusula "throws java.rmi.RemoteException" es obligatoria porque puede fallar
   * por problemas de red, de comunicación con el servidor o de disponibilidad del objeto remoto.
   */

}
