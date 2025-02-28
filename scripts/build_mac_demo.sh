#!/bin/bash

set -e

./scripts/setup_apple.sh

pushd example/macApp
xcodebuild -workspace macApp.xcworkspace \
    -scheme macApp \
    -sdk macosx \
    -configuration Debug
popd
