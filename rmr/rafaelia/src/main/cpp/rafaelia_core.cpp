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
#if defined(RMR_SAFE_LEASES)
#include <pthread.h>
#include "portable/rmr_lease_v1.h"
#endif

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

#if defined(RMR_SAFE_LEASES)
/* Adapter-only lock + allocator; the freestanding lease primitive owns neither.
 * Serializing operations with close prevents concurrent use-after-free. */
static pthread_mutex_t gRmrLeaseLock = PTHREAD_MUTEX_INITIALIZER;
static rmr_lease_registry gRmrLeases = {};
static size_t gRmrLiveBytes = 0u;
constexpr size_t kRmrMaxBlock = 32u * 1024u * 1024u;
constexpr size_t kRmrMaxLive = 64u * 1024u * 1024u;
constexpr unsigned long long kRmrMaxMatrixWork = 16777216ULL;

static void RmrThrow(JNIEnv* env, const char* klassName, const char* message) {
    if (env->ExceptionCheck()) return;
    jclass klass = env->FindClass(klassName);
    if (klass != nullptr) {
        env->ThrowNew(klass, message);
        env->DeleteLocalRef(klass);
    }
}

static bool RmrView(rmr_lease_id id, size_t bytes, void** output) {
    return rmr_lease_view(&gRmrLeases, id, 0u, bytes, output) == RMR_LEASE_OK;
}

static bool RmrFloatView(rmr_lease_id id, size_t bytes, void** output) {
    if (!RmrView(id, bytes, output)) return false;
    return reinterpret_cast<uintptr_t>(*output) % alignof(float) == 0u;
}
#endif


#if defined(HAVE_SSE) || defined(HAVE_NEON)
constexpr size_t kSseAlignment = 16;
#endif

#if defined(HAVE_AVX2)
constexpr size_t kAvxAlignment = 32;
constexpr size_t kSimdWidthAvx = 8;
#endif

#if defined(HAVE_SSE)
constexpr size_t kSimdWidthSse = 4;
#endif

#if defined(HAVE_NEON)
constexpr size_t kSimdWidthNeon = 4;
#endif

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
    (void)env;
    (void)clazz;
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
#if defined(RMR_SAFE_LEASES)

    (void)clazz;
    if (size <= 0 || alignment < static_cast<jint>(sizeof(void*)) ||
            alignment > 4096 || (alignment & (alignment - 1)) != 0 ||
            static_cast<size_t>(size) > kRmrMaxBlock) {
        RmrThrow(env, "java/lang/IllegalArgumentException",
                 "Invalid owned allocation size or alignment");
        return 0;
    }
    pthread_mutex_lock(&gRmrLeaseLock);
    if (static_cast<size_t>(size) > kRmrMaxLive - gRmrLiveBytes) {
        pthread_mutex_unlock(&gRmrLeaseLock);
        RmrThrow(env, "java/lang/OutOfMemoryError", "RMR memory quota reached");
        return 0;
    }
    void* block = nullptr;
    if (posix_memalign(&block, static_cast<size_t>(alignment),
                       static_cast<size_t>(size)) != 0 || block == nullptr) {
        pthread_mutex_unlock(&gRmrLeaseLock);
        RmrThrow(env, "java/lang/OutOfMemoryError", "Aligned allocation failed");
        return 0;
    }
    rmr_lease_id token = 0;
    const int status = rmr_lease_open(
            &gRmrLeases, block, static_cast<size_t>(size), &token);
    if (status != RMR_LEASE_OK) {
        free(block);
        pthread_mutex_unlock(&gRmrLeaseLock);
        RmrThrow(env, "java/lang/IllegalStateException",
                 "Native lease registry exhausted");
        return 0;
    }
    memset(block, 0, static_cast<size_t>(size));
    gRmrLiveBytes += static_cast<size_t>(size);
    pthread_mutex_unlock(&gRmrLeaseLock);
    return static_cast<jlong>(token);
