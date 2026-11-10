package com.example.v2rayng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.v2rayng.ui.MainScreen
import com.example.v2rayng.ui.MainViewModel
import com.example.v2rayng.ui.SplashScreen
import com.example.v2rayng.ui.theme.AetherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            var showSplash by remember { mutableStateOf(true) }

            AetherTheme(themeMode = viewModel.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 500),
                        label = "SplashCrossfade"
                    ) { isSplashing ->
                        if (isSplashing) {
                            SplashScreen(onSplashFinished = { showSplash = false })
                        } else {
                            MainScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
