# Multiagente Identificador de Vehículos

Sistema multiagente basado en **JADE** para la detección automática de vehículos en imágenes mediante el modelo de visión por computador **YOLOv8**.

---

## Descripción

El sistema analiza imágenes de carretera e identifica automáticamente los vehículos presentes en ellas (coches, motos, camiones, autobuses, bicicletas, barcos, aviones y trenes). Para ello, combina la plataforma de agentes JADE con OpenCV y un modelo ONNX de YOLOv8, mostrando los resultados en una interfaz gráfica Swing.

---

## Arquitectura del sistema

El sistema se organiza en tres agentes que se comunican mediante mensajes ACL (FIPA):

```
┌─────────────────────────────────────────────────────────┐
│                    JADE Platform                        │
│                                                         │
│  ┌─────────────────┐       crea        ┌─────────────┐  │
│  │ PerceptionAgent │─────────────────▶ │ Processing  │  │
│  │                 │◀───────────────── │   Agent     │  │
│  │  Escanea cada   │  processor-       │ (x hasta 10)│  │
│  │  5 s la carpeta │  finished         │             │  │
│  │  imagenes/      │                   │  YOLOv8 +   │  │
│  └─────────────────┘                   │  OpenCV     │  │
│                                        └──────┬──────┘  │
│                                               │         │
│                                  detection-   │         │
│                                  result       ▼         │
│                               ┌───────────────────────┐ │
│                               │       UIAgent         │ │
│                               │                       │ │
│                               │  Interfaz Swing con   │ │
│                               │  pestañas por imagen  │ │
│                               └───────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Agentes

| Agente | Responsabilidad |
|--------|----------------|
| **PerceptionAgent** | Escanea la carpeta `imagenes/` cada 5 segundos, encola las imágenes nuevas y gestiona un pool de hasta 10 `ProcessingAgent` concurrentes. |
| **ProcessingAgent** | Agente efímero (uno por imagen). Carga el modelo YOLOv8, ejecuta la inferencia con OpenCV DNN, filtra las clases de vehículos y envía el resultado al `UIAgent`. Se autodestruye al finalizar. |
| **UIAgent** | Escucha los mensajes de resultados y actualiza la interfaz gráfica Swing, añadiendo una pestaña por cada imagen procesada. |

---

## Requisitos previos

- **Java 11** o superior
- **Maven 3.6+**
- **Windows** (la librería nativa OpenCV incluida es `.dll` para Windows x64)

> El proyecto incluye `opencv_java4120.dll` precompilada para Windows. Para ejecutarlo en Linux/macOS es necesario compilar o descargar la librería nativa de OpenCV 4.12.0 correspondiente y actualizar la ruta en `ProcessingAgent.java`.

---

## Dependencias principales

Las siguientes dependencias deben añadirse al `pom.xml` (no están incluidas en el repositorio):

- **JADE** — plataforma multiagente
- **OpenCV** (`opencv-4120.jar`) — visión por computador
- **YOLOv8n ONNX** — modelo de detección (incluido en `src/main/resources/models/`)

---

## Instalación y ejecución 

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/multiagente-identificador-vehiculos.git
cd multiagente-identificador-vehiculos
```

### 2. Añadir las dependencias

Descarga los JARs de JADE y OpenCV y configúralos en el `pom.xml` como dependencias locales o instálalos en tu repositorio Maven local:

```bash
mvn install:install-file -Dfile=jade.jar -DgroupId=com.tilab.jade \
    -DartifactId=jade -Dversion=4.6.0 -Dpackaging=jar

mvn install:install-file -Dfile=opencv-4120.jar -DgroupId=org.opencv \
    -DartifactId=opencv -Dversion=4.12.0 -Dpackaging=jar
```

### 3. Compilar

```bash
mvn clean package
```

### 4. Añadir imágenes

Coloca las imágenes que quieras analizar en la carpeta `imagenes/` del directorio raíz. Se admiten los formatos `.jpg`, `.jpeg`, `.png` y `.webp`.

```
imagenes/
├── calle1.jpg
├── autopista.png
└── ...
```

### 5. Ejecutar

Ejecutar la clase `es.upm.idvehiculos.Main` como aplicación Java.


---

## Interfaz de usuario

Al iniciar la aplicación se abre la ventana principal de JADE (RMA) y la interfaz gráfica del sistema. Por cada imagen procesada aparece una nueva pestaña con:

- La imagen analizada.
- Los tipos de vehículos detectados.

---

## Estructura del proyecto

```
multiagente-identificador-vehiculos/
├── imagenes/                          # Carpeta de entrada de imágenes
├── src/
│   └── main/
│       ├── java/es/upm/
│       │   ├── agents/
│       │   │   ├── PerceptionAgent.java   # Escaneo y gestión del pool
│       │   │   ├── ProcessingAgent.java   # Inferencia YOLOv8
│       │   │   └── UIAgent.java           # Presentación de resultados
│       │   ├── idvehiculos/
│       │   │   ├── AgentBase.java         # Clase base para todos los agentes
│       │   │   ├── AgentModel.java        # Enum de tipos de agente
│       │   │   └── Main.java              # Punto de entrada
│       │   └── interfaz/
│       │       ├── MainUIFrame.java       # Ventana principal Swing
│       │       └── ImageSectionPanel.java # Panel de pestaña por imagen
│       └── resources/
│           └── models/
│               ├── yolov8n.onnx           # Modelo de detección
│               ├── coco.names             # Etiquetas de clases COCO
│               └── opencv_java4120.dll    # Librería nativa OpenCV (Windows)
└── pom.xml
```

---

## Clases de vehículos

El sistema filtra las siguientes clases del dataset COCO:

`bicycle` · `car` · `motorcycle` · `bus` · `train` · `truck` · `boat` · `airplane`

---

## Parámetros configurables

| Parámetro | Ubicación | Valor por defecto | Descripción |
|-----------|-----------|-------------------|-------------|
| `IMAGE_FOLDER` | `PerceptionAgent.java` | `imagenes/` | Carpeta de entrada |
| `MAX_PROCESSORS` | `PerceptionAgent.java` | `10` | Máximo de agentes procesadores concurrentes |
| Intervalo de escaneo | `PerceptionAgent.java` | `5000 ms` | Frecuencia de revisión de la carpeta |
| Umbral de confianza | `ProcessingAgent.java` | `0.5` | Score mínimo para considerar una detección válida |
| Tamaño de entrada | `ProcessingAgent.java` | `640×640` | Resolución del blob para YOLOv8 |

---

## Licencia

Este proyecto está bajo la licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.

## Uso de IA

Se ha usado la AI como apoyo a la hora de programar, principalmente con el diseño de la UI, y para depurar errores.

Se ha usado además para crear este readme.

> Todos los outputs han sido validados y modificados manualmente antes de ser usados.
