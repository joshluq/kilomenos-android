plugins {
    alias(libs.plugins.pluginkit.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(deps.foundationkit.core)
    implementation(deps.javax.inject)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.testing.unit)
}
