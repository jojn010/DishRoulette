package com.example.dishroulette.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class NutritionEstimateTest {
    @Test public void allowsZeroProteinAndReasonableEstimates() {
        assertTrue(NutritionEstimate.valid(210, 12.5));
        assertTrue(NutritionEstimate.valid(100, 0.0));
    }
    @Test public void rejectsPartialNonfiniteAndOutOfRangeEstimates() {
        assertFalse(NutritionEstimate.valid(null, 10.0));
        assertFalse(NutritionEstimate.valid(100, null));
        for (int calories : new int[]{0, -1, 5001}) assertFalse(NutritionEstimate.valid(calories, 1.0));
        for (double protein : new double[]{-1, 501, Double.NaN, Double.POSITIVE_INFINITY})
            assertFalse(NutritionEstimate.valid(100, protein));
    }
}
