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

public final class RmRHardware {
    private static volatile boolean sNativeAvailable;
    private static volatile boolean sTriedNativeLoad;
    private static volatile SimdLevel sCachedSimdLevel;

    public enum SimdLevel {
        NONE,
        NEON,
        SSE,
        AVX
    }

    static {
        sNativeAvailable = false;
        sTriedNativeLoad = false;
        try {
            System.loadLibrary("rmr-core-native");
            sNativeAvailable = true;
            sTriedNativeLoad = true;
        } catch (UnsatisfiedLinkError e) {
            sNativeAvailable = false;
            sTriedNativeLoad = true;
        }
    }

    private RmRHardware() {
    }

    @NonNull
    public static SimdLevel getSimdLevel() {
        SimdLevel cached = sCachedSimdLevel;
        if (cached != null) {
            return cached;
        }
        String[] abis = Build.SUPPORTED_ABIS;
        if (abis != null) {
            for (String abi : abis) {
                if (abi == null) {
                    continue;
                }
                if (abi.contains("arm64") || abi.contains("armeabi")) {
                    return cacheSimdLevel(SimdLevel.NEON);
                }
                if (abi.contains("x86_64")) {
                    return cacheSimdLevel(SimdLevel.AVX);
                }
                if (abi.contains("x86")) {
                    return cacheSimdLevel(SimdLevel.SSE);
                }
            }
        }
        return cacheSimdLevel(SimdLevel.NONE);
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

    @NonNull
    private static SimdLevel cacheSimdLevel(@NonNull SimdLevel level) {
        sCachedSimdLevel = level;
        return level;
    }
}
