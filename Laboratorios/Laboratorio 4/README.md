# 🛜 Laboratorio: Simulación de Cajeras con Paralelismo, Comunicación con Sockets

Este proyecto contiene dos implementaciones principales en **Java**:

1. **Socket Mailbox:**  
   Programas que demuestran comunicación en red usando los protocolos **TCP** y **UDP**.  
   - TCP asegura comunicación **confiable y secuencial**.  
   - UDP permite comunicación **rápida y sin conexión**.

2. **ThreadsJar:**  
   Simulación de un supermercado donde cajeras atienden clientes.  
   - **Versión secuencial:** Una cajera atiende a todos los clientes uno tras otro.  
   - **Versión paralela:** Varias cajeras procesan simultáneamente usando **multithreading**.

---

## 👨‍💻 Autores y Datos del Proyecto

**Autores:**  
- Gabriel Jaramillo Cuberos  
- Roberth Santiago Méndez  
- Mariana Osorio  
- Juan Esteban Vera

**Asignatura:** Introducción a los Sistemas Distribuidos  
**Profesor:** John Jairo Corredor Franco  
**Institución:** Pontificia Universidad Javeriana  
**Fecha:** Septiembre de 2025

---

## 🚀 Ejecución

### ** 1. Socket Mailbox**
#### Ejecución TCP:
En dos terminales separadas:

**Servidor TCP:**
```bash
javac sockettcpser.java
java sockettcpser
```

**Cliente TCP:**
```bash
javac sockettcpcli.java
java sockettcpcli <IP_DEL_SERVIDOR>
```
> Para terminar la conexión, envía el mensaje `fin`.

---

#### Ejecución UDP:
En dos terminales separadas:

**Servidor UDP:**
```bash
javac socketudpser.java
java socketudpser
```

**Cliente UDP:**
```bash
javac socketudpcli.java
java socketudpcli <IP_DEL_SERVIDOR>
```
> Para terminar la comunicación, envía el mensaje `fin`.

---

### ** 2. ThreadsJar**
#### Ejecución con Threads:
Compila y ejecuta la clase principal:
```bash
javac threadsJarroba/*.java
java threadsJarroba.MainThread
```

#### Ejecución con Runnable:
```bash
javac threadsJarroba/*.java
java threadsJarroba.MainRunnable
```

---

## 📖 Descripción General

### **Parte 1: Socket Mailbox**
Se implementan programas que simulan la comunicación entre cliente y servidor usando dos protocolos:

- **TCP (orientado a conexión):** Comunicación confiable, secuencial y con detección de errores.  
  - *Cliente TCP (sockettcpcli.java):* Conecta con servidor en puerto `6001`, envía líneas de texto desde la consola usando `DataOutputStream.writeUTF()`. Finaliza cuando la línea empieza con `fin`.  
  - *Servidor TCP (sockettcpser.java):* Abre `ServerSocket(6001)`, acepta conexiones (`accept()`), lee mensajes con `DataInputStream.readUTF()`, imprime por consola; termina cuando recibe `fin`.

- **UDP (sin conexión):** Comunicación mediante datagramas, más rápida pero sin garantías.  
  - *Cliente UDP (socketudpcli.java):* Crea `DatagramSocket`, empaqueta mensajes en `DatagramPacket` y los envía al puerto `6000`. Finaliza cuando la línea empieza con `fin`.  
  - *Servidor UDP (socketudpser.java):* Crea `DatagramSocket(6000)`, recibe paquetes con `socket.receive(packet)`, procesa el `byte[]` y convierte a `String`. Termina cuando recibe `fin`.

**Pruebas:**  
- *Prueba local:* Cliente y servidor en la misma máquina.  
- *Prueba remota:* Ejecutar servidor en una máquina y pasar su IP como argumento al cliente.

---

### **Parte 2: ThreadsJar (Paralelismo)**
Simulación de atención en caja:

- **Modelo de datos:**  
  - `Cliente`: nombre y `int[] carroCompra` (cada entero = segundos que tarda un producto en escanearse).  
  - `Cajera`: clase que implementa `procesarCompra(Cliente, timeStamp)` y usa `Thread.sleep(segundos * 1000)` para simular cada producto.  
  - `CajeraThread`: extiende `Thread` y realiza el procesamiento en `run()`.  
  - `MainRunnable`: implementa `Runnable` y delega la lógica a `Cajera.procesarCompra(...)`.  
  - `MainThread`: crea `CajeraThread` y llama a `start()` para ejecutar cajeras en paralelo.

- **Comportamiento esperado:**  
  - *Secuencial:* el tiempo total = suma de todos los tiempos de todos los productos de todos los clientes.  
  - *Paralelo:* varias cajeras ejecutándose al mismo tiempo; el tiempo total se reduce (aprox. al máximo de los tiempos de atención que se ejecutan en paralelo), demostrado por los mensajes de consola con timestamps relativos.

---

## 📌 Conclusiones (Resumen)

1. **Diferencia de protocolos:** TCP = fiabilidad y orden; UDP = velocidad y baja sobrecarga.  
2. **Multithreading:** Permite reducir tiempos de atención cuando tareas son independientes.  
3. **Mejoras posibles:**  
   - Añadir multithreading en el servidor TCP para soportar múltiples clientes concurrentes.  
   - En UDP, implementar confirmaciones a nivel de aplicación si se necesita fiabilidad.  
4. **Aplicaciones reales:** servidores web, sistemas bancarios, streaming, videojuegos en línea, etc.


---
