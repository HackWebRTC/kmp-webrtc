#!/bin/bash

set -e

./gradlew :kmp-webrtc:podPublishReleaseXCFramework

pushd example/iosApp
xcodegen
pod install
popd

pushd example/macApp
xcodegen
pod install
popd
