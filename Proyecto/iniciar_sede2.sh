#!/bin/bash
# Script de inicio para Sede 2 - Linux
# Sistema Distribuido de Préstamo de Libros

echo "======================================"
echo "  SISTEMA DE PRÉSTAMO DE LIBROS      "
echo "           SEDE 2                     "
echo "======================================"

# Configuración de puertos Sede 2
SEDE=2
PUB_PORT=6555
REP_PORT=6556
GA_PORT=6560
GA_HOST="localhost"

# Classpath con dependencias
CP="target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar"

echo ""
echo "Iniciando todos los componentes en background..."
echo ""

# Gestor de Almacenamiento (GA) Réplica
echo "[1/5] Iniciando Gestor de Almacenamiento (GA) Réplica..."
java -cp "$CP" Gestor_Almacenamiento.ServidorGA_TCP replica $GA_PORT > logs/ga_sede2.log 2>&1 &
GA_PID=$!
sleep 2

# Gestor de Carga (GC)
echo "[2/5] Iniciando Gestor de Carga (GC)..."
java -cp "$CP" Gestor_carga.ServidorGC_ZMQ $SEDE $PUB_PORT $REP_PORT $GA_HOST $GA_PORT > logs/gc_sede2.log 2>&1 &
GC_PID=$!
sleep 2

# Actores
echo "[3/5] Iniciando Actor Devolución..."
java -cp "$CP" Gestor_carga.ActorClient_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} DEVOLUCION > logs/actor_devolucion.log 2>&1 &
DEV_PID=$!
sleep 1

echo "[4/5] Iniciando Actor Renovación..."
java -cp "$CP" Gestor_carga.ActorClient_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} RENOVACION > logs/actor_renovacion.log 2>&1 &
REN_PID=$!
sleep 1

echo "[5/5] Iniciando Actor Préstamo..."
java -cp "$CP" Gestor_carga.ActorPrestamo_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} > logs/actor_prestamo.log 2>&1 &
PRES_PID=$!
sleep 1

echo ""
echo "======================================"
echo "  ✓ SISTEMA INICIADO EXITOSAMENTE    "
echo "======================================"
echo ""
echo "Componentes activos:"
echo "  - GA Réplica (PID: $GA_PID) - Puerto: $GA_PORT"
echo "  - GC (PID: $GC_PID) - PUB: $PUB_PORT, REP: $REP_PORT"
echo "  - Actor Devolución (PID: $DEV_PID)"
echo "  - Actor Renovación (PID: $REN_PID)"
echo "  - Actor Préstamo (PID: $PRES_PID)"
echo ""
echo "Logs guardados en: logs/"
echo ""
echo "Para ver los logs en tiempo real:"
echo "  tail -f logs/ga_sede2.log"
echo "  tail -f logs/gc_sede2.log"
echo ""
echo "Para detener todo el sistema:"
echo "  kill $GA_PID $GC_PID $DEV_PID $REN_PID $PRES_PID"
echo ""
echo "Presiona Ctrl+C para detener el sistema..."

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
echo "  java -cp $CLASSPATH ClienteBatch_ZMQ src/peticiones2.txt ${GA_HOST}:${REP_PORT}"
echo ""
