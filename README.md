# kmp-webrtc

KMP wrapper for WebRTC.

![Maven Central Version](https://img.shields.io/maven-central/v/com.piasy/kmp-webrtc) ![Main branch status](https://github.com/HackWebRTC/kmp-webrtc/actions/workflows/ci.yaml/badge.svg?branch=main)

## Supported platforms

|      Platform      | 🛠Builds🛠 + 🔬Tests🔬 |
| :----------------: | :------------------: |
|     `Android`      |          🚀          |
|       `iOS`        |          🚀          |
|      `macOS`       |          🚀          |
|   `Windows X64`    |          🚀          |
| `JS`     (Chrome)  |          🚀          |
|    `Linux X64`     |          🚀          |

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

Download and extract the `libs` dir:

```bash
wget https://github.com/HackWebRTC/kmp-webrtc/releases/latest/download/libs.zip
unzip -o libs.zip
```

### macOS

You need to install [RVM](https://rvm.io/) to manage your ruby version, and install gems. You need to use homebrew to install the following tools:

```bash
brew install cocoapods xcodegen
# if you have installed them earlier, you need to remove them at first,
# or run brew link --overwrite xcodegen cocoapods
```

You may need to restart your system so that Android Studio could use the correct ruby.

### Windows

Follow [this guide](https://chromium.googlesource.com/chromium/src/+/master/docs/windows_build_instructions.md) to install Visual Studio 2022 and necessary tools.

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

### Windows

```bash
.\scripts\setup_windows.bat
# open example\winApp\winApp.sln in Visual Studio 2022, and run it.
```

### Linux

```bash
./scripts/build_linux_demo.sh
./example/linuxApp/build/loopback <path to video file>
```

### JS

```bash
./gradlew :example:webApp:jsBrowserRun
```

## Build WebRTC

[hack_webrtc_43659 branch of HackWebRTC/webrtc](https://github.com/HackWebRTC/webrtc/tree/hack_webrtc_43659), which is based on m133.

File structure for macOS:

```
- webrtc_apple
    - src
- kmp-webrtc
```

File structure for Windows:

```
- webrtc_windows
    - src
- kmp-webrtc
```

File structure for Linux:

```
- webrtc_android
    - src
- kmp-webrtc
```

### Android

CPP code need to be built on Linux.

```bash
# on Linux
./sdk/build_android_libs.sh <output path> --skip-build-ffmpeg

# on macOS, copy prebuilt_libs into sdk/android_gradle/webrtc/
# then build aar like this:
pushd ../webrtc_apple/src/sdk/android_gradle/ && \
./gradlew :webrtc:assembleRelease && \
cp webrtc/build/outputs/aar/webrtc-release.aar \
  ../../../../kmp-webrtc/libs/android/webrtc.aar && \
popd
```

### Apple

```bash
pushd ../webrtc_apple/src/ && \
./sdk/build_apple_framework.sh ../../kmp-webrtc/libs --skip-build-ffmpeg && \
popd
```

### Windows

In `x64 Native Tools Command Prompt for VS 2022`:

```bash
.\sdk\build_windows_libs.bat ..\..\kmp-webrtc
```

### Linux

```bash
pushd ../webrtc_android/src/ && \
./sdk/build_linux_libs.sh ../../kmp-webrtc/libs --skip-build-ffmpeg && \
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

## Other projects

- [shepeliev/webrtc-kmp](https://github.com/shepeliev/webrtc-kmp)
