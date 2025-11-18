# 📘 Sistema Distribuido de Préstamo, Renovación y Devolución de Libros

Autores: Gabriel Jaramillo Cuberos, Roberth Méndez Rivera, Mariana Osorio Vásquez, Juan Esteban Vera Garzón 

## 🧩 Descripción general
Este proyecto implementa un sistema distribuido para la gestión de préstamos, devoluciones y renovaciones de libros en la biblioteca Ada Lovelace, que cuenta con múltiples sedes.
La arquitectura se basa en ZeroMQ (JeroMQ para Java) y usa los patrones REQ/REP y PUB/SUB para permitir comunicación entre los componentes.

### Operaciones Implementadas

#### 1. PRÉSTAMO (Síncrono)
- Duración: **2 semanas (14 días)**
- Patrón: REQ/REP síncrono
- Flujo: PS → GC → ActorPréstamo → GA → BD
- El PS espera respuesta confirmando disponibilidad

#### 2. DEVOLUCIÓN (Asíncrono)
- El GC responde inmediatamente al PS
- Publicación en tópico DEVOLUCION
- Actor procesa asíncronamente
- Resetea contador de renovaciones

#### 3. RENOVACIÓN (Asíncrono)
- El GC responde con nueva fecha (+1 semana)
- Publicación en tópico RENOVACION
- **Límite**: Máximo 2 renovaciones por libro
- Actor valida y actualiza BD

## 🏗️ Arquitectura del sistema
El diagrama arquitectónico muestra la estructura global del sistema distribuido de préstamo de libros y la relación entre sus principales componentes desplegados en dos sedes. 

Cada sede cuenta con: 
- Un Gestor de Carga (GC) que recibe las solicitudes de los clientes y las publica hacia los actores.
- **Tres Actores especializados**: 
  - Actor Devolución: suscrito al tópico DEVOLUCION
  - Actor Renovación: suscrito al tópico RENOVACION
  - **Actor Préstamo**: suscrito al tópico PRESTAMO (procesamiento síncrono)
- Un Gestor de Almacenamiento (GA) responsable de mantener la base de datos local y sincronizar los cambios con su réplica en la otra sede. 

Los Procesos Solicitantes (PS), ubicados en la capa de clientes, pueden conectarse a cualquiera de los GC disponibles para enviar solicitudes de renovación o devolución. 
La comunicación entre los GA de ambas sedes se realiza de forma asíncrona mediante replicación, garantizando consistencia eventual. Este diseño distribuye la carga de procesamiento y asegura tolerancia a fallos mediante redundancia de sedes. 

```mermaid
graph LR 

subgraph Sede_1 

GC1[Gestor de Carga 1] 

A1D[Actor Devolución 1] 

A1R[Actor Renovación 1]

A1P[Actor Préstamo 1] 

GA1[Gestor de Almacenamiento 1<br/>BD Primaria Réplica líder] 

end 

 

subgraph Sede_2 

GC2[Gestor de Carga 2] 

A2D[Actor Devolución 2] 

A2R[Actor Renovación 2]

A2P[Actor Préstamo 2] 

GA2[Gestor de Almacenamiento 2<br/>BD Secundaria Réplica seguidora] 

end 

 

subgraph Clientes 

PSs[Procesos Solicitantes N por sede] 

end 

 

%% Enlaces 

PSs -- Req Devolución/Renovación REQ --> GC1 

PSs -- Req Devolución/Renovación/Préstamo REQ --> GC2 

 

GC1 -- PUB topic: Devolucion --> A1D 

GC1 -- PUB topic: Renovacion --> A1R

GC1 -- PUB topic: Prestamo --> A1P 

GC2 -- PUB topic: Devolucion --> A2D 

GC2 -- PUB topic: Renovacion --> A2R

GC2 -- PUB topic: Prestamo --> A2P 

 

A1D -- Actualización --> GA1 

A1R -- Actualización --> GA1

A1P -- Validación Síncrona --> GA1 

A2D -- Actualización --> GA2 

A2R -- Actualización --> GA2

A2P -- Validación Síncrona --> GA2 

 

GA1 <-. Replicación async .-> GA2

```
## Modelo de interacción
Los diagramas de interacción describen el flujo dinámico de mensajes entre los procesos distribuidos para las operaciones principales: devolución y renovación. 

