package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FbaRateCard
import com.example.FeeCalculator

@Composable
fun ResultScreen(
    result: FeeCalculator.CalculationResult,
    onRecalculate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isMetric = result.marketplace.isMetric
    val wUnit = if (isMetric) "kg" else "lbs"
    val lUnit = if (isMetric) "cm" else "in"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .safeDrawingPadding() // protects notches / camera cuts
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- HEADER WITH BACK BUTTON ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRecalculate,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF333333), shape = RoundedCornerShape(12.dp))
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Input",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Calculation Result",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Real-time fee breakdown",
                        fontSize = 12.sp,
                        color = Color(0xFF938F99)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- HERO FEE DISPLAY CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BoxBorder(Color(0xFF4D2F00))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2D1600), // Rich dark amber base
                                    Color(0xFF1E1E1E)
                                )
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ESTIMATED FBA FEE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9900),
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val formattedFee = "%.2f".format(result.fbaFee)
                        Text(
                            text = "${result.marketplace.currencySymbol}$formattedFee",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.testTag("fee_result_text")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = result.feeBreakdown,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFFFC166),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- MAIN SPECIFICATIONS CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = BoxBorder(Color(0xFF333333))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "PRODUCT METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF938F99),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Marketplace Row
                    SpecRow(
                        label = "Marketplace",
                        value = "${result.marketplace.flagEmoji} ${result.marketplace.displayName} (${result.marketplace.countryCode})"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF333333))

                    // Detected Size Tier Row
                    SpecRow(
                        label = "Size classification",
                        value = result.detectedTier.name,
                        valueColor = Color(0xFFFF9900),
                        valueFontWeight = FontWeight.Bold,
                        testTag = "detected_tier_text"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF333333))

                    // Normalized Dimensions Row
                    val nativeDimString = "%.2f x %.2f x %.2f %s".format(
                        result.nativeL, result.nativeW, result.nativeH, lUnit
                    )
                    SpecRow(label = "Dimensions in $lUnit", value = nativeDimString)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF333333))

                    // Weights comparison: Actual weight vs Dimensional weight
                    val actualWString = "%.2f %s".format(result.nativeActualWeight, wUnit)
                    val dimWString = "%.2f %s".format(result.nativeDimWeight, wUnit)

                    val isDimWeightHeavier = result.nativeDimWeight > result.nativeActualWeight

                    SpecRow(
                        label = "Actual weight",
                        value = actualWString,
                        valueFontWeight = if (!isDimWeightHeavier) FontWeight.Bold else FontWeight.Normal
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF333333))

                    SpecRow(
                        label = "Dimensional weight",
                        value = dimWString,
                        valueFontWeight = if (isDimWeightHeavier) FontWeight.Bold else FontWeight.Normal
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF333333))

                    // Final weight applied
                    SpecRow(
                        label = "Fulfillment fee weight",
                        value = "%.2f %s".format(result.weightUsedForFee, wUnit),
                        valueColor = Color(0xFFFF9900),
                        valueFontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LEGAL / RATE CARD INFO BANNER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2D1600))
                    .border(1.dp, Color(0xFF4D2F00), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Disclaimer",
                        tint = Color(0xFFFF9900),
                        modifier = Modifier.size(18.dp)
                    )

                    Column {
                        Text(
                            text = "Rate Card: ${FbaRateCard.VERSION_INFO}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC166)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Estimates reflect the official Amazon 2026 fee structure. Recommended to verify exact fulfillment rates inside Seller Central.",
                            fontSize = 11.sp,
                            color = Color(0xFFFFC166).copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- RECALCULATE / RESET ACTION BUTTON ---
            Button(
                onClick = onRecalculate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("recalculate_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9900),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Black
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Recalculate Fee",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    fontSize = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Powered footer text
            Text(
                text = "POWERED BY SELLERENGINE 2026",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFF4A4458),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun SpecRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFE6E1E5),
    valueFontWeight: FontWeight = FontWeight.Medium,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF938F99)
        )

        val modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = valueFontWeight,
            color = valueColor,
            modifier = modifier,
            textAlign = TextAlign.End
        )
    }
}

// Helpers for Border
@Composable
private fun BoxBorder(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
