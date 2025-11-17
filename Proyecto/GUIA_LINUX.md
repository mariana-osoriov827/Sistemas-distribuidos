# 🐧 GUÍA DE COMPILACIÓN Y EJECUCIÓN EN LINUX

## Requisitos Previos

### 1. Instalar Java JDK
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# Verificar instalación
java -version
javac -version
```

### 2. Instalar Maven
```bash
# Ubuntu/Debian
sudo apt install maven

# Verificar instalación
mvn -version
```

### 3. Instalar Git (si no lo tienes)
```bash
sudo apt install git
```

---

## 📦 Compilación del Proyecto

### Paso 1: Navegar al directorio del proyecto
```bash
cd ~/ruta/al/proyecto
# O en tu caso:
cd "/home/usuario/Documentos/Universidad/SÉPTIMO SEMESTRE/Sistemas distribuidos/Proyecto"
```

### Paso 2: Compilar con Maven
```bash
# Limpiar y compilar
mvn clean compile

# O compilar y empaquetar
mvn clean package
```

**Salida esperada:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 5.234 s
```

### Solución de problemas de compilación:
```bash
# Si falta dependencia ZeroMQ, agregar al pom.xml:
# <dependency>
#     <groupId>org.zeromq</groupId>
#     <artifactId>jeromq</artifactId>
#     <version>0.5.3</version>
# </dependency>

# Limpiar caché de Maven
mvn clean install -U
```

---

## 🚀 Ejecución del Sistema

### Opción 1: Scripts Automáticos (Recomendado)

#### Dar permisos de ejecución a los scripts:
```bash
chmod +x iniciar_sede1.sh
chmod +x iniciar_sede2.sh
```

#### Ejecutar Sede 1:
```bash
./iniciar_sede1.sh
```

#### Ejecutar Sede 2:
```bash
./iniciar_sede2.sh
```

> **Nota:** Los scripts usan `gnome-terminal`. Si usas otro terminal:
> - **KDE**: Reemplazar `gnome-terminal` por `konsole -e`
> - **XFCE**: Reemplazar por `xfce4-terminal -e`
> - **Tmux**: Ver sección de ejecución con tmux más abajo

---

### Opción 2: Ejecución Manual

#### Terminal 1 - Gestor de Almacenamiento Sede 1 (Primario):
```bash
java -cp target/classes Gestor_Almacenamiento.ServidorGA_TCP primary 5560 <IP_SEDE_2> 6560
```

Ejemplo con IP específica:
```bash
java -cp target/classes Gestor_Almacenamiento.ServidorGA_TCP primary 5560 192.168.1.100 6560
```

#### Terminal 2 - Gestor de Carga Sede 1:
```bash
java -cp target/classes Gestor_carga.ServidorGC_ZMQ 1 5555 5556 localhost 5560
```

#### Terminal 3 - Actor Devolución Sede 1:
```bash
java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:5555 localhost:5560 DEVOLUCION
```

#### Terminal 4 - Actor Renovación Sede 1:
```bash
java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:5555 localhost:5560 RENOVACION
```

#### Terminal 5 - Actor Préstamo Sede 1:
```bash
java -cp target/classes Gestor_carga.ActorPrestamo_ZMQ localhost:5555 localhost:5560
```

#### Terminal 6 - Cliente (Proceso Solicitante):
```bash
java -cp target/classes ClienteBatch_ZMQ src/peticiones.txt localhost:5556
```

---

### Opción 3: Usando Tmux (Múltiples Terminales en una Ventana)

#### Instalar tmux:
```bash
sudo apt install tmux
```

