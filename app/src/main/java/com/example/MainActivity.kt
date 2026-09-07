package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ApiKeyRepository
import com.example.model.AiProvider
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
                    val repository = ApiKeyRepository(applicationContext)
                    val defaultProvider by repository.defaultProviderFlow.collectAsState(initial = AiProvider.OPENAI)
                    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }

                    LaunchedEffect(defaultProvider) {
                        if (Build.VERSION.SDK_INT >= 37 && defaultProvider == AiProvider.OLLAMA) {
                            val permission = "android.permission.ACCESS_LOCAL_NETWORK"
                            if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                                localNetworkPermissionLauncher.launch(permission)
                            }
                        }
                    }

                    ArcAiMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
