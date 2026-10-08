# Learning Dashboard - Android Technical Assignment

A robust, modern Android application built using Kotlin, Jetpack Compose, MVVM + Repository architecture, Room local persistence, and Kotlin Coroutines/Flow.

---

## 1. Architecture & Why It Was Chosen

The application strictly implements **Unidirectional Data Flow (UDF)** adhering to Google's official Android Architecture Guide:

```
Compose Screen (Presentation)
     ↓ (events / callbacks)
ViewModel
     ↓ (triggers / observes)
Repository Interface (Domain)
     ↓
Repository Implementation (Data)
     ├── Remote API (DTOs) ──> Persists to Room
     └── Room Database (Entities) ──> Emits via Flow
              ↓
           Domain Models (Course, Lesson)
              ↓
           ViewModel StateFlow (UI State)
              ↓
           Compose UI (Renders State)
```

### Why this architecture was chosen:
1. **Single Source of Truth**: Room is the sole source of truth for all course and lesson data displayed by the UI. The UI never observes API responses directly; the API updates Room, and Room emits updates via reactive `Flow`.
2. **Separation of Concerns**: 
   - **Domain Layer**: Contains pure business models (`Course`, `Lesson`) and logic (`ProgressCalculator`) completely decoupled from Android framework dependencies or database schemas.
   - **Data Layer**: Coordinates network and database operations behind a clean `CourseRepository` interface.
   - **Presentation Layer**: Thin, stateless Composables driven by immutable `UiState` exposed through `StateFlow` from ViewModels.
3. **Single Activity Design**: A single `MainActivity` hosts the `AppNavHost` and manages edge-to-edge system insets. This avoids heavy multi-activity lifecycle overhead and prevents window state loss during transitions.
4. **Maintainability & Simplicity**: Uses a centralized `AppContainer` for dependency injection rather than heavy DI framework boilerplate, keeping setup transparent and lightweight.

---

## 2. Offline Implementation

The application guarantees offline functionality through the following mechanism:
- **Cache-First Reactive Streams**: The UI observes continuous `Flow` streams from Room DAOs (`CourseDao`, `LessonDao`).
- **Graceful Refresh Failure**: When the user refreshes courses:
  - If network is available: Remote courses and lessons are saved into Room via `OnConflictStrategy.REPLACE` for courses and `OnConflictStrategy.IGNORE` for lessons to preserve local lesson completion progress.
  - If network is disconnected or fails: The repository catches the error without wiping Room. The existing cached courses remain visible in the UI, and a non-blocking message informs the user they are in offline mode.
- **Offline Progress Updates**: When marking a lesson as complete, the ViewModel immediately writes the change to Room (`lessonDao.updateLessonCompletion`). The `combine` Flow between courses and lessons recalculates the course progress on the fly and pushes new state to the UI without requiring network connectivity.

### Note on Generative AI Progress Calculation:
The UI spec indicates 40% for the "Generative AI" course, but 6 completed lessons out of 16 equals mathematically 37.5% (or 37% truncated). Per the specification, **lesson completion is the sole source of truth**, and progress is purely derived using `ProgressCalculator.calculateProgress(completedLessons, totalLessons)`. This prevents state inconsistencies between lesson checklists and progress indicators.

---

## 3. Production Authentication Token Storage

In a production environment, sensitive authentication tokens (e.g., JWT access tokens, refresh tokens) must never be stored in plain text (such as standard `SharedPreferences`):

1. **EncryptedSharedPreferences (Jetpack Security)**:
   - Uses the **Android Keystore System** to encrypt keys (AES-256-SIV) and values (AES-256-GCM).
   - Backed by Hardware Security Modules (HSM) or StrongBox when available on supported devices.
2. **Token Lifecycle Management**:
   - Short-lived Access Tokens kept in memory or `EncryptedSharedPreferences`.
   - Long-lived Refresh Tokens used via an `Authenticator` or OkHttp Interceptor to seamlessly refresh expired access tokens without user interruption.
3. **Biometric Authentication Prompt**:
   - For high-security environments, decrypting tokens can require user re-authentication via the Android `BiometricPrompt` API.

---

## 4. Improvements Needed for 1 Million Users / Hundreds of Courses

1. **Paging 3 with Room RemoteMediator**:
   - Currently, all courses are loaded into memory. For hundreds of courses, implement Jetpack Paging 3 to paginate database queries and network fetches in chunks (e.g., 20 courses per page).
2. **Database Indexing & Full-Text Search (FTS)**:
   - Add database indexes on foreign keys (e.g., `@Index(value = ["courseId"])` on `LessonEntity`).
   - Implement Room FTS4/FTS5 for fast course name, category, and instructor search.
3. **Robust Two-Way Sync Engine (WorkManager)**:
   - Implement an offline sync queue using `WorkManager` with exponential backoff and network constraints.
   - Introduce vector clocks or timestamp-based conflict resolution to sync offline lesson completions with the backend when internet is restored.
4. **CDN & Image Caching**:
   - Integrate Coil or Glide with memory and disk LRU caching to handle course thumbnails, instructor avatars, and media banners from a distributed CDN.
5. **Observability & Analytics**:
   - Integrate Firebase Crashlytics, Datadog/Sentry RUM, and OpenTelemetry to track network latencies, database query durations, and crash-free session metrics across high concurrency.

---

## 5. iOS / macOS Equivalent Implementation

If implementing this application on Apple platforms:

| Architectural Component | Android Implementation | iOS / macOS Equivalent |
| :--- | :--- | :--- |
| **Language** | Kotlin | Swift (with Swift Concurrency `async`/`await`) |
| **UI Framework** | Jetpack Compose | SwiftUI (`NavigationStack`, `LazyVStack`) |
| **Architecture** | MVVM (`ViewModel`, `StateFlow`) | MVVM (`@Observable` class in iOS 17+ or `ObservableObject` with `@Published`) |
| **Local Persistence** | Room (SQLite) | SwiftData or GRDB / SQLite with `@Query` / `ValueObservation` |
| **Dependency Injection** | `AppContainer` | Swift Environment (`@Environment`), Factory pattern, or Dependencies library |
| **Reactive Streams** | Kotlin `Flow` & Coroutines | Swift `AsyncStream`, `AsyncSequence`, or Apple `Combine` |
| **Secure Token Storage** | Android Keystore / `EncryptedSharedPreferences` | Apple Keychain Services API (`kSecClassGenericPassword`) |
