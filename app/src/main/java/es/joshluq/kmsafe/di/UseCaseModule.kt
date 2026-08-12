package es.joshluq.kmsafe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.kmsafe.domain.usecase.*

/**
 * Dagger module for providing UseCase dependencies for ViewModels.
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
@JvmSuppressWildcards
abstract class UseCaseModule {

    @Binds
    @GetRenting
    abstract fun bindGetRentingContractUseCase(
        useCase: GetRentingContractUseCase
    ): FlowUseCase<GetRentingContractUseCase.Input, GetRentingContractUseCase.Output>

    @Binds
    @GetHistory
    abstract fun bindGetHistoryUseCase(
        useCase: GetHistoryUseCase
    ): FlowUseCase<GetHistoryUseCase.Input, GetHistoryUseCase.Output>

    @Binds
    @GetOdometerRecord
    abstract fun bindGetOdometerRecordUseCase(
        useCase: GetOdometerRecordUseCase
    ): FlowUseCase<GetOdometerRecordUseCase.Input, GetOdometerRecordUseCase.Output>

    @Binds
    @DeleteOdometerRecord
    abstract fun bindDeleteOdometerRecordUseCase(
        useCase: DeleteOdometerRecordUseCase
    ): FlowUseCase<DeleteOdometerRecordUseCase.Input, DeleteOdometerRecordUseCase.Output>

    @Binds
    @EvaluateIdentityConflict
    abstract fun bindEvaluateIdentityConflictUseCase(
        useCase: EvaluateIdentityConflictUseCase
    ): FlowUseCase<EvaluateIdentityConflictUseCase.Input, EvaluateIdentityConflictUseCase.Output>

    @Binds
    @UpdateOdometerRecord
    abstract fun bindUpdateOdometerRecordUseCase(
        useCase: UpdateOdometerRecordUseCase
    ): FlowUseCase<UpdateOdometerRecordUseCase.Input, UpdateOdometerRecordUseCase.Output>

    @Binds
    @UploadVehicleImage
    abstract fun bindUploadVehicleImageUseCase(
        useCase: UploadVehicleImageUseCase
    ): FlowUseCase<UploadVehicleImageUseCase.Input, UploadVehicleImageUseCase.Output>

    @Binds
    @SaveInitialContract
    abstract fun bindSaveInitialContractUseCase(
        useCase: SaveInitialContractUseCase
    ): FlowUseCase<SaveInitialContractUseCase.Input, SaveInitialContractUseCase.Output>

    @Binds
    @UpdateContract
    abstract fun bindUpdateContractUseCase(
        useCase: UpdateContractUseCase
    ): FlowUseCase<UpdateContractUseCase.Input, UpdateContractUseCase.Output>

    @Binds
    @UpdateSubscription
    abstract fun bindUpdateSubscriptionUseCase(
        useCase: UpdateSubscriptionUseCase
    ): FlowUseCase<UpdateSubscriptionUseCase.Input, UpdateSubscriptionUseCase.Output>

    @Binds
    @MigrateLocalDataToRemote
    abstract fun bindMigrateLocalDataToRemoteUseCase(
        useCase: MigrateLocalDataToRemoteUseCase
    ): FlowUseCase<MigrateLocalDataToRemoteUseCase.Input, MigrateLocalDataToRemoteUseCase.Output>

    @Binds
    @GetMonthlyUsage
    abstract fun bindGetMonthlyUsageUseCase(
        useCase: GetMonthlyUsageUseCase
    ): FlowUseCase<GetMonthlyUsageUseCase.Input, GetMonthlyUsageUseCase.Output>

    @Binds
    @GetOverviewData
    abstract fun bindGetOverviewDataUseCase(
        useCase: GetOverviewDataUseCase
    ): FlowUseCase<GetOverviewDataUseCase.Input, GetOverviewDataUseCase.Output>

    @Binds
    @GetPreferences
    abstract fun bindGetPreferencesUseCase(
        useCase: GetPreferencesUseCase
    ): FlowUseCase<GetPreferencesUseCase.Input, GetPreferencesUseCase.Output>

    @Binds
    @UpdatePreferences
    abstract fun bindUpdatePreferencesUseCase(
        useCase: UpdatePreferencesUseCase
    ): FlowUseCase<UpdatePreferencesUseCase.Input, UpdatePreferencesUseCase.Output>

    @Binds
    @AddOdometerRecord
    abstract fun bindAddOdometerRecordUseCase(
        useCase: AddOdometerRecordUseCase
    ): FlowUseCase<AddOdometerRecordUseCase.Input, AddOdometerRecordUseCase.Output>

    @Binds
    @GetTripProjection
    abstract fun bindGetTripProjectionUseCase(
        useCase: GetTripProjectionUseCase
    ): FlowUseCase<GetTripProjectionUseCase.Input, GetTripProjectionUseCase.Output>

    @Binds
    @ExportData
    abstract fun bindExportDataUseCase(
        useCase: ExportDataUseCase
    ): FlowUseCase<ExportDataUseCase.Input, ExportDataUseCase.Output>

    @Binds
    @ImportData
    abstract fun bindImportDataUseCase(
        useCase: ImportDataUseCase
    ): FlowUseCase<ImportDataUseCase.Input, ImportDataUseCase.Output>

    @Binds
    @GetAllContracts
    abstract fun bindGetAllContractsUseCase(
        useCase: GetAllContractsUseCase
    ): FlowUseCase<GetAllContractsUseCase.Input, GetAllContractsUseCase.Output>

    @Binds
    @SelectContract
    abstract fun bindSelectContractUseCase(
        useCase: SelectContractUseCase
    ): FlowUseCase<SelectContractUseCase.Input, SelectContractUseCase.Output>

    @Binds
    @DeleteContract
    abstract fun bindDeleteContractUseCase(
        useCase: DeleteContractUseCase
    ): FlowUseCase<DeleteContractUseCase.Input, DeleteContractUseCase.Output>

    @Binds
    @DeleteAccount
    abstract fun bindDeleteAccountUseCase(
        useCase: DeleteAccountUseCase
    ): FlowUseCase<DeleteAccountUseCase.Input, DeleteAccountUseCase.Output>

    @Binds
    @GetVehicleById
    abstract fun bindGetVehicleByIdUseCase(
        useCase: GetVehicleByIdUseCase
    ): FlowUseCase<GetVehicleByIdUseCase.Input, GetVehicleByIdUseCase.Output>

    @Binds
    @ValidateCredentials
    abstract fun bindValidateCredentialsUseCase(
        useCase: ValidateCredentialsUseCase
    ): UseCase<ValidateCredentialsUseCase.Input, ValidateCredentialsUseCase.Output>

    @Binds
    @SignIn
    abstract fun bindSignInUseCase(
        useCase: SignInUseCase
    ): FlowUseCase<SignInUseCase.Input, SignInUseCase.Output>

    @Binds
    @SignInWithGoogle
    abstract fun bindSignInWithGoogleUseCase(
        useCase: SignInWithGoogleUseCase
    ): FlowUseCase<SignInWithGoogleUseCase.Input, SignInWithGoogleUseCase.Output>

    @Binds
    @SignUp
    abstract fun bindSignUpUseCase(
        useCase: SignUpUseCase
    ): FlowUseCase<SignUpUseCase.Input, SignUpUseCase.Output>

    @Binds
    @CheckSession
    abstract fun bindCheckSessionUseCase(
        useCase: CheckSessionUseCase
    ): FlowUseCase<CheckSessionUseCase.Input, CheckSessionUseCase.Output>

    @Binds
    @CheckDatabaseOwner
    abstract fun bindCheckDatabaseOwnerUseCase(
        useCase: CheckDatabaseOwnerUseCase
    ): FlowUseCase<CheckDatabaseOwnerUseCase.Input, CheckDatabaseOwnerUseCase.Output>

    @Binds
    @ClearLocalData
    abstract fun bindClearLocalDataUseCase(
        useCase: ClearLocalDataUseCase
    ): FlowUseCase<ClearLocalDataUseCase.Input, ClearLocalDataUseCase.Output>

    @Binds
    @SyncContracts
    abstract fun bindSyncContractsUseCase(
        useCase: SyncContractsUseCase
    ): FlowUseCase<SyncContractsUseCase.Input, SyncContractsUseCase.Output>

    @Binds
    @SyncHistory
    abstract fun bindSyncHistoryUseCase(
        useCase: SyncHistoryUseCase
    ): FlowUseCase<SyncHistoryUseCase.Input, SyncHistoryUseCase.Output>

    @Binds
    @SignOut
    abstract fun bindSignOutUseCase(
        useCase: SignOutUseCase
    ): FlowUseCase<SignOutUseCase.Input, SignOutUseCase.Output>

    @Binds
    @GetCurrentUser
    abstract fun bindGetCurrentUserUseCase(
        useCase: GetCurrentUserUseCase
    ): FlowUseCase<GetCurrentUserUseCase.Input, GetCurrentUserUseCase.Output>

    @Binds
    @ObserveTrackingState
    abstract fun bindObserveTrackingStateUseCase(
        useCase: ObserveTrackingStateUseCase
    ): FlowUseCase<ObserveTrackingStateUseCase.Input, ObserveTrackingStateUseCase.Output>

    @Binds
    @ClearTracking
    abstract fun bindClearTrackingUseCase(
        useCase: ClearTrackingUseCase
    ): FlowUseCase<ClearTrackingUseCase.Input, ClearTrackingUseCase.Output>

    @Binds
    @StopTracking
    abstract fun bindStopTrackingUseCase(
        useCase: StopTrackingUseCase
    ): FlowUseCase<StopTrackingUseCase.Input, StopTrackingUseCase.Output>

    @Binds
    @StartAutoTracking
    abstract fun bindStartAutoTrackingUseCase(
        useCase: StartAutoTrackingUseCase
    ): FlowUseCase<StartAutoTrackingUseCase.Input, StartAutoTrackingUseCase.Output>

    @Binds
    @StopAutoTracking
    abstract fun bindStopAutoTrackingUseCase(
        useCase: StopAutoTrackingUseCase
    ): FlowUseCase<StopAutoTrackingUseCase.Input, StopAutoTrackingUseCase.Output>

    @Binds
    @GetEntitlements
    abstract fun bindGetEntitlementsUseCase(
        useCase: GetEntitlementsUseCase
    ): FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output>

    @Binds
    @StartTrial
    abstract fun bindStartTrialUseCase(
        useCase: StartTrialUseCase
    ): FlowUseCase<StartTrialUseCase.Input, StartTrialUseCase.Output>

    @Binds
    @CheckFeatureAccess
    abstract fun bindCheckFeatureAccessUseCase(
        useCase: CheckFeatureAccessUseCase
    ): FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>

    @Binds
    @GetRoute
    abstract fun bindGetRouteUseCase(
        useCase: GetRouteUseCase
    ): FlowUseCase<GetRouteUseCase.Input, GetRouteUseCase.Output>

    @Binds
    @GetImageBytes
    abstract fun bindGetImageBytesUseCase(
        useCase: GetImageBytesUseCase
    ): UseCase<GetImageBytesUseCase.Input, GetImageBytesUseCase.Output>
}
