# KoolFileIndexer – Backend (Parte 1: Modelo de Dominio)

Este módulo implementa el **modelo de dominio** de KoolFileIndexer, con todas las **reglas de negocio** y validaciones definidas en la Fase 1.

---
## 📂 Estructura del Proyecto
KoolFileIndexer/
├── src/
│ ├── modelo/ # Clases de dominio
│ │ ├── Archivo.java
│ │ ├── Categoria.java
│ │ ├── Etiqueta.java
│ │ └── ValidadorEntrada.java
│ ├── logica/ # Pruebas CLI
│ │ └── MainParte1.java
│ └── persistencia/ # (Parte 2)
├── DOCS/
│ └── REGLAS_NEGOCIO.md # Detalle de RN-001…RN-006
└── README.md # Documentación principal
---

## 🔍 Parte 1: Modelo de Dominio

Se implementaron las clases y validaciones centrales:

- **Archivo**  
  Representa un archivo local indexado, con metadatos (nombre, ruta, extensión, tamaño, fechas), etiquetas y palabras clave.

- **Categoria**  
  Clasificación automática por extensión. Extensiones desconocidas → categoría **“Sin categoría”**.

- **Etiqueta**  
  Creación y renombrado de etiquetas manuales, con validación de longitud y patrones.

- **ValidadorEntrada**  
  Único punto de validación para etiquetas, palabras clave y nombres de categoría (1–50 caracteres, patrón alfanumérico, guiones y un único espacio).

---

## ✅ Reglas de Negocio Implementadas

| ID     | Descripción                                                                              |
|--------|------------------------------------------------------------------------------------------|
| **RN-001** | Rutas absolutas y normalizadas; nombres trimmeados.                                   |
| **RN-002** | Etiquetas renombrables (1–50 chars), propagación via Service Layer.                   |
| **RN-003** | Validación uniforme 1–50 chars; etiquetas permiten un espacio interno, palabras clave sin espacios. |
| **RN-004** | Categoría predeterminada `SIN_CATEGORIA` en lugar de `null`.                          |

Consulta `DOCS/REGLAS_NEGOCIO.md` para ver todas las RN hasta la versión 2.0.

---

## 🛠️ Pruebas de la Parte 1

Compila y ejecuta el test de consola para verificar todas las validaciones:

```bash
cd src
javac modelo/*.java logica/MainParte1.java
java logica.MainParte1
La salida debe validar:

Normalización de rutas y nombres

Clasificación automática

esOculto()/esValido()

Etiquetas (duplicados, longitud, espacios, caracteres especiales)

Palabras clave (duplicados, longitud, sin espacios)

Actualización de fecha de modificación

📈 Patrones de Diseño
Factory Method

Categoria.clasificar(...)

Etiqueta.crear(...)

Inmutabilidad parcial

Campos final para datos base en Archivo y Categoria.

Singleton (implementado en Parte 2)

⏭ Próximos Pasos (Parte 2)
Indexador

Lectura recursiva de directorios

Exclusiones

Identificación de archivos por fileKey

Actualización continua por lotes

Buscador

Filtrado por nombre, extensión, tamaño, etiquetas, palabras clave y categoría

Persistencia

Integración con RepositorioArchivo y base de datos PostgreSQL

Service Layer

Propagación de renombrados de etiquetas

Eliminación de etiquetas huérfanas