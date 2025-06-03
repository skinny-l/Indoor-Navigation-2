# Indoor Navigation Android App

A modern Android application for indoor navigation using Jetpack Compose, Material 3 design, and
advanced positioning technology.

## Features

- 🗺️ **Interactive Floor Plans**: View and navigate through building floor plans
- 📍 **Real-time Positioning**: BLE beacon-based indoor positioning system
- 🔍 **Smart Search**: Search for points of interest (POIs) within buildings
- 🧭 **Turn-by-turn Navigation**: A* pathfinding algorithm for optimal routing
- 🏢 **Multi-floor Support**: Navigate between different floors using elevators/stairs
- ♿ **Accessibility**: Support for wheelchair accessibility information
- 🎨 **Modern UI**: Material 3 design with dynamic theming support

## Technology Stack

### Frontend

- **Jetpack Compose**: Modern declarative UI toolkit
- **Material 3**: Latest Material Design components
- **Navigation Compose**: Type-safe navigation
- **ViewModel**: MVVM architecture pattern
- **StateFlow**: Reactive state management

### Backend & Data

- **Kotlin Serialization**: JSON serialization/deserialization
- **Coroutines**: Asynchronous programming
- **Flow**: Reactive data streams
- **Repository Pattern**: Clean architecture data layer

### Positioning & Navigation

- **BLE Beacons**: Bluetooth Low Energy indoor positioning
- **Trilateration**: Mathematical positioning algorithms
- **A* Pathfinding**: Optimal route calculation
- **Sensor Fusion**: Accelerometer, gyroscope, magnetometer integration

## Project Structure

```
app/src/main/java/com/example/indoornavigation20/
├── data/
│   └── repository/           # Data access layer
├── domain/
│   └── model/               # Business models
├── navigation/              # Pathfinding algorithms
├── positioning/             # Indoor positioning engine
└── presentation/
    ├── screens/            # UI screens
    ├── theme/              # Material 3 theming
    └── viewmodel/          # UI state management
```

## Key Components

### Domain Models

- **Position**: Coordinates with floor information
- **FloorPlan**: Building floor layout with navigation nodes
- **PointOfInterest**: Searchable locations with categories
- **Beacon**: BLE beacon configuration for positioning
- **NavigationPath**: Route with turn-by-turn instructions

### Positioning Engine

- **BeaconScanning**: BLE device detection and RSSI measurement
- **Trilateration**: Calculate position from multiple beacon distances
- **SensorFusion**: Combine positioning data with device sensors
- **PathfindingEngine**: A* algorithm for optimal route calculation

### UI Screens

- **SplashScreen**: App initialization and branding
- **WelcomeScreen**: User onboarding
- **LoginScreen**: Authentication (mock implementation)
- **MapScreen**: Main navigation interface
- **SettingsScreen**: App configuration

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Kotlin 2.1.20+

### Installation

1. Clone the repository:

```bash
git clone <repository-url>
cd IndoorNavigation20
```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Run the app on an emulator or physical device

### Configuration

The app currently uses mock data for demonstration. To integrate with real systems:

1. **Floor Plans**: Replace mock data in `NavigationRepository` with actual floor plan APIs
2. **BLE Beacons**: Configure real beacon UUIDs, majors, and minors
3. **Authentication**: Implement actual login/registration system
4. **Backend Integration**: Connect to your building management system

## Architecture

The app follows Clean Architecture principles:

- **Presentation Layer**: Compose UI, ViewModels, UI state
- **Domain Layer**: Business models, use cases
- **Data Layer**: Repositories, data sources, caching

### State Management

- **Unidirectional Data Flow**: Data flows down, events flow up
- **StateFlow**: Reactive state observation
- **Immutable State**: Predictable state updates

## Positioning Technology

### BLE Beacon Setup

1. Deploy BLE beacons throughout the building
2. Configure beacon positions in the coordinate system
3. Set appropriate transmission power levels
4. Map beacon coverage areas

### Trilateration Algorithm

The app uses distance measurements from multiple beacons to calculate position:

1. Measure RSSI from nearby beacons
2. Convert RSSI to distance using path loss model
3. Apply trilateration with 3+ beacon measurements
4. Filter and smooth position using sensor fusion

## Navigation Algorithm

The A* pathfinding algorithm provides optimal routes:

1. Create navigation graph from floor plan nodes
2. Calculate heuristic costs to destination
3. Find shortest path considering obstacles
4. Generate turn-by-turn instructions
5. Support multi-floor navigation via elevators/stairs

## Material 3 Design

The app leverages Material 3 features:

- **Dynamic Color**: Adapts to system wallpaper colors
- **Large Screens**: Responsive layout for tablets
- **Dark Theme**: Automatic dark/light theme switching
- **Accessibility**: High contrast, large text support

## Future Enhancements

- **Augmented Reality**: AR overlays for navigation guidance
- **Voice Navigation**: Audio turn-by-turn instructions
- **Offline Maps**: Download floor plans for offline use
- **Analytics**: Track popular routes and POIs
- **Admin Panel**: Web interface for building management
- **IoT Integration**: Connect with smart building systems

## Testing

Run tests using:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Material Design team for the excellent design system
- Jetpack Compose team for the modern UI toolkit
- Indoor positioning research community
- Open source contributors

---

Built with ❤️ using Kotlin and Jetpack Compose