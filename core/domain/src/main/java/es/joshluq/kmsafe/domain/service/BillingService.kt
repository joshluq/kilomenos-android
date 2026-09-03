package es.joshluq.kmsafe.domain.service

import kotlinx.coroutines.flow.SharedFlow

/**
 * Domain service interface for observing in-app billing events.
 */
interface BillingService {
    val purchaseSuccessFlow: SharedFlow<String>
    val errorFlow: SharedFlow<String>
}
