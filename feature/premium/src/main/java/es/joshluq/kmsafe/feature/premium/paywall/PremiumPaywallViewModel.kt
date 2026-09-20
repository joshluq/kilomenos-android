package es.joshluq.kmsafe.feature.premium.paywall

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.analytics.model.KmAnalyticsEvent
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.service.BillingService
import es.joshluq.kmsafe.domain.usecase.MigrateLocalDataToRemoteUseCase
import es.joshluq.kmsafe.domain.usecase.RestorePurchasesUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateSubscriptionUseCase
import es.joshluq.kmsafe.feature.premium.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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

    private var loadingWatchdogJob: Job? = null

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
                if (state.value.isLoading) return
                analytics.track(KmAnalyticsEvent.Monetization.PlanSelected(event.plan.name))
                updateState { copy(selectedPlan = event.plan) }
            }
            Event.OnUpgradeClicked -> {
                if (state.value.isLoading) return
                analytics.track(
                    KmAnalyticsEvent.Monetization.UpgradeClicked(
                        source = state.value.source,
                        selectedPlan = state.value.selectedPlan.name
                    )
                )
                launchEffect(Effect.LaunchBillingFlow)
            }
            Event.OnRestorePurchasesClicked -> {
                if (state.value.isLoading) return
                logger.d("PremiumPaywallViewModel", "Restore purchases clicked")
                handleRestorePurchases()
            }
            Event.OnDismissClicked -> {
                if (state.value.isLoading) return
                logger.d("PremiumPaywallViewModel", "Dismiss clicked")
                analytics.track(KmAnalyticsEvent.Monetization.PaywallDismissed)
                launchEffect(Effect.NavigateBack)
            }
            Event.OnDismissError -> updateState { copy(error = null) }
            Event.OnDismissMessage -> updateState { copy(message = null) }
        }
    }

    private fun observeBilling() {
        billingService.purchaseProcessingFlow
            .onEach { isProcessing ->
                logger.i("PremiumPaywallViewModel", "Purchase processing flow event: $isProcessing")
                if (isProcessing) {
                    updateState { copy(isLoading = true, error = null) }
                    startLoadingWatchdog()
                } else {
                    if (!state.value.isMigrating) {
                        stopLoadingWatchdog()
                        updateState { copy(isLoading = false) }
                    }
                }
            }
            .launchIn(viewModelScope)

        billingService.purchaseSuccessFlow
            .onEach { orderId ->
                logger.i("PremiumPaywallViewModel", "Purchase detected: $orderId")
                handleUpgrade()
            }
            .launchIn(viewModelScope)

        billingService.errorFlow
            .onEach { error ->
                stopLoadingWatchdog()
                analytics.track(
                    KmAnalyticsEvent.Monetization.PurchaseResult(
                        result = if (error.contains("canceled", ignoreCase = true)) "USER_CANCELED" else "ERROR",
                        errorCode = error,
                        plan = state.value.selectedPlan.name
                    )
                )
                updateState { copy(error = TextProvider.Dynamic(error), isLoading = false, isRestoring = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun handleUpgrade() {
        startLoadingWatchdog()
        updateSubscriptionUseCase(UpdateSubscriptionUseCase.Input(SubscriptionLevel.PREMIUM))
            .onEach { output ->
                when (output) {
                    UpdateSubscriptionUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    UpdateSubscriptionUseCase.Output.Failure -> {
                        stopLoadingWatchdog()
                        updateState {
                            copy(
                                isLoading = false,
                                isMigrating = false,
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
                        startLoadingWatchdog()
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
                        stopLoadingWatchdog()
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
                        stopLoadingWatchdog()
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
                        stopLoadingWatchdog()
                        updateState { copy(isLoading = false, isRestoring = false, isMigrating = false) }
                        launchEffect(Effect.NavigateToDashboard)
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun startLoadingWatchdog(timeoutMillis: Long = 25_000L) {
        loadingWatchdogJob?.cancel()
        loadingWatchdogJob = viewModelScope.launch {
            delay(timeoutMillis.milliseconds)
            if (state.value.isLoading) {
                logger.w("PremiumPaywallViewModel", "Watchdog triggered: operation timed out after $timeoutMillis ms")
                updateState {
                    copy(
                        isLoading = false,
                        isRestoring = false,
                        isMigrating = false,
                        error = TextProvider.Resource(R.string.premium_operation_timeout)
                    )
                }
            }
        }
    }

    private fun stopLoadingWatchdog() {
        loadingWatchdogJob?.cancel()
        loadingWatchdogJob = null
    }

    override fun onCleared() {
        stopLoadingWatchdog()
    }
}
