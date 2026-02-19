/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.rmr.core;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class RmRHardware {
    private static final int SIMD_NATIVE_NONE = 0;
    private static final int SIMD_NATIVE_NEON = 1;
    private static final int SIMD_NATIVE_SSE = 2;
    private static final int SIMD_NATIVE_AVX = 3;

    private static volatile boolean sNativeAvailable;
    private static volatile boolean sTriedNativeLoad;
    private static volatile SimdLevel sCachedSimdLevel;

    public enum SimdLevel {
        NONE,
        NEON,
        SSE,
        AVX
    }

    private RmRHardware() {
    }

    @NonNull
    public static SimdLevel getSimdLevel() {
        SimdLevel cached = sCachedSimdLevel;
        if (cached != null) {
            return cached;
        }

        SimdLevel nativeLevel = detectSimdLevelFromNative();
        if (nativeLevel != null) {
            return cacheSimdLevel(nativeLevel);
        }

        return cacheSimdLevel(getSimdLevelFromAbi(Build.SUPPORTED_ABIS));
    }

    @NonNull
    static SimdLevel getSimdLevelFromAbi(@Nullable String[] abis) {
        if (abis == null) {
            return SimdLevel.NONE;
        }

        for (String abi : abis) {
            if (abi == null) {
                continue;
            }

            if (abi.contains("arm64") || abi.contains("armeabi")) {
                return SimdLevel.NEON;
            }
            if (abi.contains("x86_64")) {
                return SimdLevel.SSE;
            }
            if (abi.contains("x86")) {
                return SimdLevel.SSE;
            }
        }
        return SimdLevel.NONE;
    }

    @Nullable
    private static SimdLevel detectSimdLevelFromNative() {
        if (!ensureNativeLoaded()) {
            return null;
        }

        try {
            return simdLevelFromNativeValue(nativeGetSimdLevel());
        } catch (UnsatisfiedLinkError e) {
            sNativeAvailable = false;
            return null;
        }
    }

    @Nullable
    static SimdLevel simdLevelFromNativeValue(int nativeValue) {
        if (nativeValue == SIMD_NATIVE_AVX) {
            return SimdLevel.AVX;
        }
        if (nativeValue == SIMD_NATIVE_SSE) {
            return SimdLevel.SSE;
        }
        if (nativeValue == SIMD_NATIVE_NEON) {
            return SimdLevel.NEON;
        }
        if (nativeValue == SIMD_NATIVE_NONE) {
            return SimdLevel.NONE;
        }
        return null;
    }

    static boolean isNativeAvailable() {
        return sNativeAvailable;
    }

    static boolean ensureNativeLoaded() {
        if (sTriedNativeLoad) {
            return sNativeAvailable;
        }
        synchronized (RmRHardware.class) {
            if (sTriedNativeLoad) {
                return sNativeAvailable;
            }
            sTriedNativeLoad = true;
            try {
                System.loadLibrary("rmr-core-native");
                sNativeAvailable = true;
            } catch (UnsatisfiedLinkError e) {
                sNativeAvailable = false;
            }
            return sNativeAvailable;
        }
    }

    static void setNativeAvailable(boolean available) {
        sNativeAvailable = available;
    }

    private static native int nativeGetSimdLevel();

    @NonNull
    private static SimdLevel cacheSimdLevel(@NonNull SimdLevel level) {
        sCachedSimdLevel = level;
        return level;
    }
}
