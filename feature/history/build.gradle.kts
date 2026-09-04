import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.compose)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.navigation)
    alias(libs.plugins.pluginkit.android.testing)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.feature.history"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:monetization"))

    implementation(deps.canvaskit)
    implementation(deps.foundationkit)
    implementation(deps.analyticskit)
    implementation(deps.javax.inject)
    implementation(deps.play.services.maps)
    implementation(deps.maps.compose)
    implementation(deps.android.maps.utils)
}
