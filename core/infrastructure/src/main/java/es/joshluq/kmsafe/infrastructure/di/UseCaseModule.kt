package es.joshluq.kmsafe.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.core.domain.usecase.HandleGeofenceTransitionUseCase
import es.joshluq.kmsafe.core.domain.usecase.HandleGeofenceTransitionUseCaseImpl
import es.joshluq.kmsafe.core.domain.usecase.SyncStationGeofencesUseCase
import es.joshluq.kmsafe.core.domain.usecase.SyncStationGeofencesUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.AddOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.AddOdometerRecordUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.CalculateContractMetricsUseCase
import es.joshluq.kmsafe.domain.usecase.CalculateContractMetricsUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.CheckDatabaseOwnerUseCase
import es.joshluq.kmsafe.domain.usecase.CheckDatabaseOwnerUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCase
import es.joshluq.kmsafe.domain.usecase.CheckSessionUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.ClearLocalDataUseCase
import es.joshluq.kmsafe.domain.usecase.ClearLocalDataUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.ClearTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.ClearTrackingUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.DeleteAccountUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteAccountUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.DeleteContractUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteContractUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.DeleteFuelExpenseUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteFuelExpenseUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.DeleteOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteOdometerRecordUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.DeleteServiceStationUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteServiceStationUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.EvaluateIdentityConflictUseCase
import es.joshluq.kmsafe.domain.usecase.EvaluateIdentityConflictUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.ExportDataUseCase
import es.joshluq.kmsafe.domain.usecase.ExportDataUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetAllServiceStationsUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllServiceStationsUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetElectrificationSavingsUseCase
import es.joshluq.kmsafe.domain.usecase.GetElectrificationSavingsUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetExpensesByVehicleUseCase
import es.joshluq.kmsafe.domain.usecase.GetExpensesByVehicleUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetFavoriteServiceStationsUseCase
import es.joshluq.kmsafe.domain.usecase.GetFavoriteServiceStationsUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetHistoryUseCase
import es.joshluq.kmsafe.domain.usecase.GetHistoryUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetImageBytesUseCase
import es.joshluq.kmsafe.domain.usecase.GetImageBytesUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetMonthlyUsageUseCase
import es.joshluq.kmsafe.domain.usecase.GetMonthlyUsageUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetOdometerRecordUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetRouteUseCase
import es.joshluq.kmsafe.domain.usecase.GetRouteUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetServiceStationDetailUseCase
import es.joshluq.kmsafe.domain.usecase.GetServiceStationDetailUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetStationVolatilityUseCase
import es.joshluq.kmsafe.domain.usecase.GetStationVolatilityUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.GetVehicleByIdUseCase
import es.joshluq.kmsafe.domain.usecase.GetVehicleByIdUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.ImportDataUseCase
import es.joshluq.kmsafe.domain.usecase.ImportDataUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.MigrateLocalDataToRemoteUseCase
import es.joshluq.kmsafe.domain.usecase.MigrateLocalDataToRemoteUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.ObserveTrackingStateUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveTrackingStateUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SaveFuelExpenseUseCase
import es.joshluq.kmsafe.domain.usecase.SaveFuelExpenseUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SaveInitialContractUseCase
import es.joshluq.kmsafe.domain.usecase.SaveInitialContractUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SaveServiceStationUseCase
import es.joshluq.kmsafe.domain.usecase.SaveServiceStationUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SetFavoriteStationUseCase
import es.joshluq.kmsafe.domain.usecase.SetFavoriteStationUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SignInUseCase
import es.joshluq.kmsafe.domain.usecase.SignInUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SignInWithGoogleUseCase
import es.joshluq.kmsafe.domain.usecase.SignInWithGoogleUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SignOutUseCase
import es.joshluq.kmsafe.domain.usecase.SignOutUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SignUpUseCase
import es.joshluq.kmsafe.domain.usecase.SignUpUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.StartAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StartAutoTrackingUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.StartTrialUseCase
import es.joshluq.kmsafe.domain.usecase.StartTrialUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.StopAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopAutoTrackingUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.StopTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopTrackingUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SyncHistoryUseCase
import es.joshluq.kmsafe.domain.usecase.SyncHistoryUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.SyncStationsUseCase
import es.joshluq.kmsafe.domain.usecase.SyncStationsUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.UpdateContractUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateContractUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.UpdateOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateOdometerRecordUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.UpdateSubscriptionUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateSubscriptionUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.UploadVehicleImageUseCase
import es.joshluq.kmsafe.domain.usecase.UploadVehicleImageUseCaseImpl
import es.joshluq.kmsafe.domain.usecase.ValidateCredentialsUseCase
import es.joshluq.kmsafe.domain.usecase.ValidateCredentialsUseCaseImpl

