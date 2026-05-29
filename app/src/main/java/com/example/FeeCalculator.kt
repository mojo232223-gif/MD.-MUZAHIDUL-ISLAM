package com.example

object FeeCalculator {
    // Standard conversions
    const val INCH_TO_CM = 2.54
    const val LBS_TO_KG = 0.45359237
    const val OZ_TO_LBS = 1.0 / 16.0
    const val GRAMS_TO_KG = 1.0 / 1000.0

    enum class LengthUnit(val label: String) { INCHES("inches"), CM("cm") }
    enum class WeightUnit(val label: String) { OZ("oz"), LBS("lbs"), GRAMS("grams"), KG("kg") }

    data class CalculationResult(
        val marketplace: Marketplace,
        val nativeL: Double,
        val nativeW: Double,
        val nativeH: Double,
        val nativeActualWeight: Double,
        val nativeDimWeight: Double,
        val weightUsedForFee: Double,
        val detectedTier: SizeTier,
        val fbaFee: Double,
        val feeBreakdown: String,
        val rateCardVersion: String = FbaRateCard.VERSION_INFO
    )

    /**
     * Conducts FBA Fee Calculation following normalized, accurate steps.
     */
    fun calculateFee(
        l: Double,
        w: Double,
        h: Double,
        weightValue: Double,
        lUnit: LengthUnit,
        wUnit: WeightUnit,
        marketplace: Marketplace
    ): CalculationResult {
        // Step 1: Unit Normalisation
        val nativeL = normalizeLength(l, lUnit, marketplace.isMetric)
        val nativeW = normalizeLength(w, lUnit, marketplace.isMetric)
        val nativeH = normalizeLength(h, lUnit, marketplace.isMetric)
        val nativeActualWeight = normalizeWeight(weightValue, wUnit, marketplace.isMetric)

        // Step 2: Dimensional Weight
        val nativeDimWeight = if (marketplace.isMetric) {
            // UK/EU: (L x W x H) / 5000 (gives dim weight in kg)
            (nativeL * nativeW * nativeH) / 5000.0
        } else {
            // USA/Canada: (L x W x H) / 139 (gives dim weight in lbs)
            (nativeL * nativeW * nativeH) / 139.0
        }

        // According to Amazon rules, dimensional weight is used if it exceeds actual weight,
        // typically only applied to Large Standard / standard parcel and above.
        val detectedTier = SizeTierEngine.classify(nativeL, nativeW, nativeH, nativeActualWeight, marketplace)

        // Dimensional weight starts on Large Standard / Parcel
        val isDimWeightApplicable = when (marketplace) {
            Marketplace.USA -> detectedTier.id != "small_std"
            Marketplace.CANADA -> detectedTier.id != "envelope"
            else -> detectedTier.id != "small_env" && detectedTier.id != "std_env" && detectedTier.id != "large_env"
        }

        val weightUsedForFee = if (isDimWeightApplicable) {
            maxOf(nativeActualWeight, nativeDimWeight)
        } else {
            nativeActualWeight
        }

        // Step 3: Fee lookup
        val fbaFee = lookupFee(detectedTier, weightUsedForFee, marketplace)
        val feeBreakdown = generateBreakdown(detectedTier, weightUsedForFee, fbaFee, marketplace)

        return CalculationResult(
            marketplace = marketplace,
            nativeL = nativeL,
            nativeW = nativeW,
            nativeH = nativeH,
            nativeActualWeight = nativeActualWeight,
            nativeDimWeight = nativeDimWeight,
            weightUsedForFee = weightUsedForFee,
            detectedTier = detectedTier,
            fbaFee = fbaFee,
            feeBreakdown = feeBreakdown
        )
    }

    private fun normalizeLength(value: Double, unit: LengthUnit, targetMetric: Boolean): Double {
        if (targetMetric && unit == LengthUnit.CM) return value
        if (!targetMetric && unit == LengthUnit.INCHES) return value

        return if (targetMetric) {
            // Target CM
            value * INCH_TO_CM
        } else {
            // Target Inches
            value / INCH_TO_CM
        }
    }

