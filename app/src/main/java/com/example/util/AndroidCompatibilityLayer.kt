package com.example.util

import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Android Platform Compatibility Layer for ArcAI Assistant.
 * Handles API differences, permission behavior shifts, and feature fallbacks 
 * from Android 7.0 (API 24) through Android 17 (API 36+).
 */
object AndroidCompatibilityLayer {

    val currentSdkInt: Int
        get() = Build.VERSION.SDK_INT

    val deviceReleaseName: String
        get() = Build.VERSION.RELEASE ?: "Unknown"

    val deviceModel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"

    val androidVersionSummary: String
        get() = "Android $deviceReleaseName (API $currentSdkInt - ${getAndroidCodeName(currentSdkInt)})"

    /**
     * Maps API level to Android OS release marketing / dessert names.
     */
    fun getAndroidCodeName(apiLevel: Int): String {
        return when (apiLevel) {
            Build.VERSION_CODES.N -> "Nougat (7.0)"
            Build.VERSION_CODES.N_MR1 -> "Nougat (7.1)"
            Build.VERSION_CODES.O -> "Oreo (8.0)"
            Build.VERSION_CODES.O_MR1 -> "Oreo (8.1)"
            Build.VERSION_CODES.P -> "Pie (9.0)"
            Build.VERSION_CODES.Q -> "Android 10"
            Build.VERSION_CODES.R -> "Android 11"
            Build.VERSION_CODES.S -> "Android 12"
            Build.VERSION_CODES.S_V2 -> "Android 12L"
            Build.VERSION_CODES.TIRAMISU -> "Android 13 (Tiramisu)"
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "Android 14 (UpsideDownCake)"
            35 -> "Android 15 (VanillaIceCream)"
            36 -> "Android 16 (Baklava)"
            37 -> "Android 17"
            else -> if (apiLevel > 37) "Android 17+" else "Android (API $apiLevel)"
        }
    }

    // --- Feature Capabilities & API Version Checks ---

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    val supportsNotificationChannels: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val supportsStrongBoxKeystore: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val supportsScopedStorage: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val supportsWindowInsetsController: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val supportsDynamicColor: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val requiresRuntimeNotificationPermission: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val supportsPhotoPicker: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val requiresForegroundServiceTypes: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @get:ChecksSdkIntAtLeast(api = 35)
    val isAndroid15OrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= 35

    @get:ChecksSdkIntAtLeast(api = 36)
    val isAndroid16OrHigher: Boolean
        get() = Build.VERSION.SDK_INT >= 36

    /**
     * Detailed feature availability map for diagnostic displays & Settings screens.
     */
    fun getCompatibilityDiagnostics(context: Context): List<CompatibilityFeatureItem> {
        val features = mutableListOf<CompatibilityFeatureItem>()

        features.add(
            CompatibilityFeatureItem(
                featureName = "Minimum SDK Baseline",
                supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
                description = "Min SDK API 24 (Android 7.0 Nougat) baseline satisfied."
            )
        )

        features.add(
            CompatibilityFeatureItem(
                featureName = "Android Keystore Encryption",
                supported = true,
                description = if (supportsStrongBoxKeystore) 
                    "Hardware-backed StrongBox Keystore security available." 
                else 
                    "Standard Android Keystore AES-GCM encryption active."
            )
        )

        features.add(
            CompatibilityFeatureItem(
                featureName = "Notification System",
                supported = true,
                description = when {
                    requiresRuntimeNotificationPermission -> "Android 13+ runtime POST_NOTIFICATIONS permission model active."
                    supportsNotificationChannels -> "Android 8.0+ Notification Channels active."
                    else -> "Standard Notification Manager fallback active."
                }
            )
        )

        features.add(
            CompatibilityFeatureItem(
                featureName = "Storage Access Framework",
                supported = true,
                description = when {
                    supportsPhotoPicker -> "Android 13+ Granular Media Permissions & System PhotoPicker active."
                    supportsScopedStorage -> "Android 10+ Scoped Storage model active."
                    else -> "Android 7-9 Storage Access Framework (SAF) active."
                }
            )
        )

        features.add(
            CompatibilityFeatureItem(
                featureName = "Accessibility Device Automation",
                supported = true,
                description = "Android Accessibility Service API active across API 24-36+."
            )
        )

        features.add(
            CompatibilityFeatureItem(
                featureName = "Screen Capture & Vision API",
                supported = true,
                description = when {
                    requiresForegroundServiceTypes -> "Android 14+ MediaProjection Foreground Service type enforcement enabled."
                    supportsScopedStorage -> "Android 10+ MediaProjection virtual display capture enabled."
                    else -> "Legacy Screen Capture SAF fallback enabled."
                }
            )
        )

        features.add(
            CompatibilityFeatureItem(
                featureName = "Dynamic Material You Colors",
                supported = supportsDynamicColor,
                description = if (supportsDynamicColor) 
                    "Android 12+ System Dynamic Material You color palettes supported." 
                else 
                    "ArcAI Custom Material 3 fallback theme active."
            )
        )

        return features
    }
}

data class CompatibilityFeatureItem(
    val featureName: String,
    val supported: Boolean,
    val description: String
)
