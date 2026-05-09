#!/usr/bin/env bash
set -e

source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk use java 21.0.6-tem

./gradlew --stop > /dev/null 2>&1

./gradlew testDebugUnitTest "$@"