    private fun normalizeWeight(value: Double, unit: WeightUnit, targetMetric: Boolean): Double {
        if (targetMetric && unit == WeightUnit.KG) return value
        if (targetMetric && unit == WeightUnit.GRAMS) return value * GRAMS_TO_KG
        if (!targetMetric && unit == WeightUnit.LBS) return value
        if (!targetMetric && unit == WeightUnit.OZ) return value * OZ_TO_LBS

        // Mix conversion (Metric <-> Imperial) if needed:
        val kgValue = when (unit) {
            WeightUnit.OZ -> value * OZ_TO_LBS * LBS_TO_KG
            WeightUnit.LBS -> value * LBS_TO_KG
            WeightUnit.GRAMS -> value * GRAMS_TO_KG
            WeightUnit.KG -> value
        }

        return if (targetMetric) {
            kgValue
        } else {
            kgValue / LBS_TO_KG
        }
    }

    private fun lookupFee(tier: SizeTier, weight: Double, marketplace: Marketplace): Double {
        return when (marketplace) {
            Marketplace.USA -> lookupFeeUS(tier.id, weight)
            Marketplace.UK -> lookupFeeUK(tier.id, weight)
            Marketplace.GERMANY -> lookupFeeDE(tier.id, weight)
            Marketplace.FRANCE -> lookupFeeFR(tier.id, weight)
            Marketplace.ITALY -> lookupFeeIT(tier.id, weight)
            Marketplace.SPAIN -> lookupFeeES(tier.id, weight)
            Marketplace.CANADA -> lookupFeeCA(tier.id, weight)
        }
    }

    private fun lookupFeeUS(tierId: String, weight: Double): Double {
        return when (tierId) {
            "small_std" -> {
                // weight in lbs (16 oz = 1 lb)
                val oz = weight * 16.0
                when {
                    oz <= 2.0 -> 3.06
                    oz <= 4.0 -> 3.15
                    oz <= 8.0 -> 3.25
                    oz <= 12.0 -> 3.45
                    else -> 3.65
                }
            }
            "large_std" -> {
                val oz = weight * 16.0
                when {
                    oz <= 4.0 -> 3.68
                    oz <= 8.0 -> 3.90
                    oz <= 12.0 -> 4.15
                    oz <= 16.0 -> 4.55
                    weight <= 1.5 -> 4.85
                    weight <= 2.0 -> 5.05
                    weight <= 3.0 -> 5.32
                    else -> {
                        // $5.32 + $0.08 per 0.5 lb (or fraction thereof) over 3 lbs
                        val extraWeight = maxOf(0.0, weight - 3.0)
                        val halfLbsHeight = kotlin.math.ceil(extraWeight / 0.5)
                        5.32 + (halfLbsHeight * 0.08)
                    }
                }
            }
            "large_bulky" -> {
                // $9.61 base + $0.38 per lb over 1 lb
                val basis = maxOf(0.0, weight - 1.0)
                val ceilLbs = kotlin.math.ceil(basis)
                9.61 + (ceilLbs * 0.38)
            }
            else -> { // extra_large
                // $26.33 base + $0.38 per lb over 1 lb
                val basis = maxOf(0.0, weight - 1.0)
                val ceilLbs = kotlin.math.ceil(basis)
                26.33 + (ceilLbs * 0.38)
            }
        }
    }

    private fun lookupFeeUK(tierId: String, weight: Double): Double {
        val grams = weight * 1000.0
        return when (tierId) {
            "small_env" -> 1.58
            "std_env" -> 1.72
            "large_env" -> 2.01
            "small_parcel" -> 2.70
            "std_parcel" -> {
                when {
                    grams <= 150 -> 3.40
                    grams <= 400 -> 3.60
                    grams <= 900 -> 3.90
                    weight <= 1.4 -> 4.25
                    weight <= 1.9 -> 4.50
                    weight <= 2.9 -> 4.85
                    else -> 5.20
                }
            }
            "large_parcel" -> {
                // £6.90 + £0.13 per kg over 1 kg
                val extraKg = maxOf(0.0, weight - 1.0)
                val ceilKg = kotlin.math.ceil(extraKg)
                6.90 + (ceilKg * 0.13)
            }
            else -> { // oversize
                // £9.50 + £0.15 per kg over 1 kg
                val extraKg = maxOf(0.0, weight - 1.0)
                val ceilKg = kotlin.math.ceil(extraKg)
                9.55 + (ceilKg * 0.15)
            }
        }
    }

