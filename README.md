# ArcAI

**ArcAI** is a native Android AI assistant focused on chat, coding, study help, image generation, voice interaction, files, and safe automation.

## v0.2.0

### Highlights
- Modern Jetpack Compose + Material 3 interface
- Premium ArcAI blue/cyan/violet visual system
- Dark and light themes
- Android 7 (API 24) through Android 17 (API 37) target range
- Multi-provider AI architecture with centralized routing
- Room-backed chat history
- Android Keystore-backed API-key encryption with legacy migration
- Image Studio with provider-backed image generation
- Voice input/output using Android speech services
- File picker and AI file analysis workflow
- WorkManager-based scheduled automation foundation
- GitHub Actions Android build verification

## AI providers

ArcAI's provider catalog includes major hosted and local providers. Provider support is capability-based: a provider is only presented as fully supported when the corresponding client implementation exists.

## Security

API keys are encrypted before persistence using Android Keystore-backed AES-GCM. Do not commit real API keys, `.env` files, signing keys, or other credentials to the repository.

## Build

Open the project in a current Android Studio release compatible with Android 17 / API 37, install the Android 17 SDK, and use JDK 17. Android Gradle Plugin 9.1.1 supports API 37.

For CI, the repository includes `.github/workflows/android-build.yml`.

## Compatibility

- Minimum SDK: Android 7 / API 24
- Target SDK: Android 17 / API 37
- Compile SDK: Android 17 / API 37

Newer Android APIs must be guarded or avoided when they are not available on API 24.

## Project structure

```text
app/src/main/java/com/example/
├── data/       # Room and DataStore repositories
├── model/      # Provider/model definitions
├── security/   # API-key encryption
├── service/    # AI, image, verification and background services
├── ui/         # Compose screens, components and theme
└── util/       # Android compatibility helpers
```

## Development branch

The v0.2.0 work is developed on `arcai-v0.2-upgrade` so the stable `main` branch remains protected while the upgrade is verified.

## Author

Created by **Mukamyi Izere Arcange**.
