# Atrapame Si Puedes

Un juego móvil de estrategia y lógica desarrollado para Android con estética futurista inspirada en Tron. Los jugadores deben atrapar a un enemigo inteligente en un tablero isométrico con obstáculos dinámicos que cambian según la dificultad.

## Equipo de Desarrollo

- **Johan Esteban Solano Rojas** - 20202578112
- **Diego David Chinchilla Leal** - 20221578047  
- **Juan Eduardo Morales Santana** - 20221578034

## Características Principales

### Arquitectura y Desarrollo
- **Arquitectura MVVM** - Separación clara entre modelo, vista y lógica de negocio
- **Kotlin** - Lenguaje moderno y seguro para Android
- **Material Design 3** - Interfaz moderna con tema Tron personalizado
- **Firebase Firestore** - Base de datos en tiempo real para puntuaciones globales
- **Soporte Multilingüe** - Español e inglés con detección automática del sistema

### Funcionalidades del Juego
- **Tablero Isométrico 3D** - Vista tridimensional con efectos visuales neón
- **Sistema de Colisiones Robusto** - Obstáculos sólidos que bloquean completamente el movimiento
- **Mapas Dinámicos por Dificultad** - Tres layouts completamente diferentes:
  - **Fácil**: Pocos obstáculos dispersos para aprendizaje
  - **Medio**: Laberinto moderado con formas en L y paredes estratégicas
  - **Difícil**: Laberinto denso con patrones complejos y corredores mínimos
- **Sistema de Dificultad Inteligente** - Velocidad del enemigo variable:
  - **Fácil**: 1000ms entre movimientos
  - **Medio**: 750ms entre movimientos  
  - **Difícil**: 500ms entre movimientos
- **Timer en Tiempo Real** - Cronómetro preciso con actualizaciones cada 100ms
- **Sistema de Pausa Completo** - Pausa el timer y movimientos del enemigo
- **Notificaciones Internas** - Alertas de victoria y derrota con opciones
- **Diálogos Interactivos** - Ventanas de resultado con estadísticas y navegación

