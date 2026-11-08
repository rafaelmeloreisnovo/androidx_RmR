#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
export LC_ALL=C
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SRC="$HERE/../../main/cpp/portable"
WORK=$(mktemp -d "${TMPDIR:-/tmp}/rmr-tri.XXXXXXXX")
trap 'rm -rf -- "$WORK"' EXIT HUP INT TERM
FLAGS='-std=c11 -O2 -ffreestanding -fno-builtin -fno-stack-protector -fvisibility=hidden -Wall -Wextra -Wshadow -Wstrict-prototypes -Wconversion -Wsign-conversion -Werror -pedantic'

# Same oracle and bit-sliced implementation on GCC + Clang.
for CC in gcc clang; do
  "$CC" $FLAGS "$SRC/rmr_tri21_scalar.c" "$SRC/rmr_tri21_bitsliced.c" \
    "$SRC/rmr_tri40.c" "$HERE/rmr_tri21_exact_test.c" -o "$WORK/test-$CC"
  "$WORK/test-$CC"
  echo "HOST_$CC=PASS"
done

# Compile-only cross-target proof. ARM object creation is NOT device execution.
for TARGET in armv7a-none-eabi aarch64-none-elf; do
  for F in rmr_tri21_scalar rmr_tri21_bitsliced rmr_tri40; do
    clang --target="$TARGET" $FLAGS -c "$SRC/$F.c" -o "$WORK/$TARGET-$F.o"
    if [ -n "$(nm -u "$WORK/$TARGET-$F.o")" ]; then
      echo "ERROR_IMPORT:$TARGET:$F"; exit 1
    fi
  done
  echo "TARGET_$TARGET=COMPILE_AND_SYMBOL_PASS"
done

for F in rmr_tri21_scalar rmr_tri21_bitsliced rmr_tri40; do
  clang --target=armv7a-none-eabi -mthumb -march=armv7-a \
    $FLAGS -c "$SRC/$F.c" -o "$WORK/thumb-$F.o"
  if [ -n "$(nm -u "$WORK/thumb-$F.o")" ]; then
    echo "ERROR_IMPORT:thumb:$F"; exit 1
  fi
done
echo 'THUMB2=COMPILE_AND_SYMBOL_PASS'

# Host sanitizer does not replace Android NDK or physical execution.
clang -std=c11 -O1 -g -Wall -Wextra -Werror \
  -fsanitize=address,undefined -fno-omit-frame-pointer \
  "$SRC/rmr_tri21_scalar.c" "$SRC/rmr_tri21_bitsliced.c" \
  "$SRC/rmr_tri40.c" "$HERE/rmr_tri21_exact_test.c" -o "$WORK/san"
"$WORK/san"
echo 'ASAN_UBSAN=PASS; DEVICE=NOT_RUN; JNI=NOT_RUN; GPU=NOT_RUN'
