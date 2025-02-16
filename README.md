# kmp-webrtc

KMP wrapper for WebRTC.

![Maven Central Version](https://img.shields.io/maven-central/v/com.piasy/kmp-webrtc)

## Supported platforms

|      Platform      | 🛠Builds🛠 + 🔬Tests🔬 |
| :----------------: | :------------------: |
|      `JVM` 17      |          🔮          |
| `JS`     (Chrome)  |          🔮          |
| `WasmJS` (Chrome)  |          🔮          |
|     `Android`      |          🚀          |
|       `iOS`        |          🚀          |
|      `macOS`       |          🔮          |
|   `Windows X64`    |          🔮          |
|    `Linux X64`     |          🔮          |

## Dependency

You only need to add gradle dependency:

```kotlin
// add common source set dependency
kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.piasy:kmp-webrtc:$version")
      }
    }
  }
}
```

## Usage

```kotlin
```

## Env Setup

You need to install [RVM](https://rvm.io/) to manage your ruby version, and install gems. You need to use homebrew to install the following tools:

```bash
brew install cocoapods xcodegen
# if you have installed them earlier, you need to remove them at first,
# or run brew link --overwrite xcodegen cocoapods
```

You may need to restart your system so that Android Studio could use the correct ruby.

## Example

### Android

Open the project (the repo root dir) in Android studio, and run the example.androidApp target.

### iOS

```bash
cd example/iosApp
xcodegen
pod install
# open iosApp.xcworkspace in Xcode, and run it.
```

## Publish

Maven central portal credentials and signing configs are set in `~/.gradle/gradle.properties`.

```bash
```

Login to https://central.sonatype.com/publishing/deployments, and release them manually.
