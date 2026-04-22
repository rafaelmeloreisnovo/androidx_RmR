#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

ABIS="${RMR_NATIVE_ABIS:-arm64-v8a}"

cd "${REPO_ROOT}"
echo "[rmr] Building release artifacts with ABIs: ${ABIS}"

./gradlew --stacktrace --no-daemon \
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
