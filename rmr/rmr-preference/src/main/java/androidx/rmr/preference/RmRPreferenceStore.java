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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.rmr.core.RmRMatrix;
import androidx.rmr.core.RmRState;

/**
 * Optimized preference storage using RmR matrix-based approach.
 * 
 * <p>RmRPreferenceStore provides minimal footprint preference management without
 * HashMap overhead. Preferences are stored as matrix indices with values in
 * deterministic matrix positions, enabling:
 * <ul>
 *   <li>O(1) preference access</li>
 *   <li>Predictable memory layout</li>
 *   <li>Cache-friendly preference storage</li>
 *   <li>Minimal allocation overhead</li>
 * </ul>
 * 
 * <p>Storage dimensions:
 * <ul>
 *   <li>0: Preference key hash</li>
 *   <li>1: Preference value (encoded as double)</li>
 *   <li>2: Preference type (0=int, 1=float, 2=boolean, 3=string hash)</li>
 *   <li>3: Dirty flag for persistence</li>
 * </ul>
 * 
 * @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRPreferenceStore {
    
    /**
     * Preference type constants
     */
    public static final int TYPE_INT = 0;
    public static final int TYPE_FLOAT = 1;
    public static final int TYPE_BOOLEAN = 2;
    public static final int TYPE_STRING_HASH = 3;
    
    /**
     * Maximum number of preference slots
     */
    private static final int MAX_PREFERENCES = 64;
    
    /**
     * Internal state representation
     */
    @NonNull
    private final RmRState state;
    
    /**
     * Preference mapping matrix (maps key hash to slot)
     */
    @NonNull
    private final RmRMatrix mapping;
    
    /**
     * Creates empty preference store
     */
    public RmRPreferenceStore() {
        this(RmRState.forPreferences(), new RmRMatrix(MAX_PREFERENCES, 4));
    }
    
    /**
     * Creates preference store with specified state
     * 
     * @param state RmR state
     * @param mapping preference mapping matrix
     */
    private RmRPreferenceStore(@NonNull RmRState state, @NonNull RmRMatrix mapping) {
        this.state = state;
        this.mapping = mapping;
    }
    
    /**
     * Stores integer preference
     * 
     * @param key preference key
     * @param value integer value
     * @return new preference store with value stored
     */
    @NonNull
    public RmRPreferenceStore putInt(@NonNull String key, int value) {
        return putValue(key, (double) value, TYPE_INT);
    }
    
    /**
     * Gets integer preference
     * 
     * @param key preference key
     * @param defaultValue default if not found
     * @return stored value or default
     */
    public int getInt(@NonNull String key, int defaultValue) {
        double[] entry = getValue(key);
        if (entry != null && (int) entry[2] == TYPE_INT) {
            return (int) entry[1];
        }
        return defaultValue;
    }
    
    /**
     * Stores float preference
     * 
     * @param key preference key
     * @param value float value
     * @return new preference store with value stored
     */
    @NonNull
    public RmRPreferenceStore putFloat(@NonNull String key, float value) {
        return putValue(key, (double) value, TYPE_FLOAT);
    }
    
    /**
     * Gets float preference
     * 
     * @param key preference key
     * @param defaultValue default if not found
     * @return stored value or default
     */
    public float getFloat(@NonNull String key, float defaultValue) {
        double[] entry = getValue(key);
        if (entry != null && (int) entry[2] == TYPE_FLOAT) {
            return (float) entry[1];
        }
        return defaultValue;
    }
    
    /**
     * Stores boolean preference
     * 
     * @param key preference key
     * @param value boolean value
     * @return new preference store with value stored
     */
    @NonNull
    public RmRPreferenceStore putBoolean(@NonNull String key, boolean value) {
        return putValue(key, value ? 1.0 : 0.0, TYPE_BOOLEAN);
    }
    
    /**
     * Gets boolean preference
     * 
     * @param key preference key
     * @param defaultValue default if not found
     * @return stored value or default
     */
    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        double[] entry = getValue(key);
        if (entry != null && (int) entry[2] == TYPE_BOOLEAN) {
            return entry[1] != 0.0;
        }
        return defaultValue;
    }
    
    /**
     * Stores string preference as hash
     * 
     * @param key preference key
     * @param value string value
     * @return new preference store with value stored
     */
    @NonNull
    public RmRPreferenceStore putString(@NonNull String key, @NonNull String value) {
        return putValue(key, (double) value.hashCode(), TYPE_STRING_HASH);
    }
    
    /**
     * Gets string hash preference
     * 
     * @param key preference key
     * @param defaultHash default hash if not found
     * @return stored hash or default
     */
    public int getStringHash(@NonNull String key, int defaultHash) {
        double[] entry = getValue(key);
        if (entry != null && (int) entry[2] == TYPE_STRING_HASH) {
            return (int) entry[1];
        }
        return defaultHash;
    }
    
    /**
     * Removes preference
     * 
     * @param key preference key
     * @return new preference store without the key
     */
    @NonNull
    public RmRPreferenceStore remove(@NonNull String key) {
        int slot = findSlot(key);
        if (slot < 0) {
            return this; // Not found
        }
        
        RmRMatrix newMapping = mapping.clone();
        // Clear slot
        newMapping.set(slot, 0, 0.0);
        newMapping.set(slot, 1, 0.0);
        newMapping.set(slot, 2, 0.0);
        newMapping.set(slot, 3, 0.0);
        
        return new RmRPreferenceStore(state, newMapping);
    }
    
    /**
     * Checks if preference exists
     * 
     * @param key preference key
     * @return true if key exists
     */
    public boolean contains(@NonNull String key) {
        return findSlot(key) >= 0;
    }
    
    /**
     * Clears all preferences
     * 
     * @return empty preference store
     */
    @NonNull
    public RmRPreferenceStore clear() {
        return new RmRPreferenceStore();
    }
    
    /**
     * Gets all dirty preference keys (modified since last commit)
     * 
     * @return array of key hashes that are dirty
     */
    @NonNull
    public int[] getDirtyKeys() {
        int count = 0;
        for (int i = 0; i < MAX_PREFERENCES; i++) {
            if (mapping.get(i, 3) != 0.0) {
                count++;
            }
        }
        
        int[] dirty = new int[count];
        int index = 0;
        for (int i = 0; i < MAX_PREFERENCES; i++) {
            if (mapping.get(i, 3) != 0.0) {
                dirty[index++] = (int) mapping.get(i, 0);
            }
        }
        
        return dirty;
    }
    
    /**
     * Marks all preferences as committed (clears dirty flags)
     * 
     * @return new preference store with clean state
     */
    @NonNull
    public RmRPreferenceStore commit() {
        RmRMatrix newMapping = mapping.clone();
        for (int i = 0; i < MAX_PREFERENCES; i++) {
            newMapping.set(i, 3, 0.0); // Clear dirty flag
        }
        return new RmRPreferenceStore(state, newMapping);
    }
    
    /**
     * Internal: stores value in preference matrix
     */
    @NonNull
    private RmRPreferenceStore putValue(@NonNull String key, double value, int type) {
        int keyHash = key.hashCode();
        int slot = findOrAllocateSlot(keyHash);
        
        if (slot < 0) {
            throw new IllegalStateException("Preference store full");
        }
        
        RmRMatrix newMapping = mapping.clone();
        newMapping.set(slot, 0, (double) keyHash);
        newMapping.set(slot, 1, value);
        newMapping.set(slot, 2, (double) type);
        newMapping.set(slot, 3, 1.0); // Mark as dirty
        
        return new RmRPreferenceStore(state, newMapping);
    }
    
    /**
     * Internal: gets value from preference matrix
     */
    @Nullable
    private double[] getValue(@NonNull String key) {
        int slot = findSlot(key);
        if (slot < 0) {
            return null;
        }
        
        return new double[] {
            mapping.get(slot, 0), // key hash
            mapping.get(slot, 1), // value
            mapping.get(slot, 2), // type
            mapping.get(slot, 3)  // dirty
        };
    }
    
    /**
     * Internal: finds slot for key
     */
    private int findSlot(@NonNull String key) {
        int keyHash = key.hashCode();
        for (int i = 0; i < MAX_PREFERENCES; i++) {
            if ((int) mapping.get(i, 0) == keyHash) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Internal: finds existing slot or allocates new one
     */
    private int findOrAllocateSlot(int keyHash) {
        // First try to find existing
        for (int i = 0; i < MAX_PREFERENCES; i++) {
            if ((int) mapping.get(i, 0) == keyHash) {
                return i;
            }
        }
        
        // Allocate new slot
        for (int i = 0; i < MAX_PREFERENCES; i++) {
            if (mapping.get(i, 0) == 0.0) {
                return i;
            }
        }
        
        return -1; // Store full
    }
}
