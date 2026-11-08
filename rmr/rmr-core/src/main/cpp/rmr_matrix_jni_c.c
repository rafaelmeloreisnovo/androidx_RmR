/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * Minimal C/JNI adapter for the opt-in freestanding matrix kernel.
 * JNI and Android are NOT freestanding; only portable/rmr_matrix_f64.c is.
 */
#include <jni.h>
#include "rmr_matrix_contract.h"
#include "portable/rmr_matrix_f64.h"

static void rmr_throw_argument(JNIEnv *env, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass klass = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (klass != NULL) {
        (*env)->ThrowNew(env, klass, message);
        (*env)->DeleteLocalRef(env, klass);
    }
}

JNIEXPORT void JNICALL
Java_androidx_rmr_core_RmRMatrixOps_multiplyNative(
        JNIEnv *env, jclass clazz,
        jdoubleArray a_data, jint a_rows, jint a_cols,
        jdoubleArray b_data, jint b_rows, jint b_cols,
        jdoubleArray result_data) {
    (void)clazz;
    if (a_data == NULL || b_data == NULL || result_data == NULL ||
            (*env)->IsSameObject(env, result_data, a_data) ||
            (*env)->IsSameObject(env, result_data, b_data)) {
        rmr_throw_argument(env, "Null or aliased matrix array");
        return;
    }
    const jsize a_length = (*env)->GetArrayLength(env, a_data);
    const jsize b_length = (*env)->GetArrayLength(env, b_data);
    const jsize out_length = (*env)->GetArrayLength(env, result_data);
    if (!rmr_matrix_multiply_shape_valid(a_rows, a_cols, b_rows, b_cols,
            a_length, b_length, out_length)) {
        rmr_throw_argument(env, "Invalid matrix dimensions or array lengths");
        return;
    }
    jdouble *a = (*env)->GetDoubleArrayElements(env, a_data, NULL);
    jdouble *b = NULL;
    jdouble *out = NULL;
    if (a != NULL) b = (*env)->GetDoubleArrayElements(env, b_data, NULL);
    if (b != NULL) out = (*env)->GetDoubleArrayElements(env, result_data, NULL);

    if (a == NULL || b == NULL || out == NULL) {
        if (a != NULL) (*env)->ReleaseDoubleArrayElements(env, a_data, a, JNI_ABORT);
        if (b != NULL) (*env)->ReleaseDoubleArrayElements(env, b_data, b, JNI_ABORT);
        if (out != NULL) (*env)->ReleaseDoubleArrayElements(env, result_data, out, JNI_ABORT);
        if (!(*env)->ExceptionCheck(env)) {
            jclass klass = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
            if (klass != NULL) {
                (*env)->ThrowNew(env, klass, "Cannot acquire JNI matrix arrays");
                (*env)->DeleteLocalRef(env, klass);
            }
        }
        return;
    }

    const int status = rmr_matrix_f64_multiply(
            a, a_length, a_rows, a_cols, b, b_length, b_rows, b_cols,
            out, out_length);
    /* A and B are read-only; output commits only on a successful kernel. */
    (*env)->ReleaseDoubleArrayElements(env, a_data, a, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, b_data, b, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, result_data, out,
            status == RMR_MATRIX_F64_OK ? 0 : JNI_ABORT);
    if (status != RMR_MATRIX_F64_OK) {
        rmr_throw_argument(env, "Invalid or overlapping native matrix buffers");
    }
}

JNIEXPORT jint JNICALL
Java_androidx_rmr_core_RmRHardware_nativeGetSimdLevel(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;
    /* This reports CPU capability, not the selected C11 scalar backend. */
#if defined(__ARM_NEON) || defined(__ARM_NEON__)
    return 1;
#elif defined(__x86_64__) || defined(__i386__)
#if defined(__GNUC__)
    if (__builtin_cpu_supports("avx")) return 3;
#endif
#if defined(__SSE2__)
    return 2;
#else
    return 0;
#endif
#else
    return 0;
#endif
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    JNIEnv *env = NULL;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass hardware_class = (*env)->FindClass(env, "androidx/rmr/core/RmRHardware");
    if (hardware_class != NULL) {
        jmethodID set_available = (*env)->GetStaticMethodID(
                env, hardware_class, "setNativeAvailable", "(Z)V");
        if (set_available != NULL) {
            (*env)->CallStaticVoidMethod(env, hardware_class, set_available, JNI_TRUE);
        }
        (*env)->DeleteLocalRef(env, hardware_class);
    }
    return JNI_VERSION_1_6;
}