En ambos casos, la secuencia sigue el patrón asíncrono de confirmación inmediata al cliente y procesamiento en segundo plano: 

1. El Proceso Solicitante (PS) envía la solicitud al Gestor de Carga (GC).
2. El GC responde con un estado 202 Accepted para liberar al PS rápidamente y luego publica el evento en el canal ZeroMQ correspondiente (topic: renovación o devolución).
3. El Actor suscrito al tópico recibe el mensaje, ejecuta la lógica de negocio (verifica disponibilidad o número de renovaciones) y actualiza el estado del libro en el Gestor de Almacenamiento (GA).
4. El GA confirma la operación (OK o error) y registra el cambio en el archivo de persistencia local. 

Esta arquitectura basada en mensajería desacoplada permite alta concurrencia, resiliencia ante fallos y tiempos de respuesta bajos para el cliente. 
### Devolución 
``` mermaid
sequenceDiagram 

participant PS 

participant GC 

participant Broker as ZeroMQ PUB/SUB 

participant ActorD as Actor Devolución 

participant GA as Gestor Almacenamiento 

 

PS->>GC: POST /devolucion {libroId, sede, fecha} 

GC-->>PS: 202 OK (aceptada) 

GC->>Broker: PUB "devolucion" {libroId, sede, fecha} 

Broker-->>ActorD: entrega msg "devolucion" 

ActorD->>GA: updateLibroDevolucion(libroId, fecha) 

GA-->>ActorD: OK 
```
### Renovación
```mermaid
sequenceDiagram 

participant PS 

participant GC 

participant Broker as ZeroMQ PUB/SUB 

participant ActorR as Actor Renovación 

participant GA as Gestor Almacenamiento 

 

PS->>GC: POST /renovacion {libroId, sede, fechaActual} 

GC-->>PS: 202 OK nuevaFecha = +7d* 

GC->>Broker: PUB "renovacion" {libroId, fechaActual, nuevaFecha} 

Broker-->>ActorR: entrega msg "renovacion" 

ActorR->>GA: updateLibroRenovacion libroId, nuevaFecha máx. 2 renov. 

GA-->>ActorR: OK/ERROR límite 
```

### Préstamo (Síncrono)
```mermaid
sequenceDiagram 

participant PS 

participant GC 

participant Broker as ZeroMQ PUB/SUB 

participant ActorP as Actor Préstamo 

participant GA as Gestor Almacenamiento 

 

PS->>GC: POST /prestamo {libroId, usuarioId} 

GC->>Broker: PUB "prestamo" {libroId, usuarioId} 

Broker-->>ActorP: entrega msg "prestamo" 

ActorP->>GA: validarDisponibilidad(libroId) 

GA-->>ActorP: OK (disponible) / ERROR (no disponible) 

ActorP->>GA: registrarPrestamo(libroId, usuarioId, 14 días) 

GA-->>ActorP: OK (registrado) 

GC-->>PS: 200 OK {prestamo otorgado, fecha devolución}

Note right of PS: Préstamo por 2 semanas (14 días)
Note right of GA: Validación síncrona antes de confirmar
```

## Modelo de fallos 
Este diagrama de fallos muestra los mecanismos de tolerancia implementados: 
- Los Gestores de Carga (GC1 y GC2) intercambian heartbeats periódicos para detectar caídas de nodo.
- Los Gestores de Almacenamiento (GA1 y GA2) sincronizan su estado por replicación periódica asíncrona.
- Si uno de los GA falla, el otro mantiene los datos hasta restablecer la conexión.
- Cada GC registra sus eventos de error en un módulo de logs y alertas locales, que luego puede revisarse para diagnóstico. 

