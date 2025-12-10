# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep source file names and line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Keep all model classes (used with Gson)
-keep class com.metelci.ardunakon.model.** { *; }
-keep class com.metelci.ardunakon.data.** { *; }

# Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep JSON classes (for profile serialization)
-keep class org.json.** { *; }

# Keep Jetpack Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Android Security Crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Keep Bluetooth classes
-keep class com.metelci.ardunakon.bluetooth.** { *; }
-keep class android.bluetooth.** { *; }

# Keep Protocol Manager (used for reflection)
-keep class com.metelci.ardunakon.protocol.ProtocolManager { *; }

# Keep Security Manager
-keep class com.metelci.ardunakon.security.SecurityManager { *; }

# Keep service classes
-keep class com.metelci.ardunakon.service.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Keep data classes and their properties
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep asset files and documentation utility
-keep class **.assets.** { *; }
-keep class com.metelci.ardunakon.util.AssetReader { *; }

# Keep UI components (Help and About dialogs)
-keep class com.metelci.ardunakon.ui.components.HelpDialog { *; }
-keep class com.metelci.ardunakon.ui.components.AboutDialog { *; }

# Missing rules detected by R8 - Google Tink and error-prone annotations
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn javax.annotation.Nullable

# Keep Google Tink classes (used by androidx.security.crypto)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
