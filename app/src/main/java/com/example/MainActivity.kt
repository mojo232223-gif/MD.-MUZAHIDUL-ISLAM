package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.InputScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FbaCalculatorViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val result by viewModel.calculationResult.collectAsState()

                    AnimatedContent(
                        targetState = currentScreen,
                        modifier = Modifier.padding(innerPadding),
                        transitionSpec = {
                            if (targetState == AppScreen.RESULT) {
                                // Transition from Input to Result (slide in from right, exit to left)
                                (slideInHorizontally { width -> width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                // Transition from Result to Input (slide in from left, exit to right)
                                (slideInHorizontally { width -> -width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                            }
                        },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.INPUT -> {
                                InputScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            AppScreen.RESULT -> {
                                result?.let { calculationResult ->
                                    ResultScreen(
                                        result = calculationResult,
                                        onRecalculate = { viewModel.navigateToInput() },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
