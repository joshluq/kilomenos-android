// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.pluginkit.android.application) apply false
    alias(libs.plugins.pluginkit.android.library) apply false
    alias(libs.plugins.pluginkit.jvm.library) apply false
    alias(libs.plugins.pluginkit.android.compose) apply false
    alias(libs.plugins.pluginkit.android.network) apply false
    alias(libs.plugins.pluginkit.android.hilt) apply false
    alias(libs.plugins.pluginkit.android.room) apply false
    alias(libs.plugins.pluginkit.android.work) apply false
    alias(libs.plugins.pluginkit.android.navigation) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.pluginkit.quality) apply false
    alias(libs.plugins.pluginkit.android.testing) apply false
    alias(deps.plugins.google.services) apply false
    alias(deps.plugins.crashlytics) apply false
}