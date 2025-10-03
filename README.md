# Study Planner (CP3406 — Scenario 6)

Turn assessments into **scheduled focus sessions**. Plan realistically, execute with one tap, and track simple insights.

## ✨ Features (MVP)
- **Plan:** Courses & assessments, task decomposition, effort estimates
- **Schedule:** Auto-schedule sessions into preferred study windows, weekly hour caps, rollover missed sessions
- **Focus:** One-tap timer with optional Do Not Disturb (DND) prompt
- **Calendar:** Simple view of upcoming study blocks (Day/Week grid later)
- **Insights:** Weekly focused hours, completion %, burn-down, streaks
- **Local-first:** Room DB; privacy-friendly by default

## 📸 Screens
<img width="173" height="362" alt="image" src="https://github.com/user-attachments/assets/688740fd-8ef6-4596-a8ca-852ceeca5c0b" />
- Plan | Focus | Calendar | Insights

## 🧱 Tech Stack
- **Android** (Kotlin, Jetpack Compose, Material 3)
- **Architecture:** MVVM, Repository pattern
- **Storage:** Room (SQLite)
- **Background:** WorkManager (reminders), Foreground Service (timer)
- **Navigation:** `navigation-compose`

## ✅ Requirements
- Android Studio **Koala** or newer
- **JDK 17 or 21** (set in *Settings ▸ Build Tools ▸ Gradle ▸ Gradle JDK*)
- Kotlin **2.x** + **Compose Compiler Gradle plugin**
- Android SDK **API 34** (emulator image)
