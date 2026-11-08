#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
export LC_ALL=C
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SRC="$HERE/../../main/cpp/portable/rmr_lease_v1.c"
TEST="$HERE/rmr_lease_v1_test.c"
CPP="$HERE/../../main/cpp/rafaelia_core.cpp"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/rmr-lease-p0.XXXXXXXX")
trap 'rm -rf -- "$WORK"' EXIT HUP INT TERM
CC=${CC:-cc}
FLAGS='-std=c11 -O2 -ffreestanding -fno-builtin -fno-stack-protector -Wall -Wextra -Wshadow -Wstrict-prototypes -Wconversion -Wsign-conversion -Werror -pedantic'
"$CC" $FLAGS -c "$SRC" -o "$WORK/lease.o"
if [ -n "$(nm -u "$WORK/lease.o")" ]; then
  echo 'LEASE_CORE_UNEXPECTED_SYMBOL_IMPORT'; exit 1
fi
"$CC" $FLAGS "$SRC" "$TEST" -o "$WORK/test"
"$WORK/test"
# Four controlled link profiles on the SAME host/compiler/lld and SAME source.
# This compares section GC, safe ICF and compact per-leaf flags independently.
# Metrics are host ELF only, not Android binaries or speed measurements.
if ! command -v ld.lld >/dev/null 2>&1; then
  echo 'RMR_LAYOUT_BINARY=BLOCKED_LLD_UNAVAILABLE'
  exit 1
fi
for MODE in plain sections icf compact; do
  EXTRA=''
  LINK=''
  case "$MODE" in
    plain) ;;
    sections)
      EXTRA='-ffunction-sections -fdata-sections -fno-common'
      LINK='-Wl,--gc-sections' ;;
    icf)
      EXTRA='-ffunction-sections -fdata-sections -fno-common'
      LINK='-Wl,--gc-sections -Wl,--icf=safe' ;;
    compact)
      EXTRA='-ffunction-sections -fdata-sections -fno-common -Os -fno-unroll-loops'
      LINK='-Wl,--gc-sections -Wl,--icf=safe' ;;
  esac
  "$CC" $FLAGS $EXTRA -fuse-ld=lld "$SRC" "$TEST" $LINK \
    "-Wl,-Map,$WORK/layout-$MODE.map" -o "$WORK/layout-$MODE"
  "$WORK/layout-$MODE"
  for SYMBOL in rmr_lease_init rmr_lease_open rmr_lease_view rmr_lease_close; do
    if ! nm -g --defined-only "$WORK/layout-$MODE" | grep -q " $SYMBOL$"; then
      echo "RMR_LAYOUT_MISSING_LIVE_SYMBOL=$MODE:$SYMBOL"
      exit 1
    fi
  done
  printf 'RMR_LAYOUT_MODE=%s; HOST_BYTES=%s; ' "$MODE" \
    "$(wc -c < "$WORK/layout-$MODE")"
  size -B "$WORK/layout-$MODE" | tail -n 1
  readelf -SW "$WORK/layout-$MODE" | grep -E '[[:space:]]\.text[[:space:]]' | head -n 1
done
if command -v clang >/dev/null 2>&1; then
  clang --target=armv7a-none-eabi -march=armv7-a -marm $FLAGS -c "$SRC" -o "$WORK/armv7-a32.o"
  clang --target=armv7a-none-eabi -march=armv7-a -mthumb $FLAGS -c "$SRC" -o "$WORK/armv7-thumb2.o"
  clang --target=aarch64-none-elf $FLAGS -c "$SRC" -o "$WORK/aarch64.o"
  if [ -z "${RMR_SKIP_SANITIZERS:-}" ]; then
    clang -std=c11 -O1 -g -Wall -Wextra -Werror \
      -fsanitize=address,undefined -fno-omit-frame-pointer \
      "$SRC" "$TEST" -o "$WORK/asan-test"
    "$WORK/asan-test"
  fi
else
  echo 'CLANG_CROSS_COMPILER_UNAVAILABLE'; exit 1
fi
# Hosted JNI object compile validates both native safety profiles without
# claiming a linked Android .so or runtime device. No installed Android SDK.
if [ -n "${JAVA_HOME:-}" ]; then
  mkdir -p "$WORK/stub/android"
  cat >"$WORK/stub/android/log.h" <<'STUB'
#ifndef ANDROID_LOG_H
#define ANDROID_LOG_H
#define ANDROID_LOG_INFO 4
#define ANDROID_LOG_ERROR 6
#ifdef __cplusplus
extern "C" {
#endif
int __android_log_print(int priority, const char *tag, const char *format, ...);
#ifdef __cplusplus
}
#endif
#endif
STUB
  for MODE in default safe; do
    DEF=''
    if [ "$MODE" = safe ]; then DEF='-DRMR_SAFE_LEASES=1'; fi
    c++ -std=c++20 -O2 -Wall -Wextra -Werror -fno-exceptions -fno-rtti \
      -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$WORK/stub" \
      $DEF -c "$CPP" -o "$WORK/jni-$MODE.o"
  done
else
  echo 'HOST_JNI_OBJECT=SKIPPED_NO_JAVA_HOME'
fi
echo 'RMR_LEASE_SCOPED_HOST=PASS; ARM_OBJECTS=COMPILED; ANDROID_DEVICE=NOT_RUN'
