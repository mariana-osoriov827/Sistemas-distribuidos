# 📄 Laboratorio de programación de sockets
Este repositorio contiene el código fuente de un laboratorio práctico sobre programación de sockets en Java, abordando los dos protocolos principales: TCP (Protocolo de Control de Transmisión) y UDP (Protocolo de Datagramas de Usuario). El objetivo es demostrar y entender la diferencia en la comunicación entre cliente y servidor para cada protocolo.

## 🚀 Contenido del repositorio
El laboratorio está estructurado en cuatro programas principales, dos para cada protocolo. Cada par de archivos (cliente y servidor) debe ejecutarse de forma independiente para establecer la comunicación.

### 📁 Archivos TCP
**sockettcpcli.java:** El cliente TCP. Se conecta a un servidor, envía mensajes de texto y termina la comunicación cuando se introduce el mensaje "fin". La comunicación es orientada a la conexión, lo que garantiza la entrega de los mensajes en orden.

**sockettcpser.java:** El servidor TCP. Escucha en el puerto 6001, acepta una única conexión de un cliente y recibe los mensajes que este le envía, mostrándolos en la consola.

### 📁 Archivos UDP
**socketudpcli.java:** El cliente UDP. Envía mensajes al servidor como datagramas (paquetes de datos). No establece una conexión persistente. La comunicación finaliza cuando se envía el mensaje "fin".

**socketudpser.java:** El servidor UDP. Escucha en el puerto 6000, recibe los datagramas enviados por el cliente y muestra su contenido en la consola. La comunicación es sin conexión, por lo que no hay garantía de orden o entrega.

## 🛠️ Requisitos
- **Java Development Kit (JDK)** instalado en tu sistema.
- Un editor de texto o un entorno de desarrollo integrado (IDE) como Visual Studio Code o Eclipse.

## 💻 Instrucciones de Uso
Para compilar y ejecutar los programas, sigue estos pasos desde la línea de comandos en la carpeta donde se encuentran los archivos.

### Para la comunicación TCP
- Compilar los archivos:

```
javac sockettcpser.java sockettcpcli.java
```

- Ejecutar el servidor: Abre una nueva ventana de terminal y ejecuta el servidor. Este se quedará esperando la conexión del cliente.
```
java sockettcpser
```

- Ejecutar el cliente: En una terminal diferente, ejecuta el cliente, proporcionando la dirección IP del servidor (generalmente localhost si se ejecuta en la misma máquina).
```
java sockettcpcli localhost
```

- Ahora puedes escribir mensajes en la terminal del cliente y ver cómo aparecen en la del servidor.

### Para la comunicación UDP
- Compilar los archivos:
```
javac socketudpser.java socketudpcli.java
```
- Ejecutar el servidor: En una terminal, inicia el servidor.
```
java socketudpser
```
- Ejecutar el cliente: En una terminal diferente, ejecuta el cliente, especificando la dirección del servidor.
```
java socketudpcli localhost
```
- Escribe mensajes en el cliente y observa cómo se reciben en el servidor.