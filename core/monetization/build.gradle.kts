import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.compose)
    alias(libs.plugins.pluginkit.android.hilt)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.core.monetization"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(deps.foundationkit)
    implementation(deps.play.services.ads)
    implementation(deps.user.messaging.platform)
}
