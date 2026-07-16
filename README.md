# Catch Me If You Can

A mobile strategy and logic game developed for Android with a futuristic Tron-inspired aesthetic. Players must catch an intelligent enemy on an isometric board with dynamic obstacles that change according to difficulty level.

## Development Team

- **Johan Esteban Solano Rojas** - 20202578112
- **Diego David Chinchilla Leal** - 20221578047  
- **Juan Eduardo Morales Santana** - 20221578034

## Main Features

### Architecture & Development
- **MVVM Architecture** - Clear separation between model, view, and business logic
- **Kotlin** - Modern and safe language for Android
- **Material Design 3** - Modern interface with custom Tron theme
- **Firebase Firestore** - Real-time database for global scores
- **Multilingual Support** - Spanish and English with automatic system detection

### Game Features
- **3D Isometric Board** - Three-dimensional view with neon visual effects
- **Robust Collision System** - Solid obstacles that fully block movement
- **Dynamic Maps by Difficulty** - Three completely different layouts:
  - **Easy**: Few scattered obstacles for learning
  - **Medium**: Moderate maze with L-shaped walls and strategic barriers
  - **Hard**: Dense maze with complex patterns and minimal corridors
- **Intelligent Difficulty System** - Variable enemy speed:
  - **Easy**: 1000ms between moves
  - **Medium**: 750ms between moves  
  - **Hard**: 500ms between moves
- **Real-Time Timer** - Precise stopwatch with updates every 100ms
- **Full Pause System** - Pauses both timer and enemy movements
- **Internal Notifications** - Victory and defeat alerts with options
- **Interactive Dialogs** - Result windows with statistics and navigation

