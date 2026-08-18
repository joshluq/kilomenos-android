import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.kotlin.serialization)
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.core.domain"
}

dependencies {
    implementation(deps.foundationkit)
    implementation(deps.javax.inject)
    implementation(libs.kotlinx.serialization.json)
}
