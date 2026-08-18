package es.joshluq.kmsafe.domain.di

import javax.inject.Qualifier

/**
 * Qualifier for the GetRentingUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetRenting

/**
 * Qualifier for the GetHistoryUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetHistory

/**
 * Qualifier for the DeleteOdometerRecordUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeleteOdometerRecord

/**
 * Qualifier for the UpdateOdometerRecordUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UpdateOdometerRecord

/**
 * Qualifier for the SaveInitialContractUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SaveInitialContract

/**
 * Qualifier for the UpdateContractUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UpdateContract

/**
 * Qualifier for the UpdateSubscriptionUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UpdateSubscription

/**
 * Qualifier for the DeleteAccountUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeleteAccount

/**
 * Qualifier for the MigrateLocalDataToRemoteUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MigrateLocalDataToRemote

/**
 * Qualifier for the GetMonthlyUsageUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetMonthlyUsage

/**
 * Qualifier for the GetOverviewDataUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetOverviewData

/**
 * Qualifier for the AddOdometerRecordUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AddOdometerRecord

/**
 * Qualifier for the ExportDataUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExportData

/**
 * Qualifier for the ImportDataUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImportData

/**
 * Qualifier for the UploadVehicleImageUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UploadVehicleImage

/**
 * Qualifier for the GetTripProjectionUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetTripProjection

/**
 * Qualifier for the GetAllContractsUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetAllContracts

/**
 * Qualifier for the SelectContractUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SelectContract

/**
 * Qualifier for the DeleteContractUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeleteContract

/**
 * Qualifier for the GetVehicleByIdUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetVehicleById

/**
 * Qualifier for Email validation.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EmailValidator

/**
 * Qualifier for Password validation.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PasswordValidator

/**
 * Qualifier for the ValidateCredentialsUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ValidateCredentials

/**
 * Qualifier for the SignInUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SignIn

/**
 * Qualifier for the SignInWithGoogleUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SignInWithGoogle

/**
 * Qualifier for the SignUpUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SignUp

/**
 * Qualifier for the CheckSessionUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CheckSession

/**
 * Qualifier for the CheckDatabaseOwnerUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CheckDatabaseOwner

/**
 * Qualifier for the ClearLocalDataUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClearLocalData

/**
 * Qualifier for the EvaluateIdentityConflictUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EvaluateIdentityConflict

/**
 * Qualifier for the GetOdometerRecordUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetOdometerRecord

/**
 * Qualifier for the Authenticated Retrofit instance.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Authenticated

/**
 * Qualifier for the SyncContractsUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncContracts

/**
 * Qualifier for the SyncHistoryUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncHistory

/**
 * Qualifier for the SignOutUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SignOut

/**
 * Qualifier for the GetCurrentUserUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetCurrentUser

/**
 * Qualifier for the GetPreferencesUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetPreferences

/**
 * Qualifier for the UpdatePreferencesUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UpdatePreferences

/**
 * Qualifier for the ObserveTrackingStateUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ObserveTrackingState

/**
 * Qualifier for the ClearTrackingUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClearTracking

/**
 * Qualifier for the StopTrackingUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StopTracking

/**
 * Qualifier for the StartAutoTrackingUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StartAutoTracking

/**
 * Qualifier for the StopAutoTrackingUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StopAutoTracking

/**
 * Qualifier for the GetEntitlementsUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetEntitlements

/**
 * Qualifier for the StartTrialUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StartTrial

/**
 * Qualifier for the GetImageBytesUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetImageBytes

/**
 * Qualifier for the GetRouteUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GetRoute

/**
 * Qualifier for the CheckFeatureAccessUseCase.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CheckFeatureAccess
