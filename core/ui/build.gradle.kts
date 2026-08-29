import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.compose)
    alias(libs.plugins.pluginkit.android.testing)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.core.ui"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(deps.foundationkit)
    implementation(deps.canvaskit)
    implementation(deps.coil.compose)
}
