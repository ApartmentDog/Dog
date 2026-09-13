package com.evyr.rads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.evyr.rads.data.AppPrefs
import com.evyr.rads.ui.screens.BootScreen
import com.evyr.rads.ui.screens.TodayScreen
import com.evyr.rads.ui.theme.RadsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RadsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    // Read once per launch so toggling it mid-session doesn't re-trigger.
                    var booting by remember {
                        mutableStateOf(AppPrefs.bootSequenceEnabled(context))
                    }

                    if (booting) {
                        BootScreen(onFinished = { booting = false })
                    } else {
                        // Single-screen app for now; tabs live inside TodayScreen.
                        TodayScreen()
                    }
                }
            }
        }
    }
}
