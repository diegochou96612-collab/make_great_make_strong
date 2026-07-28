package com.petmed.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.petmed.app.ui.navigation.AppNavGraph
import com.petmed.app.ui.theme.PetMedAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetMedAppTheme {
                AppNavGraph()
            }
        }
    }
}
