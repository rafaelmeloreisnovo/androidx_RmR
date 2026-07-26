#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

ABIS="${RMR_NATIVE_ABIS:-arm64-v8a}"
FAST_MATH="${RMR_FAST_MATH:-${ORG_GRADLE_PROJECT_rmrFastMath:-false}}"
VERIFY_CMAKE_CONFIGURATION="${RMR_VERIFY_CMAKE_CONFIGURATION:-false}"
GRADLE_JVM_ARGS="${RMR_GRADLE_JVM_ARGS:-}"
GRADLE_WORKERS="${RMR_GRADLE_WORKERS:-}"
GRADLE_PROPERTIES=()

case "${FAST_MATH}" in
  true|false) ;;
  *)
    echo "[rmr] RMR_FAST_MATH must be true or false" >&2
    exit 2
    ;;
esac

case "${VERIFY_CMAKE_CONFIGURATION}" in
  true|false) ;;
  *)
    echo "[rmr] RMR_VERIFY_CMAKE_CONFIGURATION must be true or false" >&2
    exit 2
    ;;
esac

if [[ -n "${GRADLE_JVM_ARGS}" ]]; then
  GRADLE_PROPERTIES+=("-Dorg.gradle.jvmargs=${GRADLE_JVM_ARGS}")
fi
if [[ -n "${GRADLE_WORKERS}" ]]; then
  GRADLE_PROPERTIES+=("-Dorg.gradle.workers.max=${GRADLE_WORKERS}")
fi

cd "${REPO_ROOT}"
echo "[rmr] Building release artifacts with ABIs: ${ABIS}; fast-math: ${FAST_MATH}"
if [[ -n "${GRADLE_JVM_ARGS}" || -n "${GRADLE_WORKERS}" ]]; then
  echo "[rmr] Using CI-supplied Gradle resource bounds"
fi

./gradlew --stacktrace --no-daemon \
  "${GRADLE_PROPERTIES[@]}" \
  -PrmrNativeAbis="${ABIS}" \
  -PrmrFastMath="${FAST_MATH}" \
  :rmr:rmr-core:assembleRelease \
  :rmr:rafaelia:assembleRelease \
  :rmr:rafaelia-core:assembleRelease \
  :rmr:rmr-room:assembleRelease \
  :rmr:rmr-navigation:assembleRelease \
  :rmr:rmr-lifecycle:assembleRelease \
  :rmr:rmr-preference:assembleRelease

# AndroidX's settings redirect build output outside the source checkout. Public CI
# supplies RMR_BUILD_OUTPUT_ROOT explicitly; retain the source-tree fallback for
# local layouts that do not use AndroidX's out directory.
OUTPUT_ROOT="${RMR_BUILD_OUTPUT_ROOT:-${OUT_DIR:-${REPO_ROOT}/../../out/androidx}/rmr}"
if [[ ! -d "${OUTPUT_ROOT}" ]]; then
  OUTPUT_ROOT="${REPO_ROOT}/rmr"
fi

if [[ "${VERIFY_CMAKE_CONFIGURATION}" == "true" ]]; then
  expected_cmake_value="OFF"
  if [[ "${FAST_MATH}" == "true" ]]; then
    expected_cmake_value="ON"
  fi
  cmake_cache_root="${OUTPUT_ROOT}/rafaelia/nativeBuildStaging"
  cmake_cache_count=0
  while IFS= read -r -d '' cmake_cache; do
    grep -Fqx "RMR_FAST_MATH:BOOL=${expected_cmake_value}" "${cmake_cache}"
    cmake_cache_count=$((cmake_cache_count + 1))
  done < <(find "${cmake_cache_root}" -type f -name CMakeCache.txt -print0)
  if (( cmake_cache_count == 0 )); then
    echo "[rmr] No Rafaelia CMake cache was found for configuration verification" >&2
    exit 1
  fi
  echo "[rmr] Verified RMR_FAST_MATH=${expected_cmake_value} in ${cmake_cache_count} CMake cache(s)"
  if [[ -n "${RMR_BUILD_RECEIPT_PATH:-}" ]]; then
    mkdir -p "$(dirname "${RMR_BUILD_RECEIPT_PATH}")"
    printf '{"rmr_fast_math":"%s","cmake_value":"%s","verified_cmake_cache_count":%s}\n' \
      "${FAST_MATH}" "${expected_cmake_value}" "${cmake_cache_count}" > "${RMR_BUILD_RECEIPT_PATH}"
  fi
fi

echo "[rmr] Generated artifacts from: ${OUTPUT_ROOT}"
find "${OUTPUT_ROOT}" -type f -path "*/build/outputs/aar/*.aar" -print | sort