#else
    (void)env;
    (void)clazz;
    
    void* ptr = nullptr;
    
    // Use posix_memalign for aligned allocation
    if (posix_memalign(&ptr, alignment, size) != 0) {
        LOGE("Failed to allocate aligned memory: size=%d, alignment=%d", size, alignment);
        return 0;
    }
    
    // Zero-initialize for security
    memset(ptr, 0, size);
    
    return reinterpret_cast<jlong>(ptr);

#endif
}

/**
 * Free aligned memory.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeFreeAligned(
        JNIEnv* env, jclass clazz, jlong address) {
#if defined(RMR_SAFE_LEASES)

    (void)clazz;
    if (address == 0) return;
    void* base = nullptr;
    rmr_lease_size capacity = 0;
    pthread_mutex_lock(&gRmrLeaseLock);
    const int status = rmr_lease_close(
            &gRmrLeases, static_cast<rmr_lease_id>(address),
            &base, &capacity);
    if (status == RMR_LEASE_OK) {
        /* Closing and freeing under the same lock blocks other lease readers. */
        memset(base, 0, capacity);
        free(base);
        gRmrLiveBytes -= capacity;
    }
    pthread_mutex_unlock(&gRmrLeaseLock);
    if (status != RMR_LEASE_OK) {
        RmrThrow(env, "java/lang/IllegalArgumentException",
                 "Stale, forged or double-freed native lease");
    }
#else
    (void)env;
    (void)clazz;
    
    if (address == 0) return;
    
    void* ptr = reinterpret_cast<void*>(address);
    free(ptr);

#endif
}

/**
 * Optimized memory copy using SIMD when available.
 * Falls back to memcpy for unaligned or small sizes.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeMemoryCopy(
        JNIEnv* env, jclass clazz, jlong src, jlong dst, jint size) {
#if defined(RMR_SAFE_LEASES)

    (void)clazz;
    if (size == 0) return;
    bool valid = size > 0;
    void* source = nullptr;
    void* target = nullptr;
    if (valid) {
        pthread_mutex_lock(&gRmrLeaseLock);
        valid = RmrView(static_cast<rmr_lease_id>(src),
                        static_cast<size_t>(size), &source) &&
                RmrView(static_cast<rmr_lease_id>(dst),
                        static_cast<size_t>(size), &target);
        if (valid) memmove(target, source, static_cast<size_t>(size));
        pthread_mutex_unlock(&gRmrLeaseLock);
    }
    if (!valid) {
        RmrThrow(env, "java/lang/IllegalArgumentException",
                 "Invalid native lease copy extent or lifetime");
    }
#else
    (void)env;
    (void)clazz;
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

#endif
}

/**
 * Returns the native address of a direct ByteBuffer, or 0 if unavailable.
 */
JNIEXPORT jlong JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeGetDirectBufferAddress(
        JNIEnv* env, jclass clazz, jobject buffer) {
    (void)clazz;
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
#if defined(RMR_SAFE_LEASES)

    (void)clazz;
    if (size == 0) return;
    bool valid = size > 0;
    void* destination = nullptr;
    if (valid) {
        pthread_mutex_lock(&gRmrLeaseLock);
        valid = RmrView(static_cast<rmr_lease_id>(address),
                        static_cast<size_t>(size), &destination);
        if (valid) memset(destination, static_cast<unsigned char>(value),
                          static_cast<size_t>(size));
        pthread_mutex_unlock(&gRmrLeaseLock);
    }
    if (!valid) {
        RmrThrow(env, "java/lang/IllegalArgumentException",
                 "Invalid native lease set extent or lifetime");
    }
#else
    (void)env;
    (void)clazz;
    if (size <= 0 || address == 0) {
        return;
    }
    void* ptr = reinterpret_cast<void*>(address);
    memset(ptr, value, size);

#endif
}

