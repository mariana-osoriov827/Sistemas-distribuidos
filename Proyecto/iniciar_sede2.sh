#!/bin/bash
# Script de inicio para Sede 2 - Linux
# Sistema Distribuido de Préstamo de Libros

echo "======================================"
echo "  SISTEMA DE PRÉSTAMO DE LIBROS      "
echo "           SEDE 2                     "
echo "======================================"

# --- CONFIGURACIÓN DE PUERTOS Y IPs ---
SEDE=2
PUB_PORT=6555   # Puerto PUB para el Gestor de Carga (GC) de Sede 2
REP_PORT=6556   # Puerto REP para Clientes (PS) de Sede 2
GA_PORT=6560    # Puerto GA local de Sede 2 (Primario)

# Lista de GAs: primario (local) y backup (Sede 1)
# ¡IMPORTANTE! Reemplaza '10.43.103.49' por la IP real del servidor de la Sede 1.
GA_LIST="localhost:${GA_PORT},10.43.103.49:5560"

# IP de la réplica (Sede 1)
REPLICA_HOST="10.43.103.49" 
REPLICA_PORT=5560

# IP local del GC de Sede 2 para que los Actores se suscriban (debe ser la IP real o 'localhost')
GC_IP="localhost" 

# Classpath con dependencias (Verifica la versión de JeroMQ)
CP="target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar"


# Crear directorio de logs si no existe
mkdir -p logs

# Contadores globales
COMPONENTS_FAILED=0
COMPONENTS_STARTED=0

# Función para checar componentes (Se deja igual)
check_component() {
    name="$1"
    pid=$2
    log=$3
    sleep 1
    if kill -0 $pid 2>/dev/null; then
        if grep -qi "error\|exception\|could not find" "$log" 2>/dev/null; then
            echo "[FALLO] $name (PID: $pid) - Errores detectados"
            cat "$log"
            COMPONENTS_FAILED=$((COMPONENTS_FAILED + 1))
            kill $pid 2>/dev/null
        else
            echo "[OK] $name iniciado correctamente (PID: $pid)"
            COMPONENTS_STARTED=$((COMPONENTS_STARTED + 1))
        fi
    else
        echo "[FALLO] $name - El proceso terminó inmediatamente"
        if [ -f "$log" ]; then
            cat "$log"
        fi
        COMPONENTS_FAILED=$((COMPONENTS_FAILED + 1))
    fi
}


# --- INICIO DE COMPONENTES ---

# 1. Gestor de Almacenamiento (GA) - Primario de Sede 2
echo "[1/5] Iniciando Gestor de Almacenamiento (GA)..."
GA_LOG=$(mktemp)
java -cp "$CP" Gestor_Almacenamiento.ServidorGA_TCP primary $GA_PORT $REPLICA_HOST $REPLICA_PORT > "$GA_LOG" 2>&1 &
GA_PID=$!
check_component "Gestor de Almacenamiento (GA)" $GA_PID "$GA_LOG"

# 2. Gestor de Carga (GC) - Único en Sede 2
echo "[2/5] Iniciando Gestor de Carga (GC)..."
GC_LOG=$(mktemp)
# El GC recibe la lista de GAs para su Proxy
java -cp "$CP" Gestor_carga.ServidorGC_ZMQ $SEDE $PUB_PORT $REP_PORT $GA_LIST > "$GC_LOG" 2>&1 &
GC_PID=$!
check_component "Gestor de Carga (GC)" $GC_PID "$GC_LOG"

# 3. Actor Devolución (Se suscribe al GC y usa la lista de GAs)
echo "[3/5] Iniciando Actor Devolución..."
DEV_LOG=$(mktemp)
# CORRECCIÓN: Pasa ${GA_LIST}
java -cp "$CP" Gestor_carga.ActorClient_ZMQ ${GC_IP}:${PUB_PORT} ${GA_LIST} DEVOLUCION > "$DEV_LOG" 2>&1 &
DEV_PID=$!
check_component "Actor Devolución" $DEV_PID "$DEV_LOG"

# 4. Actor Renovación (Se suscribe al GC y usa la lista de GAs)
echo "[4/5] Iniciando Actor Renovación..."
REN_LOG=$(mktemp)
# CORRECCIÓN: Pasa ${GA_LIST}
java -cp "$CP" Gestor_carga.ActorClient_ZMQ ${GC_IP}:${PUB_PORT} ${GA_LIST} RENOVACION > "$REN_LOG" 2>&1 &
REN_PID=$!
check_component "Actor Renovación" $REN_PID "$REN_LOG"

# 5. Actor Préstamo (Se suscribe al GC y usa la lista de GAs)
echo "[5/5] Iniciando Actor Préstamo..."
PRES_LOG=$(mktemp)
# CORRECCIÓN: Pasa ${GA_LIST}
java -cp "$CP" Gestor_carga.ActorPrestamo_ZMQ ${GC_IP}:${PUB_PORT} ${GA_LIST} > "$PRES_LOG" 2>&1 &
PRES_PID=$!
check_component "Actor Préstamo" $PRES_PID "$PRES_LOG"

# --- CIERRE Y MONITOREO (Se deja igual) ---
echo ""
echo "======================================"
if [ $COMPONENTS_FAILED -eq 0 ]; then
    echo "  SISTEMA INICIADO EXITOSAMENTE      "
    echo "======================================"
    echo ""
    echo "Componentes activos: $COMPONENTS_STARTED/5"
    echo "  - GA (PID: $GA_PID) - Puerto: $GA_PORT"
    echo "  - GC (PID: $GC_PID) - PUB: $PUB_PORT, REP: $REP_PORT"
    echo "  - Actor Devolución (PID: $DEV_PID)"
    echo "  - Actor Renovación (PID: $REN_PID)"
    echo "  - Actor Préstamo (PID: $PRES_PID)"
    echo ""
    echo "Para ejecutar cliente interactivo:"
    echo "  ./cliente.sh [IP_GC_SEDE2]"
    echo ""
    echo "======================================"
    echo ""

    cleanup() {
        echo ""
        echo "Deteniendo sistema..."
        kill $GA_PID $GC_PID $DEV_PID $REN_PID $PRES_PID 2>/dev/null
        rm -f "$GA_LOG" "$GC_LOG" "$DEV_LOG" "$REN_LOG" "$PRES_LOG"
        echo "Sistema detenido."
        exit 0
    }

    trap cleanup SIGINT SIGTERM

    echo "Monitoreando salidas del sistema (Ctrl+C para detener):"
    echo "======================================"
    tail -f "$GA_LOG" "$GC_LOG" "$DEV_LOG" "$REN_LOG" "$PRES_LOG" 2>/dev/null
else
    echo "  FALLO AL INICIAR SISTEMA           "
    echo "======================================"
    echo ""
    echo "Componentes iniciados: $COMPONENTS_STARTED/5"
    echo "Componentes fallidos: $COMPONENTS_FAILED/5"
    echo ""
    echo "DIAGNÓSTICO:"
    echo "- Verifica que compilaste con: mvn clean compile"
    echo "- Verifica los logs de fallo arriba."
    echo ""
    
    kill $GA_PID $GC_PID $DEV_PID $REN_PID $PRES_PID 2>/dev/null
    rm -f "$GA_LOG" "$GC_LOG" "$DEV_LOG" "$REN_LOG" "$PRES_LOG"
    exit 1
fi