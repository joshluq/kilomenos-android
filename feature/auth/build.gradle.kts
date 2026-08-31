import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.compose)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.navigation)
    alias(libs.plugins.kotlin.serialization)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.feature.auth"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))

    implementation(deps.canvaskit)
    implementation(deps.foundationkit)
    implementation(deps.authkit)
    implementation(deps.analyticskit)
    implementation(deps.javax.inject)
    implementation(deps.googleid)
    implementation(libs.kotlinx.serialization.json)
}
