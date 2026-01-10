# Contributing to LinkOps

First off, thank you for considering contributing to LinkOps! It's people like you that make LinkOps such a great tool for Android developers.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Style Guidelines](#style-guidelines)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Getting Started

### Prerequisites

- JDK 17 or higher
- Android SDK with ADB
- IntelliJ IDEA or Android Studio (recommended)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/link-ops.git
   cd link-ops
   ```
3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/manjees/link-ops.git
   ```

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

When creating a bug report, include:
- Clear and descriptive title
- Steps to reproduce the issue
- Expected behavior vs actual behavior
- Screenshots if applicable
- Your environment (OS, Java version, etc.)

### Suggesting Features

Feature suggestions are welcome! Please:
- Check if the feature is already in the roadmap
- Provide a clear description of the feature
- Explain why this feature would be useful
- Consider how it fits with the existing architecture

### Pull Requests

1. Create a feature branch from `main`
2. Make your changes
3. Write or update tests as needed
4. Ensure all tests pass
5. Submit a pull request

## Development Setup

```bash
# Clone and enter the project
git clone https://github.com/YOUR_USERNAME/link-ops.git
cd link-ops

# Build the project
./gradlew build

# Run tests
./gradlew :composeApp:jvmTest

# Run the application
./gradlew :composeApp:run
```

### Project Structure

```
composeApp/src/jvmMain/kotlin/com/manjee/linkops/
├── data/           # Repository implementations, parsers
├── domain/         # Models, repository interfaces, use cases
├── infrastructure/ # ADB executor, network clients
├── ui/             # Compose screens, components, ViewModels
└── di/             # Dependency injection (AppContainer)
```

### Architecture

This project follows **Clean Architecture** principles:

- **Domain Layer**: Business logic, models, use cases
- **Data Layer**: Repository implementations, data parsing
- **Infrastructure Layer**: External services (ADB, HTTP)
- **UI Layer**: Compose screens and ViewModels

## Style Guidelines

### Kotlin Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Prefer immutability (`val` over `var`)

### Code Organization

- One class per file (with exceptions for sealed classes)
- Group related functionality in the same package
- Use extension functions appropriately

### Documentation

- Add KDoc comments for public APIs
- Include `@param` and `@return` tags where applicable
- Update README if adding new features

## Commit Messages

Follow conventional commit format:

```
type(scope): subject

body (optional)
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Examples:
```
feat(manifest): add deep link testing functionality
fix(adb): handle device disconnection gracefully
docs(readme): update installation instructions
```

## Pull Request Process

1. **Update documentation** if your changes affect usage
2. **Add tests** for new functionality
3. **Ensure CI passes** - all tests must pass
4. **Request review** from maintainers
5. **Address feedback** promptly

### PR Title Format

Use the same format as commit messages:
```
feat(manifest): add deep link testing functionality
```

### PR Description

Include:
- Summary of changes
- Related issue (if any)
- Screenshots for UI changes
- Testing instructions

## Questions?

Feel free to open an issue with the `question` label or reach out to the maintainers.

---

Thank you for contributing! 🎉
