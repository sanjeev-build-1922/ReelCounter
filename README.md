# REEL // COUNTER (Doom-Scroll Telemetry for Android 14/15)

A production-ready Android utility app inspired by the **Nothing OS** aesthetic (pure AMOLED `#000000` monochrome palette, custom 5x7 dot-matrix LED numerals, industrial hairline borders, and signature `#D71921` red telemetry accents) that tracks short-form video consumption across **Instagram Reels** and **YouTube Shorts** in real time.

## Architecture & Modules

```text
ReelCounter/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/nothing/reelcounter/
        │   ├── ReelCounterApp.kt
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── ReelPlatform.kt
        │   │   ├── ReelEntity.kt
        │   │   ├── ReelDao.kt
        │   │   ├── ReelDatabase.kt
        │   │   └── ReelRepository.kt
        │   ├── service/
        │   │   ├── ReelAccessibilityService.kt
        │   │   └── FloatingHudService.kt
        │   ├── ui/
        │   │   ├── ReelViewModel.kt
        │   │   ├── ReelDashboardScreen.kt
        │   │   ├── components/
        │   │   │   ├── DotMatrixNumber.kt
        │   │   │   └── PermissionOnboardingCard.kt
        │   │   └── theme/
        │   │       ├── Color.kt
        │   │       ├── Type.kt
        │   │       └── Theme.kt
        │   └── widget/
        │       └── ReelGlanceWidget.kt
        └── res/
            ├── drawable/
            │   ├── ic_launcher_background.xml
            │   ├── ic_launcher_foreground.xml
            │   └── ic_notification.xml
            ├── mipmap-anydpi-v26/
            │   ├── ic_launcher.xml
            │   └── ic_launcher_round.xml
            ├── values/
            │   ├── strings.xml
            │   └── themes.xml
            └── xml/
                ├── accessibility_service_config.xml
                ├── backup_rules.xml
                ├── data_extraction_rules.xml
                └── reel_widget_info.xml
```

## Quick Start in Android Studio

1. Extract `ReelCounter.zip` and open the `ReelCounter` root folder in **Android Studio Ladybug (2024.2.1) or newer**.
2. Let Gradle sync (uses **Kotlin 2.0.21**, **AGP 8.7.2**, **KSP**, and **Compile/Target SDK 35**).
3. Run the `app` configuration on a physical device running Android 8.0+ (optimized for **Android 14 / 15, API 34 / 35**).
4. In the app's **SYSTEM // ONBOARDING** card:
   - Tap **ENABLE** on Step `01` to enable **ReelCounter Doom-Scroll Tracker** in Android Accessibility Settings. *(Note: On Android 13/14/15, if sideloaded via APK rather than ADB/Android Studio, open App Info -> top-right menu -> "Allow restricted settings" first).*
   - Tap **ALLOW** on Step `02` to grant **Display over other apps** (`SYSTEM_ALERT_WINDOW`) for the draggable `[ REELS: 042 ]` floating HUD pill.
   - Tap **PERMIT** on Step `03` to allow the low-priority foreground telemetry notification.
5. Open **Instagram Reels** or **YouTube Shorts** and swipe vertically—each reel swipe triggers a crisp `EFFECT_CLICK` haptic pulse, increments the Room database counter, and updates the floating `[ REELS: 042 ]` pill in real time.
