import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.compose)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.navigation)
    alias(libs.plugins.pluginkit.android.testing)
    alias(libs.plugins.kotlin.serialization)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.feature.expenses"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))

    implementation(deps.canvaskit)
    implementation(deps.foundationkit)
    implementation(deps.javax.inject)
    implementation(deps.maps.compose)
    implementation(deps.play.services.maps)
    implementation(deps.play.services.location)
    implementation(deps.accompanist.permissions)
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation(libs.kotlinx.serialization.json)
}
