package es.joshluq.kmsafe.ui.premium

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.infrastructure.remote.billing.BillingManager
import es.joshluq.kmsafe.domain.di.MigrateLocalDataToRemote
import es.joshluq.kmsafe.domain.di.SyncContracts
import es.joshluq.kmsafe.domain.di.UpdateSubscription
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.usecase.MigrateLocalDataToRemoteUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateSubscriptionUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PremiumPaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    @param:UpdateSubscription private val updateSubscriptionUseCase:
    @JvmSuppressWildcards FlowUseCase<UpdateSubscriptionUseCase.Input, UpdateSubscriptionUseCase.Output>,
    @param:MigrateLocalDataToRemote private val migrateLocalDataUseCase:
    @JvmSuppressWildcards FlowUseCase<MigrateLocalDataToRemoteUseCase.Input, MigrateLocalDataToRemoteUseCase.Output>,
    @param:SyncContracts private val syncContractsUseCase:
    @JvmSuppressWildcards FlowUseCase<SyncContractsUseCase.Input, SyncContractsUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        analytics.track(AnalyticsEvent.ScreenView("premium_paywall"))
        observeBilling()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("PremiumPaywallViewModel", "Event received: $event")
        when (event) {
            Event.OnUpgradeClicked -> {
                analytics.track(AnalyticsEvent.Custom("premium_upgrade_clicked"))
                launchEffect(Effect.LaunchBillingFlow)
            }
            Event.OnDismissClicked -> {
                logger.d("PremiumPaywallViewModel", "Dismiss clicked")
                analytics.track(AnalyticsEvent.Custom("premium_paywall_dismissed"))
                launchEffect(Effect.NavigateBack)
            }
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun observeBilling() {
        billingManager.purchaseSuccessFlow
            .onEach { purchase ->
                logger.i("PremiumPaywallViewModel", "Purchase detected: ${purchase.orderId}")
                handleUpgrade()
            }
            .launchIn(viewModelScope)

        billingManager.errorFlow
            .onEach { error ->
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
                                error = TextProvider.Resource(R.string.onboarding_register_error)
                            )
                        }
                    }
                    is UpdateSubscriptionUseCase.Output.Success -> {
                        analytics.track(AnalyticsEvent.Custom("premium_upgrade_success"))
                        startDataMigration()
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
                        updateState { copy(isLoading = false) }
                        launchEffect(Effect.NavigateToDashboard)
                    }
                }
            }.launchIn(viewModelScope)
    }
}
