package com.example

enum class Marketplace(
    val countryCode: String,
    val displayName: String,
    val currencySymbol: String,
    val flagEmoji: String,
    val isMetric: Boolean // UK and EU are metric (cm, grams/kg), US and CA are imperial (inches, oz/lbs)
) {
    USA("US", "United States", "$", "🇺🇸", isMetric = false),
    UK("GB", "United Kingdom", "£", "🇬🇧", isMetric = true),
    GERMANY("DE", "Germany", "€", "🇩🇪", isMetric = true),
    FRANCE("FR", "France", "€", "🇫🇷", isMetric = true),
    ITALY("IT", "Italy", "€", "🇮🇹", isMetric = true),
    SPAIN("ES", "Spain", "€", "🇪🇸", isMetric = true),
    CANADA("CA", "Canada", "C$", "🇨🇦", isMetric = false)
}

/**
 * Representation of a configured Size Tier bounds.
 * Dimensions are sorted internally (Length as largest, Width as second, Height as smallest).
 */
data class SizeTier(
    val id: String,
    val name: String,
    val maxL: Double, // largest dimension
    val maxW: Double, // median dimension
    val maxH: Double, // smallest dimension
    val maxWeight: Double, // in native weight unit (lbs for US/CA, kg for UK/EU)
)

object FbaRateCard {
    const val VERSION_INFO = "Rates based on Amazon 2026 fee schedule (effective early 2026/late 2025)."

    // --- USA SIZE TIERS (Native units: inches, lbs) ---
    // Note: weight limits in lbs. 16 oz = 1 lb.
    val usTiers = listOf(
        SizeTier("small_std", "Small Standard", 15.0, 12.0, 0.75, 1.0),
        SizeTier("large_std", "Large Standard", 18.0, 14.0, 8.0, 20.0),
        SizeTier("large_bulky", "Large Bulky", 59.0, 33.0, 33.0, 50.0),
        SizeTier("extra_large", "Extra-Large", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
    )

    // --- CANADA SIZE TIERS (We treat in native units: inches, lbs) ---
    val caTiers = listOf(
        SizeTier("envelope", "Envelope", 15.0, 10.6, 0.8, 1.1),
        SizeTier("standard_parcel", "Standard Parcel", 18.0, 14.0, 8.0, 20.0),
        SizeTier("oversize", "Oversize / Bulky", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
    )

    // --- UK/EU SIZE TIERS (Native units: cm, kg) ---
    val ukTiers = listOf(
        SizeTier("small_env", "Small Envelope", 16.0, 11.4, 0.9, 0.1),
        SizeTier("std_env", "Standard Envelope", 33.0, 22.9, 2.4, 0.1),
        SizeTier("large_env", "Large Envelope", 33.0, 22.9, 2.4, 0.25),
        SizeTier("small_parcel", "Small Parcel", 33.0, 23.0, 5.0, 1.0),
        SizeTier("std_parcel", "Standard Parcel", 45.0, 34.0, 26.0, 12.0),
        SizeTier("large_parcel", "Large Parcel", 61.0, 46.0, 46.0, 30.0),
        SizeTier("oversize", "Oversize / Bulky", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
    )

    // Germany uses similar envelopes and parcels
    val deTiers = ukTiers
    val frTiers = ukTiers
    val itTiers = ukTiers
    val esTiers = ukTiers

    fun getTiersFor(marketplace: Marketplace): List<SizeTier> {
        return when (marketplace) {
            Marketplace.USA -> usTiers
            Marketplace.CANADA -> caTiers
            Marketplace.UK -> ukTiers
            Marketplace.GERMANY -> deTiers
            Marketplace.FRANCE -> frTiers
            Marketplace.ITALY -> itTiers
            Marketplace.SPAIN -> esTiers
        }
    }
}
