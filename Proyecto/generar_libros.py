"""
Generador de libros.txt para el proyecto de biblioteca Ada Lovelace
Genera 1000 libros con 200 prestados (50 en sede 1, 150 en sede 2)
Actualizado: 17/11/2025
"""
import random
from datetime import datetime, timedelta

# Nombres de libros variados y realistas
titulos = [
    "Algoritmos y Complejidad", "Estructuras de Datos Avanzadas", "Redes de Computadores", 
    "Bases de Datos Distribuidas", "Inteligencia Artificial Moderna",
    "Cálculo Diferencial e Integral", "Álgebra Lineal Aplicada", "Física para Ingenieros I", 
    "Física para Ingenieros II", "Química Orgánica Básica",
    "Programación Orientada a Objetos en Java", "Python para Data Science", 
    "Programación Avanzada en C++", "Desarrollo Web Full Stack",
    "Ingeniería de Software Práctica", "Sistemas Operativos Modernos", 
    "Arquitectura de Computadores y Ensamblador",
    "Diseño de Compiladores", "Teoría de la Computación", "Criptografía y Seguridad",
    "Seguridad Informática Empresarial", "Machine Learning Práctico", 
    "Deep Learning con TensorFlow", "Procesamiento de Lenguaje Natural",
    "Visión por Computador con OpenCV", "Robótica Industrial", 
    "Internet de las Cosas con Arduino", "Cloud Computing con AWS",
    "DevOps y Metodologías Ágiles", "Arquitectura de Microservicios", 
    "Contenedores con Docker", "Orquestación con Kubernetes",
    "Análisis Numérico Computacional", "Ecuaciones Diferenciales Ordinarias", 
    "Probabilidad y Estadística para Ingeniería",
    "Investigación de Operaciones", "Métodos de Optimización", "Teoría de Grafos Aplicada",
    "Lógica Matemática y Computacional", "Geometría Analítica del Espacio", 
    "Historia de la Computación",
    "Ética en Tecnología e IA", "Gestión Ágil de Proyectos", 
    "Emprendimiento Tecnológico y Startups",
    "Marketing Digital Estratégico", "E-commerce y Negocios Digitales", 
    "Blockchain y Criptomonedas", "Ciberseguridad Avanzada y Hacking Ético",
    "Big Data y Analytics", "Computación Cuántica", "Realidad Virtual y Aumentada",
    "Game Development con Unity", "Diseño de Experiencia de Usuario", 
    "Testing y Quality Assurance"
]

def generar_fecha_prestamo():
    """Genera una fecha de préstamo aleatoria en el último mes"""
    dias_atras = random.randint(1, 30)
    fecha = datetime.now() - timedelta(days=dias_atras)
    return fecha.strftime("%d-%m-%Y")

def generar_libros(filename, total_libros=1000, prestados_sede1=50, prestados_sede2=150):
    """Genera archivo de libros con la estructura requerida"""
    
    with open(filename, 'w', encoding='utf-8') as f:
        codigo_base = 100
        prestados_generados = 0
        
        for i in range(total_libros):
            # Seleccionar título (puede repetirse)
            titulo = random.choice(titulos)
            if random.random() < 0.3:  # 30% tienen sufijo numérico
                titulo += f" {random.randint(1, 5)}"
            
            # Código único
            codigo = codigo_base + i
            
            # Número de ejemplares (1-5)
            num_ejemplares = random.randint(1, 5)
            
            # Escribir encabezado del libro
            f.write(f"{titulo}, {codigo}, {num_ejemplares}\n")
            
            # Generar ejemplares
            for j in range(1, num_ejemplares + 1):
                # Determinar si este ejemplar está prestado
                debe_estar_prestado = False
                
                if prestados_generados < prestados_sede1 + prestados_sede2:
                    # Probabilidad de estar prestado
                    if random.random() < 0.25:  # 25% de probabilidad
                        debe_estar_prestado = True
                        prestados_generados += 1
                
                if debe_estar_prestado:
                    estado = 'P'
                    fecha = generar_fecha_prestamo()
                    f.write(f"{j}, {estado}, {fecha}\n")
                else:
                    estado = 'D'
                    f.write(f"{j}, {estado}, \n")
            
            # Línea en blanco entre libros
            f.write("\n")
    
    print(f"✓ Generados {total_libros} libros en {filename}")
    print(f"✓ {prestados_generados} ejemplares en estado prestado")

if __name__ == "__main__":
    generar_libros("src/libros.txt", 1000, 50, 150)
    print("\n¡Listo! Archivo src/libros.txt generado con 1000 libros.")
