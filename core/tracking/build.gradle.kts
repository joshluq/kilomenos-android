import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.testing)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.core.tracking"
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":core:domain"))

    implementation(deps.foundationkit)
    implementation(project(":core:analytics"))
    implementation(deps.play.services.location)
}
