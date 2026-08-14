# AGENTS.md - KiloMenos Architecture & Guidelines 🚗

## 1. Objective
The goal of **KiloMenos** is to provide a high-quality tool for renting or leasing drivers, allowing them to manage **multiple** mileage consumption contracts daily and cumulatively. The app eliminates uncertainty regarding excess mileage penalties through a dynamic "balance" calculation per vehicle.

## 2. Architecture
The project follows **Clean Architecture** and **Domain-Driven Design (DDD)** principles within a single module (`:app`). Priority is given to readability, testability, and strict isolation of business rules.

### Layers:
- **`buildSrc/`**: Kotlin DSL for centralized dependency and environment-based configuration (API URLs, Legal T&C, AdMob IDs, Billing SKUs).
- **`data/`**: Repository implementations, data sources (Room DAOs and Entities), and mappers.
- **`domain/`**: Pure business logic. **Strict rule: Zero Android dependencies.**
- **`ui/`**: User interface with Jetpack Compose using the **MVI (Model-View-Intent)** pattern and the **Coordinator (Route)** pattern to decouple navigation from business logic.
- **`di/`**: Hilt modules for dependency injection.
- **`designsystem/`**: Reusable components via **CanvasKit**.

## 3. Business Logic (The KiloMenos Algorithm)
KiloMenos uses an **Additive Data Model**. Instead of storing absolute odometer snapshots, each `OdometerRecord` represents an **increment** (a specific trip or daily distance).

### Core Calculations:
1.  **Daily Base Budget (DBB):** `Total Km / Total Contract Days`.
2.  **Real Km Consumed (RKC):** Sum of all `odometerValue` in the history (excluding the initial record).
3.  **Theoretical Km (TK):** `Days Elapsed * DBB`.
4.  **Updated Balance (UB):** `TK - RKC`.
5.  **Current Odometer:** `Initial Odometer + RKC`.

## 4. Tech Stack
- **UI Framework**: Jetpack Compose + Material 3.
- **Dependency Injection**: Dagger Hilt.
- **Asynchrony**: Kotlin Coroutines & Flow (via `DispatcherProvider`).
- **Persistence**: Room (Relational model with String-based IDs (v8) for Cloud compatibility).
- **Background Sync**: WorkManager for reliable offline-to-online data promotion.
- **Location Services**: Fused Location Provider with Foreground Services for persistent trip tracking.

## 5. Coding Standards & Communication
- **KDoc**: Technical documentation mandatory and exclusively in **English**.
- **Main-Safety**: Repositories are responsible for threading (using `dispatchers.io`). High-performance UI logic (filters, groupings) must use `dispatchers.default`.
- **Domain Purity**: No platform-specific types (e.g., Context) in the domain layer.
- **RGPD Compliance**: Explicit "Delete Account" flows to wipe both local and remote data (Auth, DB, and Preferences).
- **Accessibility**: Mandatory `contentDescription` for all interactive elements to comply with EAA standards.

## 6. Product Strategy: Freemium Model 💎
KiloMenos implements a tiered access model to balance user value and monetization:

### Core Version (Free):
- **Local-First**: Data is stored in Room; sync is disabled but state is tracked as `PENDING`.
- **Assisted GPS Tracking**: Manual trip start/stop with distance accumulation.
- **Single Vehicle**: Limited to one active renting contract.
- **Ad-Supported**: Non-intrusive AdMob banners initialized after user consent.
- **Fair-Use Export**: Only CSV format is allowed.

### Premium Version (Paid):
- **Native Billing**: Managed via Google Play Billing Library.
- **Remote Synchronization**: Real-time cloud backup and multi-device sync.
- **Data Promotion**: Automatic "push" of local data to the cloud upon upgrade.
- **Fleet Management**: Unlimited vehicle contracts and active switcher.
- **Data Portability**: Full JSON backup and restoration.
- **Advanced Insights**: AI-driven financial projections.

## 7. Technical Roadmap 🚀
Planned and Achieved high-value implementations:
1.  ✅ **Offline Sync**: Reliable WorkManager integration with ID Swap logic.
2.  ✅ **Native Billing**: Fully integrated Google Play Billing flow.
3.  ✅ **Legibility Fix**: Chart legends and interactive info dialogs.
4.  ✅ **GDPR/EAA**: Full European regulation compliance.
5.  ✅ **GPS Tracking (Fase 1)**: Foreground Service for manual trip recording.
6.  ✅ **Resilient Tracking**: Disk-persisted tracking state (Stateless Repositories).
7.  📅 **Computer Vision (OCR)**: ML Kit for dashboard scanning.
8.  📅 **Smart Tracking (Fase 2)**: Activity Recognition for automated trip detection.
9.  📅 **Reporting Engine**: PDF generator for professional reports.

## 8. Stateless Repositories (Anti-Pattern Prevention)
All repositories MUST be **stateless**.
- **Forbidden**: Using `MutableStateFlow` or `var` variables inside a Repository to store business state (e.g., accumulated distance, permissions).
- **Mandatory**: Delegate persistence to a `DataSource` (Room/DataStore) or the `AuthKit` session. The repository should only orquestrate reactive flows.

## 9. SSOT: Identity vs. Access (Entitlements)
- **Identity**: Managed by the `User` object (ID, Email, Name).
- **Access/Permissions**: Managed by the `Entitlements` object persisted in the **encrypted session**.
- **Rule**: Never query the `User` object for subscription levels. Always use the `GetEntitlementsUseCase` or `CheckFeatureAccessUseCase`.

## 10. Critical Sequential Flows
To avoid race conditions during app initialization (Cold Start):
- **Login/Launch**: MUST synchronize `Entitlements` first. Only after a response (Success/Failure) is the contract/vehicle synchronization allowed to start. This ensures the sync engine knows the user's rights before evaluating data promotion.

## 11. Idempotency & Client-Side Identity
To ensure data integrity and prevent duplicates in unstable network conditions:
- **Client-Side IDs**: The App is the owner of identity. Every `RentingContract` and `OdometerRecord` is born with a UUID v4 generated on the device.
- **Idempotent Requests**: POST requests include the `id` in the body. The backend uses this as the Primary Key or Idempotency Key to avoid creating duplicate rows on retries.
- **Identity Resolution**: The `SyncIdHandler` centralizes the logic for verifying if a remote response matches the local ID, handling atomic "ID Swaps" for legacy fallback scenarios.

## 12. Navigation & Result Handling (Coordinator Pattern)
To maintain pure ViewModels and testable Screens:
- **Routes**: Composable functions at the navigation level acting as coordinators. They are the only ones allowed to observe the `NavBackStackEntry` for navigation results (e.g., `cropped_uri`).
- **ViewModels**: Agnostic to navigation infrastructure. They receive navigation results via UI Events triggered by the Route coordinators.
- **SavedStateHandle**: Used in ViewModels only for **input arguments** (e.g., `vehicleId`), not for transient navigation results.