#### Crear sesión y ejecutar componentes:
```bash
# Crear sesión
tmux new-session -d -s biblioteca

# Ventana 0: GA
tmux send-keys -t biblioteca:0 "java -cp target/classes Gestor_Almacenamiento.ServidorGA_TCP primary 5560 localhost 6560" C-m

# Ventana 1: GC
tmux new-window -t biblioteca:1
tmux send-keys -t biblioteca:1 "sleep 3 && java -cp target/classes Gestor_carga.ServidorGC_ZMQ 1 5555 5556 localhost 5560" C-m

# Ventana 2: Actor Devolución
tmux new-window -t biblioteca:2
tmux send-keys -t biblioteca:2 "sleep 5 && java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:5555 localhost:5560 DEVOLUCION" C-m

# Ventana 3: Actor Renovación
tmux new-window -t biblioteca:3
tmux send-keys -t biblioteca:3 "sleep 6 && java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:5555 localhost:5560 RENOVACION" C-m

# Ventana 4: Actor Préstamo
tmux new-window -t biblioteca:4
tmux send-keys -t biblioteca:4 "sleep 7 && java -cp target/classes Gestor_carga.ActorPrestamo_ZMQ localhost:5555 localhost:5560" C-m

# Ventana 5: Cliente
tmux new-window -t biblioteca:5
tmux send-keys -t biblioteca:5 "sleep 10 && java -cp target/classes ClienteBatch_ZMQ src/peticiones.txt localhost:5556" C-m

# Conectar a la sesión
tmux attach -t biblioteca
```

**Comandos útiles de tmux:**
- `Ctrl+b` seguido de `n`: Siguiente ventana
- `Ctrl+b` seguido de `p`: Ventana anterior
- `Ctrl+b` seguido de `0-5`: Ir a ventana específica
- `Ctrl+b` seguido de `d`: Desconectar (deja corriendo en background)
- `tmux attach -t biblioteca`: Reconectar
- `tmux kill-session -t biblioteca`: Cerrar todas las ventanas

---

## 🔧 Configuración de Red (Múltiples Computadores)

### Verificar IP de tu máquina:
```bash
# Ver todas las interfaces
ip addr show

# O más simple
hostname -I
```

### Configurar Firewall (permitir puertos):
```bash
# Ubuntu con ufw
sudo ufw allow 5555/tcp
sudo ufw allow 5556/tcp
sudo ufw allow 5560/tcp
sudo ufw allow 6555/tcp
sudo ufw allow 6556/tcp
sudo ufw allow 6560/tcp
sudo ufw reload

# O desactivar firewall temporalmente (solo para pruebas)
sudo ufw disable
```

### Probar conectividad entre máquinas:
```bash
# Desde computador cliente, probar conexión al servidor
telnet <IP_SERVIDOR> 5556

# O con netcat
nc -zv <IP_SERVIDOR> 5556
```

---

## 📊 Distribución en 3 Computadores Linux

### Computador 1 (GC + Actores Sede 1):
```bash
# Terminal 1: GA
java -cp target/classes Gestor_Almacenamiento.ServidorGA_TCP primary 5560 <IP_COMP2> 6560

# Terminal 2: GC
java -cp target/classes Gestor_carga.ServidorGC_ZMQ 1 5555 5556 localhost 5560

# Terminales 3-5: Actores
java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:5555 localhost:5560 DEVOLUCION
java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:5555 localhost:5560 RENOVACION
java -cp target/classes Gestor_carga.ActorPrestamo_ZMQ localhost:5555 localhost:5560
```

### Computador 2 (GC + Actores Sede 2):
```bash
# Terminal 1: GA
java -cp target/classes Gestor_Almacenamiento.ServidorGA_TCP replica 6560

# Terminal 2: GC
java -cp target/classes Gestor_carga.ServidorGC_ZMQ 2 6555 6556 localhost 6560

# Terminales 3-5: Actores
java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:6555 localhost:6560 DEVOLUCION
java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:6555 localhost:6560 RENOVACION
java -cp target/classes Gestor_carga.ActorPrestamo_ZMQ localhost:6555 localhost:6560
```

### Computador 3 (Clientes):
```bash
# Cliente 1 conectado a Sede 1
java -cp target/classes ClienteBatch_ZMQ src/peticiones.txt <IP_COMP1>:5556

# Cliente 2 conectado a Sede 2
java -cp target/classes ClienteBatch_ZMQ src/peticiones2.txt <IP_COMP2>:6556

# Cliente 3 conectado a Sede 1
java -cp target/classes ClienteBatch_ZMQ src/peticiones.txt <IP_COMP1>:5556
```