### Futuristic Tron Interface
- **Neon Color Palette**:
  - Bright cyan (#00FFFF) for main elements
  - Neon orange (#FF6600) for enemies and alerts
  - Purple (#9966FF) for secondary elements
  - Dark backgrounds (#0A0A0A, #1A1A1A) for contrast
- **Advanced Visual Effects**:
  - Neon glow shadows on buttons and entities
  - Glowing borders on interactive elements
  - Subtle gradients on panels and backgrounds
- **Futuristic Typography** - Monospace fonts for statistics and UI
- **Smooth Animations** - Fluid transitions between game states

### Enemy Artificial Intelligence
- **A\* Algorithm** - Optimal pathfinding for escape routes
- **Strategic Behavior** - Enemy seeks to maximize distance from the player
- **Terrain Adaptation** - Intelligently navigates around obstacles
- **Multiple Objectives** - Evaluates all possible escape positions

## Game Objective

Catch the orange enemy using your cyan character on an isometric board. The enemy uses advanced artificial intelligence to escape, calculating optimal routes and avoiding obstacles. Your goal is to complete each level in the shortest time and fewest moves possible.

### Game Mechanics
1. **Movement**: Tap adjacent cells to move your character
2. **Strategy**: Use obstacles to block the enemy's escape routes
3. **Timing**: The enemy moves automatically — plan your moves carefully
4. **Victory**: Reach the same position as the enemy to capture it

## Technologies Used

- **Language**: 100% Kotlin
- **Architecture**: MVVM (Model-View-ViewModel) with LiveData
- **Database**: Firebase Firestore for global scores
- **UI Framework**: Material Design 3 with custom Tron theme
- **State Management**: LiveData, ViewModel, and Coroutines
- **Design Patterns**: Repository Pattern, Observer Pattern
- **Rendering**: Custom Canvas for 3D isometric view
- **Local Persistence**: SharedPreferences for settings

## Project Requirements Fulfilled

### Public Repository
- Complete source code available on GitHub
- Detailed commit history
- Complete technical documentation
- Release APK included

### Web Services (Firebase)
- **Firebase Firestore**: NoSQL database for score storage
- **CRUD Operations**: Create, Read, Update, Delete of scores
- **Real-time Sync**: Automatically updated scores
- **Error Handling**: Fallback to local storage when offline

### Complete Multilingual Support
- **Automatic Detection**: Based on Android device settings
- **Externalized Resources**: 
  - `res/values/strings.xml` (Spanish - default language)
  - `res/values-en/strings.xml` (English)
- **Full Coverage**: Entire interface, messages, and dialogs translated
- **Dynamic Switching**: No app restart required

### Interactive Forms
- **Settings Screen**: Complete player configuration form
- **Validated Fields**:
  - Player name (required, minimum 2 characters)
  - Difficulty selection (Easy/Medium/Hard)
- **Persistence**: Settings saved locally with SharedPreferences
- **Real-time Validation**: Immediate user feedback

### Internal Notifications
- **Dialog System**: Native Android notifications
- **Notification Types**:
  - Victory: Shows time and moves performed
  - Defeat: Retry or return to menu options
  - Settings saved: Confirmation of changes
- **Interactive Actions**: Navigation or restart buttons

### Complete MVVM Architecture
- **Model**: Data classes (GameState, Position, Score, etc.)
- **View**: Activities and custom Views (IsometricBoardView)
- **ViewModel**: Business logic and state management
- **Repository**: Data source abstraction (Firebase, Local)
- **Clear Separation**: No business logic in views

## Installation & Setup

### System Prerequisites
- **Android Studio**: Flamingo or higher (2023.1.1+)
- **Android SDK**: API Level 24+ (Android 7.0 Nougat)
- **Gradle**: 8.0+ (included with Android Studio)
- **Device**: Physical Android device or emulator with API 24+

### Step-by-Step Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/tu-usuario/atrapame-si-puedes.git
   cd atrapame-si-puedes
   ```

2. **Setup in Android Studio**
   - Open Android Studio
   - File → Open → Select project folder
   - Wait for automatic Gradle sync
   - Verify that the SDK is properly configured

3. **Firebase Configuration** (Included)
   - The `google-services.json` file is already included
   - Firebase Firestore configured for scores
   - No additional configuration required for basic functionality

4. **Build & Run**
   - Connect an Android device (enable USB debugging)
   - Or start an Android emulator from AVD Manager
   - Select "app" in the run configuration
   - Press "Run" (Shift+F10) or the green play button

### Optional Firebase Setup

If you wish to use your own Firebase instance:

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Create a new project
   - Add Android app with package `com.equipo.atrapame`

2. **Configure Firestore**
   - Enable Firestore Database
   - Set up security rules:
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

3. **Download Configuration**
   - Download `google-services.json`
   - Replace the file at `app/google-services.json`

## Project Structure

```
app/src/main/
├── java/com/equipo/atrapame/
│   ├── data/
│   │   ├── models/                    # Domain data models
│   │   │   ├── GameState.kt          # Complete game state
│   │   │   ├── Position.kt           # Board positions
│   │   │   ├── Direction.kt          # Movement directions
│   │   │   ├── CellType.kt           # Cell types
│   │   │   ├── Difficulty.kt         # Difficulty levels
│   │   │   └── Score.kt              # Score model
│   │   ├── repository/               # Data access layer
│   │   │   ├── ConfigRepository.kt   # Player configuration
│   │   │   └── ScoreRepository.kt    # Score management
│   │   └── local/                    # Local storage
│   │       └── LocalGameRepository.kt
│   ├── presentation/                 # Presentation layer (UI)
│   │   ├── MainActivity.kt           # Main screen with navigation
│   │   ├── config/                   # Player configuration
│   │   │   ├── ConfigActivity.kt     # Configuration form
│   │   │   └── ConfigViewModel.kt    # Configuration logic
│   │   ├── game/                     # Main game screen
│   │   │   ├── GameActivity.kt       # Game activity
│   │   │   ├── GameViewModel.kt      # Game logic and AI
│   │   │   ├── GameDialogs.kt        # Victory/defeat dialogs
│   │   │   ├── IsometricBoardView.kt # Custom 3D view
│   │   │   └── VirtualJoystickView.kt # Movement control
│   │   ├── score/                    # Score screen
│   │   │   ├── ScoreActivity.kt      # Best scores list
│   │   │   └── ScoreViewModel.kt     # Score logic
│   │   └── NotificationHelper.kt     # Notification system
│   └── utils/                        # Utilities and extensions
├── res/
│   ├── layout/                       # Screen XML layouts
│   │   ├── activity_main.xml         # Main screen
│   │   ├── activity_game.xml         # Game screen
│   │   ├── activity_config.xml       # Settings screen
│   │   ├── activity_score.xml        # Scores screen
│   │   └── item_score.xml            # Score list item
│   ├── values/                       # Spanish resources (default)
│   │   ├── strings.xml               # Spanish texts
│   │   ├── colors.xml                # Tron color palette
│   │   └── themes.xml                # Custom visual theme
│   ├── values-en/                    # English resources
│   │   └── strings.xml               # English texts
│   ├── drawable/                     # Graphic resources
│   │   ├── tron_button_bg.xml        # Tron button background
│   │   ├── tron_panel_bg.xml         # Panel background
│   │   └── tron_grid_bg.xml          # Grid background
│   └── mipmap-*/                     # App icons
├── google-services.json              # Firebase configuration
└── AndroidManifest.xml               # App configuration
```

### Component Architecture

**Data Layer**
- `models/`: Domain entities with business logic
- `repository/`: Interfaces and implementations for data access
- `local/`: Local storage implementations

**Presentation Layer**  
- `Activities`: Screen controllers with lifecycle management
- `ViewModels`: Presentation logic and state management
- `Views`: Custom UI components (IsometricBoardView)

**Resources Layer**
- `layout/`: Interface definitions in XML
- `values/`: Strings, colors, dimensions, and styles
- `drawable/`: Vector graphics and bitmap resources

## Game Guide

### Initial Setup
1. **First Use**
   - Open the app and go to "Settings"
   - Enter your name (minimum 2 characters)
   - Select your preferred difficulty:
     - **Easy**: Few obstacles, slow enemy (1000ms)
     - **Medium**: Moderate maze, medium speed (750ms)  
     - **Hard**: Complex maze, fast enemy (500ms)
   - Save your settings

### Game Mechanics
1. **Objective**: Catch the orange enemy with your cyan character
2. **Movement**: Tap adjacent cells to move your character
3. **Restrictions**: You cannot pass through obstacles (gray blocks)
4. **Enemy AI**: Moves automatically, always trying to escape

### Advanced Strategies
- **Use obstacles**: Block the enemy's escape routes
- **Plan your moves**: The enemy moves after you
- **Control the center**: Central positions offer more options
- **Timing**: At high difficulty, every second counts

### Scoring System
- **Moves**: Fewer moves = better score
- **Time**: Faster completion improves your score
- **Difficulty**: Higher levels grant bonuses
- **Saving**: Scores sync automatically with Firebase

### Game Controls
- **Pause**: Top-right button to pause/resume
- **Restart**: Button to start a new game
- **Menu**: Return to the main screen
- **Settings**: Change name or difficulty

## Development Configuration

### Project Dependencies

**Core Android**
```gradle
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
```

**MVVM Architecture**
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

**UI & Material Design**
```gradle
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

**Coroutines for Async**
```gradle
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

### Build Configuration

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

### Environment Variables

No special environment variables required. Firebase configuration is handled automatically through `google-services.json`.

### APK Generation

**Debug APK (for development)**
```bash
./gradlew assembleDebug
# Location: app/build/outputs/apk/debug/app-debug.apk
```

**Release APK (for distribution)**
```bash
./gradlew assembleRelease  
# Location: app/build/outputs/apk/release/app-release-unsigned.apk
```

**Android App Bundle (for Google Play)**
```bash
./gradlew bundleRelease
# Location: app/build/outputs/bundle/release/app-release.aab
```

### Signing Configuration (Release)

To generate a signed APK for distribution:

1. **Create Keystore**
   ```bash
   keytool -genkey -v -keystore atrapame-release-key.keystore -alias atrapame -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Configure in build.gradle**
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file('atrapame-release-key.keystore')
               storePassword 'your_password'
               keyAlias 'atrapame'
               keyPassword 'your_password'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

## Project Status

### Implemented Features
- ✅ **Complete MVVM architecture** with separation of concerns
- ✅ **Firebase Firestore** integrated for global scores
- ✅ **Multilingual support** (Spanish/English) with automatic detection
- ✅ **Validated forms** for player configuration
- ✅ **Internal notifications** with interactive dialogs
- ✅ **Robust collision system** validated through board array
- ✅ **Dynamic maps by difficulty** with three unique patterns
- ✅ **Real-time timer** with functional pause system
- ✅ **Advanced enemy AI** with A* pathfinding algorithm
- ✅ **Futuristic Tron interface** with neon effects and vibrant colors
- ✅ **Custom 3D isometric view** rendered with Canvas
- ✅ **Difficulty system** with variable enemy speeds

### Technical Highlights
- **Custom rendering**: 3D isometric view drawn entirely on Canvas
- **AI algorithms**: A* implementation for intelligent enemy navigation
- **State management**: LiveData and ViewModel for reactive architecture
- **Hybrid persistence**: Firebase for global data, SharedPreferences for local configuration
- **Internationalization**: Complete multilingual resource system
- **Cohesive visual theme**: Tron color palette consistently applied

### Recently Resolved Issues
- ✅ **Collisions fixed**: Now uses board array instead of obstacle list
- ✅ **Maps by difficulty**: Three completely different patterns implemented
- ✅ **Functional timer**: Real-time update every 100ms with pause
- ✅ **Control buttons**: Pause and restart fully functional
- ✅ **Visual synchronization**: Displayed obstacles match collision detection

### Future Roadmap

**Version 1.1 - Gameplay Improvements**
- [ ] Multiple progressive levels
- [ ] Different enemy types with unique behaviors
- [ ] Achievement system and advanced statistics
- [ ] Local multiplayer mode (hot-seat)

**Version 1.2 - Enhanced Experience**  
- [ ] Sound effects and ambient music
- [ ] Transition animations between moves
- [ ] Interactive tutorial for new players
- [ ] Alternative visual themes (Matrix, Cyberpunk)

**Version 1.3 - Advanced Features**
- [ ] Online multiplayer mode
- [ ] Global leaderboards with rankings
- [ ] Replay system for reviewing games
- [ ] Custom level editor

## Project Information

### Academic Context
This project was developed as part of the **Component-Based Programming** course and fulfills all established technical requirements:

- **Public repository** with complete source code
- **Web services** via Firebase Firestore
- **Multilingual support** with automatic detection
- **Validated forms** for data input
- **Internal system notifications**
- **MVVM architecture** correctly implemented

### Technologies & Patterns
- **Language**: 100% Kotlin (modern and type-safe)
- **Architecture**: MVVM with LiveData and ViewModel
- **Database**: Firebase Firestore (NoSQL cloud database)
- **UI**: Material Design 3 with custom theme
- **Patterns**: Repository, Observer, Strategy (for difficulties)
- **Concurrency**: Kotlin Coroutines for asynchronous operations

### Development Team
- **Johan Esteban Solano Rojas** - 20202578112 - Architecture and Backend
- **Diego David Chinchilla Leal** - 20221578047 - UI/UX and Frontend  
- **Juan Eduardo Morales Santana** - 20221578034 - Game Logic and AI

### Contact & Support
- **Repository**: [GitHub - Catch Me If You Can](https://github.com/equipo-atrapame/atrapame-si-puedes)
- **Issues**: To report bugs or request features
- **Documentation**: Complete README with installation and usage guides

---

**Project developed for Component-Based Programming - Universidad Distrital Francisco José de Caldas**