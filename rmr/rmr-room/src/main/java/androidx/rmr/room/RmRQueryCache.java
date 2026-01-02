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
 *   <li>3: Cache hit count for LRU</li>
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
     */
    private final long transactionId;
    
    /**
     * Creates empty query cache
     */
    public RmRQueryCache() {
        this(RmRState.forDatabase(), new RmRMatrix(MAX_CACHE_SIZE, 4), 0L);
    }
    
    /**
     * Creates query cache with specified state
     * 
     * @param state RmR state
     * @param cache cache storage matrix
     * @param transactionId current transaction
     */
    private RmRQueryCache(@NonNull RmRState state, @NonNull RmRMatrix cache, long transactionId) {
        this.state = state;
        this.cache = cache;
        this.transactionId = transactionId;
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
        
        RmRMatrix newCache = cache.clone();
        newCache.set(slot, 0, (double) queryHash);
        newCache.set(slot, 1, (double) resultCount);
        newCache.set(slot, 2, (double) transactionId);
        newCache.set(slot, 3, 1.0); // Initial hit count
        
        return new RmRQueryCache(state, newCache, transactionId);
    }
    
    /**
     * Gets cached query result count
     * 
     * @param queryHash hash of SQL query
     * @return result count, or -1 if not cached
     */
    public int get(int queryHash) {
        int slot = findSlot(queryHash);
        if (slot < 0) {
            return -1; // Cache miss
        }
        
        // Check if cache entry is still valid (same transaction)
        if ((long) cache.get(slot, 2) != transactionId) {
            return -1; // Stale entry
        }
        
        // Update hit count for LRU
        cache.set(slot, 3, cache.get(slot, 3) + 1.0);
        
        return (int) cache.get(slot, 1);
    }
    
    /**
     * Invalidates cache for transaction
     * 
     * @return new cache for next transaction
     */
    @NonNull
    public RmRQueryCache invalidate() {
        return new RmRQueryCache(state, cache, transactionId + 1);
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
        
        return new RmRQueryCache(state, newCache, transactionId);
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
     * @return average hits per entry
     */
    public double getCacheHitRate() {
        double totalHits = 0.0;
        int activeEntries = 0;
        
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            if (cache.get(i, 0) != 0.0) {
                totalHits += cache.get(i, 3);
                activeEntries++;
            }
        }
        
        return activeEntries > 0 ? totalHits / activeEntries : 0.0;
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
     */
    private int evictLRU() {
        int lruSlot = 0;
        double minHits = Double.MAX_VALUE;
        
        for (int i = 0; i < MAX_CACHE_SIZE; i++) {
            double hits = cache.get(i, 3);
            if (hits < minHits) {
                minHits = hits;
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
        return new RmRQueryCache(optimized, cache, transactionId);
    }
}
