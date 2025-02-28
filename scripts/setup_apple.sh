#!/bin/bash

set -e

./gradlew :kmp-webrtc:podspec :kmp-webrtc:generateDummyFramework :kmp-webrtc:podPublishReleaseXCFramework

pushd example/iosApp
xcodegen
pod install
popd

pushd example/macApp
xcodegen
pod install
popd
