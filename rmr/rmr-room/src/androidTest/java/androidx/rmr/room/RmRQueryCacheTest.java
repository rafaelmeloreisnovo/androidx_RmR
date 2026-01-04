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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for RmRQueryCache, focusing on LRU eviction correctness.
 */
public class RmRQueryCacheTest {
    
    @Test
    public void testBasicCacheHit() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Put a query result
        cache = cache.put(123, 5);
        
        // Should be able to retrieve it
        int result = cache.get(123);
        assertEquals(5, result);
    }
    
    @Test
    public void testCacheMiss() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Query not in cache
        int result = cache.get(999);
        assertEquals(-1, result);
    }
    
    @Test
    public void testCacheWithUpdate() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Put a query result
        cache = cache.put(123, 5);
        
        // Get with update
        RmRQueryCache.CacheResult result = cache.getWithUpdate(123);
        assertTrue(result.isHit());
        assertEquals(5, result.resultCount);
        assertNotNull(result.cache);
    }
    
    @Test
    public void testCacheMissWithUpdate() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Query not in cache
        RmRQueryCache.CacheResult result = cache.getWithUpdate(999);
        assertFalse(result.isHit());
        assertEquals(-1, result.resultCount);
    }
    
    @Test
    public void testTransactionInvalidation() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Put a query result
        cache = cache.put(123, 5);
        
        // Invalidate for new transaction
        cache = cache.invalidate();
        
        // Old result should be stale
        int result = cache.get(123);
        assertEquals(-1, result);
    }
    
    @Test
    public void testQueryInvalidation() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Put two query results
        cache = cache.put(123, 5);
        cache = cache.put(456, 10);
        
        // Invalidate one query
        cache = cache.invalidateQuery(123);
        
        // First should be gone, second should remain
        assertEquals(-1, cache.get(123));
        assertEquals(10, cache.get(456));
    }
    
    @Test
    public void testCacheClear() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Put some queries
        cache = cache.put(123, 5);
        cache = cache.put(456, 10);
        
        // Clear cache
        cache = cache.clear();
        
        // Both should be gone
        assertEquals(-1, cache.get(123));
        assertEquals(-1, cache.get(456));
        assertEquals(0, cache.size());
    }
    
    /**
     * Critical test: Verify that LRU eviction doesn't always pick slot 0.
     * This tests the fix for the bug where evictLRU used hits (which were never incremented).
     */
    @Test
    public void testLRUEvictionDistribution() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Fill the cache completely (32 entries)
        for (int i = 0; i < 32; i++) {
            cache = cache.put(1000 + i, i);
        }
        
        // Access some entries multiple times with getWithUpdate to increase their ticks
        for (int i = 0; i < 5; i++) {
            RmRQueryCache.CacheResult result = cache.getWithUpdate(1000); // First entry
            cache = result.cache;
        }
        for (int i = 0; i < 3; i++) {
            RmRQueryCache.CacheResult result = cache.getWithUpdate(1010); // Middle entry
            cache = result.cache;
        }
        
        // Now insert a new entry, which should evict the LRU (one that wasn't accessed)
        cache = cache.put(2000, 100);
        
        // The first entry (1000) should still be there because it was accessed recently
        assertEquals(0, cache.get(1000));
        
        // The middle entry (1010) should still be there
        assertEquals(10, cache.get(1010));
        
        // New entry should be present
        assertEquals(100, cache.get(2000));
        
        // Some entry that wasn't accessed should have been evicted
        // Count how many of the original entries remain
        int remainingCount = 0;
        for (int i = 0; i < 32; i++) {
            if (cache.get(1000 + i) >= 0) {
                remainingCount++;
            }
        }
        
        // Should be 31 original entries (one was evicted) plus the new one (total 32)
        assertEquals(31, remainingCount);
    }
    
    /**
     * Test that eviction doesn't always pick the same slot when cache is full.
     */
    @Test
    public void testLRUEvictionVariesByAccessPattern() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Fill cache
        for (int i = 0; i < 32; i++) {
            cache = cache.put(1000 + i, i);
        }
        
        // Access last few entries
        for (int i = 28; i < 32; i++) {
            RmRQueryCache.CacheResult result = cache.getWithUpdate(1000 + i);
            cache = result.cache;
        }
        
        // Insert new entry - should evict one of the early entries (not the recently accessed ones)
        cache = cache.put(2000, 999);
        
        // Last 4 entries should still be present
        for (int i = 28; i < 32; i++) {
            assertTrue("Entry " + (1000 + i) + " should still be in cache", 
                       cache.get(1000 + i) >= 0);
        }
        
        // At least one of the first entries should have been evicted
        int firstEntriesPresent = 0;
        for (int i = 0; i < 4; i++) {
            if (cache.get(1000 + i) >= 0) {
                firstEntriesPresent++;
            }
        }
        assertTrue("At least one early entry should be evicted", firstEntriesPresent < 4);
    }
    
    @Test
    public void testCacheSize() {
        RmRQueryCache cache = new RmRQueryCache();
        assertEquals(0, cache.size());
        
        cache = cache.put(1, 10);
        assertEquals(1, cache.size());
        
        cache = cache.put(2, 20);
        assertEquals(2, cache.size());
        
        cache = cache.put(3, 30);
        assertEquals(3, cache.size());
        
        cache = cache.invalidateQuery(2);
        assertEquals(2, cache.size());
    }
    
    @Test
    public void testMultiplePutsToSameKey() {
        RmRQueryCache cache = new RmRQueryCache();
        
        // Put a value
        cache = cache.put(123, 5);
        assertEquals(5, cache.get(123));
        
        // Update the same key
        cache = cache.put(123, 10);
        assertEquals(10, cache.get(123));
        
        // Size should still be 1
        assertEquals(1, cache.size());
    }
}