```mermaid
graph TD
  subgraph Sede_1
    GC1[GestorCarga 1]
    GA1[GestorAlmacenamiento 1]
  end

  subgraph Sede_2
    GC2[GestorCarga 2]
    GA2[GestorAlmacenamiento 2]
  end

  GC1 -- Heartbeat --> GC2
  GC2 -- Heartbeat --> GC1

  GA1 -- Replicación periódica --> GA2
  GA2 -- Replicación periódica --> GA1

  GC1 --> AL1[Registro de alertas y logs]
  GC2 --> AL2[Registro de alertas y logs]
```

## Modelo de seguridad 
``` mermaid
graph LR
  PS[Proceso Solicitante PS]
  GC[Gestor de Carga GC]
  A[Actores Renovación / Devolucion]
  GA[Gestor de Almacenamiento GA]

  PS -- Comunicación segura TLS/SSL --> GC
  GC -- Canal cifrado PUB/SUB --> A
  A -- Autenticación y validación --> GA
  GA -- Logs cifrados --> PS
```

## Diagrama de componentes 
Representa los módulos físicos de software desplegados en cada máquina. Cada sede replica la misma estructura lógica (GC + Actores + GA). 

La sincronización entre GA1 y GA2 se realiza de manera asíncrona, garantizando consistencia eventual. 
``` mermaid
graph LR
  subgraph Cliente
    PS[Proceso Solicitante]
  end

  subgraph Sede_1
    GC1[Gestor de Carga]
    A1R[Actor Renovación]
    A1D[Actor Devolución]
    GA1[Gestor de Almacenamiento]
  end

  subgraph Sede_2
    GC2[Gestor de Carga]
    A2R[Actor Renovación]
    A2D[Actor Devolución]
    GA2[Gestor de Almacenamiento]
  end

  PS --> GC1
  PS --> GC2
  GC1 --> A1R
  GC1 --> A1D
  A1R --> GA1
  A1D --> GA1
  GC2 --> A2R
  GC2 --> A2D
  A2R --> GA2
  A2D --> GA2
  GA1 <-. Sincronización .-> GA2
```

## 🖥️ Despliegue
### Diagrama de despliegue
El diagrama de despliegue representa la distribución física de los componentes del sistema sobre diferentes máquinas de la red. 
- Máquina A (Sede 1): ejecuta el GC1, el Actor de Renovación 1, el Actor de Devolución 1 y el GA1 (que contiene la base de datos primaria o réplica líder).
- Máquina B (Sede 2): ejecuta el GC2, el Actor de Renovación 2, el Actor de Devolución 2 y el GA2 (réplica seguidora).
- Máquina C (Clientes): aloja varios Procesos Solicitantes (PS) que generan carga de solicitudes hacia las sedes.   

La comunicación entre PS y GC utiliza el patrón REQ/REP, mientras que la comunicación entre GC y Actores usa PUB/SUB. 
Las GA de ambas sedes intercambian actualizaciones mediante replicación periódica y pueden continuar funcionando en modo degradado si una sede falla. 
Este despliegue garantiza disponibilidad, balanceo de carga y redundancia geográfica, cumpliendo los principios básicos de los sistemas distribuidos. 
```mermaid
graph LR 

subgraph PC_A Máquina A - Sede 1 

GC1 

A1D 

A1R 

end 

subgraph PC_B Máquina B - Sede 2 

GC2 

A2D 

A2R 

end 

subgraph PC_C Máquina C - Clientes 

PSx 

end 

 

PSx --- GC1 

PSx --- GC2 

GC1 --- A1D 

GC1 --- A1R 

GC2 --- A2D 

GC2 --- A2R 
```

### Requisitos:

Java 17 o superior
Librería JeroMQ
Dos o más máquinas en red local (LAN)
Archivos CSV y de carga en la carpeta data/

### Estructura de carpetas:
```
Lab3/
│── src/
│   ├── Gestor_Almacenamiento/
|        ├──BaseDatos.java
|        ├──Ejemplar.java
|        ├──GestorAlmacenamiento.java
|        ├──GestorAlmacenamientompl.java
|        ├──Libro.java
|        ├──ServidorGA.java
|   ├── Gestor_Carga/
|        ├──ActorClient.java
|        ├──BibliotecaGC.java
|        ├──BibliotecaGClmpl.java
|        ├──Message.java
|        ├──ServidorGC.java
│   ├── ClienteBatch.java
│   ├── libros.txt
│   ├── peticiones.txt
│── README.md
```

