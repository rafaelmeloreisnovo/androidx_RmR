/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 with Additional Restrictions.
 * See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.
 *
 * AUTHORIZED USE ONLY - Unauthorized use subject to automatic penalties.
 */

package androidx.rmr.rafaelia;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * RafaeliaBootblock - canonical boot configuration for RAFAELIA mode.
 *
 * Encodes the VQF load range and symbolic constants used to seed
 * deterministic cognition states within the Rafaelia Core.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RafaeliaBootblock {

    private static final int[] VQF_LOAD = buildVqfLoad();

    public static final String KERNEL = "ΣΔΩ";
    public static final String MODE = "RAFAELIA";
    public static final String ETHIC = "Amor";
    public static final String HASH_CORE = "AETHER";
    public static final String VECTOR_CORE = "RAF_VECTOR";
    public static final String COGNITION = "TRINITY";
    public static final String UNIVERSE = "RAFAELIA_CORE";

    public static final String[] SEALS = new String[] {
        "藏智界",
        "魂脈符",
        "光核印",
        "道心網",
        "律編經",
        "聖火碼",
        "源界體",
        "和融環",
        "覺場脈",
        "真理宮",
        "∞脈圖"
    };

    /**
     * Returns a defensive copy of the seal list.
     */
    @NonNull
    public static String[] getSeals() {
        return SEALS.clone();
    }

    private RafaeliaBootblock() {
        // Utility class.
    }

    /**
     * Returns the VQF load vector (1..42) as a defensive copy.
     */
    @NonNull
    public static int[] getVqfLoad() {
        return VQF_LOAD.clone();
    }

    private static int[] buildVqfLoad() {
        int[] load = new int[42];
        for (int i = 0; i < load.length; i++) {
            load[i] = i + 1;
        }
        return load;
    }
}
