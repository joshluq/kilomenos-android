# Roadmap & Architecture: Multi-Plan Subscriptions (Annual & Monthly) 💎

## 1. Executive Summary & Current Status (Phase 1)
For the initial production release of **KiloMenos**, the monetization layer is streamlined into a single, transparent, and robust **Monthly Plan**:
* **Active SKU**: `subscription_premium_monthly` (defined in `buildSrc/src/main/java/Config.kt`).
* **Paywall Alignment**: `:feature:premium` presents a single focused subscription card with clear recurring monthly billing (`premium_plan_monthly_price`), direct CTA (`premium_cta_monthly`), and 1-click Google Play cancellation guarantee (`premium_guarantee_notice`).
* **Compliance**: Eliminates any risk of rejection under Google Play's *Subscription and Financial Disclosures* policy by ensuring 100% parity between what is displayed on screen and what Google Play Billing charges.

This document serves as the technical blueprint to re-introduce the **Annual Plan (with 7-Day Free Trial)** in **Phase 2** without regressions.

---

## 2. Phase 2 Architecture: Technical Blueprint

When ready to offer both **Monthly** and **Annual** options, implement the following changes across modules:

### 2.1 Google Play Console Setup
You can model the Annual plan using either of two Google Play Billing approaches:

#### Approach A: Multi-Base-Plan under Single Subscription (Recommended by Google)
* **Subscription ID**: `subscription_premium`
* **Base Plan 1**: `monthly-base` (Period: 1 month, Auto-renewing).
* **Base Plan 2**: `annual-base` (Period: 1 year, Auto-renewing, with 7-Day Free Trial Offer).
* *Advantage*: Native support for upgrades/downgrades and proration modes within the same subscription group.

#### Approach B: Two Distinct Subscription Products (Simpler Legacy Architecture)
* **Product 1**: `subscription_premium_monthly` (1 month recurring).
* **Product 2**: `subscription_premium_annual` (1 year recurring, with 7-day free trial offer attached).

---

### 2.2 Configuration Layer (`buildSrc` & `:app`)

1. **Update `Config.kt` (`buildSrc/src/main/java/Config.kt`)**:
   ```kotlin
   interface Environment {
       val premiumMonthlySku: String
       val premiumAnnualSku: String
       // ...
   }

   object Development : Environment {
       override val premiumMonthlySku: String = "subscription_premium_monthly"
       override val premiumAnnualSku: String = "subscription_premium_annual"
   }

   object Production : Environment {
       override val premiumMonthlySku: String = "subscription_premium_monthly"
       override val premiumAnnualSku: String = "subscription_premium_annual"
   }
   ```

2. **Update `InfrastructureConfig.kt` (`:core:infrastructure`)**:
   ```kotlin
   interface InfrastructureConfig {
       val premiumMonthlySku: String
       val premiumAnnualSku: String
   }
   ```

3. **Expose in `app/build.gradle.kts`**:
   ```kotlin
   buildConfigField("String", "PREMIUM_MONTHLY_SKU", "\"${env.premiumMonthlySku}\"")
   buildConfigField("String", "PREMIUM_ANNUAL_SKU", "\"${env.premiumAnnualSku}\"")
   ```

---

### 2.3 Infrastructure Layer (`BillingManager.kt`)

Update `BillingManager` to accept the targeted plan and resolve the appropriate `ProductDetails` and `offerToken`:

```kotlin
fun launchBillingFlow(activity: Activity, plan: PremiumBillingPlan) {
    val targetSku = when (plan) {
        PremiumBillingPlan.MONTHLY -> config.premiumMonthlySku
        PremiumBillingPlan.ANNUAL -> config.premiumAnnualSku
    }

    val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
        .setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(targetSku)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        )
        .build()

    billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, queryProductDetailsResult ->
        val productDetailsList = queryProductDetailsResult.productDetailsList
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
            val productDetails = productDetailsList[0]
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }
}
```

---

### 2.4 Presentation & Navigation Layer

1. **Update `Contract.kt` (`:feature:premium`)**:
   ```kotlin
   sealed interface Effect : UiEffect {
       data class LaunchBillingFlow(val plan: PremiumBillingPlan) : Effect
       data object NavigateToDashboard : Effect
       data object NavigateBack : Effect
   }
   ```

2. **Update `PremiumPaywallViewModel.kt`**:
   ```kotlin
   Event.OnUpgradeClicked -> {
       analytics.track(
           KmAnalyticsEvent.Monetization.UpgradeClicked(
               source = state.value.source,
               selectedPlan = state.value.selectedPlan.name
           )
       )
       launchEffect(Effect.LaunchBillingFlow(state.value.selectedPlan))
   }
   ```

3. **Update `AppNavigation.kt` & `MainActivity.kt`**:
   Pass the selected plan from `PremiumPaywallRoute` down to `billingManager.launchBillingFlow(this, plan)`:
   ```kotlin
   PremiumPaywallRoute(
       source = key.source,
       onNavigateToDashboard = { /* ... */ },
       onNavigateBack = onBack,
       onLaunchBilling = { selectedPlan ->
           onLaunchBilling(selectedPlan)
       }
   )
   ```

4. **Update `PremiumPaywallScreen.kt`**:
   Re-enable interactive multi-card selection:
   * **Card 1**: Annual Plan (with `badge = "7 DÍAS GRATIS · AHORRA 40%"`).
   * **Card 2**: Monthly Plan (with `badge = "Flexibilidad total"`).
   * Dynamic CTA button:
     * When `ANNUAL` is selected: `"Probar 7 días gratis"`
     * When `MONTHLY` is selected: `"Comenzar suscripción mensual"`

---

### 2.5 Best Practice: Dynamic Store Currency & Localized Prices
Rather than relying on static Euro strings in `strings.xml` (`24,99 € / año`), `BillingManager` should query `ProductDetails` at startup or on paywall enter, exposing:
```kotlin
data class SubscriptionPlanDetails(
    val plan: PremiumBillingPlan,
    val formattedPrice: String,      // e.g. "$24.99", "£21.99", "24,99 €"
    val billingPeriod: String,
    val hasFreeTrial: Boolean
)
```
This ensures international users in LatAm, US, or UK see their exact local currency and Google Play pricing before tapping the purchase button.

---

## 3. Verification Checklist for Phase 2 Rollout
- [ ] License testing configured in Google Play Console for both Monthly and Annual base plans.
- [ ] Verify that selecting `ANNUAL` launches Google Play checkout showing the 7-day trial and annual charge date.
- [ ] Verify that selecting `MONTHLY` launches Google Play checkout with immediate monthly charge.
- [ ] Verify that `purchaseSuccessFlow` acknowledges the purchase within 3 days for both plans.
- [ ] Verify that `MigrateLocalDataToRemoteUseCase` triggers on first successful activation regardless of plan type.
- [ ] Verify backend Google Cloud Pub/Sub handles renewals and expirations for both SKUs.