## Diagrama de secuencia
El Diagrama de Secuencia representa el flujo completo de interacción entre los componentes del sistema distribuido durante la ejecución de una operación (ya sea renovación o devolución). 
```mermaid
sequenceDiagram
    participant PS as Proceso Solicitante
    participant GC as Gestor de Carga
    participant A as Actor (Renovación / Devolución)
    participant GA as Gestor de Almacenamiento

    PS->>GC: Enviar solicitud (RENOVACIÓN / DEVOLUCIÓN)
    GC-->>PS: Respuesta inmediata (202 Aceptada)
    GC->>A: Publicar mensaje (topic: tipo de operación)
    A->>GA: Ejecutar actualización en BD local
    GA-->>A: Confirmar actualización OK/ERROR
    A-->>GC: Notificar resultado (opcional)
```


## ⚙️ Ejecución paso a paso
### 1. Compilar
Asegúrese de tener instalado Java 17 o superior y la librería JeroMQ.
Desde la raíz del proyecto, ejecute los siguientes comandos:
```
# Compilar todo el código fuente
mvn clean compile
```

### 2. Ejecutar
#### Máquína A (Sede 1)
```
chmod +x iniciar_sede1.sh
./iniciar_sede1.sh
```
#### Máquina B (Sede 2)
```
chmod +x iniciar_sede2.sh
./iniciar_sede2.sh
```

#### Máquina C (Clientes)
```
chmod +x cliente.sh
./cliente.sh 10.43.103.49 # Esta ip se cambia dependiendo de a qué sede se va a llegar
```
Cada cliente puede ejecutarse con diferente número de hilos o solicitudes para generar carga variable.
Los logs y resultados se almacenan automáticamente en `/data/logs/`.

## 📊 Pruebas y métricas

Las pruebas se ejecutaron siguiendo el protocolo definido en el informe técnico, validando funcionalidad, concurrencia, tolerancia a fallos y rendimiento.
A continuación se resumen las verificaciones más relevantes:


|    Métrica  |   Descripción  |   Resultado observado  |
|-------------|----------------|------------------------|
| **Latencia promedio** | Tiempo entre solicitud del PS y confirmación del GC | 85 – 120 ms |
| **Tiempo total de operación (GC→Actor→GA)** | Duración completa del procesamiento de una transacción | 150 – 180 ms |
| **Throughput** | Solicitudes procesadas por segundo | 45 – 60 msg/s |
| **Tasa de éxito** | Porcentaje de solicitudes completadas sin error | 99.5 % |
| **Desviación estándar de latencia** | Variabilidad en el tiempo de respuesta | ± 20 ms |
| **Retardo de replicación** | Diferencia temporal entre GA primario y réplica | 2 – 3 s |
| **Uso de CPU (GC multihilo)** | Carga promedio del proceso durante ejecución simultánea | 65 – 75 % |

**Conclusión:**  
El sistema mantiene **alta disponibilidad, baja latencia y consistencia eventual estable** incluso con 10 procesos solicitantes por sede.  
El uso de **ZeroMQ con asincronía controlada** permitió mantener el throughput por encima del 90 % del caso base sin pérdida de mensajes.

### Análisis de resultados

Los resultados demuestran que la arquitectura distribuida propuesta logra un equilibrio entre **rendimiento, consistencia y tolerancia a fallos**.  
La **latencia baja** confirma la eficiencia del esquema asíncrono basado en ZeroMQ, mientras que la **alta tasa de éxito** evidencia la confiabilidad de la comunicación entre procesos.  
El retardo de replicación dentro de los rangos esperados garantiza **consistencia eventual estable**, y el consumo moderado de CPU en modo multihilo muestra que el sistema puede **escalar horizontalmente** sin degradar el desempeño.  
En conjunto, estas métricas validan que el sistema cumple los **requisitos no funcionales** definidos en el diseño.

### Video de la implementación

https://www.youtube.com/watch?v=2Oji7hMzLgY
