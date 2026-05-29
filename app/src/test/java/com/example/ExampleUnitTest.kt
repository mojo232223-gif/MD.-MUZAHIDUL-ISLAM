package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUsaSmallStandardFee() {
        // Under Small Standard size tier: ≤ 15x12x0.75 in, ≤ 16 oz (1 lb)
        // Let's input a small item of 4 oz (0.25 lb)
        val result = FeeCalculator.calculateFee(
            l = 10.0,
            w = 8.0,
            h = 0.5,
            weightValue = 4.0,
            lUnit = FeeCalculator.LengthUnit.INCHES,
            wUnit = FeeCalculator.WeightUnit.OZ,
            marketplace = Marketplace.USA
        )

        assertEquals("small_std", result.detectedTier.id)
        // US Small Standard <= 4 oz is $3.15
        assertEquals(3.15, result.fbaFee, 0.001)
    }

    @Test
    fun testUsaLargeStandardFee() {
        // Large Standard size tier: ≤ 18x14x8 in, ≤ 20 lbs
        // Use small dimensions so dimensional weight doesn't exceed actual weight (5x5x2 = 50, dim wt = 50/139 = 0.36 lbs)
        val result = FeeCalculator.calculateFee(
            l = 8.0,
            w = 5.0,
            h = 1.0,
            weightValue = 2.0,
            lUnit = FeeCalculator.LengthUnit.INCHES,
            wUnit = FeeCalculator.WeightUnit.LBS,
            marketplace = Marketplace.USA
        )

        assertEquals("large_std", result.detectedTier.id)
        // US Large Standard for 2 lbs is $5.05
        assertEquals(5.05, result.fbaFee, 0.001)
    }

    @Test
    fun testUsaLargeStandardDimWeightFee() {
        // Test same weight but with bulky/elongated dimensions so dimensional weight exceeds actual weight.
        // Vol = 12 x 10 x 4 = 480 cubic inches. Dim wt = 480 / 139 = 3.45 lbs.
        val result = FeeCalculator.calculateFee(
            l = 12.0,
            w = 10.0,
            h = 4.0,
            weightValue = 2.0,
            lUnit = FeeCalculator.LengthUnit.INCHES,
            wUnit = FeeCalculator.WeightUnit.LBS,
            marketplace = Marketplace.USA
        )

        assertEquals("large_std", result.detectedTier.id)
        // Greater weight used is dim weight (3.45 lbs), which falls into > 3 lbs category:
        // $5.32 + $0.08 per 0.5 lb over 3 lbs = $5.32 + 1 * $0.08 = $5.40
        assertEquals(5.40, result.fbaFee, 0.001)
    }

    @Test
    fun testUkSmallEnvelopeFee() {
        // Small Envelope UK size tier: ≤ 16x11.4x0.9 cm, ≤ 100g
        val result = FeeCalculator.calculateFee(
            l = 15.0,
            w = 10.0,
            h = 0.5,
            weightValue = 80.0,
            lUnit = FeeCalculator.LengthUnit.CM,
            wUnit = FeeCalculator.WeightUnit.GRAMS,
            marketplace = Marketplace.UK
        )

        assertEquals("small_env", result.detectedTier.id)
        assertEquals(1.58, result.fbaFee, 0.001)
    }
}
