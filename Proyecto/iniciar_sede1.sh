#!/bin/bash
# Script de inicio para Sede 1 - Linux
# Sistema Distribuido de Préstamo de Libros

echo "======================================"
echo "  SISTEMA DE PRÉSTAMO DE LIBROS      "
echo "           SEDE 1                     "
echo "======================================"

# Configuración de puertos Sede 1
SEDE=1
PUB_PORT=5555
REP_PORT=5556
GA_PORT=5560
GA_HOST="localhost"

# IP de la réplica
REPLICA_HOST="10.43.102.177"
REPLICA_PORT=6560

# Classpath con dependencias
CP="target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar"

# Crear directorio de logs si no existe
mkdir -p logs

echo ""
echo "Iniciando sistema..."
echo "Todas las operaciones se mostrarán en esta terminal"
echo ""

# Gestor de Almacenamiento (GA)
echo "[1/5] Iniciando Gestor de Almacenamiento (GA)..."
java -cp "$CP" Gestor_Almacenamiento.ServidorGA_TCP primary $GA_PORT $REPLICA_HOST $REPLICA_PORT &
GA_PID=$!
sleep 2

# Gestor de Carga (GC)
echo "[2/5] Iniciando Gestor de Carga (GC)..."
java -cp "$CP" Gestor_carga.ServidorGC_ZMQ $SEDE $PUB_PORT $REP_PORT $GA_HOST $GA_PORT &
GC_PID=$!
sleep 2

# Actores
echo "[3/5] Iniciando Actor Devolución..."
java -cp "$CP" Gestor_carga.ActorClient_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} DEVOLUCION &
DEV_PID=$!
sleep 1

echo "[4/5] Iniciando Actor Renovación..."
java -cp "$CP" Gestor_carga.ActorClient_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} RENOVACION &
REN_PID=$!
sleep 1

echo "[5/5] Iniciando Actor Préstamo..."
java -cp "$CP" Gestor_carga.ActorPrestamo_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} &
PRES_PID=$!
sleep 1

echo ""
echo "======================================"
echo "  SISTEMA INICIADO EXITOSAMENTE    "
echo "======================================"
echo ""
echo "Componentes activos:"
echo "  - GA (PID: $GA_PID) - Puerto: $GA_PORT"
echo "  - GC (PID: $GC_PID) - PUB: $PUB_PORT, REP: $REP_PORT"
echo "  - Actor Devolución (PID: $DEV_PID)"
echo "  - Actor Renovación (PID: $REN_PID)"
echo "  - Actor Préstamo (PID: $PRES_PID)"
echo ""
echo "Todas las operaciones se mostrarán aquí abajo:"
echo "======================================"
echo ""

# Función para limpiar al salir
cleanup() {
    echo ""
    echo "Deteniendo sistema..."
    kill $GA_PID $GC_PID $DEV_PID $REN_PID $PRES_PID 2>/dev/null
    echo "Sistema detenido."
    exit 0
}

trap cleanup SIGINT SIGTERM

# Mantener el script corriendo
wait
echo "  - Publisher (PUB): $PUB_PORT"
echo "  - Replier (REP): $REP_PORT"
echo "  - GA (TCP): $GA_PORT"
echo ""
echo "Para enviar peticiones desde clientes:"
echo "  java -cp $CLASSPATH ClienteBatch_ZMQ src/peticiones.txt ${GA_HOST}:${REP_PORT}"
echo ""
