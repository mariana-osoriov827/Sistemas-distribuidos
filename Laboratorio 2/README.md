# 📝 Laboratorio Multi-threaded Client/Server in Java
Este proyecto implementa una arquitectura cliente/servidor multihilo en Java. Su objetivo es demostrar cómo un servidor puede manejar múltiples conexiones de clientes de forma simultánea y eficiente. El servidor crea un hilo independiente para cada cliente que se conecta, permitiendo que las solicitudes sean procesadas en paralelo sin bloquear las operaciones para otros usuarios. Esto mejora la escalabilidad y capacidad de respuesta del sistema.

## 💻 Descripción del Cliente (TCPClient)
El cliente es una aplicación de consola sencilla que se conecta al servidor. Su función es permitir al usuario ingresar un número, enviarlo al servidor para su procesamiento y, posteriormente, mostrar la respuesta recibida en pantalla. El cliente mantiene una comunicación continua hasta que el usuario ingresa la palabra clave "bye", momento en el que la conexión se cierra.

## 🖥️ Descripción del Servidor (MultithreadedSocketServer)
El servidor actúa como el punto de escucha del sistema. Inicia un socket en el puerto 8888 y se mantiene en un bucle infinito, esperando nuevas conexiones. Cuando un cliente solicita una conexión, el servidor la acepta y delega la gestión de esa comunicación a una instancia de ServerClientThread. Este hilo dedicado se encarga de todo el ciclo de vida de la solicitud: recibir el número del cliente, calcular su cuadrado y enviar el resultado de vuelta, garantizando así que el hilo principal del servidor esté siempre disponible para aceptar nuevas conexiones.
