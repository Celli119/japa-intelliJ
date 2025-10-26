# Japa Test Runner for WebStorm

A WebStorm/IntelliJ IDEA plugin that provides interactive test running for the [Japa](https://japa.dev/) testing framework, similar to how Vitest and Jest tests work in the IDE.

## Features

- 🎯 **Run Individual Tests**: Click the gutter icon next to any `test()` to run it
- 📦 **Run Test Groups**: Execute entire `test.group()` blocks with one click
- 🐛 **Debug Support**: Set breakpoints and debug your Japa tests
- 📊 **Integrated Test Results**: View test results in the IDE's test runner UI
- 🔍 **Auto-Detection**: Automatically detects Japa tests in your project
- ⚡ **Context Actions**: Right-click on tests to create run configurations

## Requirements

- WebStorm 2024.1+ or IntelliJ IDEA Ultimate 2024.1+
- Node.js installed
- A project with `@japa/runner` installed

## Installation

### From Source

1. Clone this repository
2. Run `./gradlew buildPlugin`
3. Install the plugin from `build/distributions/japaTestRunner-*.zip` in your IDE
   - Go to **Settings → Plugins → ⚙️ → Install Plugin from Disk**

## Usage

### Running Tests

1. Open any test file (typically in `tests/` directory)
2. Look for the ▶️ gutter icon next to:
   - `test('test name', ...)` - runs a single test
   - `test.group('group name', ...)` - runs all tests in the group
3. Click the icon to run or right-click for more options

### Creating Run Configurations

1. Right-click on a test or test group
2. Select **Run 'Test: ...'** or **Debug 'Test: ...'**
3. The plugin will create and save a run configuration automatically

### Manual Configuration

Create a new run configuration:
1. Go to **Run → Edit Configurations**
2. Click **+** → **Japa Tests**
3. Configure:
   - **Test file**: Path to the test file
   - **Test name** (optional): Specific test to run
   - **Group name** (optional): Specific group to run
   - **Additional arguments**: Any extra CLI flags
   - **Working directory**: Project root (auto-detected)

## How It Works

The plugin:
1. Detects Japa by checking for `@japa/runner` in `package.json`
2. Parses test files to find `test()` and `test.group()` calls
3. Executes tests using your project's test runner (e.g., `node ace test`, `node bin/test.js`)
4. Parses the output and displays results in the IDE's test runner UI

## Test Execution Detection

The plugin attempts to run tests using these methods in order:
1. `node ace test` (AdonisJS projects)
2. `node bin/test.js` (Custom test runner)
3. `node_modules/.bin/japa` (Direct Japa CLI)

You can customize the command using additional arguments in the run configuration.

## Supported Test Patterns

```typescript
// Single test
test('should do something', async ({ assert }) => {
  assert.equal(1, 1)
})

// Test group
test.group('User tests', () => {
  test('should create user', async ({ assert }) => {
    // test code
  })

  test('should delete user', async ({ assert }) => {
    // test code
  })
})
```

## Debugging

To debug tests:
1. Set breakpoints in your test files
2. Click the 🐛 debug icon in the gutter, or
3. Right-click → **Debug 'Test: ...'**

The plugin will attach the Node.js debugger to your test process.

## Configuration

### Environment Variables

You can add environment variables in the run configuration editor:
- Go to **Run → Edit Configurations**
- Select your Japa configuration
- Use the environment variables section

### Additional CLI Arguments

Add custom flags to the test command:
- `--watch` - Enable watch mode
- `--timeout 5000` - Set timeout
- Any other Japa CLI flags

## Troubleshooting

### Tests Not Detected

- Ensure `@japa/runner` is in your `package.json` dependencies
- Check that your test files use the standard `test()` syntax
- Verify the IDE has indexed your project (wait for indexing to complete)

### Tests Won't Run

- Check the working directory is set to your project root
- Verify Node.js is properly configured in **Settings → Languages & Frameworks → Node.js**
- Ensure your test runner script exists (`ace`, `bin/test.js`, etc.)

### Debug Not Working

- Ensure Node.js debugger is enabled
- Check that source maps are generated for TypeScript projects
- Verify the debug port is not in use

## Development

### Building

```bash
./gradlew buildPlugin
```

### Running in Development

```bash
./gradlew runIde
```

This will start a new IDE instance with the plugin installed.

### Code Quality

This project uses automated quality checks at multiple levels:

- **Git Hooks** (automatically installed on first Gradle sync):
  - `commit-msg`: Validates conventional commit format
  - Manual installation: `./gradlew installGitHooks`
- **Code Formatting**: Spotless with ktlint ensures consistent code style
    - Runs automatically on Gradle sync
  - Run locally: `./gradlew spotlessCheck`
  - Auto-fix: `./gradlew spotlessApply`
- **CI/CD**: GitHub Actions runs on every push and pull request
  - Validates Gradle wrapper integrity
  - Checks code formatting
  - Builds and tests the plugin

### Testing

```bash
./gradlew test
```

## Project Structure

```
src/main/kotlin/com/japaTestRunner/
├── detection/          # Test detection logic
│   └── JapaTestDetector.kt
├── editor/            # Gutter icons and line markers
│   └── JapaTestLineMarkerProvider.kt
├── execution/         # Output parsing
│   └── JapaTestOutputParser.kt
├── model/            # Data models
│   └── JapaTestElement.kt
└── run/              # Run configuration and execution
    ├── JapaConfigurationFactory.kt
    ├── JapaConsoleProperties.kt
    ├── JapaDebugRunner.kt
    ├── JapaRunConfiguration.kt
    ├── JapaRunConfigurationEditor.kt
    ├── JapaRunConfigurationProducer.kt
    ├── JapaRunConfigurationType.kt
    ├── JapaRunProfileState.kt
    ├── JapaTestLocator.kt
    └── JapaTestRunner.kt
```

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes with proper code formatting (`./gradlew spotlessApply`)
4. Ensure the build passes (`./gradlew build`)
5. Submit a pull request

All pull requests are automatically validated by CI/CD, which checks:
- Code formatting (Spotless/ktlint)
- Build success
- Gradle wrapper integrity

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## Releases

This project uses automated semantic versioning and releases. See [.github/RELEASE.md](.github/RELEASE.md) for details on:
- How releases are triggered
- Commit message conventions
- Publishing to JetBrains Marketplace
- Required GitHub secrets

## License

[Specify your license here]

## Credits

Built with ❤️ for the Japa testing framework community.
