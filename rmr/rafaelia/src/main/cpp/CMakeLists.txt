# Copyright (C) 2026 Rafael Melo Reis (RmR)
#
# Licensed under the Apache License, Version 2.0 with Additional Restrictions.
# See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.

cmake_minimum_required(VERSION 3.18.1)

project(rafaelia-native)

# Keep the floating-point model explicit and consistent with the Gradle property.
option(RMR_FAST_MATH "Enable aggressive floating-point optimizations" OFF)

# Maximum optimization flags for bare-metal performance
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++20 -O3 -funroll-loops")
if(RMR_FAST_MATH)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffast-math")
else()
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-fast-math")
endif()
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -fno-exceptions -fvisibility=hidden")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -Werror")

# Architecture-specific optimizations supported by Android NDK clang
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    # AArch64 baseline with NEON/FP enabled by ABI
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a")
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    # ARMv7-A hard-float NEON profile supported by current NDK toolchain
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv7-a -mfpu=neon -mfloat-abi=softfp")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    # x86_64 Android baseline supports SSE4.2
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=x86-64 -msse4.2")
elseif(${ANDROID_ABI} STREQUAL "x86")
    # x86 Android baseline with atom tuning and SSE3
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=i686 -mtune=atom -msse3 -mstackrealign -mfpmath=sse")
endif()

# Link-time optimization for maximum performance
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -flto -fwhole-program-vtables")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_RELEASE} -flto")

# Source files
set(SOURCES
    rafaelia_core.cpp
    rafaelia_memory.cpp
    rafaelia_vector.cpp
    rafaelia_matrix.cpp
    rafaelia_cpu_detect.cpp
)

# Create shared library
add_library(rafaelia-native SHARED ${SOURCES})

# Link with log library for Android logging
find_library(log-lib log)
target_link_libraries(rafaelia-native ${log-lib})

# Strip symbols in release builds for minimal size
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    add_custom_command(TARGET rafaelia-native POST_BUILD
        COMMAND ${CMAKE_STRIP} --strip-unneeded $<TARGET_FILE:rafaelia-native>
    )
endif()
