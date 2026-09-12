import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.testing)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.core.analytics"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(deps.foundationkit)
    implementation(deps.analyticskit)
    implementation(deps.javax.inject)

    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
}
