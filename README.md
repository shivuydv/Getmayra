# MYRA — Ultra-Realistic AI Companion

MYRA is a comprehensive Android AI assistant combining emotional intelligence with deep system control. This project is optimized for both personal companionship and smart device automation.

---

### 🟢 Perspective 1: The User Experience (Angle: Product)
*   **Expressive Personalities**: Switch between Nakhre (Sassy), Sweet (Loving), and Friendly modes.
*   **Dual UI Interface**:
    *   **Anime Mode**: A cute animated character with dynamic blinking and hair sway.
    *   **Cyber HUD**: A futuristic holographic orb interface for tech enthusiasts.
*   **Live Home Companion**: A fully interactive Live Wallpaper where MYRA breathes and reacts to touch.
*   **Voice Control**: Automate tasks like "Open WhatsApp", "Switch to HUD mode", or "Turn on Flashlight".

### 🔵 Perspective 2: Developer & CI/CD (Angle: GitHub Build)
*   **GitHub Actions Ready**: The repository includes a pre-configured workflow (`android.yml`) that automatically builds and verifies the code on every push.
*   **CI-Friendly Configuration**: The build script is optimized to ignore missing keystores during the CI phase, allowing public contributions without leaking secrets.
*   **Modern Stack**: Built using 100% Kotlin and Jetpack Compose (Material 3).
*   **Version Catalog**: Centralized dependency management via `libs.versions.toml`.

### 🟣 Perspective 3: Technical Architecture (Angle: Logic)
*   **Multi-Engine Brain**: Support for Gemini 1.5 Flash (primary) and OpenAI GPT-4o-mini (fallback).
*   **Background Persistence**: Uses Android Foreground Services (`MayaSessionService`) to maintain voice sessions across apps.
*   **Real-Time Listening**: Integrated `NotificationListenerService` to read incoming WhatsApp messages aloud.
*   **Vision Scanner**: Built-in Google ML Kit for high-speed QR and Barcode scanning.
*   **Voice Biometrics**: Maya Guardian engine for deepfake call detection simulation.

### 🔴 Perspective 4: Security & Ethics (Angle: Privacy)
*   **Zero-Budget Security**: Runs entirely on free-tier APIs with local credential storage.
*   **Private Key Management**: API keys are stored in device-private `SharedPreferences` and never uploaded to any cloud server.
*   **Permission Transparency**: Standard Android runtime permissions for Microphone, Location, and Notifications.
*   **Local Processing**: VAD (Voice Activity Detection) runs locally using Silero TFLite to prevent unnecessary cloud streaming.

---

## 🛠 Setup & Build Instructions

### Prerequisites
1.  **JDK 17**: Required for compilation.
2.  **API Keys**:
    *   [Google AI Studio](https://aistudio.google.com/) (Gemini)
    *   [ElevenLabs](https://elevenlabs.io/) (Voice)
    *   [OpenAI](https://platform.openai.com/) (Optional)

### Local Development
1.  Clone the repository.
2.  Create a `.env` file in the root directory (refer to `.env.example`).
3.  Open in Android Studio or use `./gradlew assembleDebug`.

### GitHub Release
1.  The project is ready for **Push to GitHub**.
2.  The GitHub Build process will generate a **Debug APK** which can be downloaded from the "Actions" tab.

---
**Lead Developer**: Shivam Yadav  
**Project**: MYRA Official
