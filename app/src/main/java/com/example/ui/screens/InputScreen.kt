package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.FbaCalculatorViewModel
import com.example.FeeCalculator
import com.example.Marketplace

@Composable
fun InputScreen(
    viewModel: FbaCalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val marketplace by viewModel.marketplace.collectAsState()
    val length by viewModel.length.collectAsState()
    val width by viewModel.width.collectAsState()
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val lengthUnit by viewModel.lengthUnit.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()

    val lengthError by viewModel.lengthError.collectAsState()
    val widthError by viewModel.widthError.collectAsState()
    val heightError by viewModel.heightError.collectAsState()
    val weightError by viewModel.weightError.collectAsState()

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Get live calculation size tier
    val liveEstimatedTier = viewModel.getLiveEstimatedTier()

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
                .safeDrawingPadding() // respects notches / camera cuts beautifully
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- ELEGANT HEADER ---
            HeaderSection()

            Spacer(modifier = Modifier.height(16.dp))

            // --- MARKETPLACE FLAGS ROW ---
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("marketplace_selector")
                    .padding(vertical = 4.dp)
            ) {
                items(Marketplace.values()) { market ->
                    MarketplaceItem(
                        marketplace = market,
                        isSelected = market == marketplace,
                        onClick = { viewModel.onMarketplaceSelected(market) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- PRODUCT DIMENSIONS CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = BoxBorder(Color(0xFF333333))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Product Dimensions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE6E1E5)
                        )

                        // Segmented control (IN vs CM)
                        CustomSegmentedControl(
                            selected = if (lengthUnit == FeeCalculator.LengthUnit.INCHES) "IN" else "CM",
                            options = listOf("IN", "CM"),
                            onSelectedChange = { option ->
                                val unit = if (option == "IN") FeeCalculator.LengthUnit.INCHES else FeeCalculator.LengthUnit.CM
                                viewModel.onLengthUnitChanged(unit)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Side-by-Side 3 Column Grid Inputs for Length, Width, Height
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CenteredCellInput(
                            label = "Length",
                            value = length,
                            onValueChange = viewModel::onLengthChanged,
                            error = lengthError,
                            modifier = Modifier.weight(1f),
                            testTag = "length_input",
                            keyAction = ImeAction.Next,
                            onKeyAction = { focusManager.moveFocus(FocusDirection.Right) }
                        )

                        CenteredCellInput(
                            label = "Width",
                            value = width,
                            onValueChange = viewModel::onWidthChanged,
                            error = widthError,
                            modifier = Modifier.weight(1f),
                            testTag = "width_input",
                            keyAction = ImeAction.Next,
                            onKeyAction = { focusManager.moveFocus(FocusDirection.Right) }
                        )

                        CenteredCellInput(
                            label = "Height",
                            value = height,
                            onValueChange = viewModel::onHeightChanged,
                            error = heightError,
                            modifier = Modifier.weight(1f),
                            testTag = "height_input",
                            keyAction = ImeAction.Next,
                            onKeyAction = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- UNIT WEIGHT CARD with LIVE ESTIMATION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = BoxBorder(Color(0xFF333333))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unit Weight",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE6E1E5)
                        )

                        // Segmented control (OZ vs LB vs G vs KG)
                        CustomSegmentedControl(
                            selected = when (weightUnit) {
                                FeeCalculator.WeightUnit.OZ -> "OZ"
                                FeeCalculator.WeightUnit.LBS -> "LB"
                                FeeCalculator.WeightUnit.GRAMS -> "G"
                                FeeCalculator.WeightUnit.KG -> "KG"
                            },
                            options = listOf("OZ", "LB", "G", "KG"),
                            onSelectedChange = { option ->
                                val unit = when (option) {
                                    "OZ" -> FeeCalculator.WeightUnit.OZ
                                    "LB" -> FeeCalculator.WeightUnit.LBS
                                    "G" -> FeeCalculator.WeightUnit.GRAMS
                                    else -> FeeCalculator.WeightUnit.KG
                                }
                                viewModel.onWeightUnitChanged(unit)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Side-by-Side row containing weight input and Estimated Tier indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = viewModel::onWeightChanged,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("weight_input"),
                            placeholder = { Text("0.0", color = Color(0xFF4A4458)) },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onAny = {
                                    focusManager.clearFocus()
                                    viewModel.calculate()
                                }
                            ),
                            isError = weightError != null,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F0F0F),
                                unfocusedContainerColor = Color(0xFF0F0F0F),
                                focusedBorderColor = Color(0xFFFF9900),
                                unfocusedBorderColor = Color(0xFF4A4458),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                cursorColor = Color(0xFFFF9900)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(0.8f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "ESTIMATED TIER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFF938F99)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = liveEstimatedTier,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF9900),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    if (weightError != null) {
                        Text(
                            text = weightError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .testTag("weight_input_error")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- AMBER ALERT / DIVISOR BANNER ---
            DivisorAlertBanner(marketplace = marketplace)

            Spacer(modifier = Modifier.height(24.dp))

            // --- SUBMIT ACTION BUTTON ---
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.calculate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("calculate_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9900),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Calculate FBA Fee",
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
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "FBA Calculator",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Amazon 2026 Rate Card v2.2",
                fontSize = 12.sp,
                color = Color(0xFF938F99)
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF1E1E1E), shape = CircleShape)
                .border(1.dp, Color(0xFF333333), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⚙️", fontSize = 16.sp)
        }
    }
}

@Composable
fun MarketplaceItem(
    marketplace: Marketplace,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFFFF9900) else Color(0xFF1E1E1E)
    val text = if (isSelected) Color.Black else Color.White
    val borderCol = if (isSelected) Color(0xFFFF9900) else Color(0xFF333333)

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("marketplace_tab_${marketplace.countryCode.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = marketplace.flagEmoji, fontSize = 16.sp)
            Text(
                text = marketplace.countryCode,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = text
            )
        }
    }
}

@Composable
fun CustomSegmentedControl(
    selected: String,
    options: List<String>,
    onSelectedChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D2D2D))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val tabBg = if (isSelected) Color(0xFF4A4458) else Color.Transparent
            val tagColor = if (isSelected) Color.White else Color(0xFF938F99)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tabBg)
                    .clickable { onSelectedChange(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("unit_selector_${option.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = tagColor
                )
            }
        }
    }
}

@Composable
fun CenteredCellInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    testTag: String,
    keyAction: ImeAction,
    onKeyAction: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color(0xFF938F99),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            placeholder = { Text("-", color = Color(0xFF4A4458)) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = keyAction
            ),
            keyboardActions = KeyboardActions(
                onAny = { onKeyAction() }
            ),
            isError = error != null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F0F0F),
                unfocusedContainerColor = Color(0xFF0F0F0F),
                focusedBorderColor = Color(0xFFFF9900),
                unfocusedBorderColor = Color(0xFF4A4458),
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = Color(0xFFFF9900)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("${testTag}_error")
            )
        }
    }
}

@Composable
fun DivisorAlertBanner(marketplace: Marketplace) {
    val isMetric = marketplace.isMetric
    val divisorMsg = if (isMetric) {
        "Dimensional weight logic applied for UK/EU marketplaces based on (L×W×H)/5000 divisor."
    } else {
        "Dimensional weight logic applied for US/CA marketplaces based on (L×W×H)/139 divisor."
    }

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
            Text(text = "ℹ️", fontSize = 16.sp)
            Text(
                text = divisorMsg,
                fontSize = 12.sp,
                color = Color(0xFFFFC166),
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// Helpers for Border
@Composable
private fun BoxBorder(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
