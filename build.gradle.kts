// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.pluginkit.android.application) apply false
    alias(libs.plugins.pluginkit.android.compose) apply false
    alias(libs.plugins.pluginkit.android.network) apply false
    alias(libs.plugins.pluginkit.android.hilt) apply false
    alias(libs.plugins.pluginkit.android.navigation) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.pluginkit.quality) apply false
    alias(libs.plugins.pluginkit.android.testing) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.firebase.crashlytics") version "3.0.7" apply false
}