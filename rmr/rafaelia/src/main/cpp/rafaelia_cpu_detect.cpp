/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * CPU feature detection
 */

#include <jni.h>

#if defined(__linux__)
    #include <sys/auxv.h>
    #if defined(__aarch64__) || defined(__arm__)
        #include <asm/hwcap.h>
    #endif
#endif

namespace {

#if defined(__x86_64__) || defined(__i386__)
constexpr jint kCpuFeatureSse = 1 << 0;
constexpr jint kCpuFeatureSse2 = 1 << 1;
constexpr jint kCpuFeatureSse3 = 1 << 2;
constexpr jint kCpuFeatureSse41 = 1 << 3;
constexpr jint kCpuFeatureSse42 = 1 << 4;
constexpr jint kCpuFeatureAvx = 1 << 5;
constexpr jint kCpuFeatureAvx2 = 1 << 6;
constexpr jint kCpuFeatureFma = 1 << 7;
#elif defined(__linux__) && (defined(__aarch64__) || defined(__arm__))
constexpr jint kCpuFeatureNeon = 1 << 8;
#endif

jint DetectCpuFeaturesRuntime() {
    jint features = 0;

#if defined(__x86_64__) || defined(__i386__)
    __builtin_cpu_init();
    if (__builtin_cpu_supports("sse")) {
        features |= kCpuFeatureSse;
    }
    if (__builtin_cpu_supports("sse2")) {
        features |= kCpuFeatureSse2;
    }
    if (__builtin_cpu_supports("sse3")) {
        features |= kCpuFeatureSse3;
    }
    if (__builtin_cpu_supports("sse4.1")) {
        features |= kCpuFeatureSse41;
    }
    if (__builtin_cpu_supports("sse4.2")) {
        features |= kCpuFeatureSse42;
    }
    if (__builtin_cpu_supports("avx")) {
        features |= kCpuFeatureAvx;
    }
    if (__builtin_cpu_supports("avx2")) {
        features |= kCpuFeatureAvx2;
    }
    if (__builtin_cpu_supports("fma")) {
        features |= kCpuFeatureFma;
    }
#elif defined(__aarch64__) || defined(__arm__)
#if defined(__linux__)
    unsigned long caps = getauxval(AT_HWCAP);
#if defined(__aarch64__)
    if ((caps & HWCAP_ASIMD) != 0) {
        features |= kCpuFeatureNeon;
    }
#else
    if ((caps & HWCAP_NEON) != 0) {
        features |= kCpuFeatureNeon;
    }
#endif
#endif
#endif

    return features;
}

}  // namespace

extern "C" jint RafaeliaDetectCpuFeatures() {
    static const jint cachedFeatures = DetectCpuFeaturesRuntime();
    return cachedFeatures;
}

// CPU detection implementation
// Detects available SIMD instruction sets
