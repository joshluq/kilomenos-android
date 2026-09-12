import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.hilt)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.core.tracking"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(deps.foundationkit)
    implementation(project(":core:analytics"))
    implementation(deps.play.services.location)
}
