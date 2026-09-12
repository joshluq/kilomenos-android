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
                val adMobOverviewBanner = secrets.getProperty("${envPrefix}_ADMOB_OVERVIEW_BANNER_ID")
                    ?: secrets.getProperty("${envPrefix}_ADMOB_BANNER_ID")
                    ?: "ca-app-pub-3940256099942544/6300978111"
                val adMobHistoryBanner = secrets.getProperty("${envPrefix}_ADMOB_HISTORY_BANNER_ID")
                    ?: secrets.getProperty("${envPrefix}_ADMOB_BANNER_ID")
                    ?: "ca-app-pub-3940256099942544/6300978111"
                val adMobExpensesBanner = secrets.getProperty("${envPrefix}_ADMOB_EXPENSES_BANNER_ID")
                    ?: secrets.getProperty("${envPrefix}_ADMOB_BANNER_ID")
                    ?: "ca-app-pub-3940256099942544/6300978111"
                val adMobProjectionBanner = secrets.getProperty("${envPrefix}_ADMOB_PROJECTION_BANNER_ID")
                    ?: secrets.getProperty("${envPrefix}_ADMOB_BANNER_ID")
                    ?: "ca-app-pub-3940256099942544/6300978111"
                val googleClientId = secrets.getProperty("${envPrefix}_GOOGLE_WEB_CLIENT_ID") ?: ""
                val mapsKey = secrets.getProperty("${envPrefix}_MAPS_API_KEY") ?: ""

                buildConfigField("String", "SERVER_URL", "\"${env.serverUrl}\"")
                buildConfigField("String", "API_KEY", "\"$apiKey\"")
                buildConfigField("String", "STORAGE_URL", "\"${env.storageUrl}\"")
                buildConfigField("String", "TERMS_URL", "\"${env.termsUrl}\"")
                buildConfigField("String", "PRIVACY_URL", "\"${env.privacyUrl}\"")
                buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
                buildConfigField("String", "ADMOB_BANNER_ID", "\"$adMobBanner\"")
                buildConfigField("String", "ADMOB_OVERVIEW_BANNER_ID", "\"$adMobOverviewBanner\"")
                buildConfigField("String", "ADMOB_HISTORY_BANNER_ID", "\"$adMobHistoryBanner\"")
                buildConfigField("String", "ADMOB_EXPENSES_BANNER_ID", "\"$adMobExpensesBanner\"")
                buildConfigField("String", "ADMOB_PROJECTION_BANNER_ID", "\"$adMobProjectionBanner\"")
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
    implementation(project(":feature:history"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:fleet"))
    implementation(project(":feature:overview"))
    implementation(project(":feature:projection"))
    implementation(project(":feature:premium"))
    implementation(project(":feature:widget"))
    implementation(project(":core:ui"))
    implementation(project(":core:infrastructure"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:monetization"))
    implementation(project(":core:tracking"))

    implementation(deps.foundationkit)
    implementation(deps.canvaskit)
    implementation(deps.encryptionkit)
    implementation(deps.authkit)
    implementation(deps.analyticskit)

    implementation(deps.androidx.splashscreen)
    implementation(deps.androidx.datastore.preferences)
    implementation(deps.coil.compose)
    implementation(deps.play.services.location)
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
