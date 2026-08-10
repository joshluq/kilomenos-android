# KiloMenos 🚗💨
**"Intelligent Mileage Management for Renting and Leasing"**

KiloMenos transforms the complexity of renting contracts into absolute financial control. Forget about excess mileage penalties and optimize your investment with a technical assistant that understands your lifestyle.

---

## 📈 Business Value: Control in Your Hands

In the renting ecosystem, lack of information is costly. KiloMenos directly addresses the three pain points of the modern driver:

1.  **Elimination of Penalties:** Through preventive monitoring, we avoid unexpected bills at the end of the contract.
2.  **Asset Optimization:** Maximize the use of the kilometers you've already paid for. If you don't drive today, your kilometers are saved for your vacation.
3.  **Fleet Management:** Manage multiple vehicles independently (Premium) or your daily driver (Free).

---

## 🔄 Functional Flows and User Experience

### 1. Multi-Vehicle Configuration
Define limits for one or several contracts. The app maintains independent histories for each vehicle.

### 2. Assisted GPS Tracking 🛰️🕹️
Register your trips in real-time with high precision. Use the "Co-Pilot" mode to track your kilometers via GPS without manual typing. The app follows an **incremental model**: you only need to register the kilometers of your trip, and KiloMenos automatically updates your total balance.

### 3. Resilience and Offline-First 📡
KiloMenos is designed to work anywhere. Your data is always saved locally first and automatically synchronized with the cloud using **WorkManager** as soon as a network connection is detected.
*   ✅ **Zero Data Loss**: Trip tracking state persists on disk, ensuring current trips survive system restarts, battery depletion, or low-memory process kills.
*   ✅ **Dynamic Entitlements**: A granular feature-gating system allows instant access to Premium features or Trial periods without app updates.

### 4. Professional Analytics 📊
Interactive charts with legends and contextual help to understand your consumption patterns at a glance.

---

## 💎 Product Strategy: Freemium Model

KiloMenos follows a **Local-First** priority to ensure maximum privacy and offline reliability.

### 🌟 Core Experience (Free)
*   🚗 **Single Vehicle**: Track your main renting contract with full precision.
*   💾 **Local-Only Storage**: Your mileage data is persisted in a Room database.
*   🛰️ **Manual GPS Tracking**: High-precision trip logging (Asisted Co-Pilot).
*   📺 **Discreet Ads**: Non-intrusive banner ads powered by **Google AdMob**.
*   📊 **Basic Analytics**: Real-time balance and daily limit calculations.
*   📤 **Fair-Use Export**: Export your data to CSV (Excel) for personal records.

### 👑 Premium Services (Remote)
*   💳 **Native Subscription**: Seamless upgrade via **Google Play Billing**.
*   ☁️ **Cloud Sync**: Securely backup and sync your mileage data across multiple devices.
*   🚜 **Fleet Mode**: Manage an unlimited number of vehicles and contracts simultaneously.
*   📥 **Data Portability**: Full JSON backup and restoration capabilities.
*   🤖 **Advanced Projections**: Predictive financial impact analysis based on usage trends.

---

## ⚖️ Ethics and Compliance (EU Standards)

KiloMenos is built with the highest European standards:
*   🛡️ **GDPR Compliant**: Full "Right to be Forgotten" with permanent account and data deletion from the app.
*   ⚖️ **Transparency**: Integrated privacy policy and terms of service in Login/Signup and Profile.
*   🛡️ **Consent Management**: Granular privacy controls via UMP SDK.
*   ♿ **Accessibility (EAA)**: Optimized for TalkBack and inclusive interaction, following the European Accessibility Act standards.

---

## 🏗️ Reliability Guarantee: Clean Architecture & MVI

*   **Clean Architecture (Domain-Driven):** Isolated business logic ensures 100% calculation accuracy.
*   **Relational Persistence (Room v3):** Robust data integrity with versioned migrations and String-based IDs.
*   **MVI (Model-View-Intent):** Unidirectional data flow for a predictable and synchronized UI.
*   **Background Sync**: Intelligent background processing with WorkManager and ID consistency logic.

---

## 🛠️ Tech Stack
*   **UI:** Jetpack Compose (Material 3).
*   **DI:** Dagger Hilt.
*   **Network:** Retrofit + OkHttp with JWT auth and segregated public/private channels.
*   **Async:** Kotlin Coroutines & Flow.
*   **Monetization:** Google Play Billing & AdMob.
*   **Infrastructure:** buildSrc for environment-based configuration.
