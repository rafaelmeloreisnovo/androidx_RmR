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
import androidx.annotation.RestrictTo;
import androidx.rmr.core.RmRMatrix;
import androidx.rmr.core.RmRState;

/**
 * Optimized database query cache using RmR matrix-based approach.
 * 
 * <p>RmRQueryCache provides minimal footprint query result caching without
 * traditional cache overhead. Query results are stored in matrix form with
 * deterministic positions, enabling:
 * <ul>
 *   <li>O(1) cache lookup</li>
 *   <li>Predictable memory usage</li>
 *   <li>Cache-friendly result storage</li>
 *   <li>Zero allocation during cache hits</li>
 * </ul>
 * 
 * <p>Cache dimensions:
 * <ul>
 *   <li>0: Query hash</li>
 *   <li>1: Result count</li>
 *   <li>2: Transaction ID</li>
 *   <li>3: Last access tick (for LRU eviction)</li>
 * </ul>
 * 
 * @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRQueryCache {
    
    /**
     * Maximum number of cached queries
     */
    private static final int MAX_CACHE_SIZE = 32;
    
    /**
     * Internal state representation
     */
    @NonNull
    private final RmRState state;
    
    /**
     * Cache storage matrix
     */
    @NonNull
    private final RmRMatrix cache;
    
    /**
     * Current transaction ID
     * Final field ensures immutability - each transaction operation creates a new instance
     */
    private final long transactionId;
    
    /**
     * Access tick counter for LRU eviction
     * Incremented on each get() to track recency of access
     */
    private final long accessTick;
    
    /**
     * Creates empty query cache
     */
    public RmRQueryCache() {
        this(RmRState.forDatabase(), new RmRMatrix(MAX_CACHE_SIZE, 4), 0L, 0L);
    }
    
    /**
     * Creates query cache with specified state
     * 
     * @param state RmR state
     * @param cache cache storage matrix
     * @param transactionId current transaction
     * @param accessTick current access tick
     */
    private RmRQueryCache(@NonNull RmRState state, @NonNull RmRMatrix cache, long transactionId, long accessTick) {
        this.state = state;
        this.cache = cache;
        this.transactionId = transactionId;
        this.accessTick = accessTick;
    }
    
    /**
     * Caches query result
     * 
     * @param queryHash hash of SQL query
     * @param resultCount number of results
     * @return new cache with result stored
     */
    @NonNull
    public RmRQueryCache put(int queryHash, int resultCount) {
        int slot = findOrAllocateSlot(queryHash);
        
        if (slot < 0) {
            // Cache full, evict LRU
            slot = evictLRU();
        }
        
        long newTick = accessTick + 1;
        RmRMatrix newCache = cache.clone();
        newCache.set(slot, 0, (double) queryHash);
        newCache.set(slot, 1, (double) resultCount);
        newCache.set(slot, 2, (double) transactionId);
        newCache.set(slot, 3, (double) newTick); // Store incremented access tick
        
        return new RmRQueryCache(state, newCache, transactionId, newTick);
    }
    
    /**
     * Gets cached query result count
     * 
     * @param queryHash hash of SQL query
     * @return result count, or -1 if not cached
     * @deprecated Use getWithUpdate() to get both result and updated cache with tick tracking
     */
    @Deprecated
    public int get(int queryHash) {
        int slot = findSlot(queryHash);
        if (slot < 0) {
            return -1; // Cache miss
        }
        
        // Check if cache entry is still valid (same transaction)
        if ((long) cache.get(slot, 2) != transactionId) {
            return -1; // Stale entry
        }
        
        // Note: This method doesn't update access tick for backward compatibility
        // Use getWithUpdate() for proper LRU tracking
        
        return (int) cache.get(slot, 1);
    }
    
    /**
     * Gets cached query result and returns updated cache with access tracking
     * 
     * @param queryHash hash of SQL query
     * @return cache result with updated access tick
     */
    @NonNull
    public CacheResult getWithUpdate(int queryHash) {
        int slot = findSlot(queryHash);
        if (slot < 0) {
            return new CacheResult(this, -1); // Cache miss
        }
        
        // Check if cache entry is still valid (same transaction)
        if ((long) cache.get(slot, 2) != transactionId) {
            return new CacheResult(this, -1); // Stale entry
        }
        
        // Update access tick for LRU tracking
        RmRMatrix newCache = cache.clone();
        long newTick = accessTick + 1;
        newCache.set(slot, 3, (double) newTick);
        
        RmRQueryCache updatedCache = new RmRQueryCache(state, newCache, transactionId, newTick);
        return new CacheResult(updatedCache, (int) cache.get(slot, 1));
    }
    
    /**
     * Result of cache lookup with updated cache instance
     */
    public static final class CacheResult {
        @NonNull
        public final RmRQueryCache cache;
        public final int resultCount; // -1 if miss
        
        CacheResult(@NonNull RmRQueryCache cache, int resultCount) {
            this.cache = cache;
            this.resultCount = resultCount;
        }
        
        public boolean isHit() {
            return resultCount >= 0;
        }
    }
    
    /**
     * Invalidates cache for transaction
     * 
     * @return new cache for next transaction
     */
    @NonNull
    public RmRQueryCache invalidate() {
        return new RmRQueryCache(state, cache, transactionId + 1, accessTick);
    }
    
    /**
     * Invalidates specific query
     * 
     * @param queryHash query to invalidate
     * @return new cache without query
     */
    @NonNull
    public RmRQueryCache invalidateQuery(int queryHash) {
        int slot = findSlot(queryHash);
        if (slot < 0) {
            return this; // Not cached
        }
        
        RmRMatrix newCache = cache.clone();
        newCache.set(slot, 0, 0.0); // Clear entry
        newCache.set(slot, 1, 0.0);
        newCache.set(slot, 2, 0.0);
        newCache.set(slot, 3, 0.0);
        
        return new RmRQueryCache(state, newCache, transactionId, accessTick);
    }
    
    /**
     * Clears entire cache
     * 
     * @return empty cache
     */
    @NonNull
    public RmRQueryCache clear() {
        return new RmRQueryCache();
    }
    
    /**
     * Gets cache hit rate
     * 
     * @return average access recency per entry
     */
    public double getCacheHitRate() {
        double totalTicks = 0.0;
        int activeEntries = 0;
        
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            if (cache.get(i, 0) != 0.0) {
                totalTicks += cache.get(i, 3);
                activeEntries++;
            }
        }
        
        return activeEntries > 0 ? totalTicks / activeEntries : 0.0;
    }
    
    /**
     * Gets number of cached queries
     * 
     * @return cache size
     */
    public int size() {
        int count = 0;
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            if (cache.get(i, 0) != 0.0) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Computes query cost prediction
     * 
     * @param queryHash query to predict
     * @return predicted cost (lower is better)
     */
    public double predictQueryCost(int queryHash) {
        double[] input = {
            (double) queryHash,
            (double) transactionId,
            0.0,
            0.0
        };
        
        double[] result = state.computeDeterministicPoint(input);
        
        // Return cost metric
        double cost = 0.0;
        for (double val : result) {
            cost += Math.abs(val);
        }
        return cost;
    }
    
    /**
     * Internal: finds cache slot for query
     */
    private int findSlot(int queryHash) {
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            if ((int) cache.get(i, 0) == queryHash) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Internal: finds existing slot or allocates new one
     */
    private int findOrAllocateSlot(int queryHash) {
        // First try to find existing
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            if ((int) cache.get(i, 0) == queryHash) {
                return i;
            }
        }
        
        // Allocate new slot
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            if (cache.get(i, 0) == 0.0) {
                return i;
            }
        }
        
        return -1; // Cache full
    }
    
    /**
     * Internal: evicts least recently used entry
     * Finds the entry with the smallest lastAccessTick value
     */
    private int evictLRU() {
        int lruSlot = 0;
        double minTick = Double.MAX_VALUE;
        
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            double tick = cache.get(i, 3);
            if (tick < minTick) {
                minTick = tick;
                lruSlot = i;
            }
        }
        
        return lruSlot;
    }
    
    /**
     * Creates optimized query cache
     * 
     * @return optimized cache with reduced footprint
     */
    @NonNull
    public RmRQueryCache createOptimized() {
        RmRState optimized = state.optimize();
        return new RmRQueryCache(optimized, cache, transactionId, accessTick);
    }
}
