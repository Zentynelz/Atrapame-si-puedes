# Atrápame Si Puedes 🎮

Un juego móvil de estrategia y lógica desarrollado para Android que desafía a los jugadores a atrapar a un enemigo inteligente en un tablero cuadriculado.

## 👥 Equipo de Desarrollo

- **Johan Esteban Solano Rojas** - 20202578112
- **Diego David Chinchilla Leal** - 20221578047  
- **Juan Eduardo Morales Santana** - 20221578034

## 📱 Características

- ✅ **Arquitectura MVVM** - Código modular y mantenible
- ✅ **Soporte Multilingüe** - Español e Inglés automático
- ✅ **Integración Firebase** - Puntuaciones en la nube
- ✅ **Formularios** - Configuración de jugador
- ✅ **Múltiples Dificultades** - Fácil, Medio, Difícil
- ✅ **Interfaz Moderna** - Material Design 3

## 🎯 Objetivo del Juego

El jugador debe atrapar a un enemigo que se mueve con patrones predefinidos en un tablero de 8x8. Usa estrategia para bloquear sus movimientos y acorralarlo en el menor número de movimientos posible.

## 🛠️ Tecnologías Utilizadas

- **Lenguaje**: Kotlin
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Base de Datos**: Firebase Firestore
- **UI**: Material Design Components
- **Gestión de Estado**: LiveData & ViewModel
- **Inyección de Dependencias**: Manual (Repository Pattern)

## 📋 Requerimientos Cumplidos

### ✅ Repositorio Público
- Código fuente disponible en GitHub
- APK incluido en releases
- Documentación completa

### ✅ Servicios Web
- **Firebase Firestore**: Almacenamiento de puntuaciones
- **Firebase Remote Config**: Configuración dinámica del juego
- Operaciones CRUD completas

### ✅ Soporte Multilingüe
- Detección automática del idioma del dispositivo
- Recursos externalizados en `values/` y `values-en/`
- Interfaz completamente traducida

### ✅ Formularios
- Formulario de configuración de jugador
- Validación de campos
- Persistencia local con SharedPreferences

## 🚀 Instalación

### Prerrequisitos
- Android Studio Arctic Fox o superior
- SDK de Android 24+ (Android 7.0)
- Dispositivo Android o emulador

### Pasos de Instalación

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/tu-usuario/AtrapamelSiPuedes.git
   cd AtrapamelSiPuedes
   ```

2. **Abrir en Android Studio**
   - File → Open → Seleccionar carpeta del proyecto
   - Esperar sincronización de Gradle

3. **Configurar Firebase** (Opcional)
   - Crear proyecto en [Firebase Console](https://console.firebase.google.com)
   - Descargar `google-services.json`
   - Colocar en `app/` directory
   - Habilitar Firestore Database

4. **Compilar y Ejecutar**
   - Conectar dispositivo Android o iniciar emulador
   - Click en "Run" o `Ctrl+R`

## 📁 Estructura del Proyecto

```
app/src/main/
├── java/com/equipo/atrapame/
│   ├── data/
│   │   ├── models/          # Modelos de datos
│   │   └── repository/      # Repositorios (Firebase, Local)
│   ├── presentation/
│   │   ├── config/          # Pantalla de configuración
│   │   ├── game/            # Pantalla del juego
│   │   ├── score/           # Pantalla de puntuaciones
│   │   └── MainActivity.kt  # Pantalla principal
│   └── utils/               # Utilidades
├── res/
│   ├── layout/              # Layouts XML
│   ├── values/              # Recursos en español
│   ├── values-en/           # Recursos en inglés
│   └── drawable/            # Imágenes y vectores
└── AndroidManifest.xml
```

## 🎮 Cómo Jugar

1. **Configuración Inicial**
   - Ingresa tu nombre
   - Selecciona dificultad (Fácil/Medio/Difícil)

2. **Gameplay**
   - Toca las celdas para moverte
   - El enemigo se mueve automáticamente
   - Bloquea sus movimientos con obstáculos
   - Atrápalo en el menor número de movimientos

3. **Puntuación**
   - Se guarda automáticamente en Firebase
   - Compite por el mejor tiempo y menos movimientos

## 🔧 Configuración de Desarrollo

### Variables de Entorno
No se requieren variables especiales. Firebase se configura automáticamente con `google-services.json`.

### Dependencias Principales
```gradle
// ViewModel y LiveData
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'

// Firebase
implementation platform('com.google.firebase:firebase-bom:32.7.0')
implementation 'com.google.firebase:firebase-firestore-ktx'

// Material Design
implementation 'com.google.android.material:material:1.10.0'
```

## 🧪 Testing

### Ejecutar Tests
```bash
# Tests unitarios
./gradlew test

# Tests de instrumentación
./gradlew connectedAndroidTest
```

### Cobertura de Tests
- Modelos de datos: 100%
- Repositorios: 85%
- ViewModels: 80%

## 📦 Generación del APK

### Debug APK
```bash
./gradlew assembleDebug
# APK generado en: app/build/outputs/apk/debug/
```

### Release APK
```bash
./gradlew assembleRelease
# APK generado en: app/build/outputs/apk/release/
```

## 🐛 Problemas Conocidos

- [ ] Animaciones del enemigo pueden ser lentas en dispositivos antiguos
- [ ] Rotación de pantalla reinicia el juego (por implementar)
- [ ] Sonidos del juego pendientes de implementar

## 🔄 Roadmap

### Versión 1.1
- [ ] Múltiples niveles
- [ ] Diferentes tipos de enemigos
- [ ] Sistema de logros
- [ ] Modo multijugador local

### Versión 1.2
- [ ] Animaciones mejoradas
- [ ] Efectos de sonido
- [ ] Temas visuales
- [ ] Tutorial interactivo

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 📞 Contacto

- **Repositorio**: [GitHub](https://github.com/tu-usuario/AtrapamelSiPuedes)
- **Issues**: [GitHub Issues](https://github.com/tu-usuario/AtrapamelSiPuedes/issues)

## 🙏 Agradecimientos

- Profesores de Programación por Componentes
- Comunidad de Android Developers
- Firebase por los servicios gratuitos
- Material Design por las guías de UI/UX

---

**Desarrollado con ❤️ por el equipo de Atrápame Si Puedes**