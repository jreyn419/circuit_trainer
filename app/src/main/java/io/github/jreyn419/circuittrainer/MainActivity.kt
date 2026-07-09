package io.github.jreyn419.circuittrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jreyn419.circuittrainer.ui.theme.CircuitTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CircuitTrainerTheme {
                CircuitTrainerApp()
            }
        }
    }
}
