import com.android.build.api.dsl.LibraryExtension
import java.util.Properties

plugins {
    alias(libs.plugins.pluginkit.android.library)
    alias(libs.plugins.pluginkit.android.compose)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.navigation)
}

// Load secrets from local file
val secrets = Properties().apply {
    val secretsFile = project.rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        load(secretsFile.inputStream())
    } else {
        val defaultsFile = project.rootProject.file("secrets.defaults.properties")
        if (defaultsFile.exists()) {
            load(defaultsFile.inputStream())
        }
    }
}

configure<LibraryExtension> {
    namespace = "es.joshluq.kmsafe.feature.history"

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions.add("environment")

    productFlavors {
        AppConfig.Environments.availableEnvironments.forEach { env ->
            create(env.name) {
                dimension = "environment"

                val envPrefix = env.name.uppercase()
                val adMobBanner = secrets.getProperty("${envPrefix}_ADMOB_BANNER_ID") ?: ""
                buildConfigField("String", "ADMOB_BANNER_ID", "\"$adMobBanner\"")
            }
        }
    }
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
