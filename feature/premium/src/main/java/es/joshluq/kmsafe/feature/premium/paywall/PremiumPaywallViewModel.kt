package es.joshluq.kmsafe.feature.premium.paywall

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.analytics.model.KmAnalyticsEvent
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.service.BillingService
import es.joshluq.kmsafe.domain.usecase.MigrateLocalDataToRemoteUseCase
import es.joshluq.kmsafe.domain.usecase.RestorePurchasesUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateSubscriptionUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import es.joshluq.kmsafe.feature.premium.R

@HiltViewModel
class PremiumPaywallViewModel @Inject constructor(
    private val billingService: BillingService,
    private val updateSubscriptionUseCase: UpdateSubscriptionUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val migrateLocalDataUseCase: MigrateLocalDataToRemoteUseCase,
    private val syncContractsUseCase: SyncContractsUseCase,
    private val analytics: AnalyticsTracker,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        observeBilling()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("PremiumPaywallViewModel", "Event received: $event")
        when (event) {
            is Event.OnInitialize -> {
                updateState { copy(source = event.source) }
                analytics.track(KmAnalyticsEvent.Monetization.PaywallViewed(source = event.source))
            }
            is Event.OnPlanSelected -> {
                analytics.track(KmAnalyticsEvent.Monetization.PlanSelected(event.plan.name))
                updateState { copy(selectedPlan = event.plan) }
            }
            Event.OnUpgradeClicked -> {
                analytics.track(
                    KmAnalyticsEvent.Monetization.UpgradeClicked(
                        source = state.value.source,
                        selectedPlan = state.value.selectedPlan.name
                    )
                )
                launchEffect(Effect.LaunchBillingFlow)
            }
            Event.OnRestorePurchasesClicked -> {
                logger.d("PremiumPaywallViewModel", "Restore purchases clicked")
                handleRestorePurchases()
            }
            Event.OnDismissClicked -> {
                logger.d("PremiumPaywallViewModel", "Dismiss clicked")
                analytics.track(KmAnalyticsEvent.Monetization.PaywallDismissed)
                launchEffect(Effect.NavigateBack)
            }
            Event.OnDismissError -> updateState { copy(error = null) }
            Event.OnDismissMessage -> updateState { copy(message = null) }
        }
    }

    private fun observeBilling() {
        billingService.purchaseSuccessFlow
            .onEach { orderId ->
                logger.i("PremiumPaywallViewModel", "Purchase detected: $orderId")
                handleUpgrade()
            }
            .launchIn(viewModelScope)

        billingService.errorFlow
            .onEach { error ->
                analytics.track(
                    KmAnalyticsEvent.Monetization.PurchaseResult(
                        result = if (error.contains("canceled", ignoreCase = true)) "USER_CANCELED" else "ERROR",
                        errorCode = error,
                        plan = state.value.selectedPlan.name
                    )
                )
                updateState { copy(error = TextProvider.Dynamic(error), isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun handleUpgrade() {
        updateSubscriptionUseCase(UpdateSubscriptionUseCase.Input(SubscriptionLevel.PREMIUM))
            .onEach { output ->
                when (output) {
                    UpdateSubscriptionUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    UpdateSubscriptionUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isLoading = false,
                                error = TextProvider.Resource(R.string.premium_upgrade_error)
                            )
                        }
                    }
                    is UpdateSubscriptionUseCase.Output.Success -> {
                        analytics.track(KmAnalyticsEvent.Monetization.UpgradeSuccess(plan = state.value.selectedPlan.name))
                        startDataMigration()
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleRestorePurchases() {
        restorePurchasesUseCase(RestorePurchasesUseCase.Input)
            .onEach { output ->
                when (output) {
                    RestorePurchasesUseCase.Output.Progress -> {
                        updateState { copy(isLoading = true, isRestoring = true, error = null, message = null) }
                    }
                    is RestorePurchasesUseCase.Output.Success -> {
                        logger.i("PremiumPaywallViewModel", "Purchases restored successfully: ${output.restoredCount}")
                        analytics.track(KmAnalyticsEvent.Monetization.UpgradeSuccess(plan = state.value.selectedPlan.name))
                        updateState {
                            copy(
                                message = TextProvider.Resource(R.string.premium_restore_success),
                                isRestoring = false
                            )
                        }
                        startDataMigration()
                    }
                    RestorePurchasesUseCase.Output.NoPurchasesFound -> {
                        logger.i("PremiumPaywallViewModel", "No active purchases found to restore")
                        updateState {
                            copy(
                                isLoading = false,
                                isRestoring = false,
                                message = TextProvider.Resource(R.string.premium_restore_empty)
                            )
                        }
                    }
                    is RestorePurchasesUseCase.Output.Failure -> {
                        logger.e("PremiumPaywallViewModel", "Restore failed: ${output.message}")
                        updateState {
                            copy(
                                isLoading = false,
                                isRestoring = false,
                                error = TextProvider.Resource(R.string.premium_restore_error)
                            )
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun startDataMigration() {
        migrateLocalDataUseCase(MigrateLocalDataToRemoteUseCase.Input)
            .onEach { output ->
                when (output) {
                    MigrateLocalDataToRemoteUseCase.Output.Progress -> updateState {
                        copy(
                            isLoading = true,
                            isMigrating = true
                        )
                    }
                    MigrateLocalDataToRemoteUseCase.Output.Failure -> {
                        // Migration failed, but user is already premium.
                        // We skip to sync to at least get what's on server.
                        logger.e("PremiumPaywallViewModel", "Migration failed, skipping to final sync")
                        finalizeUpgrade()
                    }
                    MigrateLocalDataToRemoteUseCase.Output.Success -> {
                        logger.i("PremiumPaywallViewModel", "Migration success, finalizing upgrade")
                        finalizeUpgrade()
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun finalizeUpgrade() {
        syncContractsUseCase(SyncContractsUseCase.Input)
            .onEach { output ->
                when (output) {
                    SyncContractsUseCase.Output.Progress -> updateState { copy(isLoading = true, isMigrating = false) }
                    else -> {
                        updateState { copy(isLoading = false, isRestoring = false) }
                        launchEffect(Effect.NavigateToDashboard)
                    }
                }
            }.launchIn(viewModelScope)
    }
}
