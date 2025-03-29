#!/bin/bash

set -e

./scripts/setup_linux.sh

mkdir example/linuxApp/build

pushd example/linuxApp/build
cmake ..
make
popd
