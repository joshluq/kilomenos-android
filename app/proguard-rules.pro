# =================================================================
# KiloMenos - ProGuard / R8 Rules 🛡️
# =================================================================

# --- General Optimizations ---
-keepattributes SourceFile,LineNumberTable,Signature,Annotation
-renamesourcefileattribute SourceFile

# --- Kotlin Serialization & Coroutines ---
-keepclassmembers class kotlinx.coroutines.** { *; }

# --- Retrofit & OkHttp ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# --- Jackson Serialization (DTOs) ---
# We MUST keep our data models and their members to avoid JSON parsing issues.
-keep class es.joshluq.kmsafe.infrastructure.remote.request.** { *; }
-keep class es.joshluq.kmsafe.infrastructure.remote.response.** { *; }
-keep class es.joshluq.kmsafe.infrastructure.remote.model.** { *; }
-keep class es.joshluq.kmsafe.domain.model.** { *; }
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.JsonProperty *;
}

# --- Room Persistence ---
-keep class es.joshluq.kmsafe.infrastructure.local.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# --- Dagger Hilt ---
-keep class * extends androidx.lifecycle.ViewModel
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.EntryPoint
-keep class * extends android.app.Service
-keep class * extends android.app.Application

# --- Google Identity & Credentials ---
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# --- AdMob ---
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# --- Compose (Artisanal UI) ---
# Compose rules are usually handled by the compiler, but we keep our foundations safe.
-keep class es.joshluq.canvaskit.** { *; }
-keep class es.joshluq.kmsafe.ui.** { *; }
