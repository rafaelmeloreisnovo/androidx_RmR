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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

final class DoubleArrayPool {
    private static final int MAX_PER_BUCKET = 8;

    private static final ThreadLocal<Map<Integer, ArrayDeque<double[]>>> sBuckets =
            ThreadLocal.withInitial(HashMap::new);

    private DoubleArrayPool() {
    }

    static double[] acquire(int length) {
        if (length == 0) {
            return new double[0];
        }
        Map<Integer, ArrayDeque<double[]>> buckets = sBuckets.get();
        ArrayDeque<double[]> bucket = buckets.get(length);
        if (bucket == null || bucket.isEmpty()) {
            return new double[length];
        }
        double[] buffer = bucket.pop();
        if (bucket.isEmpty()) {
            buckets.remove(length);
        }
        return buffer;
    }

    static void release(double[] buffer) {
        if (buffer == null || buffer.length == 0) {
            return;
        }
        Map<Integer, ArrayDeque<double[]>> buckets = sBuckets.get();
        ArrayDeque<double[]> bucket = buckets.get(buffer.length);
        if (bucket == null) {
            bucket = new ArrayDeque<>(MAX_PER_BUCKET);
            buckets.put(buffer.length, bucket);
        }
        if (bucket.size() < MAX_PER_BUCKET) {
            bucket.push(buffer);
        }
    }
}
