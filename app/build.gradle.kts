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
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
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

                buildConfigField("String", "SERVER_URL", "\"${env.serverUrl}\"")
                buildConfigField("String", "API_KEY", "\"$apiKey\"")
                buildConfigField("String", "STORAGE_URL", "\"${env.storageUrl}\"")
                buildConfigField("String", "TERMS_URL", "\"${env.termsUrl}\"")
                buildConfigField("String", "PRIVACY_URL", "\"${env.privacyUrl}\"")
                buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
                buildConfigField("String", "ADMOB_BANNER_ID", "\"$adMobBanner\"")
                buildConfigField("String", "PREMIUM_SKU", "\"${env.premiumSku}\"")
                
                manifestPlaceholders["adMobAppId"] = adMobId
            }
        }
    }
}

dependencies {
    implementation("es.joshluq.kit:foundationkit:1.3.0")
    implementation("es.joshluq.kit:canvaskit:1.0.0")
    implementation("es.joshluq.kit:encryptionkit:1.3.0")
    implementation("es.joshluq.kit:authkit:1.2.0")
    implementation("es.joshluq.kit:analyticskit:1.2.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    // Activity Recognition
    implementation("com.google.android.gms:play-services-location:21.4.0") 
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    
    // Image Processing & Cropping
    implementation("com.github.SmartToolFactory:Compose-Cropper:0.5.0")
    
    // Google Identity & Credentials
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    implementation("com.android.billingclient:billing-ktx:9.1.0")

    implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-config")
}
