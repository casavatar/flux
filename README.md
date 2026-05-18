# Flux

Flux is an Android application built with Kotlin and C++ (JNI/NDK integration). This project demonstrates a single-module architecture featuring native C++ integration with the Android framework.

## Features

- **Kotlin & C++ Integration**: Seamlessly calls native C++ code from Kotlin using JNI.
- **Modern Android Development**: Utilizes View Binding and the latest Material Design components.
- **Native Layer**: Built with CMake, the native layer compiles into a shared library (`libflux.so`) containing the application's core native functions.

## Tech Stack

- **Language**: Kotlin 1.9.x, C++17
- **Minimum SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 34
- **Build System**: Gradle 8.2.0, CMake 3.22.1
- **UI & Layout**: View Binding, ConstraintLayout, Material Design Components

## Getting Started

### Prerequisites

- Android Studio (compatible with AGP 8.2.0)
- Android NDK and CMake installed via SDK Manager
- A connected Android device or emulator running Android 7.0 (API 24) or higher

### Build & Run

To build and run the application locally, you can use Android Studio or run the following Gradle commands from the terminal:

```bash
# Build the debug APK
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug
```

### Testing

```bash
# Run local unit tests (JVM)
./gradlew test

# Run instrumented UI tests (requires a connected device)
./gradlew connectedAndroidTest
```

## Project Structure

- `app/src/main/java/com/example/flux/`: Contains the Kotlin source code (e.g., `MainActivity`).
- `app/src/main/cpp/`: Contains the native C++ source code (`native-lib.cpp`) and `CMakeLists.txt`.
- `app/src/main/res/`: Contains Android resources (layouts, values, etc.).

## License

This project is licensed under the MIT License - see the LICENSE file for details.
