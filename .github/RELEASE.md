# Release Process

This document describes the automated release process for the Japa Test Runner plugin.

## Automated Releases

The project uses [semantic-release](https://github.com/semantic-release/semantic-release) for fully automated version management and package publishing. Releases are triggered automatically when commits are pushed to the `master` or `main` branch.

### How It Works

1. **Commits trigger releases**: Every push to `master`/`main` is analyzed
2. **Version calculation**: Based on [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat:` → Minor version bump (0.X.0)
   - `fix:` → Patch version bump (0.0.X)
   - `BREAKING CHANGE:` or `!` → Major version bump (X.0.0)
   - Other types (`docs:`, `chore:`, `ci:`, etc.) → No release
3. **Changelog generation**: Automatically updates `CHANGELOG.md`
4. **Plugin build**: Builds and signs the plugin with the new version
5. **GitHub Release**: Creates a GitHub release with release notes and plugin artifact (.zip)

### Commit Message Format

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature (triggers minor release)
- `fix`: Bug fix (triggers patch release)
- `perf`: Performance improvement (triggers patch release)
- `refactor`: Code refactoring (triggers patch release)
- `build`: Build system changes (triggers patch release)
- `docs`: Documentation only
- `style`: Code style changes (formatting, etc.)
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `ci`: CI/CD changes
- `revert`: Revert a previous commit

**Breaking Changes**:
Add `BREAKING CHANGE:` in the footer or `!` after the type:
```
feat!: redesign test detection API

BREAKING CHANGE: TestDetector interface has changed
```

**Examples**:
```bash
feat(editor): add support for TypeScript test files
fix(runner): correct test path resolution on Windows
perf(parser): optimize AST traversal for large files
docs: update installation instructions
chore(deps): update IntelliJ Platform to 2024.2
```

### Release Workflow

The `.github/workflows/release.yml` workflow:

1. Runs on every push to `master`/`main`
2. Checks out code with full git history
3. Sets up Java 21 and Gradle
4. Installs semantic-release and plugins
5. Runs `semantic-release` which:
   - Analyzes commits since last release
   - Determines next version number
   - Generates changelog
   - Builds and signs plugin with new version
   - Creates GitHub release with plugin artifact
   - Commits changelog back to repository

## Required GitHub Secrets

Configure these secrets in your repository settings (Settings → Secrets and variables → Actions):

### For Plugin Signing (Optional but Recommended)

Plugin signing ensures the authenticity of your releases:

- `CERTIFICATE_CHAIN`: Plugin signing certificate chain (PEM format)
- `PRIVATE_KEY`: Plugin signing private key (PEM format)
- `PRIVATE_KEY_PASSWORD`: Password for the private key

Generate signing certificates:
```bash
# Generate private key
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096

# Generate self-signed certificate (valid for 10 years)
openssl req -x509 -key private.pem -out chain.pem -days 3650
```

**Note**: If these secrets are not configured, the plugin will still build successfully but won't be signed.

### Automatically Provided

- `GITHUB_TOKEN`: Automatically provided by GitHub Actions (no setup needed)

## Future: JetBrains Marketplace Publishing

When you're ready to publish to the JetBrains Marketplace, you'll need to:

1. Add the `PUBLISH_TOKEN` secret (get from https://plugins.jetbrains.com/author/me/tokens)
2. Update `.releaserc.json` to re-enable the `publishCmd` in the `@semantic-release/exec` plugin:
   ```json
   "publishCmd": "VERSION=${nextRelease.version} ./gradlew publishPlugin --no-daemon --stacktrace"
   ```
3. Update the workflow to include the `PUBLISH_TOKEN` environment variable

## Manual Release

To manually trigger a release (emergency fixes):

1. Ensure all commits follow Conventional Commits format
2. Push to `master`/`main`:
   ```bash
   git checkout master
   git pull
   git merge feature-branch
   git push
   ```
3. Monitor the release workflow: https://github.com/YOUR_USERNAME/japaTestRunner/actions

## Skipping CI

To push changes without triggering a release:

```bash
git commit -m "chore: update documentation [skip ci]"
```

## Pre-release Versions

Currently not configured. To add support for beta/alpha releases:
1. Add beta/alpha branches to `.releaserc.json`
2. Configure prerelease channels
3. See: https://semantic-release.gitbook.io/semantic-release/usage/workflow-configuration

## Troubleshooting

### Release fails with "No release published"

- No commits with release-worthy types since last release
- All commits were `chore:`, `docs:`, `ci:`, etc.
- Solution: Ensure commits use `feat:` or `fix:` types

### Plugin signing fails

- Check that certificate secrets are correctly configured
- Verify certificate chain is in PEM format
- Ensure private key password is correct
- Solution: Test locally with:
  ```bash
  export CERTIFICATE_CHAIN="$(cat chain.pem)"
  export PRIVATE_KEY="$(cat private.pem)"
  export PRIVATE_KEY_PASSWORD="your_password"
  ./gradlew signPlugin
  ```

### Version conflict

- Usually occurs if manual version edits conflict with semantic-release
- Solution: Let semantic-release manage versions exclusively
- Never manually edit version in `build.gradle.kts` (except for the `VERSION` env var)

## Release History

View all releases: https://github.com/YOUR_USERNAME/japaTestRunner/releases

View changelog: [CHANGELOG.md](../../CHANGELOG.md)
