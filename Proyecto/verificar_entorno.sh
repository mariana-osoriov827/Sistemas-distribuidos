#!/bin/bash
# Script de verificación del entorno antes de ejecutar

echo "======================================"
echo "  VERIFICACIÓN DEL ENTORNO           "
echo "======================================"
echo ""

# 1. Verificar directorio actual
echo "[1] Directorio actual:"
pwd
echo ""

# 2. Verificar que exista target/classes
echo "[2] Verificando target/classes..."
if [ -d "target/classes" ]; then
    echo "✓ target/classes existe"
    echo "  Contenido:"
    ls -la target/classes/
else
    echo "✗ ERROR: target/classes NO existe"
    echo "  SOLUCIÓN: Ejecuta 'mvn clean compile' primero"
fi
echo ""

# 3. Verificar clases compiladas de Gestor_carga
echo "[3] Verificando clases de Gestor_carga..."
if [ -d "target/classes/Gestor_carga" ]; then
    echo "✓ Gestor_carga existe"
    echo "  Clases encontradas:"
    ls -la target/classes/Gestor_carga/*.class | awk '{print "    - " $9}'
else
    echo "✗ ERROR: target/classes/Gestor_carga NO existe"
fi
echo ""

# 4. Verificar JeroMQ
echo "[4] Verificando JeroMQ..."
JEROMQ_PATH="$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar"
if [ -f "$JEROMQ_PATH" ]; then
    echo "✓ JeroMQ encontrado en:"
    echo "  $JEROMQ_PATH"
else
    echo "✗ ERROR: JeroMQ NO encontrado"
    echo "  SOLUCIÓN: Ejecuta 'mvn dependency:resolve' o 'mvn clean compile'"
fi
echo ""

# 5. Verificar Java
echo "[5] Verificando Java..."
java -version 2>&1 | head -n 1
echo ""

# 6. Verificar Maven
echo "[6] Verificando Maven..."
if command -v mvn &> /dev/null; then
    mvn -version | head -n 1
else
    echo "✗ Maven no encontrado en PATH"
fi
echo ""

# 7. Test del classpath
echo "[7] Test del classpath..."
CLASSPATH="target/classes:$HOME/.m2/repository/org/zeromq/jeromq/0.6.0/jeromq-0.6.0.jar"
echo "  CLASSPATH configurado como:"
echo "  $CLASSPATH"
echo ""

echo "======================================"
echo "Si hay errores (✗), debes solucionarlos antes de ejecutar los scripts"
echo "======================================"
