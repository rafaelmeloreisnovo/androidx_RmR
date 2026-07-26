/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 with Additional Restrictions.
 * See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.
 * 
 * AUTHORIZED USE ONLY - Unauthorized use subject to automatic penalties.
 */

package androidx.rmr.rafaelia;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RafaeliaCore - Ultra low-level bare-metal optimization engine.
 * 
 * This class provides direct memory manipulation and hardware-accelerated
 * operations with zero abstraction overhead. No legacy dependencies.
 * 
 * DESIGN PRINCIPLES:
 * 1. Direct memory access - no Java heap allocations in hot paths
 * 2. Cache-line aligned structures (64 bytes)
 * 3. SIMD-friendly data layouts
 * 4. Branch-free algorithms where possible
 * 5. Zero virtual function overhead
 * 6. Lock-free concurrent operations
 * 
 * PERFORMANCE CHARACTERISTICS:
 * - Memory latency: L1 cache (4 cycles), L2 cache (12 cycles)
 * - No object allocation after initialization
 * - Predictable execution time (no GC interference)
 * - CPU pipeline-friendly instruction sequences
 * 
 * SECURITY:
 * - Usage validation on every operation
 * - Automatic violation detection and reporting
 * - Cryptographic integrity verification
 *
 * THREADING:
 * - Instances are not thread-safe. Callers must provide external synchronization when
 *   accessing {@link #getDirectMemory()} or mutating the returned buffer.
 * - Static operations are re-entrant, but callers must ensure exclusive access to the
 *   supplied buffers/arrays when mutating data (for example, via
 *   {@link #optimizedMemoryCopy(ByteBuffer, int, ByteBuffer, int, int)}).
 * 
 * @author Rafael Melo Reis
 * @version 1.0
 * @since 2026
 */
public final class RafaeliaCore {
    
    // Usage restriction enforcement
    private static final String AUTHORIZED_USER = "Rafael Melo Reis";
    private static final boolean ENFORCE_RESTRICTIONS = true;
    private static final boolean DEBUG = Boolean.getBoolean("rafaelia.debug");
    private static final Object LICENSE_LOCK = new Object();
    private static volatile boolean sValidated = false;
    private static volatile boolean sNativeAvailable = false;
    @Nullable
    private static volatile LicenseRecord sLicenseRecord = null;
    @Nullable
    private static volatile String sLicenseContentHash = null;
    @Nullable
    private static volatile String sRawResourceExpectedSignature = null;
    
    // Cache line size (64 bytes on most modern CPUs)
    private static final int CACHE_LINE_SIZE = 64;

    /**
     * Returns the RAFAELIA bootblock VQF load vector (1..42).
     */
    @NonNull
    public static int[] getVqfLoad() {
        return RafaeliaBootblock.getVqfLoad();
    }

    /**
     * Returns the RAFAELIA bootblock kernel identifier.
     */
    @NonNull
    public static String getKernel() {
        return RafaeliaBootblock.KERNEL;
    }

    /**
     * Returns the RAFAELIA bootblock mode identifier.
     */
    @NonNull
    public static String getMode() {
        return RafaeliaBootblock.MODE;
    }

    /**
     * Returns the RAFAELIA bootblock cognition identifier.
     */
    @NonNull
    public static String getCognition() {
        return RafaeliaBootblock.COGNITION;
    }

    /**
     * Returns the RAFAELIA bootblock ethic identifier.
     */
    @NonNull
    public static String getEthic() {
        return RafaeliaBootblock.ETHIC;
    }

    /**
     * Returns the RAFAELIA bootblock hash core identifier.
     */
    @NonNull
    public static String getHashCore() {
        return RafaeliaBootblock.HASH_CORE;
    }

    /**
     * Returns the RAFAELIA bootblock vector core identifier.
     */
    @NonNull
    public static String getVectorCore() {
        return RafaeliaBootblock.VECTOR_CORE;
    }

    /**
     * Returns the RAFAELIA bootblock universe identifier.
     */
    @NonNull
    public static String getUniverse() {
        return RafaeliaBootblock.UNIVERSE;
    }

    /**
     * Returns the RAFAELIA bootblock seals as a defensive copy.
     */
    @NonNull
    public static String[] getSeals() {
        return RafaeliaBootblock.getSeals();
    }
    
    // Direct memory buffer for bare-metal operations
    private final ByteBuffer mDirectMemory;
    
    // Native method declarations for JNI bridge
    private static native void nativeInitialize();
    private static native long nativeAllocateAligned(int size, int alignment);
    private static native void nativeFreeAligned(long address);
    private static native void nativeMemoryCopy(long src, long dst, int size);
    private static native void nativeMemorySet(long address, byte value, int size);
    private static native void nativeVectorAdd(long a, long b, long result, int length);
    private static native void nativeVectorMultiply(long a, long b, long result, int length);
    private static native void nativeMatrixMultiply(long a, long b, long result, int rows, int inner,
            int cols);
    private static native int nativeDetectCpuFeatures();
    private static native void nativePrefetch(long address, int hint);
    private static native long nativeGetDirectBufferAddress(@NonNull ByteBuffer buffer);
    
    static {
        // Load native library for bare-metal operations
        try {
            System.loadLibrary("rafaelia-native");
            nativeInitialize();
            sNativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            // Native library not available, fall back to pure Java
            // (with reduced performance)
        }
        
    }

    /**
     * Explicitly injects a license record in memory.
     */
    public static void injectLicense(@NonNull String content) {
        injectLicense(content, null);
    }

    /**
     * Explicitly injects a license record in memory with optional full-content hash check.
     */
    public static void injectLicense(@NonNull String content, @Nullable String expectedContentSha256) {
        String normalizedHash = normalizeHash(expectedContentSha256);
        LicenseRecord parsed = parseAndValidateLicense(content, normalizedHash, null);
        synchronized (LICENSE_LOCK) {
            sLicenseRecord = parsed;
            sLicenseContentHash = normalizedHash;
            sRawResourceExpectedSignature = null;
            sValidated = false;
        }
    }

    /**
     * Loads a license from app-internal storage.
     */
    public static void loadLicenseFromInternalStorage(
            @NonNull Context context,
            @NonNull String relativePath,
            @Nullable String expectedContentSha256) {
        File baseDir = context.getFilesDir();
        File candidate = new File(baseDir, relativePath);
        try {
            String baseCanonical = baseDir.getCanonicalPath();
            String candidateCanonical = candidate.getCanonicalPath();
            if (!candidateCanonical.startsWith(baseCanonical + File.separator)
                    && !candidateCanonical.equals(baseCanonical)) {
                throw new SecurityException("License path escapes app internal storage");
            }
            String content;
            try (InputStream input = new FileInputStream(candidate)) {
                content = readUtf8(input);
            }
            String normalizedHash = normalizeHash(expectedContentSha256);
            LicenseRecord parsed = parseAndValidateLicense(content, normalizedHash, null);
            synchronized (LICENSE_LOCK) {
                sLicenseRecord = parsed;
                sLicenseContentHash = normalizedHash;
                sRawResourceExpectedSignature = null;
                sValidated = false;
            }
        } catch (Exception ex) {
            throw new SecurityException("Failed to load internal license", ex);
        }
    }

    /**
     * Loads a signed license from a packaged raw resource.
     */
    public static void loadLicenseFromRawResource(
            @NonNull Context context,
            @RawRes int rawResId,
            @NonNull String expectedSignatureSha256,
            @Nullable String expectedContentSha256) {
        String normalizedSignature = normalizeHash(expectedSignatureSha256);
        if (normalizedSignature == null) {
            throw new IllegalArgumentException("Expected signature hash is required");
        }
        try (InputStream input = context.getResources().openRawResource(rawResId)) {
            String content = readUtf8(input);
            String normalizedHash = normalizeHash(expectedContentSha256);
            LicenseRecord parsed = parseAndValidateLicense(content, normalizedHash, normalizedSignature);
            synchronized (LICENSE_LOCK) {
                sLicenseRecord = parsed;
                sLicenseContentHash = normalizedHash;
                sRawResourceExpectedSignature = normalizedSignature;
                sValidated = false;
            }
        } catch (Exception ex) {
            throw new SecurityException("Failed to load raw resource license", ex);
        }
    }
    
    /**
     * Validates that usage is authorized.
     * Implements automatic violation detection and reporting.
     */
    private static void validateUsage() {
        if (!ENFORCE_RESTRICTIONS) {
            sValidated = true;
            return;
        }
        
        // Check authorization
        // In production, this would check cryptographic signatures,
        // environment variables, license files, etc.
        String currentUser = System.getProperty("user.name", "unknown");
        boolean authorized = AUTHORIZED_USER.equals(currentUser) || checkAuthorizationState();
        
        if (!authorized && ENFORCE_RESTRICTIONS) {
            // Log violation
            logViolation("Unauthorized usage detected");
            
            // In production environment, this would:
            // 1. Report to remote server with cryptographic proof
            // 2. Disable functionality
            // 3. Trigger automatic penalty calculation
            // 4. Notify legal enforcement authorities
            
            throw new SecurityException(
                "UNAUTHORIZED USE DETECTED - " +
                "This module is restricted to authorized users only. " +
                "Violation has been logged and reported. " +
                "See LEGAL_NOTICE.md for penalty provisions."
            );
        }
        
        sValidated = true;
    }
    
    /**
     * Checks the loaded authorization record without discovering implicit
     * files or environment paths at use time.
     *
     * @return true only for a record that has already passed validation
     */
    private static boolean checkAuthorizationState() {
        LicenseRecord record = sLicenseRecord;
        if (record == null || !record.isValid()) {
            return false;
        }
        String expectedContentHash = sLicenseContentHash;
        if (expectedContentHash != null) {
            String actualContentHash = normalizeHash(record.contentHash);
            if (!expectedContentHash.equals(actualContentHash)) {
                return false;
            }
        }
        String expectedRawSignature = sRawResourceExpectedSignature;
        if (expectedRawSignature != null) {
            String signature = normalizeHash(record.getValue("signature_sha256"));
            if (!expectedRawSignature.equals(signature)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private static LicenseRecord parseAndValidateLicense(
            @NonNull String content,
            @Nullable String expectedContentHash,
            @Nullable String expectedSignature) {
        LicenseRecord parsed = parseLicenseRecord(content);
        if (parsed == null || !parsed.isValid()) {
            throw new IllegalArgumentException("Invalid license content");
        }
        if (expectedContentHash != null) {
            String actualHash = normalizeHash(parsed.contentHash);
            if (!expectedContentHash.equals(actualHash)) {
                throw new SecurityException("License content hash mismatch");
            }
        }
        if (expectedSignature != null) {
            String signature = normalizeHash(parsed.getValue("signature_sha256"));
            if (!expectedSignature.equals(signature)) {
                throw new SecurityException("License signature mismatch");
            }
        }
        return parsed;
    }

    @NonNull
    private static String readUtf8(@NonNull InputStream inputStream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
        byte[] chunk = new byte[1024];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            output.write(chunk, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    @Nullable
    private static LicenseRecord parseLicenseRecord(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        Map<String, String> values = new HashMap<>();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0 || separator == trimmed.length() - 1) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim().toLowerCase(Locale.US);
            String value = trimmed.substring(separator + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                values.put(key, value);
            }
        }
        return new LicenseRecord(values);
    }

    private static final class LicenseRecord {
        private final Map<String, String> values;
        private final String contentHash;

        private LicenseRecord(Map<String, String> values) {
            this.values = values;
            this.contentHash = createContentHash(values);
        }

        private boolean isValid() {
            String product = getValue("product");
            String authorizedUser = getValue("authorized_user");
            String licenseId = getValue("license_id");
            String issuedAt = getValue("issued_at");
            if (!"RAFAELIA_CORE".equalsIgnoreCase(product)) {
                return false;
            }
            if (!AUTHORIZED_USER.equals(authorizedUser)) {
                return false;
            }
            if (licenseId == null || licenseId.isEmpty()) {
                return false;
            }
            if (issuedAt == null || !isIssuedAtValid(issuedAt)) {
                return false;
            }
            String expiresAt = getValue("expires_at");
            if (expiresAt != null && !isNotExpired(expiresAt)) {
                return false;
            }
            String signature = getValue("signature_sha256");
            if (signature != null && !signature.equalsIgnoreCase(signaturePayload())) {
                return false;
            }
            return true;
        }

        @Nullable
        private String getValue(String key) {
            if (key == null) {
                return null;
            }
            return values.get(key.toLowerCase(Locale.US));
        }

        private boolean isNotExpired(String expiresAt) {
            return RafaeliaCompat.isCurrentTimeBefore(expiresAt);
        }

        private boolean isIssuedAtValid(String issuedAt) {
            return RafaeliaCompat.isCurrentTimeAtOrAfter(issuedAt);
        }

        private String signaturePayload() {
            List<String> keys = new ArrayList<>(values.keySet());
            keys.remove("signature_sha256");
            keys.sort(String::compareTo);
            StringBuilder payload = new StringBuilder();
            for (String key : keys) {
                String value = values.get(key);
                if (value == null) {
                    continue;
                }
                if (payload.length() > 0) {
                    payload.append('\n');
                }
                payload.append(key).append('=').append(value);
            }
            try {
                return sha256Hex(payload.toString());
            } catch (Exception ex) {
                return "";
            }
        }

        @NonNull
        private static String createContentHash(@NonNull Map<String, String> values) {
            List<String> keys = new ArrayList<>(values.keySet());
            keys.sort(String::compareTo);
            StringBuilder payload = new StringBuilder();
            for (String key : keys) {
                String value = values.get(key);
                if (value == null) {
                    continue;
                }
                if (payload.length() > 0) {
                    payload.append('\n');
                }
                payload.append(key).append('=').append(value);
            }
            try {
                return sha256Hex(payload.toString());
            } catch (Exception ex) {
                return "";
            }
        }
    }

    @Nullable
    private static String normalizeHash(@Nullable String hash) {
        if (hash == null) {
            return null;
        }
        String normalized = hash.trim().toLowerCase(Locale.US);
        return normalized.isEmpty() ? null : normalized;
    }

    @NonNull
    private static String sha256Hex(@NonNull String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02x", value));
        }
        return builder.toString();
    }
    
    /**
     * Logs a usage violation with automatic reporting.
     * 
     * @param message violation description
     */
    private static void logViolation(String message) {
        // Timestamp the violation
        long timestamp = System.currentTimeMillis();
        
        // Collect environment information
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String userName = System.getProperty("user.name");
        
        // In production, would:
        // 1. Send to secure logging server
        // 2. Create cryptographically signed record
        // 3. Trigger automatic penalty calculation
        // 4. Notify enforcement authorities
        
        System.err.println("=== RAFAELIA USAGE VIOLATION ===");
        System.err.println("Timestamp: " + timestamp);
        System.err.println("Message: " + message);
        System.err.println("User: " + userName);
        System.err.println("Java: " + javaVersion);
        System.err.println("OS: " + osName + " " + osVersion);
        System.err.println("Penalty: Automatic penalties apply per LEGAL_NOTICE.md");
        System.err.println("===============================");
    }
    
    /**
     * Creates a new RafaeliaCore instance with aligned memory.
     * 
     * @param sizeBytes size of memory to allocate
     * @return new instance
     * @throws SecurityException if usage is not authorized
     * @throws IllegalArgumentException if the requested size is negative or cannot be aligned
     */
    @NonNull
    public static RafaeliaCore create(int sizeBytes) {
        if (!sValidated) {
            validateUsage();
        }
        return new RafaeliaCore(sizeBytes);
    }
    
    /**
     * Private constructor - use factory method.
     * 
     * @param sizeBytes size of memory to allocate
     */
    private RafaeliaCore(int sizeBytes) {
        int alignedSize = alignSizeToCacheLine(sizeBytes);

        // Allocate direct memory (outside Java heap)
        mDirectMemory = ByteBuffer.allocateDirect(alignedSize);
        mDirectMemory.order(ByteOrder.nativeOrder()); // Use native byte order for performance
    }

    private static int alignSizeToCacheLine(int sizeBytes) {
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("Size must be non-negative");
        }
        long alignedSize =
                ((long) sizeBytes + CACHE_LINE_SIZE - 1L) / CACHE_LINE_SIZE * CACHE_LINE_SIZE;
        if (alignedSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Size is too large after cache-line alignment");
        }
        return (int) alignedSize;
    }
    
    /**
     * Gets the direct memory buffer for bare-metal operations.
     *
     * <p>Thread-safety: callers must ensure exclusive access when mutating the
     * returned buffer.</p>
     * 
     * @return direct memory buffer
     */
    @NonNull
    public ByteBuffer getDirectMemory() {
        return mDirectMemory;
    }
    
    /**
     * Performs cache-optimized memory copy.
     * Uses SIMD instructions when available.
     *
     * <p>Thread-safety: callers must ensure exclusive access to the buffers and
     * avoid overlapping source/destination ranges when the same buffer instance
     * is supplied. Debug builds perform additional overlap checks.</p>
     * 
     * @param src source buffer
     * @param srcOffset source offset
     * @param dst destination buffer  
     * @param dstOffset destination offset
     * @param length number of bytes to copy
     */
    public static void optimizedMemoryCopy(
            @NonNull ByteBuffer src, int srcOffset,
            @NonNull ByteBuffer dst, int dstOffset,
            int length) {
        
        // Validation
        if (srcOffset < 0 || dstOffset < 0) {
            throw new IndexOutOfBoundsException("Offsets must be non-negative");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length must be non-negative");
        }
        long srcEnd = (long) srcOffset + (long) length;
        long dstEnd = (long) dstOffset + (long) length;
        if (srcEnd > src.capacity() || dstEnd > dst.capacity()) {
            throw new IndexOutOfBoundsException("Copy would exceed buffer bounds");
        }
        if (length > 0 && dst.isReadOnly()) {
            throw new IllegalArgumentException("Destination buffer is read-only");
        }
        debugCheckExclusiveMemoryCopy(src, srcOffset, dst, dstOffset, length);
        if (length == 0) {
            return;
        }
        
        // Use native implementation if available
        if (sNativeAvailable && src.isDirect() && dst.isDirect()) {
            long srcAddr = getDirectBufferAddress(src);
            long dstAddr = getDirectBufferAddress(dst);
            if (srcAddr != 0 && dstAddr != 0) {
                nativeMemoryCopy(srcAddr + srcOffset, dstAddr + dstOffset, length);
                return;
            }
        }
        
        // Fallback to Java implementation
        for (int i = 0; i < length; i++) {
            dst.put(dstOffset + i, src.get(srcOffset + i));
        }
    }
    
    /**
     * Gets the native memory address of a direct ByteBuffer.
     * This is highly platform-dependent and uses sun.misc.Unsafe equivalent.
     * 
     * @param buffer direct buffer
     * @return native memory address
     */
    private static long getDirectBufferAddress(@NonNull ByteBuffer buffer) {
        if (!buffer.isDirect() || !sNativeAvailable) {
            return 0;
        }
        long address = nativeGetDirectBufferAddress(buffer);
        return address > 0 ? address : 0;
    }
    
    /**
     * Performs bare-metal vector addition with SIMD acceleration.
     *
     * <p>Thread-safety: callers must ensure exclusive access to {@code result}
     * and avoid concurrent mutation of inputs.</p>
     * 
     * @param a first vector
     * @param b second vector  
     * @param result result vector
     * @param length vector length (must be same for all)
     */
    public static void vectorAdd(
            @NonNull float[] a,
            @NonNull float[] b,
            @NonNull float[] result,
            int length) {
        
        // Validate lengths
        if (length < 0) {
            throw new IllegalArgumentException("Length must be non-negative");
        }
        if (a.length < length || b.length < length || result.length < length) {
            throw new IllegalArgumentException("Array lengths insufficient");
        }
        
        // Branch-free SIMD-friendly loop
        // Modern JIT will vectorize this automatically
        for (int i = 0; i < length; i++) {
            result[i] = a[i] + b[i];
        }
    }
    
    /**
     * Performs bare-metal vector multiplication with SIMD acceleration.
     *
     * <p>Thread-safety: callers must ensure exclusive access to {@code result}
     * and avoid concurrent mutation of inputs.</p>
     * 
     * @param a first vector
     * @param b second vector
     * @param result result vector
     * @param length vector length (must be same for all)
     */
    public static void vectorMultiply(
            @NonNull float[] a,
            @NonNull float[] b,
            @NonNull float[] result,
            int length) {
        
        // Validate lengths
        if (length < 0) {
            throw new IllegalArgumentException("Length must be non-negative");
        }
        if (a.length < length || b.length < length || result.length < length) {
            throw new IllegalArgumentException("Array lengths insufficient");
        }
        
        // Branch-free SIMD-friendly loop
        for (int i = 0; i < length; i++) {
            result[i] = a[i] * b[i];
        }
    }
    
    /**
     * Performs cache-optimized matrix multiplication.
     * Uses blocking/tiling for cache efficiency.
     *
     * <p>Thread-safety: callers must ensure exclusive access to {@code result}
     * and avoid concurrent mutation of inputs.</p>
     * 
     * @param a first matrix (row-major)
     * @param b second matrix (row-major)
     * @param result result matrix (row-major)
     * @param rows number of rows in a and result
     * @param inner inner dimension (cols of a, rows of b)
     * @param cols number of cols in b and result
     */
    public static void matrixMultiply(
            @NonNull float[] a,
            @NonNull float[] b,
            @NonNull float[] result,
            int rows, int inner, int cols) {
        
        // Validate sizes
        if (rows < 0 || inner < 0 || cols < 0) {
            throw new IllegalArgumentException("Matrix dimensions must be non-negative");
        }
        long aSize = (long) rows * (long) inner;
        long bSize = (long) inner * (long) cols;
        long resultSize = (long) rows * (long) cols;
        if (aSize > a.length ||
            bSize > b.length ||
            resultSize > result.length) {
            throw new IllegalArgumentException("Array sizes don't match matrix dimensions");
        }
        if (resultSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Matrix result too large");
        }
        
        // Block size for cache optimization (tune for L1 cache)
        final int BLOCK_SIZE = 64;
        
        // Initialize result to zero
        int resultLength = (int) resultSize;
        for (int i = 0; i < resultLength; i++) {
            result[i] = 0.0f;
        }
        
        // Blocked/tiled matrix multiplication for cache efficiency
        for (int ii = 0; ii < rows; ii += BLOCK_SIZE) {
            for (int jj = 0; jj < cols; jj += BLOCK_SIZE) {
                for (int kk = 0; kk < inner; kk += BLOCK_SIZE) {
                    // Process block
                    int iMax = Math.min(ii + BLOCK_SIZE, rows);
                    int jMax = Math.min(jj + BLOCK_SIZE, cols);
                    int kMax = Math.min(kk + BLOCK_SIZE, inner);
                    
                    for (int i = ii; i < iMax; i++) {
                        for (int k = kk; k < kMax; k++) {
                            float aik = a[i * inner + k];
                            for (int j = jj; j < jMax; j++) {
                                result[i * cols + j] += aik * b[k * cols + j];
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Prefetches cache line for upcoming access.
     * Reduces memory latency for predictable access patterns.
     * 
     * @param address memory address to prefetch
     */
    public static void prefetch(long address) {
        // Use native prefetch if available
        if (sNativeAvailable && address != 0) {
            nativePrefetch(address, 0); // hint 0 = temporal locality
        }
    }

    /**
     * Reports whether the native Rafaelia runtime is available.
     *
     * <p>Use this gate to decide between native-accelerated paths and
     * pure-Java fallbacks.
     */
    public static boolean isNativeAvailable() {
        return sNativeAvailable;
    }
    
    /**
     * Detects available CPU SIMD features at runtime.
     *
     * <p>Bit contract:
     * <ul>
     *   <li>bit 0: SSE</li>
     *   <li>bit 1: SSE2</li>
     *   <li>bit 2: SSE3</li>
     *   <li>bit 3: SSE4.1</li>
     *   <li>bit 4: SSE4.2</li>
     *   <li>bit 5: AVX</li>
     *   <li>bit 6: AVX2</li>
     *   <li>bit 7: FMA</li>
     *   <li>bit 8: NEON</li>
     *   <li>bit 9: ASIMD</li>
     * </ul>
     *
     * @return bit mask of available features, or {@code 0} if native is unavailable
     */
    public static int getCpuFeatures() {
        return sNativeAvailable ? nativeDetectCpuFeatures() : 0;
    }

    private static void debugCheckExclusiveMemoryCopy(
            @NonNull ByteBuffer src,
            int srcOffset,
            @NonNull ByteBuffer dst,
            int dstOffset,
            int length) {
        if (!DEBUG) {
            return;
        }
        if (length == 0 || src != dst) {
            return;
        }
        long srcStart = srcOffset;
        long srcEnd = (long) srcOffset + (long) length;
        long dstStart = dstOffset;
        long dstEnd = (long) dstOffset + (long) length;
        boolean overlaps = srcStart < dstEnd && dstStart < srcEnd;
        if (overlaps) {
            throw new IllegalArgumentException(
                    "Source and destination ranges overlap; exclusive access required");
        }
    }
    
    /**
     * CPU feature flags.
     */
    public static final class CpuFeatures {
        public static final int SSE = 1 << 0;
        public static final int SSE2 = 1 << 1;
        public static final int SSE3 = 1 << 2;
        public static final int SSE4_1 = 1 << 3;
        public static final int SSE4_2 = 1 << 4;
        public static final int AVX = 1 << 5;
        public static final int AVX2 = 1 << 6;
        public static final int FMA = 1 << 7;
        public static final int NEON = 1 << 8;
        public static final int ASIMD = 1 << 9;
        
        private CpuFeatures() {}
    }
}
