/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 with Additional Restrictions.
 * See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.
 */

package androidx.rmr.rafaelia.benchmark;

import static org.junit.Assert.fail;

import android.os.Bundle;
import android.util.Log;

import androidx.rmr.rafaelia.RafaeliaCore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Random;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RafaeliaBenchmarkGuardrailTest {
    private static final String TAG = "RafaeliaGuardrail";
    private static final String ENFORCE_ARG = "rmr.guardrails.enforce";

    @Test
    public void guardrailMatrixMultiply() {
        int size = RafaeliaBenchmarkBaselines.MATRIX_SIZE;
        float[] a = new float[size * size];
        float[] b = new float[size * size];
        float[] result = new float[size * size];
        Random random = new Random(RafaeliaBenchmarkBaselines.RANDOM_SEED);

        for (int i = 0; i < size * size; i++) {
            a[i] = random.nextFloat();
            b[i] = random.nextFloat();
        }

        long minNs = measureMinNs(() -> RafaeliaCore.matrixMultiply(a, b, result, size, size, size));
        assertWithinGuardrail(
                "matrixMultiply",
                minNs,
                RafaeliaBenchmarkBaselines.MATRIX_MULTIPLY_BASELINE_NS
        );
    }

    @Test
    public void guardrailVectorAdd() {
        int length = RafaeliaBenchmarkBaselines.VECTOR_LENGTH;
        float[] a = new float[length];
        float[] b = new float[length];
        float[] result = new float[length];
        Random random = new Random(RafaeliaBenchmarkBaselines.RANDOM_SEED);

        for (int i = 0; i < length; i++) {
            a[i] = random.nextFloat();
            b[i] = random.nextFloat();
        }

        long minNs = measureMinNs(() -> RafaeliaCore.vectorAdd(a, b, result, length));
        assertWithinGuardrail(
                "vectorAdd",
                minNs,
                RafaeliaBenchmarkBaselines.VECTOR_ADD_BASELINE_NS
        );
    }

    @Test
    public void guardrailVectorMultiply() {
        int length = RafaeliaBenchmarkBaselines.VECTOR_LENGTH;
        float[] a = new float[length];
        float[] b = new float[length];
        float[] result = new float[length];
        Random random = new Random(RafaeliaBenchmarkBaselines.RANDOM_SEED);

        for (int i = 0; i < length; i++) {
            a[i] = random.nextFloat();
            b[i] = random.nextFloat();
        }

        long minNs = measureMinNs(() -> RafaeliaCore.vectorMultiply(a, b, result, length));
        assertWithinGuardrail(
                "vectorMultiply",
                minNs,
                RafaeliaBenchmarkBaselines.VECTOR_MULTIPLY_BASELINE_NS
        );
    }

    @Test
    public void guardrailMemoryCopy() {
        int bytes = RafaeliaBenchmarkBaselines.MEMORY_COPY_BYTES;
        ByteBuffer src = ByteBuffer.allocateDirect(bytes);
        ByteBuffer dst = ByteBuffer.allocateDirect(bytes);
        Random random = new Random(RafaeliaBenchmarkBaselines.RANDOM_SEED);

        for (int i = 0; i < bytes; i++) {
            src.put(i, (byte) random.nextInt());
        }

        long minNs = measureMinNs(() -> RafaeliaCore.optimizedMemoryCopy(src, 0, dst, 0, bytes));
        assertWithinGuardrail(
                "memoryCopy",
                minNs,
                RafaeliaBenchmarkBaselines.MEMORY_COPY_BASELINE_NS
        );
    }

    private static long measureMinNs(Workload workload) {
        int warmupIterations = 5;
        int measurementIterations = 15;
        for (int i = 0; i < warmupIterations; i++) {
            workload.run();
        }

        long minNs = Long.MAX_VALUE;
        for (int i = 0; i < measurementIterations; i++) {
            long start = System.nanoTime();
            workload.run();
            long duration = System.nanoTime() - start;
            if (duration < minNs) {
                minNs = duration;
            }
        }
        return minNs;
    }

    private static void assertWithinGuardrail(String name, long observedNs, long baselineNs) {
        double threshold = baselineNs * RafaeliaBenchmarkBaselines.MAX_REGRESSION_RATIO;
        boolean within = observedNs <= threshold;
        String message = String.format(
                "%s: observed=%.3fms baseline=%.3fms threshold=%.3fms",
                name,
                observedNs / 1_000_000.0,
                baselineNs / 1_000_000.0,
                threshold / 1_000_000.0
        );

        if (within) {
            Log.i(TAG, "PASS " + message);
            return;
        }

        Log.w(TAG, "REGRESSION " + message);
        if (shouldEnforceGuardrails()) {
            fail("Guardrail regression detected: " + message);
        }
    }

    private static boolean shouldEnforceGuardrails() {
        Bundle arguments = InstrumentationRegistry.getArguments();
        return Boolean.parseBoolean(arguments.getString(ENFORCE_ARG, "false"));
    }

    private interface Workload {
        void run();
    }
}
