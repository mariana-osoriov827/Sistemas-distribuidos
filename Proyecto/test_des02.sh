#!/bin/bash
# DES-02: Prueba de escalabilidad con 10 PS

echo "======================================"
echo "  PRUEBA DE DESEMPEÑO - DES-02"
echo "  10 Procesos Solicitantes"
echo "  20 solicitudes por proceso"
echo "======================================"

# Crear archivo de peticiones
cat > /tmp/test_requests_small.txt << 'EOF'
PRESTAMO|978-0134685991|testuser
INFO|978-0134685991|testuser
DEVOLUCION|978-0134685991|testuser
PRESTAMO|978-0596009205|testuser
INFO|978-0596009205|testuser
DEVOLUCION|978-0596009205|testuser
PRESTAMO|978-0321573513|testuser
INFO|978-0321573513|testuser
DEVOLUCION|978-0321573513|testuser
PRESTAMO|978-1449355739|testuser
INFO|978-1449355739|testuser
DEVOLUCION|978-1449355739|testuser
PRESTAMO|978-0134685991|testuser
RENOVACION|978-0134685991|testuser
DEVOLUCION|978-0134685991|testuser
PRESTAMO|978-0596009205|testuser
RENOVACION|978-0596009205|testuser
DEVOLUCION|978-0596009205|testuser
INFO|978-0321573513|testuser
INFO|978-1449355739|testuser
EOF

# Lanzar 10 procesos en paralelo
START=$(date +%s%3N)

for i in {1..10}; do
  (
    ./cliente.sh localhost /tmp/test_requests_small.txt > /tmp/log_des02_$i.txt 2>&1
  ) &
done

# Esperar que terminen todos
wait

END=$(date +%s%3N)
TOTAL_TIME=$((END - START))

echo ""
echo "======================================"
echo "  RESULTADOS DES-02"
echo "======================================"
echo "Tiempo total: ${TOTAL_TIME}ms"
echo "Total solicitudes: 200 (10 PS × 20)"
echo "Tiempo promedio por solicitud: $((TOTAL_TIME / 200))ms"
echo "Throughput: $(echo "scale=2; 200000 / $TOTAL_TIME" | bc) solicitudes/segundo"
echo ""
echo "Comparar con DES-01 para ver escalabilidad"
echo "======================================"
