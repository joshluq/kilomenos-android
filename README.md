# KiloMenos 🚗💨
**"Intelligent Mileage & Vehicle Financial Management for Renting, Leasing, and Fleets"**

KiloMenos transforms the complexity of renting and leasing contracts into absolute financial and operational control. Eliminate uncertainty regarding excess mileage penalties, automate your trip telemetry, and optimize your vehicle energy expenses with an intelligent co-pilot designed for the modern driver.

---

## 📈 Business Value: Comprehensive Control in Your Hands

In modern vehicle leasing and fleet management, lack of real-time visibility is costly. KiloMenos addresses four critical pillars:

1. **Elimination of Mileage Penalties**: Dynamic, additive balance tracking calculates your daily budget and warns you of projected excess penalties before they occur.
2. **Automated Trip Telemetry**: Seamless hands-free trip logging through car Bluetooth pairing and Activity Recognition.
3. **Smart Fuel & Energy Management**: Comprehensive tracking of fuel and EV charging expenses, station price volatility analysis ("My Stations"), and electrification savings metrics.
4. **Fleet & Contract Flexibility**: Multi-contract management for corporate fleets or private multi-vehicle households.

---

## 🔄 Core Features & Functional Flows

### 1. Dynamic Mileage Balance & Additive Model 📐
KiloMenos uses an **Additive Data Model**. Rather than storing disconnected absolute odometer values, each record represents a discrete increment (a trip or daily distance):
- **Daily Base Budget (DBB)**: `Total Contract Km / Total Contract Days`.
- **Real Km Consumed (RKC)**: Sum of all incremental records.
- **Theoretical Km (TK)**: `Days Elapsed * DBB`.
- **Updated Balance (UB)**: `TK - RKC` (Surplus vs. Penalty Risk).
- **Current Odometer**: `Initial Setup Odometer + RKC`.
*All metric evaluations are centralized in `CalculateContractMetricsUseCase` producing rich, immutable `ContractMetrics` domain models.*

### 2. Auto-Tracking & Sensor Telemetry 🛰️🔌
Powered by the dedicated `:core:tracking` module:
- **Fast-Path Bluetooth ACL Events**: Car Bluetooth connects within 2–5 seconds of vehicle ignition, triggering a silent local notification (`NotificationManager.IMPORTANCE_LOW`) and activating the live connection badge in the UI without network delays.
- **Activity Recognition (`IN_VEHICLE`)**: Wakes up `ActivityTransitionReceiver` via the Android 14+ "Foreground Service First" pattern (synchronous service invocation).
- **The "Triple Check" Validation**: Verifies (1) `AUTO_TRACKING` Premium entitlement, (2) Active contract in Room, and (3) Bluetooth tethering MAC match before recording any GPS telemetry.
- **Anti-Fraud & Accuracy Filters**: Rejects locations with speed < `1.5 m/s` (~5.4 km/h), accuracy error > `30m`, or `location.isFromMockProvider`.

### 3. Smart Fuel & Energy Expenses ("My Stations") ⛽⚡
Comprehensive vehicle financial management located in `:feature:expenses`:
- **Relational Architecture**: 1:N relationship between `ServiceStation` entities and `FuelExpense` records.
- **Station Price Volatility**: Historical price trend histograms showing whether today's price is above or below the user's personal historical average at that station.
- **Mixed Energy Modes (ICE vs. PHEV/EV)**: Dynamic forms and theme adaptations (Orange for fuel, Blue for electric).
- **Electrification Savings KPI**: Calculates the differential financial savings of EV/PHEV kWh consumption against the equivalent fuel cost for the same distance.
- **Google Places Local Caching**: Aggressive local caching prevents duplicate API costs and requires explicit user confirmation before registering new stations.

### 4. Interactive Projections & Risk Gauge 📊🚦
Located in `:feature:projection`:
- **Pacing Simulator**: Simulates contract end dates and mileage forecasts based on actual driving behavior.
- **Trip Planner**: Evaluate whether an upcoming long trip fits within your contract surplus.
- **Projection Risk Gauge**: Real-time visual traffic light for contract health.

### 5. Resilient Offline-First Architecture 📡💾
- **Zero Data Loss**: Stateless repositories delegate persistence to Room and encrypted session storage. Trip tracking state is disk-persisted, surviving process termination.
- **Intelligent Background Sync**: WorkManager (`SyncWorker`) with atomic ID Swap logic resolves device-generated UUID v4 identities with the remote cloud.

---

## 💎 Product Strategy: Freemium Model

Access tiers are strictly governed by session Entitlements:

### 🌟 Core Experience (Free)
* 🚗 **Single Vehicle**: Manage your primary renting or leasing contract.
* 💾 **Local-First Persistence**: Data stored in Room; sync disabled with records tagged as `PENDING`.
* 🛰️ **Assisted GPS Tracking**: Manual trip start/stop with distance accumulation.
* 📺 **Non-Intrusive Monetization**: AdMob banners initialized strictly post-GDPR/UMP consent.
* 📊 **Basic Projections & Balance**: Real-time contract balance and standard consumption charts.
* 📤 **Fair-Use Export**: Export mileage records to CSV for personal records.

