# Contributing to Japa Test Runner

## Development Setup

### Prerequisites
- **No manual Java installation required!** Gradle will auto-download Java 21 via toolchains
- IntelliJ IDEA 2024.1+ (includes JBR 21)

### Initial Setup

1. Clone the repository
2. Open the project in IntelliJ IDEA
   - Gradle will automatically provision Java 21 if not available
   - Spotless will automatically format code after Gradle sync

**Note:** The project uses Gradle's Java Toolchain feature, which automatically downloads and uses the correct Java version (21) for all Gradle tasks. This ensures consistent builds across all development environments without manual JDK configuration.

## Git Workflow

### Commit Message Format

This project enforces [Conventional Commits](https://www.conventionalcommits.org/). All commit messages must follow this format:

### Code Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with [ktlint](https://github.com/pinterest/ktlint) for code formatting.

#### Check formatting:
```bash
./gradlew spotlessCheck
```

#### Auto-fix formatting:
```bash
./gradlew spotlessApply
```

**Note:** Spotless automatically runs after Gradle sync to format code. Before committing, run `./gradlew spotlessCheck` to verify formatting, or rely on CI to catch issues.

## Git Hooks

The project uses a **commit-msg** hook to enforce [Conventional Commits](https://www.conventionalcommits.org/):

- ✅ **Installed automatically** on Gradle sync
- ✅ **Validates commit message format** using simple shell script
- ❌ **No pre-commit formatting check** - format before committing or let CI catch it

To manually install:
```bash
./gradlew installGitHooks
```

## Building the Plugin

```bash
./gradlew buildPlugin
```

The built plugin will be in `build/distributions/`.

## Testing

```bash
./gradlew test
```
