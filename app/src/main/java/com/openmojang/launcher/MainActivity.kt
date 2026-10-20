package com.openmojang.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.openmojang.launcher.ui.OpenMojangApp
import com.openmojang.launcher.ui.theme.OpenMojangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenMojangTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OpenMojangApp()
                }
            }
        }
    }
}
