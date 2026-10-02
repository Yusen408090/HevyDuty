package com.example.myapplication

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import com.example.myapplication.ui.HeavyDutyApp
import com.example.myapplication.ui.theme.HeavyDutyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val container = (application as HeavyDutyApplication).container
        setContent { HeavyDutyTheme { HeavyDutyApp(container) } }
    }
}
