package com.example

object SizeTierEngine {

    /**
     * Classifies the package dimensions (length, width, height) and weight into the correct Amazon FBA Size Tier.
     * Dimensions (dim1, dim2, dim3) can be inputted in any order - they will be sorted internally.
     * Weight must be in native weight units (lbs for USA/Canada, kg for UK/Europe).
     * Dimensions must be in native length units (inches for USA/Canada, cm for UK/Europe).
     */
    fun classify(
        dim1: Double,
        dim2: Double,
        dim3: Double,
        nativeWeight: Double,
        marketplace: Marketplace
    ): SizeTier {
        // Amazon sorts dimensions: largest side is Length, median side is Width, smallest side is Height
        val sortedDims = listOf(dim1, dim2, dim3).sortedDescending()
        val length = sortedDims[0]
        val width = sortedDims[1]
        val height = sortedDims[2]

        val tiers = FbaRateCard.getTiersFor(marketplace)

        // Iterate through sorted tiers to find the first one that fits
        for (tier in tiers) {
            val fitsDims = length <= tier.maxL && width <= tier.maxW && height <= tier.maxH
            val fitsWeight = nativeWeight <= tier.maxWeight

            if (fitsDims && fitsWeight) {
                return tier
            }
        }

        // Return the largest/fallback tier
        return tiers.last()
    }
}
