# 📚 Sistema de Biblioteca con RMI en Java

Este laboratorio implementa un sistema **Cliente/Servidor** utilizando **Java RMI (Remote Method Invocation)**.  
El servidor actúa como una biblioteca que administra los libros y ejemplares disponibles, mientras que los clientes pueden conectarse para realizar operaciones como **préstamos, devoluciones, renovaciones y consultas de reportes**.  

La arquitectura sigue el modelo distribuido en el que múltiples clientes pueden acceder simultáneamente al servicio remoto ofrecido por el servidor.

---

## 💻 Descripción del Cliente (ClienteRMI)

El cliente es una aplicación de consola que permite al usuario interactuar con el servidor remoto.  
El flujo general es el siguiente:

- Conectarse al servicio remoto (`BibliotecaService`) mediante RMI.
- Seleccionar operaciones disponibles:
  - `P` → Prestar libro  
  - `R` → Renovar libro  
  - `D` → Devolver libro  
  - `M` → Mostrar reporte de libros y ejemplares  
  - `Q` → Salir y desconectar del servidor  
- Ingresar los datos solicitados (nombre del libro e ISBN) según la operación seleccionada.
- Recibir la respuesta del servidor e imprimirla en consola.

---

## 🖥️ Descripción del Servidor (ServidorRMI)

El servidor gestiona las operaciones solicitadas por los clientes.  
Sus responsabilidades principales son:

- **Cargar la base de datos** inicial desde el archivo `libros.txt`.  
- **Exponer el servicio remoto** `BibliotecaService` en el puerto `1099` mediante un `Registry`.  
- **Atender las solicitudes** de los clientes:
  - Procesar préstamos, devoluciones y renovaciones.
  - Generar reportes de disponibilidad de ejemplares.
  - Registrar clientes conectados/desconectados.
- **Guardar automáticamente la base de datos** en el archivo `salida.txt` cuando el servidor se apaga, gracias a un `shutdown hook`.

---

## 📂 Estructura del Proyecto

```
lab3/
└── src/
    ├── cliente/
    │   └── ClienteRMI.java
    └── servidor/
        ├── BaseDatos.java
        ├── Biblioteca.java
        ├── BibliotecaImpl.java
        ├── Ejemplar.java
        ├── Libro.java
        ├── libros.txt
        ├── salida.txt
        └── ServidorRMI.java
```

---

## ⚙️ Requisitos

- **Java 11** (compilador `javac` y runtime `java`).  
- Archivos de base de datos:  
  - `libros.txt` (entrada inicial).  
  - `salida.txt` (archivo de salida al cerrar el servidor).  

---

## 🚀 Instrucciones de Compilación y Ejecución

1. **Entrar a la carpeta raíz del proyecto:**
   ```bash
   cd lab3/src
   ```

2. **Compilar los archivos del servidor y cliente:**
   ```bash
   javac servidor/*.java cliente/*.java
   ```

3. **Iniciar el servidor RMI:**
   ```bash
   java servidor.ServidorRMI
   ```
   - Esto levantará el servicio en el puerto `1099`.

4. **Ejecutar uno o más clientes en otras maquinas o terminales:**
   ```bash
   java cliente.ClienteRMI
   ```

5. **Interacción:**
   - El cliente mostrará el menú de opciones (Prestar, Renovar, Devolver, Reporte, Salir).  
   - El servidor registrará las operaciones en consola.  

---

## 👨‍💻 Autores
- Mariana Osorio Vásquez  
- Gabriel Jaramillo  
- Roberth Méndez  
- Juan Esteban Vera  
