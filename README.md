# 🏋️‍♂️ NeverPaidHealth Mobile (Compose Multiplatform — Android & iOS)

> **High-performance, privacy-first, enterprise-grade strength training and workout tracking mobile application.**  
> Built with **Compose Multiplatform (CMP)** to run natively on **Android (APK)** and **Apple (iOS / iPhone / iPad)** with 120 FPS buttery-smooth performance, zero-latency (0ms) offline boot, and automated **GitHub Actions CI/CD** for 1-click APK downloads and Releases.

---

## 🚀 Key Features

* **Cross-Platform Compose (Android & iOS)**:
  - 100% shared UI, MVI architecture, business logic, and math calculations between Android and Apple iOS.
  - Native performance with Apple Metal rendering on iOS and hardware acceleration on Android.
  - Native haptic feedback: **Apple Taptic Engine** (`UIImpactFeedbackGenerator`) on iPhone, **Android Vibrator** on Android.

* **Interactive Hevy/Strong-Style Workout Tracker**:
  - Live set logging with dynamic set types: **Normal** (1, 2, 3), **Warmup** (W), **Drop Set** (D), and **Failure** (F).
  - Ghost-text showing previous workout performance for immediate reference.
  - 1-tap green checkmark set completion with instant haptic vibration feedback.
  - Floating rest timer bar with countdown, quick `+30s` / `-15s` / `Skip`, and audio/haptic chime on completion.
  - Real-time **E1RM calculation** (Epley/Brzycki formula) as reps and weights change.
  - Dynamic exercise adding, swapping, and reordering.
  - Real-time workout duration and total volume calculation.
  - Celebratory completion modal with duration, volume, and sets stats.

* **Movement & Visual Catalog (870+ Exercises)**:
  - Instant debounce search with primary muscle chips (**Chest, Back, Quads, Hamstrings, Shoulders, Biceps, Triceps, Core**).
  - Equipment filters (**Barbell, Dumbbell, Cable, Machine, Bodyweight**).
  - Movement detail dialog with step-by-step form execution guide and secondary muscle recruitment.

* **Workout History & Consistency Heatmap**:
  - 12-week / 52-week annual **GitHub-style training density matrix** with color intensity indicators (0–4 workouts).
  - Chronological session feed with expandable set-by-set weight and rep breakdowns.

* **Advanced Analytics & PR Hall of Fame**:
  - Weekly volume progression tonnage bar chart.
  - MEV / MRV muscle hypertrophy weekly sets distribution.
  - Personal Records (PRs) Hall of Fame with medals and calculated 1RMs.

* **AI Strength Coach**:
  - Daily Central Nervous System (**CNS**) Readiness Score (0–100) and recovery status ("Full Throttle" / "Moderate" / "Deload").
  - Progressive overload prescriptions (e.g. `+2.5 kg on Bench Press`).
  - Interactive sports science AI coach chat.

* **Body Metrics & Health Calculator**:
  - Bodyweight log with **7-day moving average** to eliminate daily water weight noise.
  - Automated BMI calculation, body fat %, and **Mifflin-St Jeor TDEE** energy expenditure calculator.

* **Athlete Settings**:
  - Instant unit toggle: **KG** $\leftrightarrow$ **LB**.
  - Customizable default rest timers (60s, 90s, 120s, 180s).
  - Haptic vibration and sound toggles.
  - Configurable REST API Gateway URL (supports local emulator `10.0.2.2:8080` and production server).

---

## 📦 GitHub Actions CI/CD & 1-Click APK Download

The repository includes a ready-to-run GitHub Actions workflow in [`.github/workflows/build-and-release.yml`](.github/workflows/build-and-release.yml):

1. **Automatic APK Build on every push**:
   - Whenever you push code to `main`, GitHub Actions automatically compiles the application and generates the `.apk`.
2. **Immediate Download from Artifacts**:
   - Go to the **Actions** tab in your GitHub repository.
   - Click on the latest workflow run.
   - Under **Artifacts**, download `NeverPaidHealth-Android-APK` immediately to your phone or computer.
3. **Automated GitHub Releases**:
   - Push a git tag (e.g. `git tag v1.0.0 && git push --tags`) or trigger the workflow manually with the "Run workflow" button.
   - GitHub Actions creates a formal **GitHub Release** and attaches `NeverPaidHealth-v1.0.0-debug.apk` directly to the release page for 1-click public download.
4. **iOS Validation**:
   - Automatically validates compilation of the iOS framework on a macOS runner.

---

## 🌐 Linking to your GitHub Repository

To push this project to your GitHub and trigger the first automated APK build:

```bash
# 1. Add your GitHub remote repository (replace with your repo URL):
git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPO_NAME>.git

# 2. Push to GitHub:
git push -u origin main

# 3. (Optional) Create a version tag to trigger a GitHub Release with the APK attached:
git tag v1.0.0
git push --tags
```

---

## 📁 Project Structure

```
duda-mobile/
├── .agents/skills/              # Installed Compose Skills
├── .github/workflows/           # CI/CD: Automated APK Build & GitHub Releases
│   └── build-and-release.yml
├── composeApp/                  # Compose Multiplatform shared module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/          # 100% Shared UI, ViewModels, and Logic
│       │   └── kotlin/dev/neverpaid/health/
│       │       ├── core/        # Design system, MVI BaseViewModel, Math Calculators
│       │       ├── data/        # Catalog (870+ exercises), Offline Repository
│       │       ├── domain/      # Pure domain models
│       │       ├── features/    # Dashboard, Workout, Exercises, History, Progress, Coach, Body, Profile
│       │       └── navigation/  # AppNavigation and bottom bar
│       ├── androidMain/         # Android runner (MainActivity, Application)
│       └── iosMain/             # Apple iOS runner (MainViewController)
├── iosApp/                      # Xcode project wrapper for iOS (SwiftUI)
│   ├── iosApp/
│   │   ├── iOSApp.swift
│   │   └── ContentView.swift
│   └── Configuration/
└── gradle/
    ├── libs.versions.toml       # Single source of truth for all dependencies
    └── wrapper/                 # Gradle Wrapper
```
