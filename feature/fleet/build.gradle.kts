import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.compose)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.navigation)
    alias(libs.plugins.pluginkit.quality)
    alias(libs.plugins.pluginkit.android.testing)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.feature.fleet"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))

    implementation(deps.canvaskit)
    implementation(deps.foundationkit)
    implementation(deps.analyticskit)
    implementation(deps.javax.inject)
    implementation(deps.coil.compose)
    implementation(deps.play.services.location)
    implementation(deps.accompanist.permissions)
    implementation(deps.compose.cropper)
}
