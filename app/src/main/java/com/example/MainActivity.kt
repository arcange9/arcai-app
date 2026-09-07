package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ApiKeyRepository
import com.example.ui.screens.ArcAiMainScreen
import com.example.ui.theme.ArcAiTheme
import com.example.ui.viewmodel.ArcAiViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Upgrade credentials created by older ArcAI builds before any screen
        // attempts to read them. The migration is idempotent.
        lifecycleScope.launch {
            runCatching {
                ApiKeyRepository(applicationContext).migrateLegacyPlaintextKeys()
            }
        }

        setContent {
            ArcAiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: ArcAiViewModel = viewModel()
                    ArcAiMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
