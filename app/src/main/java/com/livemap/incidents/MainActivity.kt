package com.livemap.incidents

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.livemap.incidents.ui.map.MapScreen
import com.livemap.incidents.ui.theme.LiveIncidentMapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveIncidentMapTheme {
                // The map is drawn edge-to-edge; per-screen insets are handled by each screen.
                Surface(modifier = Modifier.fillMaxSize()) {
                    MapScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
