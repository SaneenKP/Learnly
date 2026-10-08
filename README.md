# Learning Dashboard - Android Technical Assignment

A robust, modern Android application built using Kotlin, Jetpack Compose, MVVM + Repository architecture, Room local persistence, and Kotlin Coroutines/Flow.

---

## 1. Architecture & Why It Was Chosen

The application strictly implements **Clean Architecture** and **Unidirectional Data Flow (UDF)**:

```
Compose Screen (Presentation UI)
     ↓ (user actions / callbacks)
ViewModel (presentation.viewmodel - Scoped with CoroutineDispatchers)
     ↓ (triggers domain operations / receives UiError)
Domain UseCases (domain.usecase - Business rules, simulated delays & validation)
     ├── LoginUseCase / ValidateCredentialsUseCase
     ├── GetCoursesUseCase / RefreshCoursesUseCase (DB-first orchestration)
     ├── GetCourseDetailsUseCase / RefreshCourseDetailsUseCase
     ├── ToggleLessonCompletionUseCase / LogoutUseCase
     └── ObserveNetworkStatusUseCase
     ↓
Repository Interface (domain.repository - Pure Data Access)
     ↓
Repository Implementation (data.repository - Room DB & Remote API sync)
     ├── Room Database (Local Source of Truth)
     └── Remote API (FakeCourseApi)
     ↓
Domain Models (Course, Lesson) & Typed Error Hierarchy (AppError)
     ├── NetworkError (API layer)
     ├── DataError (Repository layer)
     └── BusinessError (UseCase layer)
```

### Why this architecture was chosen:
1. **Single Source of Truth**: Room is the sole source of truth for all course and lesson data displayed by the UI. The UI never observes API responses directly; the API updates Room, and Room emits updates via reactive `Flow`.
2. **Dedicated UseCases for Business Logic**: ViewModels do not communicate directly with repositories. All business rules (DB-first orchestration, mock loading delays, credential verification, and completion toggles) reside strictly within dedicated UseCases, keeping ViewModels thin and focused solely on UI state.
3. **Repository Isolated for Data Only**: Repositories are strictly responsible for data querying, persistence, and network mapping, without UI or presentation state logic.
4. **Dispatcher-Scoped Coroutines & Zero Delays in ViewModels**: ViewModels launch all coroutines with explicitly injected `CoroutineDispatcher` instances (`Dispatchers.Main` by default). Artificial latency and heavy tasks are executed on `Dispatchers.IO` inside UseCases.
5. **Layered Error Handling with Typed Sealed Classes**:
   - **`AppError.NetworkError`**: Isolated to the API layer (e.g. server outage, parse errors).
   - **`AppError.DataError`**: Isolated to the Repository layer (e.g. database errors).
   - **`AppError.BusinessError`**: Handled and thrown in the UseCase layer (e.g. validation, offline action restriction, invalid credentials).
   - **`UiError`**: ViewModels transform domain errors into user-friendly UI presentation states cleanly.
6. **Separated ViewModel Architecture**: All ViewModels reside in a dedicated package `com.example.learningdashboard.presentation.viewmodel`, completely decoupled from Compose screen implementations.

---

## 2. Offline & Network Management Implementation

The application guarantees offline resilience through:
- **`NetworkManager`**: Uses Android's `ConnectivityManager` to monitor real-time network capability changes and exposes both a reactive `Flow<Boolean>` and an instantaneous `isCurrentlyOnline()` check.
- **Offline Login Protection**: The Login screen disables credential submission and displays a clear offline warning card whenever internet connectivity is unavailable.
- **DB-First Strategy**:
  - Initially, courses are not fetched from the API if internet is unavailable and Room is empty.
  - If Room already contains courses from a prior session, they are displayed instantly without touching the network.
- **Graceful Refresh Failure**:
  - When the user manually triggers a refresh while offline, existing Room data is preserved, and a non-blocking message informs the user they are in offline mode.
