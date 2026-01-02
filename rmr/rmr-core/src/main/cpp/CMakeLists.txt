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

project("rmr-core")

# Enable C++17 standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Common compiler flags for optimization
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")

# Architecture-specific optimizations
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    # ARM64 with NEON (always available on ARM64)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a")
    message(STATUS "Building for ARM64 with NEON support")
    
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    # ARM32 with NEON
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv7-a -mfpu=neon -mfloat-abi=softfp")
    message(STATUS "Building for ARM32 with NEON support")
    
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    # x86_64 with SSE2 baseline, enable AVX if available
    # Note: AVX might not be available on all x86_64 Android devices
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -msse2 -msse3")
    message(STATUS "Building for x86_64 with SSE2/SSE3 support")
    
    # Optionally enable AVX (commented out for compatibility)
    # set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mavx")
    
elseif(${ANDROID_ABI} STREQUAL "x86")
    # x86 with SSE2
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -msse2")
    message(STATUS "Building for x86 with SSE2 support")
endif()

# Additional optimization flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-exceptions -fno-rtti")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -funroll-loops")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fomit-frame-pointer")

# Link-time optimization (LTO)
if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -flto")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -flto")
endif()

# Create the native library
add_library(
    rmr-core-native
    SHARED
    rmr_matrix_native.cpp
)

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
