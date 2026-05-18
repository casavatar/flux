package com.example.flux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.flux.navigation.FluxNavHost
import com.example.flux.ui.theme.FluxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FluxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FluxNavHost()
                }
            }
        }
    }

    external fun stringFromJNI(): String

    companion object {
        init {
            System.loadLibrary("flux")
        }
    }
}
