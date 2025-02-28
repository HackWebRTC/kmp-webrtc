# kmp-webrtc

KMP wrapper for WebRTC.

![Maven Central Version](https://img.shields.io/maven-central/v/com.piasy/kmp-webrtc) ![Main branch status](https://github.com/HackWebRTC/kmp-webrtc/actions/workflows/ci.yaml/badge.svg?branch=main)

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
./scripts/setup_apple.sh
# open example/iosApp/iosApp.xcworkspace in Xcode, and run it.
```

### macOS

```bash
./scripts/setup_apple.sh
# open example/macApp/macApp.xcworkspace in Xcode, and run it.
```

## Build WebRTC

### Android

```bash
# on Linux
./sdk/build_android_libs.sh <output path> --skip-build-ffmpeg

# on macOS, copy prebuilt_libs into sdk/android_gradle/webrtc/
# then build aar like this:
pushd ../webrtc_repo/webrtc_ios/src/sdk/android_gradle/ && \
./gradlew :webrtc:assembleRelease && \
cp webrtc/build/outputs/aar/webrtc-release.aar \
  ../../../../../kmp-webrtc/libs/android/webrtc.aar && \
popd
```

### Apple

```bash
pushd ../webrtc_repo/webrtc_ios/src/ && \
./sdk/build_apple_framework.sh ../../../kmp-webrtc/libs --skip-build-ffmpeg && \
popd
```

### Upload libs zip

```bash
zip -ry build/libs.zip libs
```

Then upload build/libs.zip to GitHub releases.

## Publish

Maven central portal credentials and signing configs are set in `~/.gradle/gradle.properties`.

```bash
```

Login to https://central.sonatype.com/publishing/deployments, and release them manually.