/**
 * SIMD-accelerated vector addition.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeVectorAdd(
        JNIEnv* env, jclass clazz, jlong a, jlong b, jlong result, jint length) {
#if defined(RMR_SAFE_LEASES)

    (void)clazz;
    if (length == 0) return;
    bool valid = length > 0 &&
            static_cast<size_t>(length) <= kRmrMaxBlock / sizeof(float);
    void *av = nullptr, *bv = nullptr, *rv = nullptr;
    if (valid) {
        const size_t bytes = static_cast<size_t>(length) * sizeof(float);
        pthread_mutex_lock(&gRmrLeaseLock);
        valid = RmrFloatView(static_cast<rmr_lease_id>(a), bytes, &av) &&
                RmrFloatView(static_cast<rmr_lease_id>(b), bytes, &bv) &&
                RmrFloatView(static_cast<rmr_lease_id>(result), bytes, &rv);
        if (valid) {
            const float* ap = static_cast<const float*>(av);
            const float* bp = static_cast<const float*>(bv);
            float* rp = static_cast<float*>(rv);
            for (int i = 0; i < length; ++i) rp[i] = ap[i] + bp[i];
        }
        pthread_mutex_unlock(&gRmrLeaseLock);
    }
    if (!valid) RmrThrow(env, "java/lang/IllegalArgumentException",
                         "Invalid native vector lease add");
#else
    (void)env;
    (void)clazz;
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

#endif
}

/**
 * SIMD-accelerated vector multiplication.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeVectorMultiply(
        JNIEnv* env, jclass clazz, jlong a, jlong b, jlong result, jint length) {
#if defined(RMR_SAFE_LEASES)

    (void)clazz;
    if (length == 0) return;
    bool valid = length > 0 &&
            static_cast<size_t>(length) <= kRmrMaxBlock / sizeof(float);
    void *av = nullptr, *bv = nullptr, *rv = nullptr;
    if (valid) {
        const size_t bytes = static_cast<size_t>(length) * sizeof(float);
        pthread_mutex_lock(&gRmrLeaseLock);
        valid = RmrFloatView(static_cast<rmr_lease_id>(a), bytes, &av) &&
                RmrFloatView(static_cast<rmr_lease_id>(b), bytes, &bv) &&
                RmrFloatView(static_cast<rmr_lease_id>(result), bytes, &rv);
        if (valid) {
            const float* ap = static_cast<const float*>(av);
            const float* bp = static_cast<const float*>(bv);
            float* rp = static_cast<float*>(rv);
            for (int i = 0; i < length; ++i) rp[i] = ap[i] * bp[i];
        }
        pthread_mutex_unlock(&gRmrLeaseLock);
    }
    if (!valid) RmrThrow(env, "java/lang/IllegalArgumentException",
                         "Invalid native vector lease multiply");
#else
    (void)env;
    (void)clazz;
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

#endif
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
#if defined(RMR_SAFE_LEASES)

    (void)clazz;
    if (rows <= 0 || inner <= 0 || cols <= 0 ||
            result == a || result == b) {
        RmrThrow(env, "java/lang/IllegalArgumentException",
                 "Invalid matrix shape or aliased result lease");
        return;
    }
    const unsigned long long m = static_cast<unsigned int>(rows);
    const unsigned long long k = static_cast<unsigned int>(inner);
    const unsigned long long n = static_cast<unsigned int>(cols);
    /* Checked workload preflight bounds worst-case CPU occupation. */
    if (m > kRmrMaxMatrixWork / k ||
            n > kRmrMaxMatrixWork / (m * k)) {
        RmrThrow(env, "java/lang/IllegalArgumentException",
                 "Native matrix operation budget exceeded");
        return;
    }
    const unsigned long long aCount = m * k;
    const unsigned long long bCount = k * n;
    const unsigned long long cCount = m * n;
    if (aCount > kRmrMaxBlock / sizeof(float) ||
            bCount > kRmrMaxBlock / sizeof(float) ||
            cCount > kRmrMaxBlock / sizeof(float)) {
        RmrThrow(env, "java/lang/IllegalArgumentException",
                 "Native matrix capacity too large");
        return;
    }
    void *av = nullptr, *bv = nullptr, *rv = nullptr;
    pthread_mutex_lock(&gRmrLeaseLock);
    const bool valid =
            RmrFloatView(static_cast<rmr_lease_id>(a),
                         static_cast<size_t>(aCount) * sizeof(float), &av) &&
            RmrFloatView(static_cast<rmr_lease_id>(b),
                         static_cast<size_t>(bCount) * sizeof(float), &bv) &&
            RmrFloatView(static_cast<rmr_lease_id>(result),
                         static_cast<size_t>(cCount) * sizeof(float), &rv);
    if (valid) {
        MatrixMultiplyScalarBlocked(static_cast<const float*>(av),
                                    static_cast<const float*>(bv),
                                    static_cast<float*>(rv),
                                    rows, inner, cols);
    }
    pthread_mutex_unlock(&gRmrLeaseLock);
    if (!valid) RmrThrow(env, "java/lang/IllegalArgumentException",
                         "Invalid, stale or undersized matrix lease");
