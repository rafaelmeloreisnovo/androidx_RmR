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
#include <cstddef>

#if defined(__linux__)
    #include <sys/auxv.h>
    #if defined(__aarch64__) || defined(__arm__)
        #include <asm/hwcap.h>
    #endif
#endif

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

extern "C" jint RafaeliaDetectCpuFeatures();

extern "C" {

namespace {

constexpr int kMatrixBlockSize = 64;
constexpr size_t kSseAlignment = 16;
constexpr size_t kAvxAlignment = 32;
constexpr size_t kSimdWidthSse = 4;
constexpr size_t kSimdWidthAvx = 8;
constexpr size_t kSimdWidthNeon = 4;

inline bool IsAligned(const void* ptr, size_t alignment) {
    return (reinterpret_cast<uintptr_t>(ptr) % alignment) == 0;
}

inline bool IsSimdMatrixCompatible(const float* bPtr,
                                   const float* resultPtr,
                                   size_t cols,
                                   size_t alignment,
                                   size_t vectorWidth) {
    if (cols == 0 || cols % vectorWidth != 0) {
        return false;
    }
    if (!IsAligned(bPtr, alignment) || !IsAligned(resultPtr, alignment)) {
        return false;
    }
    size_t rowStrideBytes = cols * sizeof(float);
    if (rowStrideBytes % alignment != 0) {
        return false;
    }
    return true;
}

void MatrixMultiplyScalarBlocked(const float* aPtr,
                                 const float* bPtr,
                                 float* resultPtr,
                                 int rows,
                                 int inner,
                                 int cols) {
    size_t rowsSize = static_cast<size_t>(rows);
    size_t innerSize = static_cast<size_t>(inner);
    size_t colsSize = static_cast<size_t>(cols);
    size_t elementCount = rowsSize * colsSize;
    memset(resultPtr, 0, elementCount * sizeof(float));

    for (int ii = 0; ii < rows; ii += kMatrixBlockSize) {
        for (int jj = 0; jj < cols; jj += kMatrixBlockSize) {
            for (int kk = 0; kk < inner; kk += kMatrixBlockSize) {
                int iMax = (ii + kMatrixBlockSize < rows) ? ii + kMatrixBlockSize : rows;
                int jMax = (jj + kMatrixBlockSize < cols) ? jj + kMatrixBlockSize : cols;
                int kMax = (kk + kMatrixBlockSize < inner) ? kk + kMatrixBlockSize : inner;

                for (int i = ii; i < iMax; i++) {
                    for (int k = kk; k < kMax; k++) {
                        size_t aIndex =
                                static_cast<size_t>(i) * innerSize + static_cast<size_t>(k);
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

#if defined(HAVE_AVX2)
bool CpuHasAvx2Runtime() {
#if (defined(__x86_64__) || defined(__i386__))
    return __builtin_cpu_supports("avx2");
#else
    return false;
#endif
}

void MatrixMultiplyAvx2(const float* aPtr,
                        const float* bPtr,
                        float* resultPtr,
                        int rows,
                        int inner,
                        int cols) {
    size_t rowsSize = static_cast<size_t>(rows);
    size_t innerSize = static_cast<size_t>(inner);
    size_t colsSize = static_cast<size_t>(cols);
    size_t elementCount = rowsSize * colsSize;
    memset(resultPtr, 0, elementCount * sizeof(float));

    for (int ii = 0; ii < rows; ii += kMatrixBlockSize) {
        for (int jj = 0; jj < cols; jj += kMatrixBlockSize) {
            for (int kk = 0; kk < inner; kk += kMatrixBlockSize) {
                int iMax = (ii + kMatrixBlockSize < rows) ? ii + kMatrixBlockSize : rows;
                int jMax = (jj + kMatrixBlockSize < cols) ? jj + kMatrixBlockSize : cols;
                int kMax = (kk + kMatrixBlockSize < inner) ? kk + kMatrixBlockSize : inner;

                for (int i = ii; i < iMax; i++) {
                    for (int k = kk; k < kMax; k++) {
                        size_t aIndex =
                                static_cast<size_t>(i) * innerSize + static_cast<size_t>(k);
                        __m256 aik = _mm256_set1_ps(aPtr[aIndex]);
                        size_t baseB = static_cast<size_t>(k) * colsSize;
                        size_t baseC = static_cast<size_t>(i) * colsSize;
                        for (int j = jj; j < jMax; j += kSimdWidthAvx) {
                            size_t bIndex = baseB + static_cast<size_t>(j);
                            size_t cIndex = baseC + static_cast<size_t>(j);
                            __m256 vb = _mm256_load_ps(bPtr + bIndex);
                            __m256 vc = _mm256_load_ps(resultPtr + cIndex);
                            __m256 prod = _mm256_mul_ps(vb, aik);
                            vc = _mm256_add_ps(vc, prod);
                            _mm256_store_ps(resultPtr + cIndex, vc);
                        }
                    }
                }
            }
        }
    }
}
#endif

#if defined(HAVE_SSE)
bool CpuHasSseRuntime() {
#if (defined(__x86_64__) || defined(__i386__))
    return __builtin_cpu_supports("sse");
#else
    return false;
#endif
}

void MatrixMultiplySse(const float* aPtr,
                       const float* bPtr,
                       float* resultPtr,
                       int rows,
                       int inner,
                       int cols) {
    size_t rowsSize = static_cast<size_t>(rows);
    size_t innerSize = static_cast<size_t>(inner);
    size_t colsSize = static_cast<size_t>(cols);
    size_t elementCount = rowsSize * colsSize;
    memset(resultPtr, 0, elementCount * sizeof(float));

    for (int ii = 0; ii < rows; ii += kMatrixBlockSize) {
        for (int jj = 0; jj < cols; jj += kMatrixBlockSize) {
            for (int kk = 0; kk < inner; kk += kMatrixBlockSize) {
                int iMax = (ii + kMatrixBlockSize < rows) ? ii + kMatrixBlockSize : rows;
                int jMax = (jj + kMatrixBlockSize < cols) ? jj + kMatrixBlockSize : cols;
                int kMax = (kk + kMatrixBlockSize < inner) ? kk + kMatrixBlockSize : inner;

                for (int i = ii; i < iMax; i++) {
                    for (int k = kk; k < kMax; k++) {
                        size_t aIndex =
                                static_cast<size_t>(i) * innerSize + static_cast<size_t>(k);
                        __m128 aik = _mm_set1_ps(aPtr[aIndex]);
                        size_t baseB = static_cast<size_t>(k) * colsSize;
                        size_t baseC = static_cast<size_t>(i) * colsSize;
                        for (int j = jj; j < jMax; j += kSimdWidthSse) {
                            size_t bIndex = baseB + static_cast<size_t>(j);
                            size_t cIndex = baseC + static_cast<size_t>(j);
                            __m128 vb = _mm_load_ps(bPtr + bIndex);
                            __m128 vc = _mm_load_ps(resultPtr + cIndex);
                            vc = _mm_add_ps(vc, _mm_mul_ps(vb, aik));
                            _mm_store_ps(resultPtr + cIndex, vc);
                        }
                    }
                }
            }
        }
    }
}
#endif

#if defined(HAVE_NEON)
bool CpuHasNeonRuntime() {
#if defined(__linux__) && (defined(__aarch64__) || defined(__arm__))
    unsigned long caps = getauxval(AT_HWCAP);
#if defined(__aarch64__)
    return (caps & HWCAP_ASIMD) != 0;
#else
    return (caps & HWCAP_NEON) != 0;
#endif
#else
    return false;
#endif
}

void MatrixMultiplyNeon(const float* aPtr,
                        const float* bPtr,
                        float* resultPtr,
                        int rows,
                        int inner,
                        int cols) {
    size_t rowsSize = static_cast<size_t>(rows);
    size_t innerSize = static_cast<size_t>(inner);
    size_t colsSize = static_cast<size_t>(cols);
    size_t elementCount = rowsSize * colsSize;
    memset(resultPtr, 0, elementCount * sizeof(float));

    for (int ii = 0; ii < rows; ii += kMatrixBlockSize) {
        for (int jj = 0; jj < cols; jj += kMatrixBlockSize) {
            for (int kk = 0; kk < inner; kk += kMatrixBlockSize) {
                int iMax = (ii + kMatrixBlockSize < rows) ? ii + kMatrixBlockSize : rows;
                int jMax = (jj + kMatrixBlockSize < cols) ? jj + kMatrixBlockSize : cols;
                int kMax = (kk + kMatrixBlockSize < inner) ? kk + kMatrixBlockSize : inner;

                for (int i = ii; i < iMax; i++) {
                    for (int k = kk; k < kMax; k++) {
                        size_t aIndex =
                                static_cast<size_t>(i) * innerSize + static_cast<size_t>(k);
                        float32x4_t aik = vdupq_n_f32(aPtr[aIndex]);
                        size_t baseB = static_cast<size_t>(k) * colsSize;
                        size_t baseC = static_cast<size_t>(i) * colsSize;
                        for (int j = jj; j < jMax; j += kSimdWidthNeon) {
                            size_t bIndex = baseB + static_cast<size_t>(j);
                            size_t cIndex = baseC + static_cast<size_t>(j);
                            float32x4_t vb = vld1q_f32(bPtr + bIndex);
                            float32x4_t vc = vld1q_f32(resultPtr + cIndex);
                            vc = vmlaq_f32(vc, vb, aik);
                            vst1q_f32(resultPtr + cIndex, vc);
                        }
                    }
                }
            }
        }
    }
}
#endif

} // namespace

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
    const size_t sizeBytes = static_cast<size_t>(size);
    constexpr size_t kMaxCopyBytes = 256u * 1024u * 1024u;
    if (sizeBytes > std::numeric_limits<size_t>::max() ||
        sizeBytes > kMaxCopyBytes) {
        return;
    }
    const void* srcPtr = reinterpret_cast<const void*>(src);
    void* dstPtr = reinterpret_cast<void*>(dst);
    
    // Use standard memcpy - it's already highly optimized
    // on modern platforms with SIMD instructions
    memcpy(dstPtr, srcPtr, sizeBytes);
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
        jint rows, jint inner, jint cols) {
    if (rows <= 0 || inner <= 0 || cols <= 0 || a == 0 || b == 0 || result == 0) {
        return;
    }
    const float* aPtr = reinterpret_cast<const float*>(a);
    const float* bPtr = reinterpret_cast<const float*>(b);
    float* resultPtr = reinterpret_cast<float*>(result);

    size_t rowsSize = static_cast<size_t>(rows);
    size_t innerSize = static_cast<size_t>(inner);
    size_t colsSize = static_cast<size_t>(cols);
    if (rowsSize > std::numeric_limits<size_t>::max() / colsSize) {
        return;
    }
    if (rowsSize > std::numeric_limits<size_t>::max() / innerSize) {
        return;
    }
    if (innerSize > std::numeric_limits<size_t>::max() / colsSize) {
        return;
    }
    size_t elementCount = rowsSize * colsSize;
    if (elementCount > std::numeric_limits<size_t>::max() / sizeof(float)) {
        return;
    }
    size_t aElementCount = rowsSize * innerSize;
    if (aElementCount > std::numeric_limits<size_t>::max() / sizeof(float)) {
        return;
    }
    size_t bElementCount = innerSize * colsSize;
    if (bElementCount > std::numeric_limits<size_t>::max() / sizeof(float)) {
        return;
    }
    
    // SIMD paths assume:
    // 1) Row-major contiguous buffers (stride == cols for B/result).
    // 2) Aligned base pointers for B/result (16 bytes for SSE/NEON, 32 bytes for AVX2).
    // 3) cols is a multiple of the SIMD vector width (4 for SSE/NEON, 8 for AVX2).
    // If any requirement is not met, we fall back to scalar blocked implementation.
    bool usedSimd = false;

#if defined(HAVE_AVX2)
    if (!usedSimd &&
        CpuHasAvx2Runtime() &&
        IsSimdMatrixCompatible(bPtr, resultPtr, colsSize, kAvxAlignment, kSimdWidthAvx)) {
        MatrixMultiplyAvx2(aPtr, bPtr, resultPtr, rows, inner, cols);
        usedSimd = true;
    }
#endif

#if defined(HAVE_SSE)
    if (!usedSimd &&
        CpuHasSseRuntime() &&
        IsSimdMatrixCompatible(bPtr, resultPtr, colsSize, kSseAlignment, kSimdWidthSse)) {
        MatrixMultiplySse(aPtr, bPtr, resultPtr, rows, inner, cols);
        usedSimd = true;
    }
#endif

#if defined(HAVE_NEON)
    if (!usedSimd &&
        CpuHasNeonRuntime() &&
        IsSimdMatrixCompatible(bPtr, resultPtr, colsSize, kSseAlignment, kSimdWidthNeon)) {
        MatrixMultiplyNeon(aPtr, bPtr, resultPtr, rows, inner, cols);
        usedSimd = true;
    }
#endif

    if (!usedSimd) {
        // This is a simplified implementation
        // Production code would use highly optimized BLAS library
        MatrixMultiplyScalarBlocked(aPtr, bPtr, resultPtr, rows, inner, cols);
    }
}

/**
 * Get available CPU features.
 * Returns bitmask of supported SIMD instructions.
 */
extern jint RafaeliaDetectCpuFeaturesRuntime();

JNIEXPORT jint JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeDetectCpuFeatures(
        JNIEnv* env, jclass clazz) {
    (void)env;
    (void)clazz;
    return RafaeliaDetectCpuFeatures();
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
