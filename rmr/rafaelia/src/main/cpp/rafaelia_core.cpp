/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 with Additional Restrictions.
 * See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.
 * 
 * AUTHORIZED USE ONLY - Unauthorized use subject to automatic penalties.
 */

#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <cstdint>
#include <cstdlib>
#include <limits>

// Architecture-specific intrinsics
#if defined(__ARM_NEON) || defined(__ARM_NEON__)
    #include <arm_neon.h>
    #define HAVE_NEON 1
#elif defined(__SSE4_2__) || defined(__AVX2__)
    #include <immintrin.h>
    #define HAVE_SSE 1
    #if defined(__AVX2__)
        #define HAVE_AVX2 1
    #endif
#endif

#define LOG_TAG "RafaeliaNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Ensure cache line alignment (64 bytes on most modern CPUs)
#define CACHE_LINE_SIZE 64
#define ALIGN_TO_CACHE_LINE __attribute__((aligned(CACHE_LINE_SIZE)))

extern "C" {

/**
 * Initialize native library.
 * Called once when library is loaded.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeInitialize(JNIEnv* env, jclass clazz) {
    LOGI("Rafaelia Native Library initialized");
    LOGI("Cache line size: %d bytes", CACHE_LINE_SIZE);
    
    #ifdef HAVE_NEON
        LOGI("ARM NEON support detected");
    #endif
    #ifdef HAVE_SSE
        LOGI("x86 SSE support detected");
    #endif
    #ifdef HAVE_AVX2
        LOGI("x86 AVX2 support detected");
    #endif
}

/**
 * Allocate cache-aligned memory.
 * Returns native pointer to aligned memory block.
 */
JNIEXPORT jlong JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeAllocateAligned(
        JNIEnv* env, jclass clazz, jint size, jint alignment) {
    
    void* ptr = nullptr;
    
    // Use posix_memalign for aligned allocation
    if (posix_memalign(&ptr, alignment, size) != 0) {
        LOGE("Failed to allocate aligned memory: size=%d, alignment=%d", size, alignment);
        return 0;
    }
    
    // Zero-initialize for security
    memset(ptr, 0, size);
    
    return reinterpret_cast<jlong>(ptr);
}

/**
 * Free aligned memory.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeFreeAligned(
        JNIEnv* env, jclass clazz, jlong address) {
    
    if (address == 0) return;
    
    void* ptr = reinterpret_cast<void*>(address);
    free(ptr);
}

/**
 * Optimized memory copy using SIMD when available.
 * Falls back to memcpy for unaligned or small sizes.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeMemoryCopy(
        JNIEnv* env, jclass clazz, jlong src, jlong dst, jint size) {
    if (size <= 0 || src == 0 || dst == 0) {
        return;
    }
    const void* srcPtr = reinterpret_cast<const void*>(src);
    void* dstPtr = reinterpret_cast<void*>(dst);
    
    // Use standard memcpy - it's already highly optimized
    // on modern platforms with SIMD instructions
    memcpy(dstPtr, srcPtr, size);
}

/**
 * Returns the native address of a direct ByteBuffer, or 0 if unavailable.
 */
JNIEXPORT jlong JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeGetDirectBufferAddress(
        JNIEnv* env, jclass clazz, jobject buffer) {
    if (buffer == nullptr) {
        return 0;
    }
    void* address = env->GetDirectBufferAddress(buffer);
    if (address == nullptr) {
        return 0;
    }
    return reinterpret_cast<jlong>(address);
}

/**
 * Optimized memory set using SIMD when available.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeMemorySet(
        JNIEnv* env, jclass clazz, jlong address, jbyte value, jint size) {
    if (size <= 0 || address == 0) {
        return;
    }
    void* ptr = reinterpret_cast<void*>(address);
    memset(ptr, value, size);
}

/**
 * SIMD-accelerated vector addition.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeVectorAdd(
        JNIEnv* env, jclass clazz, jlong a, jlong b, jlong result, jint length) {
    if (length <= 0 || a == 0 || b == 0 || result == 0) {
        return;
    }
    const float* aPtr = reinterpret_cast<const float*>(a);
    const float* bPtr = reinterpret_cast<const float*>(b);
    float* resultPtr = reinterpret_cast<float*>(result);
    
    int i = 0;
    
    #ifdef HAVE_AVX2
        // Process 8 floats at a time with AVX2
        for (; i + 7 < length; i += 8) {
            __m256 va = _mm256_loadu_ps(aPtr + i);
            __m256 vb = _mm256_loadu_ps(bPtr + i);
            __m256 vr = _mm256_add_ps(va, vb);
            _mm256_storeu_ps(resultPtr + i, vr);
        }
    #elif defined(HAVE_SSE)
        // Process 4 floats at a time with SSE
        for (; i + 3 < length; i += 4) {
            __m128 va = _mm_loadu_ps(aPtr + i);
            __m128 vb = _mm_loadu_ps(bPtr + i);
            __m128 vr = _mm_add_ps(va, vb);
            _mm_storeu_ps(resultPtr + i, vr);
        }
    #elif defined(HAVE_NEON)
        // Process 4 floats at a time with NEON
        for (; i + 3 < length; i += 4) {
            float32x4_t va = vld1q_f32(aPtr + i);
            float32x4_t vb = vld1q_f32(bPtr + i);
            float32x4_t vr = vaddq_f32(va, vb);
            vst1q_f32(resultPtr + i, vr);
        }
    #endif
    
    // Process remaining elements
    for (; i < length; i++) {
        resultPtr[i] = aPtr[i] + bPtr[i];
    }
}

/**
 * SIMD-accelerated vector multiplication.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeVectorMultiply(
        JNIEnv* env, jclass clazz, jlong a, jlong b, jlong result, jint length) {
    if (length <= 0 || a == 0 || b == 0 || result == 0) {
        return;
    }
    const float* aPtr = reinterpret_cast<const float*>(a);
    const float* bPtr = reinterpret_cast<const float*>(b);
    float* resultPtr = reinterpret_cast<float*>(result);
    
    int i = 0;
    
    #ifdef HAVE_AVX2
        // Process 8 floats at a time with AVX2
        for (; i + 7 < length; i += 8) {
            __m256 va = _mm256_loadu_ps(aPtr + i);
            __m256 vb = _mm256_loadu_ps(bPtr + i);
            __m256 vr = _mm256_mul_ps(va, vb);
            _mm256_storeu_ps(resultPtr + i, vr);
        }
    #elif defined(HAVE_SSE)
        // Process 4 floats at a time with SSE
        for (; i + 3 < length; i += 4) {
            __m128 va = _mm_loadu_ps(aPtr + i);
            __m128 vb = _mm_loadu_ps(bPtr + i);
            __m128 vr = _mm_mul_ps(va, vb);
            _mm_storeu_ps(resultPtr + i, vr);
        }
    #elif defined(HAVE_NEON)
        // Process 4 floats at a time with NEON
        for (; i + 3 < length; i += 4) {
            float32x4_t va = vld1q_f32(aPtr + i);
            float32x4_t vb = vld1q_f32(bPtr + i);
            float32x4_t vr = vmulq_f32(va, vb);
            vst1q_f32(resultPtr + i, vr);
        }
    #endif
    
    // Process remaining elements
    for (; i < length; i++) {
        resultPtr[i] = aPtr[i] * bPtr[i];
    }
}

/**
 * Cache-optimized matrix multiplication.
 * Uses blocking/tiling for cache efficiency.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeMatrixMultiply(
        JNIEnv* env, jclass clazz, 
        jlong a, jlong b, jlong result, 
        jint rows, jint cols) {
    if (rows <= 0 || cols <= 0 || a == 0 || b == 0 || result == 0) {
        return;
    }
    const float* aPtr = reinterpret_cast<const float*>(a);
    const float* bPtr = reinterpret_cast<const float*>(b);
    float* resultPtr = reinterpret_cast<float*>(result);

    size_t rowsSize = static_cast<size_t>(rows);
    size_t colsSize = static_cast<size_t>(cols);
    if (rowsSize > std::numeric_limits<size_t>::max() / colsSize) {
        return;
    }
    size_t elementCount = rowsSize * colsSize;
    if (elementCount > std::numeric_limits<size_t>::max() / sizeof(float)) {
        return;
    }
    
    // This is a simplified implementation
    // Production code would use highly optimized BLAS library
    const int BLOCK_SIZE = 64;
    
    // Initialize result to zero
    memset(resultPtr, 0, elementCount * sizeof(float));
    
    // Blocked matrix multiplication
    for (int ii = 0; ii < rows; ii += BLOCK_SIZE) {
        for (int jj = 0; jj < cols; jj += BLOCK_SIZE) {
            for (int kk = 0; kk < cols; kk += BLOCK_SIZE) {
                int iMax = (ii + BLOCK_SIZE < rows) ? ii + BLOCK_SIZE : rows;
                int jMax = (jj + BLOCK_SIZE < cols) ? jj + BLOCK_SIZE : cols;
                int kMax = (kk + BLOCK_SIZE < cols) ? kk + BLOCK_SIZE : cols;
                
                for (int i = ii; i < iMax; i++) {
                    for (int k = kk; k < kMax; k++) {
                        size_t aIndex = static_cast<size_t>(i) * colsSize + static_cast<size_t>(k);
                        float aik = aPtr[aIndex];
                        for (int j = jj; j < jMax; j++) {
                            size_t resultIndex =
                                    static_cast<size_t>(i) * colsSize + static_cast<size_t>(j);
                            size_t bIndex =
                                    static_cast<size_t>(k) * colsSize + static_cast<size_t>(j);
                            resultPtr[resultIndex] += aik * bPtr[bIndex];
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get available CPU features.
 * Returns bitmask of supported SIMD instructions.
 */
JNIEXPORT jint JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeGetCpuFeatures(
        JNIEnv* env, jclass clazz) {
    
    jint features = 0;
    
    #ifdef HAVE_SSE
        features |= (1 << 0); // SSE
        features |= (1 << 1); // SSE2
        features |= (1 << 2); // SSE3
        features |= (1 << 3); // SSE4.1
        features |= (1 << 4); // SSE4.2
    #endif
    
    #ifdef HAVE_AVX2
        features |= (1 << 5); // AVX
        features |= (1 << 6); // AVX2
        features |= (1 << 7); // FMA
    #endif
    
    #ifdef HAVE_NEON
        features |= (1 << 8); // NEON
    #endif
    
    return features;
}

/**
 * Prefetch cache line.
 * Hint to CPU to load data into cache before it's needed.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativePrefetch(
        JNIEnv* env, jclass clazz, jlong address, jint hint) {
    if (address == 0) {
        return;
    }
    const void* ptr = reinterpret_cast<const void*>(address);
    
    // Use compiler builtin for prefetch
    // hint 0 = temporal (keep in all cache levels)
    // hint 1 = non-temporal (don't keep in cache)
    __builtin_prefetch(ptr, 0, hint);
}

} // extern "C"
