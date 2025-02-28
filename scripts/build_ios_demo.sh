#!/bin/bash

set -e

./scripts/setup_apple.sh

pushd example/iosApp
xcodebuild -workspace iosApp.xcworkspace \
    -scheme iosApp \
    -sdk iphonesimulator \
    -configuration Debug
popd
