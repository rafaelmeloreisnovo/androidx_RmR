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

package androidx.rmr.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.rmr.core.RmRMatrix;

public final class RmRQueryCache {
    private static final int DEFAULT_CAPACITY = 64;

    private final String[] keys;
    private final RmRMatrix[] values;
    private final int capacity;
    private int size;

    public RmRQueryCache() {
        this(DEFAULT_CAPACITY);
    }

    public RmRQueryCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        this.capacity = capacity;
        this.keys = new String[capacity];
        this.values = new RmRMatrix[capacity];
        this.size = 0;
    }

    public void put(@NonNull String queryKey, @NonNull RmRMatrix result) {
        int index = indexOf(queryKey);
        if (index >= 0) {
            values[index] = result;
            moveToFront(index);
            return;
        }
        if (size < capacity) {
            keys[size] = queryKey;
            values[size] = result;
            moveToFront(size);
            size++;
        } else {
            keys[capacity - 1] = queryKey;
            values[capacity - 1] = result;
            moveToFront(capacity - 1);
        }
    }

    @Nullable
    public RmRMatrix get(@NonNull String queryKey) {
        int index = indexOf(queryKey);
        if (index < 0) {
            return null;
        }
        RmRMatrix value = values[index];
        moveToFront(index);
        return value;
    }

    public int size() {
        return size;
    }

    private int indexOf(String queryKey) {
        for (int i = 0; i < size; i++) {
            String key = keys[i];
            if (key == queryKey || queryKey.equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private void moveToFront(int index) {
        if (index <= 0) {
            return;
        }
        String key = keys[index];
        RmRMatrix value = values[index];
        System.arraycopy(keys, 0, keys, 1, index);
        System.arraycopy(values, 0, values, 1, index);
        keys[0] = key;
        values[0] = value;
    }
}
