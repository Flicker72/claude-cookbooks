package com.holidate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.holidate.app.ui.PermissionsGate
import com.holidate.app.ui.navigation.HoliDateNavHost
import com.holidate.app.ui.theme.HoliDateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HoliDateTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Gate the whole app behind nearby-device permissions, then run the mesh UI.
                    PermissionsGate {
                        HoliDateNavHost()
                    }
                }
            }
        }
    }
}
