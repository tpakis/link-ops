<p align="center">
  <h1 align="center">LinkOps</h1>
  <p align="center">
    <strong>Android Deep Link Debugging & Orchestration Tool</strong>
  </p>
  <p align="center">
    A powerful desktop application for debugging Android App Links and Deep Links
  </p>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#installation">Installation</a> •
  <a href="#usage">Usage</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#license">License</a>
</p>

---

## Features

### Manifest Analyzer
- View all deep links configured in any installed app
- Domain verification status (`verified`, `none`, etc.)
- Test deep links directly on device with one click
- Filter by App Links, Custom Schemes, or HTTP links

### Diagnostics
- Validate `assetlinks.json` configuration
- Check domain ownership setup
- Identify common App Links issues

### Device Management
- Auto-detect connected Android devices
- Support for multiple devices simultaneously
- Real-time device status monitoring

## Screenshots

<!-- TODO: Add screenshots -->

## Installation

### Prerequisites
- Java 17 or higher
- ADB (Android Debug Bridge) installed and in PATH
- Android device with USB debugging enabled

### From Source

```bash
# Clone the repository
git clone https://github.com/manjees/link-ops.git
cd link-ops

# Run the application
./gradlew :composeApp:run
```

### Pre-built Binaries

Coming soon! Check [Releases](https://github.com/manjees/link-ops/releases) for downloadable binaries.

## Usage

### 1. Connect Your Device
1. Enable USB debugging on your Android device
2. Connect via USB cable
3. LinkOps will automatically detect the device

### 2. Analyze Deep Links
1. Go to **Manifest** tab
2. Select your device from dropdown
3. Search and select an app package
4. View all deep links with verification status
5. Click **Test** to fire the deep link on device

### 3. Validate AssetLinks
1. Go to **Diagnostics** tab
2. Enter your domain (e.g., `example.com`)
3. View validation results and any issues

## Tech Stack

- **Kotlin Multiplatform** - Cross-platform development
- **Compose Desktop** - Modern declarative UI
- **Clean Architecture** - Maintainable codebase
- **Coroutines & Flow** - Reactive programming

## Project Structure

```
composeApp/src/jvmMain/kotlin/com/manjee/linkops/
├── data/           # Data layer (repositories, parsers)
├── domain/         # Business logic (models, use cases)
├── infrastructure/ # External services (ADB, network)
├── ui/             # Presentation layer (screens, components)
└── di/             # Dependency injection
```

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Quick Start for Contributors

```bash
# Fork and clone the repo
git clone https://github.com/YOUR_USERNAME/link-ops.git

# Create a feature branch
git checkout -b feature/amazing-feature

# Make your changes and test
./gradlew :composeApp:jvmTest

# Commit and push
git commit -m "Add amazing feature"
git push origin feature/amazing-feature

# Open a Pull Request
```

## Roadmap

- [ ] Intent Builder - Create custom intents
- [ ] QR Code generation for deep links
- [ ] History & Favorites
- [ ] Export reports
- [ ] Multi-language support

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Ktor](https://ktor.io/)

---

<p align="center">
  Made with ❤️ for Android Developers
</p>