    private fun lookupFeeDE(tierId: String, weight: Double): Double {
        val grams = weight * 1000.0
        return when (tierId) {
            "small_env" -> 1.95
            "std_env" -> 2.15
            "large_env" -> 2.50
            "small_parcel" -> 3.30
            "std_parcel" -> {
                when {
                    grams <= 150 -> 4.40
                    grams <= 400 -> 4.70
                    grams <= 900 -> 5.10
                    weight <= 1.4 -> 5.45
                    weight <= 1.9 -> 5.75
                    weight <= 2.9 -> 5.95
                    else -> 6.20
                }
            }
            "large_parcel" -> {
                val extraKg = maxOf(0.0, weight - 1.0)
                val ceilKg = kotlin.math.ceil(extraKg)
                8.20 + (ceilKg * 0.15)
            }
            else -> {
                val extraKg = maxOf(0.0, weight - 1.0)
                val ceilKg = kotlin.math.ceil(extraKg)
                11.00 + (ceilKg * 0.18)
            }
        }
    }

    private fun lookupFeeFR(tierId: String, weight: Double): Double {
        val grams = weight * 1000.0
        return when (tierId) {
            "small_env" -> 2.20
            "std_env" -> 2.40
            "large_env" -> 2.80
            "small_parcel" -> 3.90
            "std_parcel" -> {
                when {
                    grams <= 150 -> 4.80
                    grams <= 400 -> 5.10
                    grams <= 900 -> 5.50
                    weight <= 1.4 -> 6.00
                    weight <= 1.9 -> 6.40
                    weight <= 2.9 -> 6.65
                    else -> 6.90
                }
            }
            "large_parcel" -> {
                val extraKg = maxOf(0.0, weight - 1.0)
                val ceilKg = kotlin.math.ceil(extraKg)
                9.50 + (ceilKg * 0.18)
            }
            else -> {
                val extraKg = maxOf(0.0, weight - 1.0)
                val ceilKg = kotlin.math.ceil(extraKg)
                12.50 + (ceilKg * 0.20)
            }
        }
    }

    private fun lookupFeeIT(tierId: String, weight: Double): Double {
        val grams = weight * 1000.0
        return when (tierId) {
            "small_env" -> 2.10
            "std_env" -> 2.30
            "large_env" -> 2.65
            "small_parcel" -> 3.70
            "std_parcel" -> {
                when {
                    grams <= 150 -> 4.60
                    grams <= 400 -> 4.90
                    grams <= 900 -> 5.30
                    weight <= 1.4 -> 5.75
                    weight <= 1.9 -> 6.15
                    weight <= 2.9 -> 6.40
                    else -> 6.60
                }
            }
            "large_parcel" -> {
                val extraKg = maxOf(0.0, weight - 1.0)
                val ceilKg = kotlin.math.ceil(extraKg)
                9.00 + (ceilKg * 0.16)
            }
            else -> {
                val extraKg = maxOf(0.0, weight - 1.0)
                val ceilKg = kotlin.math.ceil(extraKg)
                11.80 + (ceilKg * 0.18)
            }
        }
    }

    private fun lookupFeeES(tierId: String, weight: Double): Double {
        return lookupFeeIT(tierId, weight)
    }

    private fun lookupFeeCA(tierId: String, weight: Double): Double {
        val grams = weight * 453.59237
        return when (tierId) {
            "envelope" -> {
                when {
                    grams <= 100 -> 3.35
                    grams <= 250 -> 3.65
                    else -> 3.95
                }
            }
            "standard_parcel" -> {
                when {
                    weight <= 1.0 -> 5.65
                    weight <= 2.0 -> 6.45
                    weight <= 3.0 -> 7.15
                    weight <= 5.0 -> 7.85
                    else -> {
                        val extraWeight = maxOf(0.0, weight - 5.0)
                        val ceilLbs = kotlin.math.ceil(extraWeight)
                        7.85 + (ceilLbs * 0.45)
                    }
                }
            }
            else -> {
                val extraWeight = maxOf(0.0, weight - 1.0)
                val ceilLbs = kotlin.math.ceil(extraWeight)
                16.50 + (ceilLbs * 0.45)
            }
        }
    }

    private fun generateBreakdown(
        tier: SizeTier,
        weightUsed: Double,
        fee: Double,
        marketplace: Marketplace
    ): String {
        val wUnit = if (marketplace.isMetric) "kg" else "lbs"
        return when (marketplace) {
            Marketplace.USA -> {
                if (tier.id == "small_std") "Base-tier envelope rate card"
                else "Weight used: %.2f %s".format(weightUsed, wUnit)
            }
            Marketplace.CANADA -> {
                if (tier.id == "envelope") "Envelope flat rate card"
                else "Weight used: %.2f %s".format(weightUsed, wUnit)
            }
            else -> {
                if (tier.id.contains("env")) "Envelope standard rate card"
                else "Parcel weight used: %.2f %s".format(weightUsed, wUnit)
            }
        }
    }
}