/**
 * Dagger module providing domain UseCase dependencies.
 * Follows Option 2: Specific Domain Interfaces without Qualifiers.
 * Lives in `:core:infrastructure` to keep `:app` as a pure orchestration shell.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    abstract fun bindCalculateContractMetricsUseCase(impl: CalculateContractMetricsUseCaseImpl): CalculateContractMetricsUseCase

    @Binds
    abstract fun bindGetRentingContractUseCase(impl: GetRentingContractUseCaseImpl): GetRentingContractUseCase

    @Binds
    abstract fun bindGetHistoryUseCase(impl: GetHistoryUseCaseImpl): GetHistoryUseCase

    @Binds
    abstract fun bindGetOdometerRecordUseCase(impl: GetOdometerRecordUseCaseImpl): GetOdometerRecordUseCase

    @Binds
    abstract fun bindDeleteOdometerRecordUseCase(impl: DeleteOdometerRecordUseCaseImpl): DeleteOdometerRecordUseCase

    @Binds
    abstract fun bindEvaluateIdentityConflictUseCase(impl: EvaluateIdentityConflictUseCaseImpl): EvaluateIdentityConflictUseCase

    @Binds
    abstract fun bindUpdateOdometerRecordUseCase(impl: UpdateOdometerRecordUseCaseImpl): UpdateOdometerRecordUseCase

    @Binds
    abstract fun bindUploadVehicleImageUseCase(impl: UploadVehicleImageUseCaseImpl): UploadVehicleImageUseCase

    @Binds
    abstract fun bindSaveInitialContractUseCase(impl: SaveInitialContractUseCaseImpl): SaveInitialContractUseCase

    @Binds
    abstract fun bindUpdateContractUseCase(impl: UpdateContractUseCaseImpl): UpdateContractUseCase

    @Binds
    abstract fun bindUpdateSubscriptionUseCase(impl: UpdateSubscriptionUseCaseImpl): UpdateSubscriptionUseCase

    @Binds
    abstract fun bindMigrateLocalDataToRemoteUseCase(impl: MigrateLocalDataToRemoteUseCaseImpl): MigrateLocalDataToRemoteUseCase

    @Binds
    abstract fun bindGetMonthlyUsageUseCase(impl: GetMonthlyUsageUseCaseImpl): GetMonthlyUsageUseCase

    @Binds
    abstract fun bindGetOverviewDataUseCase(impl: GetOverviewDataUseCaseImpl): GetOverviewDataUseCase

    @Binds
    abstract fun bindGetPreferencesUseCase(impl: GetPreferencesUseCaseImpl): GetPreferencesUseCase

    @Binds
    abstract fun bindUpdatePreferencesUseCase(impl: UpdatePreferencesUseCaseImpl): UpdatePreferencesUseCase

    @Binds
    abstract fun bindAddOdometerRecordUseCase(impl: AddOdometerRecordUseCaseImpl): AddOdometerRecordUseCase

    @Binds
    abstract fun bindGetTripProjectionUseCase(impl: GetTripProjectionUseCaseImpl): GetTripProjectionUseCase

    @Binds
    abstract fun bindExportDataUseCase(impl: ExportDataUseCaseImpl): ExportDataUseCase

    @Binds
    abstract fun bindImportDataUseCase(impl: ImportDataUseCaseImpl): ImportDataUseCase

    @Binds
    abstract fun bindGetAllContractsUseCase(impl: GetAllContractsUseCaseImpl): GetAllContractsUseCase

    @Binds
    abstract fun bindSelectContractUseCase(impl: SelectContractUseCaseImpl): SelectContractUseCase

    @Binds
    abstract fun bindDeleteContractUseCase(impl: DeleteContractUseCaseImpl): DeleteContractUseCase

    @Binds
    abstract fun bindDeleteAccountUseCase(impl: DeleteAccountUseCaseImpl): DeleteAccountUseCase

    @Binds
    abstract fun bindGetVehicleByIdUseCase(impl: GetVehicleByIdUseCaseImpl): GetVehicleByIdUseCase

    @Binds
    abstract fun bindValidateCredentialsUseCase(impl: ValidateCredentialsUseCaseImpl): ValidateCredentialsUseCase

    @Binds
    abstract fun bindSignInUseCase(impl: SignInUseCaseImpl): SignInUseCase

    @Binds
    abstract fun bindSignInWithGoogleUseCase(impl: SignInWithGoogleUseCaseImpl): SignInWithGoogleUseCase

    @Binds
    abstract fun bindSignUpUseCase(impl: SignUpUseCaseImpl): SignUpUseCase

    @Binds
    abstract fun bindCheckSessionUseCase(impl: CheckSessionUseCaseImpl): CheckSessionUseCase

    @Binds
    abstract fun bindCheckDatabaseOwnerUseCase(impl: CheckDatabaseOwnerUseCaseImpl): CheckDatabaseOwnerUseCase

    @Binds
    abstract fun bindClearLocalDataUseCase(impl: ClearLocalDataUseCaseImpl): ClearLocalDataUseCase

    @Binds
    abstract fun bindSyncContractsUseCase(impl: SyncContractsUseCaseImpl): SyncContractsUseCase

    @Binds
    abstract fun bindSyncHistoryUseCase(impl: SyncHistoryUseCaseImpl): SyncHistoryUseCase

    @Binds
    abstract fun bindSignOutUseCase(impl: SignOutUseCaseImpl): SignOutUseCase

    @Binds
    abstract fun bindGetCurrentUserUseCase(impl: GetCurrentUserUseCaseImpl): GetCurrentUserUseCase

    @Binds
    abstract fun bindObserveTrackingStateUseCase(impl: ObserveTrackingStateUseCaseImpl): ObserveTrackingStateUseCase

    @Binds
    abstract fun bindClearTrackingUseCase(impl: ClearTrackingUseCaseImpl): ClearTrackingUseCase

    @Binds
    abstract fun bindStopTrackingUseCase(impl: StopTrackingUseCaseImpl): StopTrackingUseCase

    @Binds
    abstract fun bindStartAutoTrackingUseCase(impl: StartAutoTrackingUseCaseImpl): StartAutoTrackingUseCase

    @Binds
    abstract fun bindStopAutoTrackingUseCase(impl: StopAutoTrackingUseCaseImpl): StopAutoTrackingUseCase

    @Binds
    abstract fun bindGetEntitlementsUseCase(impl: GetEntitlementsUseCaseImpl): GetEntitlementsUseCase

    @Binds
    abstract fun bindStartTrialUseCase(impl: StartTrialUseCaseImpl): StartTrialUseCase

    @Binds
    abstract fun bindCheckFeatureAccessUseCase(impl: CheckFeatureAccessUseCaseImpl): CheckFeatureAccessUseCase

    @Binds
    abstract fun bindGetRouteUseCase(impl: GetRouteUseCaseImpl): GetRouteUseCase

    @Binds
    abstract fun bindGetImageBytesUseCase(impl: GetImageBytesUseCaseImpl): GetImageBytesUseCase

    @Binds
    abstract fun bindGetExpensesByVehicleUseCase(impl: GetExpensesByVehicleUseCaseImpl): GetExpensesByVehicleUseCase

    @Binds
    abstract fun bindSaveFuelExpenseUseCase(impl: SaveFuelExpenseUseCaseImpl): SaveFuelExpenseUseCase

    @Binds
    abstract fun bindDeleteFuelExpenseUseCase(impl: DeleteFuelExpenseUseCaseImpl): DeleteFuelExpenseUseCase

    @Binds
    abstract fun bindGetStationVolatilityUseCase(impl: GetStationVolatilityUseCaseImpl): GetStationVolatilityUseCase

    @Binds
    abstract fun bindGetElectrificationSavingsUseCase(impl: GetElectrificationSavingsUseCaseImpl): GetElectrificationSavingsUseCase

    @Binds
    abstract fun bindGetAllServiceStationsUseCase(impl: GetAllServiceStationsUseCaseImpl): GetAllServiceStationsUseCase

    @Binds
    abstract fun bindGetFavoriteServiceStationsUseCase(impl: GetFavoriteServiceStationsUseCaseImpl): GetFavoriteServiceStationsUseCase

    @Binds
    abstract fun bindSaveServiceStationUseCase(impl: SaveServiceStationUseCaseImpl): SaveServiceStationUseCase

    @Binds
    abstract fun bindSetFavoriteStationUseCase(impl: SetFavoriteStationUseCaseImpl): SetFavoriteStationUseCase

    @Binds
    abstract fun bindDeleteServiceStationUseCase(impl: DeleteServiceStationUseCaseImpl): DeleteServiceStationUseCase

    @Binds
    abstract fun bindGetServiceStationDetailUseCase(impl: GetServiceStationDetailUseCaseImpl): GetServiceStationDetailUseCase

    @Binds
    abstract fun bindSyncStationGeofencesUseCase(impl: SyncStationGeofencesUseCaseImpl): SyncStationGeofencesUseCase

    @Binds
    abstract fun bindHandleGeofenceTransitionUseCase(impl: HandleGeofenceTransitionUseCaseImpl): HandleGeofenceTransitionUseCase

    @Binds
    abstract fun bindSyncStationsUseCase(impl: SyncStationsUseCaseImpl): SyncStationsUseCase
}
