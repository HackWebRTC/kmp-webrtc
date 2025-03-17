#!/bin/bash

set -e

./gradlew :kmp-webrtc:linkReleaseSharedLinuxX64

cp kmp-webrtc/build/bin/linuxX64/releaseShared/libkmp_webrtc_api.h libs/linux/x64
cp kmp-webrtc/build/bin/linuxX64/releaseShared/libkmp_webrtc.so libs/linux/x64
