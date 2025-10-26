# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0 (2025-10-26)

### ⚠ BREAKING CHANGES

* **ci:** Version management now automated via semantic-release

### ✨ Features

* Japa Test Runner IntelliJ Plugin ([25fcce7](https://github.com/Celli119/japa-intelliJ/commit/25fcce7075a3f2bc35500dad06052681006611f6))

### 🐛 Bug Fixes

* **ci:** add missing @semantic-release/exec and update all packages to latest versions ([d61ea0b](https://github.com/Celli119/japa-intelliJ/commit/d61ea0b54500a498e0dd8cd83b3902dc1babadd1))

### 🔧 Miscellaneous

* **ci:** setup automated releases and dependency management ([303d02d](https://github.com/Celli119/japa-intelliJ/commit/303d02d9e3e34fcc96cb9cc07b1327b8c66c7d66))

## [Unreleased]

### Added
- Initial release of Japa Test Runner plugin
- Interactive test running with gutter icons for Japa tests
- Run and debug support for individual tests, test groups, and entire files
- Automatic detection of test runner (node ace test, bin/test.js, or japa CLI)
- Support for JavaScript and TypeScript test files
- Integration with IntelliJ test runner UI
- Real-time test status updates and result visualization
