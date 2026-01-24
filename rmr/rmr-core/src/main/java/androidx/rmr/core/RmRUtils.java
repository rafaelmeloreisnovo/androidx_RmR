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

import androidx.annotation.NonNull;

public final class RmRUtils {
    private RmRUtils() {
    }

    public static long computeBitrafSeed(@NonNull double[] inputVector) {
        if (inputVector.length == 0) {
            return 0L;
        }
        long seed = 0L;
        for (int i = 0; i < inputVector.length; i++) {
            seed ^= Double.doubleToLongBits(inputVector[i]);
        }
        return seed;
    }

    public static double distanceEuclidean(@NonNull double[] a, @NonNull double[] b) {
        validateSameLength(a, b);
        int length = a.length;
        if (length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    public static double distanceManhattan(@NonNull double[] a, @NonNull double[] b) {
        validateSameLength(a, b);
        int length = a.length;
        if (length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < length; i++) {
            double diff = a[i] - b[i];
            sum += diff < 0.0 ? -diff : diff;
        }
        return sum;
    }

    public static double cosineSimilarity(@NonNull double[] a, @NonNull double[] b) {
        validateSameLength(a, b);
        int length = a.length;
        if (length == 0) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < length; i++) {
            double valueA = a[i];
            double valueB = b[i];
            dot += valueA * valueB;
            normA += valueA * valueA;
            normB += valueB * valueB;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static int hashToIndex(@NonNull String key, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        int hash = 0;
        for (int i = 0; i < key.length(); i++) {
            hash = 31 * hash + key.charAt(i);
        }
        return (hash & 0x7fffffff) % capacity;
    }

    private static void validateSameLength(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same length.");
        }
    }
}
