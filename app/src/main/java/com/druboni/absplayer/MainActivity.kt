package com.druboni.absplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.druboni.absplayer.ui.navigation.AppNavGraph
import com.druboni.absplayer.ui.theme.AbsPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AbsPlayerTheme {
                AppNavGraph()
            }
        }
    }
}
