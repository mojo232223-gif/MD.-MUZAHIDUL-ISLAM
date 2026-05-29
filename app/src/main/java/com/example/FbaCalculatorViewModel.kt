package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppScreen {
    INPUT, RESULT
}

class FbaCalculatorViewModel : ViewModel() {

    // Input States
    private val _marketplace = MutableStateFlow(Marketplace.USA)
    val marketplace = _marketplace.asStateFlow()

    private val _length = MutableStateFlow("")
    val length = _length.asStateFlow()

    private val _width = MutableStateFlow("")
    val width = _width.asStateFlow()

    private val _height = MutableStateFlow("")
    val height = _height.asStateFlow()

    private val _weight = MutableStateFlow("")
    val weight = _weight.asStateFlow()

    private val _lengthUnit = MutableStateFlow(FeeCalculator.LengthUnit.INCHES)
    val lengthUnit = _lengthUnit.asStateFlow()

    private val _weightUnit = MutableStateFlow(FeeCalculator.WeightUnit.LBS)
    val weightUnit = _weightUnit.asStateFlow()

    // Validation Error States
    private val _lengthError = MutableStateFlow<String?>(null)
    val lengthError = _lengthError.asStateFlow()

    private val _widthError = MutableStateFlow<String?>(null)
    val widthError = _widthError.asStateFlow()

    private val _heightError = MutableStateFlow<String?>(null)
    val heightError = _heightError.asStateFlow()

    private val _weightError = MutableStateFlow<String?>(null)
    val weightError = _weightError.asStateFlow()

    // Screen navigation and result States
    private val _currentScreen = MutableStateFlow(AppScreen.INPUT)
    val currentScreen = _currentScreen.asStateFlow()

    private val _calculationResult = MutableStateFlow<FeeCalculator.CalculationResult?>(null)
    val calculationResult = _calculationResult.asStateFlow()

    // Handlers
    fun onMarketplaceSelected(market: Marketplace) {
        _marketplace.value = market
        // Adjust default units to match the marketplace native systems
        if (market.isMetric) {
            _lengthUnit.value = FeeCalculator.LengthUnit.CM
            _weightUnit.value = FeeCalculator.WeightUnit.KG
        } else {
            _lengthUnit.value = FeeCalculator.LengthUnit.INCHES
            _weightUnit.value = FeeCalculator.WeightUnit.LBS
        }
        clearAllErrors()
    }

    fun onLengthChanged(value: String) {
        _length.value = value
        if (_lengthError.value != null) _lengthError.value = null
    }

    fun onWidthChanged(value: String) {
        _width.value = value
        if (_widthError.value != null) _widthError.value = null
    }

    fun onHeightChanged(value: String) {
        _height.value = value
        if (_heightError.value != null) _heightError.value = null
    }

    fun onWeightChanged(value: String) {
        _weight.value = value
        if (_weightError.value != null) _weightError.value = null
    }

    fun onLengthUnitChanged(unit: FeeCalculator.LengthUnit) {
        _lengthUnit.value = unit
    }

    fun onWeightUnitChanged(unit: FeeCalculator.WeightUnit) {
        _weightUnit.value = unit
    }

    private fun clearAllErrors() {
        _lengthError.value = null
        _widthError.value = null
        _heightError.value = null
        _weightError.value = null
    }

    fun navigateToInput() {
        _currentScreen.value = AppScreen.INPUT
    }

    fun calculate() {
        var isValid = true

        val lVal = parseAndValidateDouble(_length.value, "Length") { error ->
            _lengthError.value = error
            isValid = false
        }
        val wVal = parseAndValidateDouble(_width.value, "Width") { error ->
            _widthError.value = error
            isValid = false
        }
        val hVal = parseAndValidateDouble(_height.value, "Height") { error ->
            _heightError.value = error
            isValid = false
        }
        val wtVal = parseAndValidateDouble(_weight.value, "Weight") { error ->
            _weightError.value = error
            isValid = false
        }

        if (!isValid) return

        // Compute results
        val result = FeeCalculator.calculateFee(
            l = lVal,
            w = wVal,
            h = hVal,
            weightValue = wtVal,
            lUnit = _lengthUnit.value,
            wUnit = _weightUnit.value,
            marketplace = _marketplace.value
        )

        _calculationResult.value = result
        _currentScreen.value = AppScreen.RESULT
    }

    fun getLiveEstimatedTier(): String {
        val lVal = _length.value.toDoubleOrNull() ?: return "—"
        val wVal = _width.value.toDoubleOrNull() ?: return "—"
        val hVal = _height.value.toDoubleOrNull() ?: return "—"
        val wtVal = _weight.value.toDoubleOrNull() ?: 0.1

        if (lVal <= 0.0 || wVal <= 0.0 || hVal <= 0.0 || wtVal <= 0.0) return "—"

        return try {
            val res = FeeCalculator.calculateFee(
                l = lVal,
                w = wVal,
                h = hVal,
                weightValue = wtVal,
                lUnit = _lengthUnit.value,
                wUnit = _weightUnit.value,
                marketplace = _marketplace.value
            )
            res.detectedTier.name
        } catch (e: Exception) {
            "—"
        }
    }

    private fun parseAndValidateDouble(
        input: String,
        fieldName: String,
        onError: (String) -> Unit
    ): Double {
        if (input.isBlank()) {
            onError("$fieldName is required")
            return 0.0
        }
        val parsed = input.toDoubleOrNull()
        if (parsed == null) {
            onError("Enter a valid number")
            return 0.0
        }
        if (parsed <= 0.0) {
            onError("$fieldName must be greater than 0")
            return 0.0
        }
        return parsed
    }
}
