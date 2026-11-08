#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
export LC_ALL=C
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
# ROOT is rmr/rmr-core, independent of the caller's working directory.
SRC="$ROOT/src/main/cpp/portable/rmr_matrix_f64.c"
TEST="$ROOT/src/test/native/rmr_matrix_f64_test.c"
JNI="$ROOT/src/main/cpp/rmr_matrix_jni_c.c"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/rmr-c11-matrix.XXXXXXXX")
trap 'rm -rf -- "$WORK"' EXIT HUP INT TERM
CC=${CC:-cc}
FLAGS='-std=c11 -O2 -ffreestanding -fno-builtin -fno-stack-protector -fno-fast-math -ffp-contract=off -Wall -Wextra -Werror -Wshadow -Wstrict-prototypes -Wconversion -Wsign-conversion -pedantic'
"$CC" $FLAGS -c "$SRC" -o "$WORK/core.o"
if [ -n "$(nm -u "$WORK/core.o")" ]; then
  echo 'C11_CORE_UNEXPECTED_UNDEFINED_SYMBOLS'; exit 1
fi
"$CC" $FLAGS "$SRC" "$TEST" -o "$WORK/test"
"$WORK/test"
# This is an object build, NOT emulated or physical ARM execution.
if command -v clang >/dev/null 2>&1; then
  clang --target=armv7a-none-eabi -march=armv7-a -marm $FLAGS -c "$SRC" -o "$WORK/armv7-a32.o"
  clang --target=armv7a-none-eabi -march=armv7-a -mthumb $FLAGS -c "$SRC" -o "$WORK/armv7-thumb2.o"
  clang --target=aarch64-none-elf $FLAGS -c "$SRC" -o "$WORK/aarch64.o"
else
  echo 'MISSING_CLANG_CROSS_COMPILER'; exit 1
fi
if [ -n "${JAVA_HOME:-}" ]; then
  case "$(uname -s)" in
    Linux) JNI_OS=linux ;;
    Darwin) JNI_OS=darwin ;;
    *) echo 'UNSUPPORTED_HOST_JNI_OS'; exit 1 ;;
  esac
  "$CC" -std=c11 -O2 -Wall -Wextra -Werror -pedantic \
    -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/$JNI_OS" \
    -c "$JNI" -o "$WORK/jni.o"
else
  echo 'HOST_JNI_SKIPPED_JAVA_HOME_ABSENT'
fi
printf '%s\n' 'RMR_MATRIX_C11_SCOPED_PASS=host+C11+ABI_objects; JNI_if_JAVA_HOME; device=NOT_RUN'
