package com.evyr.rads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.evyr.rads.ui.Screen
import com.evyr.rads.ui.screens.TodayScreen
import com.evyr.rads.ui.theme.RadsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RadsTheme {
                Surface(modifier = Modifier) {
                    var currentScreen by remember { mutableStateOf(Screen.TODAY) }

                    when (currentScreen) {
                        Screen.TODAY -> TodayScreen(
                            onNavigate = { currentScreen = it }
                        )
                        else -> TodayScreen(
                            onNavigate = { currentScreen = it }
                        )
                    }
                }
            }
        }
    }
}
