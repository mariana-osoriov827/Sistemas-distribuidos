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

# Crear directorio de logs si no existe
mkdir -p logs

echo ""
echo "Iniciando sistema..."
echo "Todas las operaciones se mostrarán en esta terminal"
echo ""

# Contadores de éxito/fallo
COMPONENTS_STARTED=0
COMPONENTS_FAILED=0

# Función para verificar si un proceso inició correctamente
check_component() {
    local name=$1
    local pid=$2
    local log=$3
    
    sleep 3
    
    if kill -0 $pid 2>/dev/null; then
        # El proceso está vivo, verificar si hay errores en la salida
        if grep -qi "error\|exception\|could not find" "$log" 2>/dev/null; then
            echo "[FALLO] $name (PID: $pid) - Errores detectados"
            cat "$log"
            COMPONENTS_FAILED=$((COMPONENTS_FAILED + 1))
            kill $pid 2>/dev/null
            return 1
        else
            echo "[OK] $name iniciado correctamente (PID: $pid)"
            COMPONENTS_STARTED=$((COMPONENTS_STARTED + 1))
            return 0
        fi
    else
        echo "[FALLO] $name - El proceso terminó inmediatamente"
        if [ -f "$log" ]; then
            cat "$log"
        fi
        COMPONENTS_FAILED=$((COMPONENTS_FAILED + 1))
        return 1
    fi
}

# Gestor de Almacenamiento (GA) Réplica
echo "[1/5] Iniciando Gestor de Almacenamiento (GA) Réplica..."
GA_LOG=$(mktemp)
java -cp "$CP" Gestor_Almacenamiento.ServidorGA_TCP replica $GA_PORT > "$GA_LOG" 2>&1 &
GA_PID=$!
check_component "Gestor de Almacenamiento (GA) Réplica" $GA_PID "$GA_LOG"

# Gestor de Carga (GC)
echo "[2/5] Iniciando Gestor de Carga (GC)..."
GC_LOG=$(mktemp)
java -cp "$CP" Gestor_carga.ServidorGC_ZMQ $SEDE $PUB_PORT $REP_PORT $GA_HOST $GA_PORT > "$GC_LOG" 2>&1 &
GC_PID=$!
check_component "Gestor de Carga (GC)" $GC_PID "$GC_LOG"

# Actores
echo "[3/5] Iniciando Actor Devolución..."
DEV_LOG=$(mktemp)
java -cp "$CP" Gestor_carga.ActorClient_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} DEVOLUCION > "$DEV_LOG" 2>&1 &
DEV_PID=$!
check_component "Actor Devolución" $DEV_PID "$DEV_LOG"

echo "[4/5] Iniciando Actor Renovación..."
REN_LOG=$(mktemp)
java -cp "$CP" Gestor_carga.ActorClient_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} RENOVACION > "$REN_LOG" 2>&1 &
REN_PID=$!
check_component "Actor Renovación" $REN_PID "$REN_LOG"

echo "[5/5] Iniciando Actor Préstamo..."
PRES_LOG=$(mktemp)
java -cp "$CP" Gestor_carga.ActorPrestamo_ZMQ ${GA_HOST}:${PUB_PORT} ${GA_HOST}:${GA_PORT} > "$PRES_LOG" 2>&1 &
PRES_PID=$!
check_component "Actor Préstamo" $PRES_PID "$PRES_LOG"

echo ""
echo "======================================"
if [ $COMPONENTS_FAILED -eq 0 ]; then
    echo "  SISTEMA INICIADO EXITOSAMENTE      "
    echo "======================================"
    echo ""
    echo "Componentes activos: $COMPONENTS_STARTED/5"
    echo "  - GA Réplica (PID: $GA_PID) - Puerto: $GA_PORT"
    echo "  - GC (PID: $GC_PID) - PUB: $PUB_PORT, REP: $REP_PORT"
    echo "  - Actor Devolución (PID: $DEV_PID)"
    echo "  - Actor Renovación (PID: $REN_PID)"
    echo "  - Actor Préstamo (PID: $PRES_PID)"
    echo ""
    echo "Para ejecutar cliente interactivo:"
    echo "  ./cliente.sh"
    echo ""
    echo "======================================"
    echo ""
    
    # Función para limpiar al salir
    cleanup() {
        echo ""
        echo "Deteniendo sistema..."
        kill $GA_PID $GC_PID $DEV_PID $REN_PID $PRES_PID 2>/dev/null
        rm -f "$GA_LOG" "$GC_LOG" "$DEV_LOG" "$REN_LOG" "$PRES_LOG"
        echo "Sistema detenido."
        exit 0
    }

    trap cleanup SIGINT SIGTERM

    # Mantener el script corriendo y mostrar salidas
    echo "Monitoreando salidas del sistema (Ctrl+C para detener):"
    echo "======================================"
    tail -f "$GA_LOG" "$GC_LOG" "$DEV_LOG" "$REN_LOG" "$PRES_LOG" 2>/dev/null
else
    echo "  FALLO AL INICIAR SISTEMA           "
    echo "======================================"
    echo ""
    echo "Componentes iniciados: $COMPONENTS_STARTED/5"
    echo "Componentes fallidos: $COMPONENTS_FAILED/5"
    echo ""
    echo "DIAGNÓSTICO:"
    echo "- Verifica que compilaste con: mvn clean compile"
    echo "- Verifica que los archivos estén en src/main/java/"
    echo "- Verifica que todas las clases tengan declaración de package"
    echo ""
    
    # Detener procesos que sí iniciaron
    kill $GA_PID $GC_PID $DEV_PID $REN_PID $PRES_PID 2>/dev/null
    rm -f "$GA_LOG" "$GC_LOG" "$DEV_LOG" "$REN_LOG" "$PRES_LOG"
    exit 1
fi