- **DataStore Login State Persistence**:
  - Uses Jetpack Preferences DataStore (`UserPreferencesRepository`) to persist user session state.
  - When the app is launched, `AppNavHost` checks DataStore: if the user was previously logged in, it directly displays `AppRoutes.COURSES`.
  - When the user logs out, DataStore resets `isLoggedIn = false`, and subsequent app launches direct to `AppRoutes.LOGIN`.
- **Initial API Loading on Course Screen**:
  - The Course Dashboard displays a full-screen loading indicator during initial synchronization from the API before rendering course cards.
- **Focus-Based Form Validation**:
  - Email and password validation errors are suppressed while fields are actively focused so typing is uninterrupted.
  - Errors are displayed dynamically only after a field loses focus or when a login submission is attempted.
- **Offline Progress Updates**:
  - Marking lessons as completed writes immediately to Room (`lessonDao.updateLessonCompletion`). The `combine` Flow between courses and lessons recalculates course progress dynamically and pushes new state to the UI without requiring network connectivity.
- **Logout & Database Erasure**:
  - Tapping Logout prompts the user with a confirmation dialog, clears both the courses and lessons tables in Room via `repository.clearAllData()`, and pops the entire backstack back to the Login screen.

### Note on Generative AI Progress Calculation:
The UI spec indicates 40% for the "Generative AI" course, but 6 completed lessons out of 16 equals mathematically 37.5% (or 37% integer). Per the specification, **lesson completion is the sole source of truth**, and progress is purely derived using `ProgressCalculator.calculateProgress(completedLessons, totalLessons)`. This prevents state inconsistencies between lesson checklists and progress indicators.

---

## 3. Production Authentication Token Storage

In a production environment, sensitive authentication tokens (e.g., JWT access tokens, refresh tokens) must never be stored in plain text:

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
   - Add database indexes on foreign keys (`@Index(value = ["courseId"])` on `LessonEntity`).
   - Implement Room FTS4/FTS5 for fast course name, category, and instructor search.
3. **Robust Two-Way Sync Engine (WorkManager)**:
   - Implement an offline sync queue using `WorkManager` with exponential backoff and network constraints.
   - Introduce vector clocks or timestamp-based conflict resolution to sync offline lesson completions with the backend when internet is restored.
4. **CDN & Image Caching**:
   - Integrate Coil with memory and disk LRU caching to handle course thumbnails, instructor avatars, and media banners from a distributed CDN.
5. **Observability & Analytics**:
   - Integrate Firebase Crashlytics, Datadog/Sentry RUM, and OpenTelemetry to track network latencies, database query durations, and crash-free session metrics across high concurrency.

---

## 5. iOS / macOS Equivalent Implementation

| Architectural Component | Android Implementation | iOS / macOS Equivalent |
| :--- | :--- | :--- |
| **Language** | Kotlin | Swift (with Swift Concurrency `async`/`await`) |
| **UI Framework** | Jetpack Compose | SwiftUI (`NavigationStack`, `LazyVStack`) |
| **Architecture** | MVVM (`ViewModel`, `StateFlow`) | MVVM (`@Observable` class in iOS 17+ or `ObservableObject` with `@Published`) |
| **Network Monitoring** | `NetworkManager` (`ConnectivityManager`) | `NWPathMonitor` (`Network.framework`) |
| **Local Persistence** | Room (SQLite) | SwiftData or GRDB / SQLite with `@Query` / `ValueObservation` |
| **Dependency Injection** | `AppContainer` | Swift Environment (`@Environment`), Factory pattern, or Dependencies library |
| **Reactive Streams** | Kotlin `Flow` & Coroutines | Swift `AsyncStream`, `AsyncSequence`, or Apple `Combine` |
| **Secure Token Storage** | Android Keystore / `EncryptedSharedPreferences` | Apple Keychain Services API (`kSecClassGenericPassword`) |
