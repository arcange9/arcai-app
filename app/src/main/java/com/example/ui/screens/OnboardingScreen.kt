package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ArcAiButton
import com.example.ui.theme.ArcPrimary
import com.example.ui.theme.ArcSecondary
import com.example.ui.theme.ArcTertiary

data class OnboardingStep(
    val title: String,
    val headline: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            title = "Welcome to ArcAI",
            headline = "Your AI assistant for Android.",
            description = "Intelligence, speed, privacy, and full device control combined in one unified experience.",
            icon = Icons.Default.AutoAwesome,
            color = ArcPrimary
        ),
        OnboardingStep(
            title = "Bring Your Own AI",
            headline = "Connect Gemini, OpenAI, OpenRouter and more.",
            description = "Pay only for what you use. Your API keys are encrypted locally with Android Keystore.",
            icon = Icons.Outlined.VpnKey,
            color = ArcSecondary
        ),
        OnboardingStep(
            title = "Control Your Device",
            headline = "Use natural language to automate Android actions.",
            description = "Open apps, type queries, search videos, and execute complex workflows hands-free.",
            icon = Icons.Outlined.Smartphone,
            color = ArcTertiary
        ),
        OnboardingStep(
            title = "You're in Control",
            headline = "Permissions are explicit. Stop anytime.",
            description = "Automation runs with full user transparency, safety confirmations, and instant STOP controls.",
            icon = Icons.Outlined.Lock,
            color = ArcPrimary
        )
    )

    val step = steps[currentStep]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip Button Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ArcAI Assistant",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onFinishOnboarding) {
                Text("Skip", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Center Step Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 20.dp)
        ) {
            Surface(
                color = step.color.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = step.color,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = step.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = step.color,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = step.headline,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = step.description,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bottom Controls & Indicators
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(
                                width = if (index == currentStep) 24.dp else 8.dp,
                                height = 8.dp
                            )
                            .background(
                                color = if (index == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (currentStep < steps.size - 1) {
                ArcAiButton(
                    text = "Next",
                    onClick = { currentStep++ },
                    icon = Icons.Default.ChevronRight,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ArcAiButton(
                    text = "Get Started",
                    onClick = onFinishOnboarding,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
