import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    alias(libs.plugins.pluginkit.android.application)
    alias(libs.plugins.pluginkit.android.compose)
    alias(libs.plugins.pluginkit.android.navigation)
    alias(libs.plugins.pluginkit.android.network)
    alias(libs.plugins.pluginkit.android.hilt)
    alias(libs.plugins.pluginkit.android.room)
    alias(libs.plugins.pluginkit.android.work)
    alias(libs.plugins.pluginkit.quality)
    alias(libs.plugins.pluginkit.android.testing)
    alias(deps.plugins.google.services)
    alias(deps.plugins.crashlytics)
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

configure<ApplicationExtension> {
    namespace = AppConfig.applicationNamespace

    defaultConfig {
        versionCode = AppConfig.versionCode
        versionName = AppConfig.versionName
        testInstrumentationRunner = "es.joshluq.kmsafe.HiltTestRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = secrets.getProperty("RELEASE_KEYSTORE_PATH")?.let { file(it) }
            storePassword = secrets.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = secrets.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = secrets.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions.add("environment")

    productFlavors {
        AppConfig.Environments.availableEnvironments.forEach { env ->
            create(env.name) {
                dimension = "environment"
                applicationIdSuffix = env.applicationIdSuffix
                
                val envPrefix = env.name.uppercase()
                val apiKey = secrets.getProperty("${envPrefix}_API_KEY") ?: ""
                val adMobId = secrets.getProperty("${envPrefix}_ADMOB_APP_ID") ?: ""
                val adMobBanner = secrets.getProperty("${envPrefix}_ADMOB_BANNER_ID") ?: ""
                val googleClientId = secrets.getProperty("${envPrefix}_GOOGLE_WEB_CLIENT_ID") ?: ""
                val mapsKey = secrets.getProperty("${envPrefix}_MAPS_API_KEY") ?: ""

                buildConfigField("String", "SERVER_URL", "\"${env.serverUrl}\"")
                buildConfigField("String", "API_KEY", "\"$apiKey\"")
                buildConfigField("String", "STORAGE_URL", "\"${env.storageUrl}\"")
                buildConfigField("String", "TERMS_URL", "\"${env.termsUrl}\"")
                buildConfigField("String", "PRIVACY_URL", "\"${env.privacyUrl}\"")
                buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
                buildConfigField("String", "ADMOB_BANNER_ID", "\"$adMobBanner\"")
                buildConfigField("String", "PREMIUM_SKU", "\"${env.premiumSku}\"")
                
                manifestPlaceholders["adMobAppId"] = adMobId
                manifestPlaceholders["mapsKey"] = mapsKey
            }
        }
    }
}

dependencies {
    implementation(project(":feature:expenses"))
    implementation(project(":feature:auth"))
    implementation(project(":core:ui"))
    implementation(project(":core:infrastructure"))
    implementation(project(":core:domain"))

    implementation(deps.foundationkit)
    implementation(deps.canvaskit)
    implementation(deps.encryptionkit)
    implementation(deps.authkit)
    implementation(deps.analyticskit)

    implementation(deps.androidx.splashscreen)
    implementation(deps.androidx.datastore.preferences)
    implementation(deps.coil.compose)
    implementation(deps.play.services.ads)
    implementation(deps.play.services.location)
    implementation(deps.user.messaging.platform)
    implementation(deps.compose.cropper)
    implementation(deps.googleid)
    implementation(deps.accompanist.permissions)
    implementation(deps.billing.ktx)
    implementation(deps.android.maps.utils)
    implementation(deps.maps.compose)
    implementation(deps.play.services.maps)

    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    implementation(deps.firebase.config)
}
