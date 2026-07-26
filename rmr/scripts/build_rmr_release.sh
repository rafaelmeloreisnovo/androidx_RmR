#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

ABIS="${RMR_NATIVE_ABIS:-arm64-v8a}"
GRADLE_JVM_ARGS="${RMR_GRADLE_JVM_ARGS:-}"
GRADLE_WORKERS="${RMR_GRADLE_WORKERS:-}"
GRADLE_PROPERTIES=()

if [[ -n "${GRADLE_JVM_ARGS}" ]]; then
  GRADLE_PROPERTIES+=("-Dorg.gradle.jvmargs=${GRADLE_JVM_ARGS}")
fi
if [[ -n "${GRADLE_WORKERS}" ]]; then
  GRADLE_PROPERTIES+=("-Dorg.gradle.workers.max=${GRADLE_WORKERS}")
fi

cd "${REPO_ROOT}"
echo "[rmr] Building release artifacts with ABIs: ${ABIS}"
if [[ -n "${GRADLE_JVM_ARGS}" || -n "${GRADLE_WORKERS}" ]]; then
  echo "[rmr] Using CI-supplied Gradle resource bounds"
fi

./gradlew --stacktrace --no-daemon \
  "${GRADLE_PROPERTIES[@]}" \
  -PrmrNativeAbis="${ABIS}" \
  :rmr:rmr-core:assembleRelease \
  :rmr:rafaelia:assembleRelease \
  :rmr:rafaelia-core:assembleRelease \
  :rmr:rmr-room:assembleRelease \
  :rmr:rmr-navigation:assembleRelease \
  :rmr:rmr-lifecycle:assembleRelease \
  :rmr:rmr-preference:assembleRelease

echo "[rmr] Generated artifacts:"
find rmr -path "*/build/outputs/aar/*.aar" -print | sort
