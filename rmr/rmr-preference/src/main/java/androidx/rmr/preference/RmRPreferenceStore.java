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

package androidx.rmr.preference;

import androidx.annotation.NonNull;
import androidx.rmr.core.RmRUtils;

public final class RmRPreferenceStore {
    private static final int DEFAULT_CAPACITY = 256;

    private final String[] keys;
    private final double[] values;
    private final int capacity;
    private final int capacityMask;
    private final boolean powerOfTwo;

    public RmRPreferenceStore() {
        this(DEFAULT_CAPACITY);
    }

    public RmRPreferenceStore(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        this.capacity = capacity;
        this.powerOfTwo = (capacity & (capacity - 1)) == 0;
        this.capacityMask = powerOfTwo ? capacity - 1 : 0;
        this.keys = new String[capacity];
        this.values = new double[capacity];
    }

    public void putDouble(@NonNull String key, double value) {
        int index = findSlot(key, true);
        keys[index] = key;
        values[index] = value;
    }

    public double getDouble(@NonNull String key, double defaultValue) {
        int index = findSlot(key, false);
        if (index == -1) {
            return defaultValue;
        }
        return values[index];
    }

    public boolean contains(@NonNull String key) {
        return findSlot(key, false) != -1;
    }

    private int findSlot(String key, boolean forInsert) {
        int startIndex = powerOfTwo
                ? RmRUtils.hashToIndexPowerOfTwo(key, capacityMask)
                : RmRUtils.hashToIndex(key, capacity);
        for (int i = 0; i < capacity; i++) {
            int probe = powerOfTwo ? (startIndex + i) & capacityMask : (startIndex + i) % capacity;
            String storedKey = keys[probe];
            if (storedKey == null) {
                return forInsert ? probe : -1;
            }
            if (storedKey == key || storedKey.equals(key)) {
                return probe;
            }
        }
        if (forInsert) {
            throw new IllegalStateException("Preference store is full.");
        }
        return -1;
    }
}
