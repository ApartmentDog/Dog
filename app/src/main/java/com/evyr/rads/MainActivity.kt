package com.evyr.rads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.evyr.rads.ui.screens.TodayScreen
import com.evyr.rads.ui.theme.RadsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RadsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Single-screen app for now; tabs live inside TodayScreen.
                    TodayScreen()
                }
            }
        }
    }
}

