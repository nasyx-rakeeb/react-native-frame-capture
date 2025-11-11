# Contributing to React Native Frame Capture

Thank you for your interest in contributing to React Native Frame Capture! We welcome contributions of all kinds, from bug reports and documentation improvements to new features and performance enhancements.

Please take a moment to review this document to make the contribution process smooth and effective for everyone involved.

## Code of Conduct

We want this community to be friendly and respectful to each other. Please follow our [Code of Conduct](./CODE_OF_CONDUCT.md) in all your interactions with the project.

## Development workflow

This project is a monorepo managed using [Yarn workspaces](https://yarnpkg.com/features/workspaces). It contains the following packages:

- The library package in the root directory.
- An example app in the `example/` directory.

To get started with the project, make sure you have the correct version of [Node.js](https://nodejs.org/) installed. See the [`.nvmrc`](./.nvmrc) file for the version used in this project.

Run `yarn` in the root directory to install the required dependencies for each package:

```sh
yarn
```

> Since the project relies on Yarn workspaces, you cannot use [`npm`](https://github.com/npm/cli) for development without manually migrating.

The [example app](/example/) demonstrates usage of the library. You need to run it to test any changes you make.

It is configured to use the local version of the library, so any changes you make to the library's source code will be reflected in the example app. Changes to the library's JavaScript code will be reflected in the example app without a rebuild, but native code changes will require a rebuild of the example app.

If you want to use Android Studio or Xcode to edit the native code, you can open the `example/android` or `example/ios` directories respectively in those editors. To edit the Objective-C or Swift files, open `example/ios/FrameCaptureExample.xcworkspace` in Xcode and find the source files at `Pods > Development Pods > react-native-frame-capture`.

To edit the Java or Kotlin files, open `example/android` in Android studio and find the source files at `react-native-frame-capture` under `Android`.

You can use various commands from the root directory to work with the project.

To start the packager:

```sh
yarn example start
```

To run the example app on Android:

```sh
yarn example android
```

To run the example app on iOS:

```sh
yarn example ios
```

To confirm that the app is running with the new architecture, you can check the Metro logs for a message like this:

```sh
Running "FrameCaptureExample" with {"fabric":true,"initialProps":{"concurrentRoot":true},"rootTag":1}
```

Note the `"fabric":true` and `"concurrentRoot":true` properties.

Make sure your code passes TypeScript and ESLint. Run the following to verify:

```sh
yarn typecheck
yarn lint
```

To fix formatting errors, run the following:

```sh
yarn lint --fix
```

Remember to add tests for your change if possible. Run the unit tests by:

```sh
yarn test
```

### Commit message convention

We follow the [conventional commits specification](https://www.conventionalcommits.org/en) for our commit messages:

- `fix`: bug fixes, e.g. fix crash due to deprecated method.
- `feat`: new features, e.g. add new method to the module.
- `refactor`: code refactor, e.g. migrate from class components to hooks.
- `docs`: changes into documentation, e.g. add usage example for the module.
- `test`: adding or updating tests, e.g. add integration tests using detox.
- `chore`: tooling changes, e.g. change CI config.

Our pre-commit hooks verify that your commit message matches this format when committing.

### Linting and tests

[ESLint](https://eslint.org/), [Prettier](https://prettier.io/), [TypeScript](https://www.typescriptlang.org/)

We use [TypeScript](https://www.typescriptlang.org/) for type checking, [ESLint](https://eslint.org/) with [Prettier](https://prettier.io/) for linting and formatting the code, and [Jest](https://jestjs.io/) for testing.

Our pre-commit hooks verify that the linter and tests pass when committing.

### Publishing to npm

We use [release-it](https://github.com/release-it/release-it) to make it easier to publish new versions. It handles common tasks like bumping version based on semver, creating tags and releases etc.

To publish new versions, run the following:

```sh
yarn release
```

### Scripts

The `package.json` file contains various scripts for common tasks:

- `yarn`: setup project by installing dependencies.
- `yarn typecheck`: type-check files with TypeScript.
- `yarn lint`: lint files with ESLint.
- `yarn test`: run unit tests with Jest.
- `yarn example start`: start the Metro server for the example app.
- `yarn example android`: run the example app on Android.
- `yarn example ios`: run the example app on iOS.

### Project Structure

Understanding the project structure will help you navigate the codebase:

```
react-native-frame-capture/
├── src/                          # TypeScript source code
│   ├── api.ts                    # Public API functions
│   ├── types.ts                  # TypeScript type definitions
│   ├── validation.ts             # Input validation
│   ├── normalize.ts              # Options normalization
│   ├── events.ts                 # Event handling
│   ├── errors.ts                 # Error classes
│   ├── constants.ts              # Constants and defaults
│   ├── NativeFrameCapture.ts     # TurboModule specification
│   └── index.tsx                 # Main entry point
├── android/                      # Android native code
│   └── src/main/java/com/framecapture/
│       ├── FrameCaptureModule.kt # Main TurboModule implementation
│       ├── CaptureManager.kt     # Core capture logic
│       ├── StorageManager.kt     # Storage operations
│       ├── OverlayRenderer.kt    # Overlay rendering
│       ├── ScreenCaptureService.kt # Foreground service
│       ├── Constants.kt          # Native constants
│       ├── capture/              # Capture-related classes
│       ├── models/               # Data models and enums
│       ├── service/              # Service-related classes
│       ├── storage/              # Storage strategies
│       └── utils/                # Utility classes
├── example/                      # Example React Native app
└── docs/                         # Documentation (if any)
```

### Development Guidelines

#### TypeScript Code

- Follow the existing code style (enforced by ESLint and Prettier)
- Add JSDoc comments for public APIs
- Export types for public interfaces
- Use strict TypeScript settings
- Avoid `any` types when possible

#### Kotlin Code

- Follow Kotlin coding conventions
- Use data classes for models
- Prefer immutability where possible
- Add KDoc comments for public APIs
- Use meaningful variable and function names
- Handle errors gracefully with try-catch blocks

#### Testing

- Add unit tests for new features
- Update existing tests when modifying functionality
- Ensure all tests pass before submitting PR
- Test on multiple Android versions if possible (especially Android 5.0, 10, and 13+)

#### Documentation

- Update README.md for new features or API changes
- Update CHANGELOG.md following Keep a Changelog format
- Add code examples for new features
- Update TypeScript type definitions

### Debugging Tips

#### Android Native Debugging

1. **Enable Logcat filtering:**

   ```sh
   adb logcat | grep -E "CaptureManager|FrameCaptureModule|StorageManager"
   ```

2. **Check MediaProjection permission:**

   ```sh
   adb shell dumpsys media_projection
   ```

3. **Monitor storage:**

   ```sh
   adb shell df /data/data/com.yourapp/cache
   ```

4. **View captured frames:**
   ```sh
   adb shell ls -la /data/data/com.yourapp/cache/captured_frames/
   ```

#### Common Issues

- **Build errors:** Clean and rebuild

  ```sh
  cd example/android && ./gradlew clean && cd ../..
  yarn example android
  ```

- **Metro bundler issues:** Reset cache

  ```sh
  yarn example start --reset-cache
  ```

- **Native changes not reflecting:** Rebuild the app
  ```sh
  yarn example android
  ```

### Sending a Pull Request

> **Working on your first pull request?** You can learn how from this _free_ series: [How to Contribute to an Open Source Project on GitHub](https://app.egghead.io/playlists/how-to-contribute-to-an-open-source-project-on-github).

#### Before Submitting

1. **Fork the repository** and create your branch from `main`
2. **Install dependencies:** `yarn`
3. **Make your changes** following the guidelines above
4. **Test your changes** thoroughly
5. **Run linters:** `yarn lint` and `yarn typecheck`
6. **Run tests:** `yarn test`
7. **Update documentation** if needed
8. **Commit your changes** following conventional commits

#### Pull Request Guidelines

- **Prefer small pull requests** focused on one change
- **Verify that linters and tests are passing**
- **Review the documentation** to make sure it looks good
- **Follow the pull request template** when opening a pull request
- **For API or implementation changes,** discuss with maintainers first by opening an issue
- **Add screenshots or videos** for UI-related changes
- **Reference related issues** in your PR description

#### Pull Request Checklist

- [ ] Code follows the project's style guidelines
- [ ] Self-review of code completed
- [ ] Comments added for complex logic
- [ ] Documentation updated (README, CHANGELOG, etc.)
- [ ] Tests added/updated and passing
- [ ] Linters and type checks passing
- [ ] Tested on Android device/emulator
- [ ] No breaking changes (or clearly documented)

### Reporting Bugs

When reporting bugs, please include:

1. **Description:** Clear description of the issue
2. **Steps to reproduce:** Detailed steps to reproduce the behavior
3. **Expected behavior:** What you expected to happen
4. **Actual behavior:** What actually happened
5. **Environment:**
   - React Native version
   - Android version
   - Device/Emulator
   - Library version
6. **Code sample:** Minimal reproducible example
7. **Logs:** Relevant error messages or logcat output
8. **Screenshots/Videos:** If applicable

### Requesting Features

When requesting features, please include:

1. **Use case:** Describe the problem you're trying to solve
2. **Proposed solution:** How you envision the feature working
3. **Alternatives considered:** Other solutions you've thought about
4. **Additional context:** Any other relevant information

### Questions?

If you have questions about contributing, feel free to:

- Open a [GitHub Discussion](https://github.com/nasyx-rakeeb/react-native-frame-capture/discussions)
- Open an issue with the `question` label
- Check existing issues and discussions

## License

By contributing to React Native Frame Capture, you agree that your contributions will be licensed under the MIT License.
