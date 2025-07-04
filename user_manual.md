# 📘 Manual de Usuario — KoolFileIndexer
---

## 🧩 Descripción General

**KoolFileIndexer** es una aplicación de escritorio desarrollada en Java, cuyo objetivo es **indexar archivos locales**, extraer y almacenar sus **metadatos** y **palabras clave**, para facilitar su organización y búsqueda. Solo necesitas seleccionar las carpetas que deseas procesar y la aplicación comenzará a trabajar automáticamente.

Esta herramienta mejorará y optimizará la forma en que gestionas tus archivos, evitando que pierdas tiempo en tareas repetitivas e innecesarias, y permitiéndote enfocarte en lo que realmente importa: mejorar y expandir tu trabajo, proyectos o productividad.

---

## 🚀 Instalación

## 🖥️ Requisitos del Sistema

KoolFileIndexer es una aplicación de escritorio desarrollada para funcionar en sistemas operativos modernos. A continuación se describen los requisitos mínimos y recomendados estimados para su correcto funcionamiento.

### Requisitos mínimos (estimados)

- Sistema operativo: Windows 10, Ubuntu 20.04 o macOS 11 (64 bits)
- Procesador: Doble núcleo 
- Memoria RAM: Al menos **2 GB** disponibles 
- Espacio en disco: **200 MB** libres (para el ejecutable, archivos temporales y base de datos mínima)
- Resolución de pantalla: 1024x768
- Java: JDK 11 o superior
- Base de datos: PostgreSQL 13 instalado localmente

### Requisitos recomendados (estimados)

- Sistema operativo: Windows 11, Ubuntu 22.04 o macOS 13 (64 bits)
- Procesador: Cuatro núcleos (placeholder)
- Memoria RAM: **4 GB o más**
- Espacio en disco: **500 MB o más**, dependiendo de la cantidad de archivos a indexar
- Resolución de pantalla: 1366x768 o superior
- Java: JDK 17
- Base de datos: PostgreSQL 14 o superior

> ⚠️ Nota: Estos valores son aproximados. Los requerimientos reales pueden variar según la cantidad y el tamaño de los directorios indexados, la cantidad de archivos procesados, y el uso de memoria de la interfaz gráfica y la base de datos.


### Opción 1: Instalador nativo (recomendado)

Si estás utilizando Windows, Linux o macOS, puedes instalar la aplicación como cualquier otro software utilizando un instalador.

**Pasos:**

1. Descarga el instalador correspondiente a tu sistema operativo.
2. Ejecuta el archivo y sigue el asistente de instalación.
3. Una vez instalado, encontrarás un acceso directo en tu menú de aplicaciones o escritorio.

---

### Opción 2: Ejecutar desde archivo `.jar` (avanzado)

Si prefieres o necesitas ejecutar la aplicación desde el archivo `.jar`.

**Pasos:**

1. Descarga el archivo `KoolFileIndexer.jar`
2. Abre una terminal y navega hasta la carpeta donde lo guardaste.
3. Ejecuta el siguiente comando:

   ```bash
   java -jar KoolFileIndexer.jar

---

## ⌨️ Ingreso a la Aplicación

Para ingresar a la aplicación encontraras un acceso direco en tu menú de aplicaciones o escritorio, haz doble clic para iniciar la aplicación.

### Menú Principal del Sistema

>Descripción de la pantalla principal pendiente.

>  **Captura de pantalla pendiente. Aquí se mostrará la interfaz principal de la aplicación.**

> ⚠️ **Importante:**  
> Esta aplicación ha sido desarrollada con una **Interfaz Gráfica**, por lo cual debes estar familiarizado con este entorno y conocer aspectos básicos como:
>
> - Uso del mouse  
> - Manejo de ventanas (abrir, cerrar, minimizar, maximizar, moverlas con el mouse, etc.)  
> - Uso de botones  
> - Desplazamiento de datos dentro de una ventana, utilizando barras de avance horizontal y vertical  

---

## 📝Instrucciones Basicas 

### Indexar Archivos

En este menú se muestran todas las carpetas a las que la aplicación tiene acceso (exceptuando las protegidas). Las carpetas marcadas con ✔️ están incluidas para que el sistema las recorra e indexe los archivos. Si no deseas que se acceda a alguna carpeta, simplemente desmárcala. Luego, haz clic en el botón Confirmar para que KoolFileIndexer comience el proceso.

>  **Captura de pantalla pendiente. Aquí se mostrará la interfaz de indexación de carpetas.**

### Buscar Archivos

En el menú principal encontrarás una barra de búsqueda donde puedes ingresar el nombre del archivo o palabras clave relacionadas. KoolFileIndexer buscará coincidencias en su índice y mostrará los resultados en una ventana emergente. Para refinar la búsqueda, puedes utilizar filtros como tipo de archivo, tamaño o fecha de creación.  

>  **Captura de pantalla pendiente.**

### Asociar Etiquetas 

Otra forma de mejorar la organización y búsqueda de archivos es a través de etiquetas. Puedes crearlas, modificarlas o eliminarlas desde el menú de administración de etiquetas. Luego, podrás asociar etiquetas a archivos específicos para facilitar su localización.

>  **Captura de pantalla pendiente. **

> ⚠️ **Importante: **  
> Este documento es **preliminar**, se ira actualizando a medida del desarrollo de la aplicación.

---