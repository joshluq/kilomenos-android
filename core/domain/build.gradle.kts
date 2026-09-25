import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.pluginkit.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        moduleName.set("kmsafe_core_domain")
    }
}

dependencies {
    implementation(deps.foundationkit.core)
    implementation(deps.javax.inject)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.testing.unit)
}