### Interfaz Tron Futurista
- **Paleta de Colores Neón**:
  - Cian brillante (#00FFFF) para elementos principales
  - Naranja neón (#FF6600) para enemigos y alertas
  - Púrpura (#9966FF) para elementos secundarios
  - Fondos oscuros (#0A0A0A, #1A1A1A) para contraste
- **Efectos Visuales Avanzados**:
  - Sombras neón con resplandor en botones y entidades
  - Bordes luminosos en elementos interactivos
  - Gradientes sutiles en paneles y fondos
- **Tipografía Futurista** - Fuentes monoespaciadas para estadísticas y UI
- **Animaciones Fluidas** - Transiciones suaves entre estados del juego

### Inteligencia Artificial del Enemigo
- **Algoritmo A*** - Pathfinding óptimo para encontrar rutas de escape
- **Comportamiento Estratégico** - El enemigo busca maximizar distancia del jugador
- **Adaptación al Terreno** - Navega inteligentemente alrededor de obstáculos
- **Múltiples Objetivos** - Evalúa todas las posiciones posibles para escapar

## Objetivo del Juego

Atrapa al enemigo naranja usando tu personaje cian en un tablero isométrico. El enemigo utiliza inteligencia artificial avanzada para escapar, calculando rutas óptimas y evitando obstáculos. Tu objetivo es completar cada nivel en el menor tiempo y número de movimientos posibles.

### Mecánicas de Juego
1. **Movimiento**: Toca las celdas adyacentes para mover tu personaje
2. **Estrategia**: Usa los obstáculos para bloquear las rutas de escape del enemigo
3. **Timing**: El enemigo se mueve automáticamente, planifica tus movimientos
4. **Victoria**: Alcanza la misma posición que el enemigo para capturarlo

## Tecnologías Utilizadas

- **Lenguaje**: Kotlin 100%
- **Arquitectura**: MVVM (Model-View-ViewModel) con LiveData
- **Base de Datos**: Firebase Firestore para puntuaciones globales
- **UI Framework**: Material Design 3 con tema personalizado Tron
- **Gestión de Estado**: LiveData, ViewModel y Coroutines
- **Patrones de Diseño**: Repository Pattern, Observer Pattern
- **Renderizado**: Canvas personalizado para vista isométrica 3D
- **Persistencia Local**: SharedPreferences para configuración

## Requerimientos del Proyecto Cumplidos

### Repositorio Público
- Código fuente completo disponible en GitHub
- Historial de commits detallado
- Documentación técnica completa
- APK de release incluido

### Servicios Web (Firebase)
- **Firebase Firestore**: Base de datos NoSQL para almacenamiento de puntuaciones
- **Operaciones CRUD**: Create, Read, Update, Delete de scores
- **Sincronización en tiempo real**: Puntuaciones actualizadas automáticamente
- **Manejo de errores**: Fallback a almacenamiento local si no hay conexión

### Soporte Multilingüe Completo
- **Detección automática**: Basada en configuración del dispositivo Android
- **Recursos externalizados**: 
  - `res/values/strings.xml` (Español - idioma por defecto)
  - `res/values-en/strings.xml` (Inglés)
- **Cobertura total**: Toda la interfaz, mensajes y diálogos traducidos
- **Cambio dinámico**: Sin necesidad de reiniciar la aplicación

### Formularios Interactivos
- **Pantalla de Configuración**: Formulario completo de jugador
- **Campos validados**:
  - Nombre del jugador (requerido, mínimo 2 caracteres)
  - Selección de dificultad (Easy/Medium/Hard)
- **Persistencia**: Configuración guardada localmente con SharedPreferences
- **Validación en tiempo real**: Feedback inmediato al usuario

### Notificaciones Internas
- **Sistema de diálogos**: Notificaciones nativas de Android
- **Tipos de notificación**:
  - Victoria: Muestra tiempo y movimientos realizados
  - Derrota: Opciones de reintentar o volver al menú
  - Configuración guardada: Confirmación de cambios
- **Acciones interactivas**: Botones para navegar o reiniciar

### Arquitectura MVVM Completa
- **Model**: Clases de datos (GameState, Position, Score, etc.)
- **View**: Activities y Views personalizadas (IsometricBoardView)
- **ViewModel**: Lógica de negocio y gestión de estado
- **Repository**: Abstracción de fuentes de datos (Firebase, Local)
- **Separación clara**: Sin lógica de negocio en las vistas

## Instalación y Configuración

### Prerrequisitos del Sistema
- **Android Studio**: Flamingo o superior (2023.1.1+)
- **Android SDK**: API Level 24+ (Android 7.0 Nougat)
- **Gradle**: 8.0+ (incluido con Android Studio)
- **Dispositivo**: Android físico o emulador con API 24+

### Instalación Paso a Paso

1. **Clonar el Repositorio**
   ```bash
   git clone https://github.com/tu-usuario/atrapame-si-puedes.git
   cd atrapame-si-puedes
   ```

2. **Configuración en Android Studio**
   - Abrir Android Studio
   - File → Open → Seleccionar carpeta del proyecto
   - Esperar sincronización automática de Gradle
   - Verificar que el SDK esté configurado correctamente

3. **Configuración de Firebase** (Incluida)
   - El archivo `google-services.json` ya está incluido
   - Firebase Firestore configurado para puntuaciones
   - No requiere configuración adicional para funcionalidad básica

4. **Compilación y Ejecución**
   - Conectar dispositivo Android (habilitar depuración USB)
   - O iniciar emulador Android desde AVD Manager
   - Seleccionar "app" en la configuración de run
   - Presionar "Run" (Shift+F10) o el botón verde de play

### Configuración Opcional de Firebase

Si deseas usar tu propia instancia de Firebase:

1. **Crear Proyecto Firebase**
   - Ir a [Firebase Console](https://console.firebase.google.com)
   - Crear nuevo proyecto
   - Agregar aplicación Android con package `com.equipo.atrapame`

2. **Configurar Firestore**
   - Habilitar Firestore Database
   - Configurar reglas de seguridad:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /scores/{document} {
         allow read, write: if true;
       }
     }
   }
   ```

3. **Descargar Configuración**
   - Descargar `google-services.json`
   - Reemplazar el archivo en `app/google-services.json`

## Estructura del Proyecto

```
app/src/main/
├── java/com/equipo/atrapame/
│   ├── data/
│   │   ├── models/                    # Modelos de datos del dominio
│   │   │   ├── GameState.kt          # Estado completo del juego
│   │   │   ├── Position.kt           # Posiciones en el tablero
│   │   │   ├── Direction.kt          # Direcciones de movimiento
│   │   │   ├── CellType.kt           # Tipos de celdas
│   │   │   ├── Difficulty.kt         # Niveles de dificultad
│   │   │   └── Score.kt              # Modelo de puntuaciones
│   │   ├── repository/               # Capa de acceso a datos
│   │   │   ├── ConfigRepository.kt   # Configuración del jugador
│   │   │   └── ScoreRepository.kt    # Gestión de puntuaciones
│   │   └── local/                    # Almacenamiento local
│   │       └── LocalGameRepository.kt
│   ├── presentation/                 # Capa de presentación (UI)
│   │   ├── MainActivity.kt           # Pantalla principal con navegación
│   │   ├── config/                   # Configuración del jugador
│   │   │   ├── ConfigActivity.kt     # Formulario de configuración
│   │   │   └── ConfigViewModel.kt    # Lógica de configuración
│   │   ├── game/                     # Pantalla principal del juego
│   │   │   ├── GameActivity.kt       # Activity del juego
│   │   │   ├── GameViewModel.kt      # Lógica del juego y IA
│   │   │   ├── GameDialogs.kt        # Diálogos de victoria/derrota
│   │   │   ├── IsometricBoardView.kt # Vista 3D personalizada
│   │   │   └── VirtualJoystickView.kt # Control de movimiento
│   │   ├── score/                    # Pantalla de puntuaciones
│   │   │   ├── ScoreActivity.kt      # Lista de mejores scores
│   │   │   └── ScoreViewModel.kt     # Lógica de puntuaciones
│   │   └── NotificationHelper.kt     # Sistema de notificaciones
│   └── utils/                        # Utilidades y extensiones
├── res/
│   ├── layout/                       # Layouts XML de las pantallas
│   │   ├── activity_main.xml         # Pantalla principal
│   │   ├── activity_game.xml         # Pantalla del juego
│   │   ├── activity_config.xml       # Pantalla de configuración
│   │   ├── activity_score.xml        # Pantalla de puntuaciones
│   │   └── item_score.xml            # Item de lista de scores
│   ├── values/                       # Recursos en español (por defecto)
│   │   ├── strings.xml               # Textos en español
│   │   ├── colors.xml                # Paleta de colores Tron
│   │   └── themes.xml                # Tema visual personalizado
│   ├── values-en/                    # Recursos en inglés
│   │   └── strings.xml               # Textos en inglés
│   ├── drawable/                     # Recursos gráficos
│   │   ├── tron_button_bg.xml        # Fondo de botones Tron
│   │   ├── tron_panel_bg.xml         # Fondo de paneles
│   │   └── tron_grid_bg.xml          # Fondo de cuadrícula
│   └── mipmap-*/                     # Iconos de la aplicación
├── google-services.json              # Configuración de Firebase
└── AndroidManifest.xml               # Configuración de la aplicación
```

### Arquitectura de Componentes

**Capa de Datos (Data Layer)**
- `models/`: Entidades del dominio con lógica de negocio
- `repository/`: Interfaces y implementaciones para acceso a datos
- `local/`: Implementaciones de almacenamiento local

**Capa de Presentación (Presentation Layer)**  
- `Activities`: Controladores de pantalla con ciclo de vida
- `ViewModels`: Lógica de presentación y gestión de estado
- `Views`: Componentes UI personalizados (IsometricBoardView)

**Capa de Recursos (Resources Layer)**
- `layout/`: Definiciones de interfaz en XML
- `values/`: Strings, colores, dimensiones y estilos
- `drawable/`: Recursos gráficos vectoriales y bitmaps

## Guía de Juego

### Configuración Inicial
1. **Primer Uso**
   - Al abrir la app, ve a "Configuración"
   - Ingresa tu nombre (mínimo 2 caracteres)
   - Selecciona tu dificultad preferida:
     - **Fácil**: Pocos obstáculos, enemigo lento (1000ms)
     - **Medio**: Laberinto moderado, velocidad media (750ms)  
     - **Difícil**: Laberinto complejo, enemigo rápido (500ms)
   - Guarda la configuración

### Mecánicas de Juego
1. **Objetivo**: Atrapar al enemigo naranja con tu personaje cian
2. **Movimiento**: Toca las celdas adyacentes para mover tu personaje
3. **Restricciones**: No puedes atravesar obstáculos (bloques grises)
4. **IA del Enemigo**: Se mueve automáticamente buscando escapar de ti

### Estrategias Avanzadas
- **Usa los obstáculos**: Bloquea las rutas de escape del enemigo
- **Planifica movimientos**: El enemigo se mueve después de ti
- **Controla el centro**: Posiciones centrales dan más opciones
- **Timing**: En dificultad alta, cada segundo cuenta

### Sistema de Puntuación
- **Movimientos**: Menos movimientos = mejor puntuación
- **Tiempo**: Completar rápido mejora tu score
- **Dificultad**: Niveles más altos otorgan bonificaciones
- **Guardado**: Puntuaciones se sincronizan automáticamente con Firebase

### Controles del Juego
- **Pausa**: Botón superior derecho para pausar/reanudar
- **Reiniciar**: Botón para empezar un nuevo juego
- **Menú**: Volver a la pantalla principal
- **Configuración**: Cambiar nombre o dificultad

## Configuración de Desarrollo

### Dependencias del Proyecto

**Core Android**
```gradle
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
```

**Arquitectura MVVM**
```gradle
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
implementation 'androidx.activity:activity-ktx:1.8.2'
```

**Firebase Services**
```gradle
implementation platform('com.google.firebase:firebase-bom:32.7.0')
implementation 'com.google.firebase:firebase-firestore-ktx'
implementation 'com.google.firebase:firebase-analytics-ktx'
```

**UI y Material Design**
```gradle
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

**Corrutinas para Async**
```gradle
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

### Configuración del Build

**build.gradle (Module: app)**
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.equipo.atrapame"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
}
```

### Variables de Entorno

No se requieren variables de entorno especiales. La configuración de Firebase se maneja automáticamente a través de `google-services.json`.

### Generación de APK

**APK de Debug (para desarrollo)**
```bash
./gradlew assembleDebug
# Ubicación: app/build/outputs/apk/debug/app-debug.apk
```

**APK de Release (para distribución)**
```bash
./gradlew assembleRelease  
# Ubicación: app/build/outputs/apk/release/app-release-unsigned.apk
```

**Bundle de Android (para Google Play)**
```bash
./gradlew bundleRelease
# Ubicación: app/build/outputs/bundle/release/app-release.aab
```

### Configuración de Firma (Release)

Para generar APK firmado para distribución:

1. **Crear Keystore**
   ```bash
   keytool -genkey -v -keystore atrapame-release-key.keystore -alias atrapame -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configurar en build.gradle**
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('atrapame-release-key.keystore')
               storePassword 'tu_password'
               keyAlias 'atrapame'
               keyPassword 'tu_password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

## Estado del Proyecto

### Funcionalidades Implementadas
- ✅ **Arquitectura MVVM completa** con separación de responsabilidades
- ✅ **Firebase Firestore** integrado para puntuaciones globales
- ✅ **Soporte multilingüe** (Español/Inglés) con detección automática
- ✅ **Formularios validados** para configuración de jugador
- ✅ **Notificaciones internas** con diálogos interactivos
- ✅ **Sistema de colisiones robusto** con validación por board array
- ✅ **Mapas dinámicos por dificultad** con tres patrones únicos
- ✅ **Timer en tiempo real** con sistema de pausa funcional
- ✅ **IA avanzada del enemigo** con algoritmo A* para pathfinding
- ✅ **Interfaz Tron futurista** con efectos neón y colores vibrantes
- ✅ **Vista isométrica 3D** personalizada con Canvas
- ✅ **Sistema de dificultad** con velocidades variables del enemigo

### Características Técnicas Destacadas
- **Renderizado personalizado**: Vista isométrica 3D dibujada completamente en Canvas
- **Algoritmos de IA**: Implementación de A* para navegación inteligente del enemigo
- **Gestión de estado**: LiveData y ViewModel para arquitectura reactiva
- **Persistencia híbrida**: Firebase para datos globales, SharedPreferences para configuración local
- **Internacionalización**: Sistema completo de recursos multilingües
- **Tema visual cohesivo**: Paleta de colores Tron aplicada consistentemente

### Problemas Resueltos Recientemente
- ✅ **Colisiones corregidas**: Ahora usa board array en lugar de lista de obstáculos
- ✅ **Mapas por dificultad**: Tres patrones completamente diferentes implementados
- ✅ **Timer funcional**: Actualización en tiempo real cada 100ms con pausa
- ✅ **Botones de control**: Pausa y reinicio completamente funcionales
- ✅ **Sincronización visual**: Obstáculos mostrados coinciden con colisiones

### Roadmap Futuro

**Versión 1.1 - Mejoras de Gameplay**
- [ ] Múltiples niveles progresivos
- [ ] Diferentes tipos de enemigos con comportamientos únicos
- [ ] Sistema de logros y estadísticas avanzadas
- [ ] Modo multijugador local (hot-seat)

**Versión 1.2 - Experiencia Mejorada**  
- [ ] Efectos de sonido y música ambiente
- [ ] Animaciones de transición entre movimientos
- [ ] Tutorial interactivo para nuevos jugadores
- [ ] Temas visuales alternativos (Matrix, Cyberpunk)

**Versión 1.3 - Características Avanzadas**
- [ ] Modo online multijugador
- [ ] Leaderboards globales con rankings
- [ ] Replay system para revisar partidas
- [ ] Editor de niveles personalizado

## Información del Proyecto

### Contexto Académico
Este proyecto fue desarrollado como parte del curso de **Programación por Componentes** y cumple con todos los requerimientos técnicos establecidos:

- **Repositorio público** con código fuente completo
- **Servicios web** mediante Firebase Firestore
- **Soporte multilingüe** con detección automática
- **Formularios validados** para entrada de datos
- **Notificaciones internas** del sistema
- **Arquitectura MVVM** implementada correctamente

### Tecnologías y Patrones
- **Lenguaje**: Kotlin 100% (moderno y type-safe)
- **Arquitectura**: MVVM con LiveData y ViewModel
- **Base de datos**: Firebase Firestore (NoSQL en la nube)
- **UI**: Material Design 3 con tema personalizado
- **Patrones**: Repository, Observer, Strategy (para dificultades)
- **Concurrencia**: Kotlin Coroutines para operaciones asíncronas

### Equipo de Desarrollo
- **Johan Esteban Solano Rojas** - 20202578112 - Arquitectura y Backend
- **Diego David Chinchilla Leal** - 20221578047 - UI/UX y Frontend  
- **Juan Eduardo Morales Santana** - 20221578034 - Lógica de juego y IA

### Contacto y Soporte
- **Repositorio**: [GitHub - Atrapame Si Puedes](https://github.com/equipo-atrapame/atrapame-si-puedes)
- **Issues**: Para reportar bugs o solicitar features
- **Documentación**: README completo con guías de instalación y uso

---

**Proyecto desarrollado para Programación por Componentes - Universidad Distrital Francisco José de Caldas**