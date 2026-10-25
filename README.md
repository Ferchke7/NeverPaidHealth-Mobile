# 🏋️‍♂️ NeverPaidHealth Mobile (Jetpack Compose)

> **High-performance, privacy-first, enterprise-grade strength training and workout tracking mobile application.**  
> Designed for **120 FPS buttery-smooth performance**, zero-latency (0ms) offline-first operation, and built in strict accordance with the [Meet-Miyani/compose-skill](https://github.com/Meet-Miyani/compose-skill) standard.

---

## 🚀 Key Features

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

## 🏛 Architecture & Tech Stack

Built strictly following the **`compose-skill`** rules:

- **Unidirectional Data Flow (MVI)**: Built on `BaseViewModel<Action, State, Effect>` with a single public entry point `onAction(action)`.
- **Stateless Screens**: `Route` collects state and effects; `Screen` is 100% stateless (`state in, callbacks out`).
- **High-Performance Compose**: Clock-driven values (rest timer, workout duration) are passed as timestamps (`Instant` / Epoch ms) and formatted only at the leaf composable scope, completely eliminating whole-screen recompositions.
- **Dependency Injection**: Koin 4.x (`koin-androidx-compose`).
- **Networking**: Ktor 3.x (CIO Engine) with JSON Content Negotiation.
- **Theme**: Athletic OLED Dark (`#0B0E14`) with Emerald Neon (`#10B981`) accents and high-contrast gym ergonomics.

---

## 📁 Project Structure

```
duda-mobile/
├── .agents/skills/              # Installed Compose Skills (compose, compose-architecture, etc.)
├── gradle/
│   ├── libs.versions.toml       # Single source of truth for all dependencies & versions
│   └── wrapper/                 # Gradle Wrapper distribution
├── app/
│   ├── build.gradle.kts         # App module build definition
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/dev/neverpaid/health/
│       │       ├── MainActivity.kt
│       │       ├── NeverPaidHealthApplication.kt
│       │       ├── core/
│       │       │   ├── designsystem/  # Colors, Typography, AthleticCard, SetTypeBadge, Theme
│       │       │   ├── di/            # Koin AppModule
│       │       │   ├── error/         # AppError and AppErrorType
│       │       │   ├── math/          # StrengthCalculator (E1RM, Volume, 7-day Avg, BMI, TDEE)
│       │       │   └── mvi/           # BaseViewModel, UiAction, UiState, UiEffect, CollectEffect
│       │       ├── data/
│       │       │   ├── catalog/       # Standard 870+ exercises & pre-built routines
│       │       │   └── repository/    # NeverPaidRepositoryImpl (0ms boot offline cache)
│       │       ├── domain/
│       │       │   ├── model/         # Domain entities (Workout, Exercise, Set, Coach, Body)
│       │       │   └── repository/    # Repository interfaces
│       │       ├── features/
│       │       │   ├── dashboard/     # Dashboard screen & routines quick-start
│       │       │   ├── workout/       # Live workout tracker & floating rest timer
│       │       │   ├── exercises/     # Catalog search & form guides
│       │       │   ├── history/       # Session logs & 52-week heatmap
│       │       │   ├── progress/      # Volume charts & PR Hall of Fame
│       │       │   ├── coach/         # AI Coach & sports science chat
│       │       │   ├── body/          # Weigh-ins, 7-day avg, BMI & TDEE
│       │       │   └── profile/       # Units (KG/LB) & preferences
│       │       └── navigation/        # Type-safe bottom bar & NavHost
│       └── test/java/dev/neverpaid/health/
│           ├── core/math/             # Golden calculation vectors tests
│           ├── features/workout/      # WorkoutViewModel tests
│           └── data/repository/       # Repository tests
```

---

## 💻 Running the App

### 1. In Android Studio
1. Open Android Studio.
2. Select **File $\rightarrow$ Open** and choose the `duda-mobile` folder.
3. Allow Gradle to sync.
4. Select an Android Emulator (API 26+) or a physical device.
5. Click **Run (`Shift + F10`)**.

### 2. From the Terminal
Run unit tests verifying the golden calculation vectors and ViewModels:
```bash
./gradlew test
```

Build the debug APK:
```bash
./gradlew assembleDebug
```
The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.
