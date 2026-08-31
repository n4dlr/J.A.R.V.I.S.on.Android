package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.jarvis.presentation.JarvisViewModel
import com.example.jarvis.presentation.screens.JarvisMainScreen
import com.example.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSpotifyIntent(intent)
        setContent {
            JarvisTheme {
                JarvisMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleSpotifyIntent(intent)
    }

    private fun handleSpotifyIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "jarvis" && uri.host == "spotify-callback") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                viewModel.handleSpotifyCallback(code)
            }
        }
    }
}

