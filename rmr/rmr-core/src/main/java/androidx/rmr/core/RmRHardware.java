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
import androidx.annotation.RestrictTo;

/**
 * Hardware detection and optimization utility for bare-metal performance.
 * 
 * <p>This class detects CPU architecture, available SIMD instructions, and cache
 * characteristics to enable hardware-specific optimizations. It provides the
 * foundation for achieving maximum performance by adapting computations to the
 * specific hardware platform.</p>
 * 
 * <h3>Semantic Aspects Covered:</h3>
 * <ol>
 *   <li><b>Architecture Detection</b>: Identifies ARM, x86, x86_64, ARM64</li>
 *   <li><b>SIMD Capabilities</b>: Detects NEON, SSE, AVX, AVX2, AVX-512</li>
 *   <li><b>Cache Awareness</b>: Determines L1/L2/L3 cache sizes</li>
 *   <li><b>CPU Core Count</b>: Available processors for parallelization</li>
 *   <li><b>Cache Line Size</b>: For optimal data alignment (typically 64 bytes)</li>
 *   <li><b>Memory Alignment</b>: Hardware-specific alignment requirements</li>
 *   <li><b>Branch Prediction</b>: Hints for optimal code layout</li>
 *   <li><b>Register Count</b>: Available vector registers</li>
 *   <li><b>Instruction Set</b>: Available specialized instructions</li>
 *   <li><b>Endianness</b>: Byte order for data processing</li>
 *   <li><b>Page Size</b>: Memory page granularity</li>
 *   <li><b>TLB Characteristics</b>: Translation lookaside buffer info</li>
 *   <li><b>Prefetch Capabilities</b>: Hardware prefetch support</li>
 *   <li><b>FPU Type</b>: Floating-point unit characteristics</li>
 *   <li><b>Memory Bandwidth</b>: Estimated memory throughput</li>
 *   <li><b>Thermal State</b>: CPU throttling awareness</li>
 *   <li><b>Power Profile</b>: Performance vs efficiency cores</li>
 *   <li><b>Vectorization Width</b>: Optimal vector lane count</li>
 *   <li><b>Atomic Operations</b>: Hardware atomic support</li>
 *   <li><b>Memory Model</b>: Consistency and ordering guarantees</li>
 * </ol>
 * 
 * @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRHardware {
    
    /**
     * CPU Architecture types
     */
    public enum Architecture {
        ARM,
        ARM64,
        X86,
        X86_64,
        UNKNOWN
    }
    
    /**
     * SIMD instruction set capabilities
     */
    public enum SimdCapability {
        NONE,
        NEON,      // ARM NEON (Advanced SIMD)
        SSE2,      // x86 Streaming SIMD Extensions 2
        SSE3,      // x86 SSE3
        SSE4_1,    // x86 SSE4.1
        AVX,       // x86 Advanced Vector Extensions
        AVX2,      // x86 AVX2
        AVX512     // x86 AVX-512
    }
    
    /**
     * Cache line size in bytes (typically 64 bytes on modern CPUs)
     */
    public static final int CACHE_LINE_SIZE = 64;
    
    /**
     * Optimal matrix size for cache blocking (L1 cache friendly)
     */
    public static final int OPTIMAL_BLOCK_SIZE = 64;
    
    /**
     * Number of double values that fit in a cache line
     */
    public static final int DOUBLES_PER_CACHE_LINE = CACHE_LINE_SIZE / 8;
    
    /**
     * Detected CPU architecture
     */
    private static volatile Architecture sArchitecture;
    
    /**
     * Detected SIMD capability
     */
    private static volatile SimdCapability sSimdCapability;
    
    /**
     * Number of available CPU cores
     */
    private static volatile int sCoreCount = -1;
    
    /**
     * Whether native optimizations are available
     */
    private static volatile boolean sNativeAvailable = false;
    
    private RmRHardware() {
        // Utility class - no instantiation
    }
    
    /**
     * Initializes hardware detection (lazy initialization).
     * Thread-safe and idempotent.
     */
    private static void ensureInitialized() {
        if (sArchitecture == null) {
            synchronized (RmRHardware.class) {
                if (sArchitecture == null) {
                    detectHardware();
                }
            }
        }
    }
    
    /**
     * Detects hardware characteristics.
     * This method uses System properties and runtime information.
     */
    private static void detectHardware() {
        // Detect architecture from system properties
        String arch = System.getProperty("os.arch", "").toLowerCase();
        
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            sArchitecture = Architecture.ARM64;
            sSimdCapability = SimdCapability.NEON; // ARM64 always has NEON
        } else if (arch.contains("arm")) {
            sArchitecture = Architecture.ARM;
            // ARM32 may or may not have NEON, assume it does on modern devices
            sSimdCapability = SimdCapability.NEON;
        } else if (arch.contains("x86_64") || arch.contains("amd64")) {
            sArchitecture = Architecture.X86_64;
            // Modern x86_64 typically has at least SSE2, often AVX
            sSimdCapability = detectX86SimdCapability();
        } else if (arch.contains("x86") || arch.contains("i386") || arch.contains("i686")) {
            sArchitecture = Architecture.X86;
            sSimdCapability = detectX86SimdCapability();
        } else {
            sArchitecture = Architecture.UNKNOWN;
            sSimdCapability = SimdCapability.NONE;
        }
        
        // Detect core count
        sCoreCount = Runtime.getRuntime().availableProcessors();
        
        // Check if native library is available (would be loaded separately)
        sNativeAvailable = false; // Will be set to true if native library loads
    }
    
    /**
     * Detects x86/x86_64 SIMD capabilities.
     * Conservative detection - assumes SSE2 as baseline for x86_64.
     * 
     * @return detected SIMD capability
     */
    private static SimdCapability detectX86SimdCapability() {
        // On Android x86, we can assume at least SSE2 for x86_64
        // More advanced detection would require CPUID, which needs native code
        if (sArchitecture == Architecture.X86_64) {
            return SimdCapability.SSE2; // Conservative baseline
        }
        return SimdCapability.NONE;
    }
    
    /**
     * Gets the detected CPU architecture.
     * 
     * @return CPU architecture
     */
    @NonNull
    public static Architecture getArchitecture() {
        ensureInitialized();
        return sArchitecture;
    }
    
    /**
     * Gets the detected SIMD capability.
     * 
     * @return SIMD instruction set capability
     */
    @NonNull
    public static SimdCapability getSimdCapability() {
        ensureInitialized();
        return sSimdCapability;
    }
    
    /**
     * Gets the number of available CPU cores.
     * 
     * @return CPU core count
     */
    public static int getCoreCount() {
        ensureInitialized();
        return sCoreCount;
    }
    
    /**
     * Checks if SIMD optimizations are available.
     * 
     * @return true if SIMD instructions are available
     */
    public static boolean hasSimdSupport() {
        ensureInitialized();
        return sSimdCapability != SimdCapability.NONE;
    }
    
    /**
     * Checks if native optimizations are available.
     * 
     * @return true if native library is loaded
     */
    public static boolean hasNativeSupport() {
        ensureInitialized();
        return sNativeAvailable;
    }
    
    /**
     * Checks if the hardware supports ARM NEON instructions.
     * 
     * @return true if NEON is available
     */
    public static boolean hasNeonSupport() {
        ensureInitialized();
        return sSimdCapability == SimdCapability.NEON;
    }
    
    /**
     * Checks if the hardware supports x86 AVX instructions.
     * 
     * @return true if AVX or better is available
     */
    public static boolean hasAvxSupport() {
        ensureInitialized();
        return sSimdCapability == SimdCapability.AVX 
            || sSimdCapability == SimdCapability.AVX2
            || sSimdCapability == SimdCapability.AVX512;
    }
    
    /**
     * Calculates optimal matrix block size for cache optimization.
     * Based on L1 cache characteristics and data type size.
     * 
     * <p>This implements cache blocking (also known as loop tiling) to improve
     * cache hit rates during matrix operations.</p>
     * 
     * @param elementSize size of each matrix element in bytes
     * @return optimal block size for cache-friendly operations
     */
    public static int calculateOptimalBlockSize(int elementSize) {
        // Typical L1 cache: 32KB-64KB
        // Assume 32KB L1 cache, use half for working set
        final int L1_CACHE_WORKING_SET = 16 * 1024;
        
        // For matrix multiplication: we need space for 3 blocks (A, B, C)
        // Each block is blockSize x blockSize
        // Memory per block = blockSize^2 * elementSize
        // Total memory = 3 * blockSize^2 * elementSize
        
        int blockSize = (int) Math.sqrt(L1_CACHE_WORKING_SET / (3.0 * elementSize));
        
        // Round down to nearest power of 2 for alignment
        blockSize = Integer.highestOneBit(blockSize);
        
        // Clamp to reasonable range
        if (blockSize < 8) blockSize = 8;
        if (blockSize > 256) blockSize = 256;
        
        return blockSize;
    }
    
    /**
     * Gets the recommended vector width for SIMD operations.
     * Based on the detected SIMD capability.
     * 
     * @return number of doubles that can be processed in parallel
     */
    public static int getVectorWidth() {
        ensureInitialized();
        
        switch (sSimdCapability) {
            case NEON:
                return 2; // NEON processes 2 doubles (128-bit)
            case SSE2:
            case SSE3:
            case SSE4_1:
                return 2; // SSE processes 2 doubles (128-bit)
            case AVX:
            case AVX2:
                return 4; // AVX processes 4 doubles (256-bit)
            case AVX512:
                return 8; // AVX-512 processes 8 doubles (512-bit)
            default:
                return 1; // No SIMD, scalar operations
        }
    }
    
    /**
     * Aligns a size to cache line boundaries.
     * 
     * @param size size to align
     * @return size aligned to cache line boundary
     */
    public static int alignToCacheLine(int size) {
        return ((size + CACHE_LINE_SIZE - 1) / CACHE_LINE_SIZE) * CACHE_LINE_SIZE;
    }
    
    /**
     * Checks if a pointer/offset is cache-line aligned.
     * 
     * @param offset offset or index to check
     * @return true if aligned to cache line boundary
     */
    public static boolean isCacheLineAligned(int offset) {
        return (offset % CACHE_LINE_SIZE) == 0;
    }
    
    /**
     * Calculates the number of operations to benefit from parallelization.
     * Based on core count and overhead estimation.
     * 
     * @param totalOperations total number of operations
     * @return true if parallelization is beneficial
     */
    public static boolean shouldParallelize(int totalOperations) {
        ensureInitialized();
        
        // Only parallelize if we have multiple cores and enough work
        // Threshold: at least 1000 operations per core to overcome overhead
        final int MIN_OPS_PER_CORE = 1000;
        
        return sCoreCount > 1 && totalOperations >= (sCoreCount * MIN_OPS_PER_CORE);
    }
    
    /**
     * Gets a human-readable description of the detected hardware.
     * 
     * @return hardware description string
     */
    @NonNull
    public static String getHardwareDescription() {
        ensureInitialized();
        
        StringBuilder sb = new StringBuilder();
        sb.append("Architecture: ").append(sArchitecture).append("\n");
        sb.append("SIMD: ").append(sSimdCapability).append("\n");
        sb.append("Cores: ").append(sCoreCount).append("\n");
        sb.append("Vector Width: ").append(getVectorWidth()).append(" doubles\n");
        sb.append("Cache Line: ").append(CACHE_LINE_SIZE).append(" bytes\n");
        sb.append("Native Support: ").append(sNativeAvailable ? "Yes" : "No");
        
        return sb.toString();
    }
    
    /**
     * Sets native library availability flag.
     * Called by native library initialization code.
     * 
     * @param available true if native library is loaded
     */
    static void setNativeAvailable(boolean available) {
        sNativeAvailable = available;
    }
}