### 👑 Premium Experience (Paid)
* 💳 **Native Billing**: Google Play Billing Library v7+ integration with frictionless upgrades.
* ☁️ **Remote Cloud Sync**: Real-time multi-device sync and automatic batch promotion of local pending records.
* 🚜 **Fleet Management**: Unlimited vehicles and contracts with instant active switcher.
* 🛰️ **Auto-Tracking**: Hands-free trip detection via Activity Recognition and car Bluetooth pairing.
* ⛽ **Advanced Energy Analytics**: Station volatility histograms, refueling insights, and electrification savings KPIs.
* 📥 **Full Data Portability**: Complete JSON database backup and restoration.

---

## 🏗️ Multi-Module Architecture & Guidelines

KiloMenos is structured as a decoupled, multi-module architecture following **Clean Architecture**, **SOLID**, and **Domain-Driven Design (DDD)**.

### Core Principle: Separating the "Being" from the "Doing" ("Separar el 'Hacer' del 'Ser'")
- **The "Being" (El Ser)**: Rich domain entities with invariants (`RentingContract`, `ContractMetrics`, `FuelExpense`, `ServiceStation`), public contracts, and immutable MVI UI state.
- **The "Doing" (El Hacer)**: Standardized UseCases (`FlowUseCase` - orchestration only), ViewModels/MVI Reducers (events to immutable state with zero business math), Room DAOs, and network services.

### Module Topology:
```
├── :app                         # Thin shell (Application class, Manifest, Root Navigation, DI Root)
├── :core:domain                 # Pure Kotlin/JVM library (Zero Android dependencies, 60 UseCases)
├── :core:infrastructure         # Room DAOs/Entities, Retrofit, DataSources, SyncWorker, DI Modules
├── :core:navigation             # Typed Compose destinations (Destination.kt)
├── :core:ui                     # Design system, tokens, and CanvasKit components
├── :core:monetization           # AdMob SDK and GDPR/UMP consent management
├── :core:tracking               # LocationTrackingService, Bluetooth and Activity Receivers
└── :feature:*                   # Isolated UI features (MVI + Coordinator Route)
    ├── :feature:dashboard       # Tab host with CanvasKitBottomBar using slot pattern
    ├── :feature:overview        # Tab 1: Real-time balance, odometer, consumption charts
    ├── :feature:history         # Tab 2: Odometer increment logs, GPS trip routes
    ├── :feature:expenses        # Tab 3: Fuel/EV expenses, My Stations, volatility histograms
    ├── :feature:projection      # Tab 4: Pacing simulator, trip planner, risk gauge
    ├── :feature:profile         # Tab 5: Profile, preferences, DataManagement import/export
    ├── :feature:fleet           # Vehicle setup wizard, list, detail, contract editor
    ├── :feature:auth            # Launch, Login, Signup, lifecycle session management
    └── :feature:premium         # Paywall, subscription offers, Google Play Billing
```

### Architectural Guardrails:
1. **Feature Isolation**: `:feature:*` modules depend ONLY on `:core:domain`, `:core:ui`, `:core:navigation`, and `:core:monetization`. **Features never depend directly on `:core:infrastructure`**.
2. **Pure Domain Shield**: `:core:domain` is a Kotlin/JVM module (`pluginkit.jvm.library`) with zero platform types (`android.*`).
3. **SSOT for Calculations**: ViewModels never perform arithmetic shortcuts on metrics. All calculations originate from Domain UseCases (`CalculateContractMetricsUseCase`).
4. **"UseCase First" Mandate**: Every single operation or process must be encapsulated in a dedicated UseCase. ViewModels, Workers, and Services never consume Repositories directly.

---

## 🧪 Testing & Quality Assurance

* **100% Domain Coverage**: 60 UseCases tested with JUnit 4, MockK, and Kotlin Coroutines Test (`runTest`).
* **100% ViewModel Coverage**: 19 Feature ViewModels tested with MockK and `StandardTestDispatcher` / `UnconfinedTestDispatcher`, verifying states, events, and side-effects.
* **Fast JVM Test Execution**: Domain runs purely on the JVM without Android Robolectric or emulator overhead (~5 seconds execution time).

---

## 🛠️ Tech Stack

* **UI Framework**: Jetpack Compose + Material 3 + CanvasKit Design System.
* **Architecture**: Multi-Module Clean Architecture + DDD + MVI + Coordinator (Route) pattern.
* **Dependency Injection**: Dagger Hilt with Convention Plugins (`pluginkit.android.hilt`).
* **Asynchrony**: Kotlin Coroutines & Flow (structured concurrency via `DispatcherProvider`).
* **Persistence**: Room v2.6+ with relational 1:N schema and client-side UUID v4 identity.
* **Telemetry & Sensors**: Fused Location Provider, Android 14+ Foreground Service (`location`), Google Play Activity Recognition API, Bluetooth ACL hardware broadcasts.
* **Networking & Sync**: Retrofit + OkHttp with JWT auth, WorkManager (`SyncWorker`) with ID swap logic.
* **Monetization & Privacy**: Google Play Billing Library v7+, Google AdMob, Google UMP (GDPR / EAA).
* **Build System**: Gradle Version Catalogs (`libs.versions.toml`, `deps.versions.toml`) and Custom Convention Plugins.

---

## ⚖️ Ethics & Compliance (EU Standards)

* 🛡️ **GDPR / RGPD Compliant**: Explicit account deletion wiping Auth, Room, Preferences, and Cloud records ("Right to be Forgotten").
* 🇪🇺 **European Accessibility Act (EAA)**: Mandatory `contentDescription` on all interactive UI components, optimized for TalkBack.
* 🔒 **Encrypted Storage Hygiene**: Sensitive session stores excluded from Android Auto Backup to prevent post-reinstall Keystore corruption.