#else
    (void)env;
    (void)clazz;
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

#endif
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
#if defined(RMR_SAFE_LEASES)

    (void)clazz;
    (void)hint;
    void* target = nullptr;
    pthread_mutex_lock(&gRmrLeaseLock);
    const bool valid = RmrView(static_cast<rmr_lease_id>(address), 1u, &target);
    if (valid) __builtin_prefetch(target, 0, 3);
    pthread_mutex_unlock(&gRmrLeaseLock);
    /* Unknown handles are ignored: prefetch is only an advisory hint. */
#else
    (void)env;
    (void)clazz;
    if (address == 0) {
        return;
    }
    const void* ptr = reinterpret_cast<const void*>(address);
    
    // __builtin_prefetch requires its cache-locality argument to be a
    // compile-time constant. Preserve the Java contract: 0 is temporal and
    // 1 is non-temporal; unknown values use the safe temporal default.
    if (hint == 1) {
        __builtin_prefetch(ptr, 0, 0);
    } else {
        __builtin_prefetch(ptr, 0, 3);
    }

#endif
}


/*
 * Bounded copy boundary: Java passes direct ByteBuffer objects, not addresses.
 * The JNI references preserve reachability during the synchronous call.
 * Offsets are bytes; JNI capacity is rechecked against underlying direct views.
 */
JNIEXPORT void JNICALL
Java_androidx_rmr_rafaelia_RafaeliaCore_nativeCopyDirectBuffers(
        JNIEnv* env, jclass clazz,
        jobject source, jint sourceOffset,
        jobject destination, jint destinationOffset, jint length) {
    (void)clazz;
    bool valid = source != nullptr && destination != nullptr &&
            sourceOffset >= 0 && destinationOffset >= 0 && length >= 0;
    if (valid) {
        const jlong aCapacity = env->GetDirectBufferCapacity(source);
        const jlong bCapacity = env->GetDirectBufferCapacity(destination);
        valid = aCapacity >= 0 && bCapacity >= 0 &&
                static_cast<jlong>(sourceOffset) <= aCapacity &&
                static_cast<jlong>(destinationOffset) <= bCapacity &&
                static_cast<jlong>(length) <= aCapacity - sourceOffset &&
                static_cast<jlong>(length) <= bCapacity - destinationOffset;
    }
    if (valid && length > 0) {
        jclass bufferClass = env->GetObjectClass(destination);
        if (bufferClass == nullptr) return; // pending VM exception
        jmethodID readOnly = env->GetMethodID(bufferClass, "isReadOnly", "()Z");
        env->DeleteLocalRef(bufferClass);
        if (readOnly == nullptr) return; // pending VM exception
        valid = env->CallBooleanMethod(destination, readOnly) == JNI_FALSE;
        if (env->ExceptionCheck()) return;
    }
    void* a = nullptr;
    void* b = nullptr;
    if (valid && length > 0) {
        a = env->GetDirectBufferAddress(source);
        b = env->GetDirectBufferAddress(destination);
        valid = a != nullptr && b != nullptr;
    }
    if (!valid) {
        jclass klass = env->FindClass("java/lang/IllegalArgumentException");
        if (klass != nullptr) {
            env->ThrowNew(klass, "Invalid direct buffer capacity, mutability or offset");
            env->DeleteLocalRef(klass);
        }
        return;
    }
    if (length == 0) return;
    memmove(static_cast<unsigned char*>(b) + destinationOffset,
            static_cast<const unsigned char*>(a) + sourceOffset,
            static_cast<size_t>(length));
}

} // extern "C"
