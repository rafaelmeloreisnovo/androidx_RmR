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

    private String[] keys;
    private double[] values;
    private int capacity;
    private int capacityMask;
    private boolean powerOfTwo;
    private int size;
    private String lastKey;
    private int lastIndex = -1;

    public RmRPreferenceStore() {
        this(DEFAULT_CAPACITY);
    }

    public RmRPreferenceStore(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        init(capacity);
    }

    public void putDouble(@NonNull String key, double value) {
        ensureCapacityForInsert();
        int index = findSlot(key, true);
        if (keys[index] == null) {
            size++;
        }
        keys[index] = key;
        values[index] = value;
        lastKey = key;
        lastIndex = index;
    }

    public double getDouble(@NonNull String key, double defaultValue) {
        int index = findSlot(key, false);
        if (index == -1) {
            return defaultValue;
        }
        lastKey = key;
        lastIndex = index;
        return values[index];
    }

    public boolean contains(@NonNull String key) {
        return findSlot(key, false) != -1;
    }

    private int findSlot(String key, boolean forInsert) {
        if (lastIndex >= 0 && lastKey != null && (lastKey == key || lastKey.equals(key))) {
            String cachedKey = keys[lastIndex];
            if (cachedKey != null && (cachedKey == key || cachedKey.equals(key))) {
                return lastIndex;
            }
        }
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
                lastKey = key;
                lastIndex = probe;
                return probe;
            }
        }
        if (forInsert) {
            throw new IllegalStateException("Preference store is full.");
        }
        return -1;
    }

    private void ensureCapacityForInsert() {
        int threshold = (int) (capacity * 0.7f);
        if (size + 1 <= threshold) {
            return;
        }
        resize(capacity * 2);
    }

    private void resize(int newCapacity) {
        String[] oldKeys = keys;
        double[] oldValues = values;
        init(newCapacity);
        for (int i = 0; i < oldKeys.length; i++) {
            String key = oldKeys[i];
            if (key != null) {
                int index = findSlot(key, true);
                keys[index] = key;
                values[index] = oldValues[i];
                size++;
            }
        }
        lastKey = null;
        lastIndex = -1;
    }

    private void init(int newCapacity) {
        capacity = newCapacity;
        powerOfTwo = (capacity & (capacity - 1)) == 0;
        capacityMask = powerOfTwo ? capacity - 1 : 0;
        keys = new String[capacity];
        values = new double[capacity];
        size = 0;
    }
}
