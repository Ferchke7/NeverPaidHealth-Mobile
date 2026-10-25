# AGENTS.md — Engineering, Architecture & UI/UX Contract (`duda-mobile`)

This file defines the mandatory engineering, architectural, UI/UX, and localization standards for the **`duda.uz`** Compose Multiplatform (Android & iOS) mobile application (`C:\Users\ferda\OneDrive\Desktop\duda-mobile`).

---

## 1. Brand Identity (`duda` / `duda.uz`)
- **Official Brand Name:** Always use **`duda`** or **`duda.uz`** across all UI headers, titles, dialogs, strings, and documentation.
- **Forbidden Legacy Name:** Never display `NeverPaidHealth` in any user-facing UI, strings, or notifications.

---

## 2. Strict "One Screen — One Job" Principle (1 Страница = 1 Задача)
- **Single Primary Focus per Screen:** Every screen in the app must have **strictly one primary responsibility** without visual clutter or overloaded multi-widget dashboards.
  - **Workouts (`DashboardScreen`):** Focused strictly on starting today's scheduled/active workout or picking a routine template.
  - **Active Workout (`WorkoutScreen`):** Focused strictly on logging sets, weights, reps, and the floating rest timer.
  - **Programs (`ProgramsScreen`):** Focused on browsing training programs/splits and activating one. Community comments open strictly inside a dedicated **Instagram-style `ModalBottomSheet`** (`ProgramCommentsBottomSheet`), never inline beneath cards.
  - **Daily Plan (`TodoScreen`):** Focused strictly on the selected date's task checklist (`YYYY-MM-DD` isolated). Task creation/editing and the **Pomodoro Focus Timer** open in clean `ModalBottomSheet`s. No redundant mini-calendars or cluttered sidebars.
  - **Nutrition (`NutritionScreen`):** Focused strictly on the selected date's calorie/macro progress and meal log. Adding a meal or running AI photo/text food analysis opens in a `ModalBottomSheet`.
  - **Progress (`ProgressScreen`):** Uses a clean `PrimaryTabRow` / `SegmentedButton` to view **one chart/metric category at a time** (Volume Trend, Muscle Distribution, or Personal Records Hall of Fame) instead of stacking 5 heavy charts vertically.
  - **AI Coach (`CoachScreen`):** Focused strictly on the conversational AI coach chat with quick action chips.

---

## 3. Use Standard Ready-Made Material 3 Components Only
- **Forbidden:** Do NOT build custom raw `Box + clickable` checkboxes, custom hand-rolled tabs, custom switches, or ad-hoc input wrappers when an official `androidx.compose.material3` component exists.
- **Mandatory Component Mapping:**
  - **Layout & Navigation:** `Scaffold`, `CenterAlignedTopAppBar` / `TopAppBar`, `NavigationBar`, `NavigationBarItem`, `PrimaryTabRow`, `Tab`, `SingleChoiceSegmentedButtonRow`, `SegmentedButton`.
  - **Cards & Containers:** `Card`, `ElevatedCard`, `OutlinedCard`, `Surface` (wrapped in theme tokens).
  - **Buttons & Chips:** `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton`, `IconButton`, `FilterChip`, `AssistChip`, `SuggestionChip`, `ExtendedFloatingActionButton`.
  - **Inputs & Controls:** `OutlinedTextField`, `Checkbox`, `Switch`, `Slider`, `LinearProgressIndicator`, `CircularProgressIndicator`.
  - **Sheets & Dialogs:** `ModalBottomSheet` (for comments, forms, Pomodoro timer, exercise picker), `AlertDialog`, `SnackbarHost`.

---

## 4. Full Trilingual Localization (`RU` / `UZ` / `EN`)
- **Zero Hardcoded UI Strings:** No `@Composable` function may contain hardcoded Russian or English UI literals.
- **Supported Locales:**
  - **Russian (`ru`)** — Русский
  - **Uzbek (`uz`)** — O'zbekcha
  - **English (`en`)** — English
- **Implementation:**
  - All strings are provided via `LocalDudaStrings.current` (`core/i18n/DudaStrings.kt`) and reactive to `UserPreferencesData.languageCode`.
  - Every HTTP request through `DudaApiClient` automatically sends `Accept-Language: <ru|uz|en>`.

---

## 5. MVI & Clean Architecture Contract (Compose Kit)
1. **ViewModel Contract:**
   - Every screen has a dedicated `*Contract.kt` containing `UiState`, `UiAction`, and `UiEffect`.
   - Every ViewModel extends `BaseViewModel<Action, State, Effect>` with `onAction(action: Action)` as its sole public entry point.
   - All asynchronous operations in ViewModels MUST run inside `launchGuarded(onError = ...)` — never raw `try/catch` chains or swallowed exceptions.
2. **Real Business Logic & Zero Fake Data:**
   - Never hardcode fake arrays in Composables (`remember { mutableStateListOf(...) }`) or ViewModels (`listOf("W-4" to 8200.0)`).
   - Every metric (Volume Tonnage, e1RM, 7-day Muscle Sets, 84-day Consistency Heatmap, Todo daily completion ratio, BMI, TDEE, CNS Readiness) must be computed deterministically from real user records persisted locally and synced with `https://duda.uz/api/v1`.
3. **Date-Isolated Daily Logs (`YYYY-MM-DD`):**
   - Daily Todo tasks and Nutrition logs are strictly isolated by date string (`YYYY-MM-DD`).
   - Completing a recurring task today marks it completed **only** for today's date in the activity log (`completedDates`), so tomorrow starts fresh while preserving full historical logs and streak calculations.
