import com.android.build.api.dsl.LibraryExtension


plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.network)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.room)
    alias(libs.plugins.pluginkit.android.work)
    alias(libs.plugins.pluginkit.android.testing)
    alias(libs.plugins.kotlin.serialization)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.core.infrastructure"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:analytics"))

    implementation(deps.foundationkit)
    implementation(deps.encryptionkit)
    implementation(deps.authkit)
    implementation(deps.javax.inject)
    implementation(libs.kotlinx.serialization.json)

    implementation(deps.androidx.splashscreen)
    implementation(deps.androidx.datastore.preferences)
    implementation(deps.androidx.credentials)
    implementation(deps.androidx.credentials.play.services.auth)
    implementation(deps.googleid)
    implementation(deps.billing.ktx)
    implementation(deps.play.services.maps)
    implementation(deps.play.services.location)
    implementation(deps.android.maps.utils)

    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    implementation(deps.firebase.config)
    implementation("androidx.exifinterface:exifinterface:1.4.2")
}
