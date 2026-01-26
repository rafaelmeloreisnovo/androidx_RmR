/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 with Additional Restrictions.
 * See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.
 */

package androidx.rmr.rafaelia.benchmark;

import androidx.benchmark.junit4.BenchmarkRule;
import androidx.benchmark.BenchmarkState;
import androidx.rmr.rafaelia.RafaeliaCore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Random;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RafaeliaCoreBenchmark {

    @Rule
    public final BenchmarkRule benchmarkRule = new BenchmarkRule();

    @Test
    public void benchmarkMatrixMultiply() {
        int size = RafaeliaBenchmarkBaselines.MATRIX_SIZE;
        float[] a = new float[size * size];
        float[] b = new float[size * size];
        float[] result = new float[size * size];
        Random random = new Random(RafaeliaBenchmarkBaselines.RANDOM_SEED);

        for (int i = 0; i < size * size; i++) {
            a[i] = random.nextFloat();
            b[i] = random.nextFloat();
        }

        BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            RafaeliaCore.matrixMultiply(a, b, result, size, size, size);
        }
    }

    @Test
    public void benchmarkVectorAdd() {
        int length = RafaeliaBenchmarkBaselines.VECTOR_LENGTH;
        float[] a = new float[length];
        float[] b = new float[length];
        float[] result = new float[length];
        Random random = new Random(RafaeliaBenchmarkBaselines.RANDOM_SEED);

        for (int i = 0; i < length; i++) {
            a[i] = random.nextFloat();
            b[i] = random.nextFloat();
        }

        BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            RafaeliaCore.vectorAdd(a, b, result, length);
        }
    }

    @Test
    public void benchmarkVectorMultiply() {
        int length = RafaeliaBenchmarkBaselines.VECTOR_LENGTH;
        float[] a = new float[length];
        float[] b = new float[length];
        float[] result = new float[length];
        Random random = new Random(RafaeliaBenchmarkBaselines.RANDOM_SEED);

        for (int i = 0; i < length; i++) {
            a[i] = random.nextFloat();
            b[i] = random.nextFloat();
        }

        BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            RafaeliaCore.vectorMultiply(a, b, result, length);
        }
    }

    @Test
    public void benchmarkMemoryCopy() {
        int bytes = RafaeliaBenchmarkBaselines.MEMORY_COPY_BYTES;
        ByteBuffer src = ByteBuffer.allocateDirect(bytes);
        ByteBuffer dst = ByteBuffer.allocateDirect(bytes);
        Random random = new Random(RafaeliaBenchmarkBaselines.RANDOM_SEED);

        for (int i = 0; i < bytes; i++) {
            src.put(i, (byte) random.nextInt());
        }

        BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            RafaeliaCore.optimizedMemoryCopy(src, 0, dst, 0, bytes);
        }
    }
}
