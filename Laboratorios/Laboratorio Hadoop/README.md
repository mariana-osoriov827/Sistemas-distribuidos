# Autores
- Gabriel Jaramillo Cuberos
- Juan Esteban Vera
- Mariana Osorio Vásquez
- Roberth Santiago Méndez

# 🗃️ Sistema de Ficheros Hadoop HDFS – Laboratorio

## 📘 Descripción General
Este repositorio contiene el desarrollo completo del **laboratorio de configuración del Sistema de Ficheros Distribuido de Hadoop (HDFS)** y la implementación de un **clúster Hadoop multinodo** con soporte para YARN y MapReduce.  
El propósito fue instalar, configurar y validar un entorno funcional de procesamiento distribuido, siguiendo la guía oficial de Apache Hadoop y el documento académico *“Sistema de Ficheros Hadoop HDFS”* de la Pontificia Universidad Javeriana.

---

## ⚙️ Objetivos del Laboratorio
- Comprender el funcionamiento del sistema de ficheros distribuido **HDFS**.  
- Configurar un **clúster multinodo** que integre HDFS, YARN y MapReduce.  
- Establecer comunicación SSH sin contraseña entre nodos.  
- Validar los servicios principales: **NameNode**, **DataNode**, **ResourceManager** y **NodeManager**.  
- Ejecutar un trabajo de procesamiento distribuido con MapReduce y analizar los resultados obtenidos.

---

## 📂 Archivos de Configuración

### `core-site.xml`
Define el sistema de archivos por defecto y la dirección del NameNode:
```xml
<configuration>
  <property>
    <name>fs.defaultFS</name>
    <value>hdfs://nn-host:9870</value>
  </property>
</configuration>
```

### `hdfs-site.xml`
Especifica los directorios locales y el factor de replicación:
```xml
<configuration>
  <property>
    <name>dfs.replication</name>
    <value>2</value>
  </property>
  <property>
    <name>dfs.namenode.name.dir</name>
    <value>file:///data/nn/name</value>
  </property>
  <property>
    <name>dfs.datanode.data.dir</name>
    <value>file:///data/dn/data</value>
  </property>
</configuration>
```

### `yarn-site.xml`
Configura el planificador de recursos:
```xml
<configuration>
  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>rm-host</value>
  </property>
  <property>
    <name>yarn.nodemanager.aux-services</name>
    <value>mapreduce_shuffle</value>
  </property>
</configuration>
```

### `mapred-site.xml`
Define el framework MapReduce:
```xml
<configuration>
  <property>
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
  </property>
</configuration>
```

---

## Inicio del Clúster

1. **Formatear el NameNode**
   ```bash
   hdfs namenode -format
   ```

2. **Iniciar HDFS**
   ```bash
   start-dfs.sh
   ```

3. **Iniciar YARN**
   ```bash
   start-yarn.sh
   ```

4. **Verificar procesos activos**
   ```bash
   jps
   ```

5. **Interfaces web**
   - NameNode → `http://nn-host:9870`
   - ResourceManager → `http://rm-host:8088`

---

## 🧪 Funcionamiento

### Operaciones básicas de HDFS
```bash
hdfs dfs -mkdir /user/hadoop
hdfs dfs -put etc/hadoop/*.xml /user/hadoop/input
hdfs dfs -ls /user/hadoop/input
```

### Ejecución de trabajo MapReduce (ejemplo `grep`)
```bash
hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-*.jar grep input output 'dfs[a-z.]+'
hdfs dfs -cat /user/hadoop/output/part-r-00000
```

Resultados esperados:
- Directorio `/output` generado correctamente.  
- Comunicación entre nodos estable.  
- Replicación de bloques en distintos DataNodes.  
- Trabajo MapReduce ejecutado sin errores.

---

## ⚒️ Resultados
- Todos los daemons (`NameNode`, `DataNodes`, `ResourceManager`, `NodeManagers`) activos y sincronizados.  
- Replicación funcional (`dfs.replication = 2`) confirmada.  
- Correcta comunicación entre maestros y trabajadores mediante SSH.  
- Interfaz web del NameNode muestra el estado y capacidad total del clúster.  
- El ejemplo MapReduce completó su ejecución y generó los resultados esperados.  
- Problemas iniciales con `pdsh` fueron resueltos ajustando permisos SSH y variables de entorno.

---

## 📚 Referencias
- Apache Software Foundation. (2025). [*Cluster Setup – Hadoop 3.4.2 Documentation*](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html).  
---
**Juan Esteban Vera**  
Pontificia Universidad Javeriana – Ingeniería de Sistemas (2025)
