# Copyright (C) 2026 Rafael Melo Reis (RmR)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cmake_minimum_required(VERSION 3.18.1)

project("rmr-core" LANGUAGES C CXX)

# Opt-in migration: preserve the existing SIMD C++ implementation by default.
option(RMR_MATRIX_C11_BACKEND "Use candidate freestanding C11 matrix leaf with C JNI adapter" OFF)

# Keep legacy C++17 ABI available while the C11 candidate undergoes parity gates.
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(RMR_MATRIX_C11_BACKEND)
    set(RMR_MATRIX_NATIVE_SOURCES
        rmr_matrix_jni_c.c
        portable/rmr_matrix_f64.c)
else()
    set(RMR_MATRIX_NATIVE_SOURCES rmr_matrix_native.cpp)
endif()

# Create the native library
add_library(
    rmr-core-native
    SHARED
    ${RMR_MATRIX_NATIVE_SOURCES}
)

# Flags remain scoped: do not feed C++ flags to the C11 leaf or JNI adapter.
if(RMR_MATRIX_C11_BACKEND)
    target_compile_features(rmr-core-native PRIVATE c_std_11)
    target_compile_options(rmr-core-native PRIVATE -O3 -funroll-loops -fno-fast-math -ffp-contract=off)
    set_source_files_properties(portable/rmr_matrix_f64.c PROPERTIES
        COMPILE_OPTIONS "-ffreestanding;-fno-builtin;-fno-stack-protector;-fvisibility=hidden;-Wall;-Wextra;-Werror;-Wshadow;-Wstrict-prototypes")
else()
    target_compile_options(rmr-core-native PRIVATE
        -O3
        -fno-exceptions
        -fno-rtti
        -funroll-loops)
endif()

# Architecture-specific optimizations
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    # ARM64 with NEON (always available on ARM64)
    target_compile_options(rmr-core-native PRIVATE -march=armv8-a)
    message(STATUS "Building for ARM64 with NEON support")
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    # ARM32 with NEON
    target_compile_options(rmr-core-native PRIVATE -march=armv7-a -mfpu=neon -mfloat-abi=softfp)
    message(STATUS "Building for ARM32 with NEON support")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    # x86_64 with SSE2 baseline, enable AVX if available
    # Note: AVX might not be available on all x86_64 Android devices
    target_compile_options(rmr-core-native PRIVATE -msse2 -msse3)
    message(STATUS "Building for x86_64 with SSE2/SSE3 support")
elseif(${ANDROID_ABI} STREQUAL "x86")
    # x86 with SSE2
    target_compile_options(rmr-core-native PRIVATE -msse2)
    message(STATUS "Building for x86 with SSE2 support")
endif()

# Link-time optimization (LTO)
if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(rmr-core-native PRIVATE -flto)
    target_link_options(rmr-core-native PRIVATE -flto)
endif()

# Link against Android log library (for potential logging)
find_library(log-lib log)
target_link_libraries(rmr-core-native ${log-lib})

# Strip symbols in release builds
if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_custom_command(
        TARGET rmr-core-native
        POST_BUILD
        COMMAND ${CMAKE_STRIP} --strip-unneeded $<TARGET_FILE:rmr-core-native>
    )
endif()