---

## 🐛 Solución de Problemas

### Error: "Address already in use"
```bash
# Ver qué proceso usa el puerto
sudo netstat -tlnp | grep :5560

# O con ss
sudo ss -tlnp | grep :5560

# Matar proceso
sudo kill -9 <PID>
```

### Error: "Connection refused"
```bash
# Verificar que el servidor esté corriendo
ps aux | grep java

# Verificar puertos abiertos
sudo netstat -tlnp | grep LISTEN
```

### Error: "ClassNotFoundException"
```bash
# Verificar que las clases estén compiladas
ls -R target/classes/

# Recompilar
mvn clean compile
```

### Error: "Cannot find jeromq"
```bash
# Agregar dependencia al pom.xml y recompilar
mvn clean install
```

---

## 📝 Comandos Útiles

### Ver logs en tiempo real:
```bash
# Redirigir salida a archivo
java -cp target/classes Gestor_carga.ServidorGC_ZMQ 1 5555 5556 localhost 5560 > gc_sede1.log 2>&1 &

# Ver log en tiempo real
tail -f gc_sede1.log
```

### Ejecutar en background:
```bash
# Con nohup
nohup java -cp target/classes Gestor_Almacenamiento.ServidorGA_TCP primary 5560 &

# Ver procesos Java corriendo
jps -l
```

### Detener todos los procesos Java:
```bash
# Con cuidado - mata TODOS los procesos Java
pkill -9 java

# Más específico
ps aux | grep "ServidorGC_ZMQ" | awk '{print $2}' | xargs kill -9
```

---

## 🎯 Checklist de Ejecución

- [ ] Java JDK 11+ instalado
- [ ] Maven instalado
- [ ] Proyecto compilado (`mvn clean compile`)
- [ ] Firewall configurado (puertos abiertos)
- [ ] IPs correctas en scripts/comandos
- [ ] Archivo `libros.txt` existe en `src/`
- [ ] Archivos `peticiones.txt` y `peticiones2.txt` existen
- [ ] GA iniciado primero
- [ ] GC iniciado después
- [ ] Actores iniciados después del GC
- [ ] Clientes iniciados al final

---

## 🚀 Script Todo-en-Uno (Para Pruebas Locales)

Crea un archivo `ejecutar_local.sh`:

```bash
#!/bin/bash

# Compilar
echo "Compilando proyecto..."
mvn clean compile

# Verificar compilación
if [ $? -ne 0 ]; then
    echo "Error en compilación"
    exit 1
fi

# Ejecutar componentes
echo "Iniciando sistema..."

# GA
gnome-terminal -- bash -c "java -cp target/classes Gestor_Almacenamiento.ServidorGA_TCP primary 5560 localhost 6560; exec bash" &
sleep 3

# GC
gnome-terminal -- bash -c "java -cp target/classes Gestor_carga.ServidorGC_ZMQ 1 5555 5556 localhost 5560; exec bash" &
sleep 3

# Actores
gnome-terminal -- bash -c "java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:5555 localhost:5560 DEVOLUCION; exec bash" &
gnome-terminal -- bash -c "java -cp target/classes Gestor_carga.ActorClient_ZMQ localhost:5555 localhost:5560 RENOVACION; exec bash" &
gnome-terminal -- bash -c "java -cp target/classes Gestor_carga.ActorPrestamo_ZMQ localhost:5555 localhost:5560; exec bash" &
sleep 2

echo "Sistema iniciado. Presiona Enter para enviar peticiones..."
read

# Cliente
gnome-terminal -- bash -c "java -cp target/classes ClienteBatch_ZMQ src/peticiones.txt localhost:5556; exec bash"

echo "¡Listo!"
```

Dale permisos y ejecútalo:
```bash
chmod +x ejecutar_local.sh
./ejecutar_local.sh
```

---

**¡Tu sistema está listo para ejecutarse en Linux!** 🐧✨
