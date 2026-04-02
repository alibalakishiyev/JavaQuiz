# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# ============ WEBVIEW KEEP RULES ============
-keep class android.webkit.WebView { *; }
-keep class android.webkit.WebSettings { *; }
-keep class android.webkit.WebViewClient { *; }
-keep class android.webkit.WebChromeClient { *; }

# ============ CHAQUOPY KEEP RULES ============
-keep class com.chaquo.python.** { *; }
-keep class com.chaquo.python.android.** { *; }

# ============ FIREBASE KEEP RULES ============
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ============ GSON KEEP RULES ============
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter { *; }
-keep class * extends com.google.gson.TypeAdapterFactory { *; }
-keep class * extends com.google.gson.JsonSerializer { *; }
-keep class * extends com.google.gson.JsonDeserializer { *; }
-keep class * extends com.google.gson.JsonElement { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    public <init>();
}

# ============ YOUR APP CLASSES ============
-keep class com.ali.systemIn.** { *; }
-keepclassmembers class com.ali.systemIn.** { *; }

# ============ JIT KEEP RULES ============
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-keep class org.eclipse.** { *; }
-dontwarn org.eclipse.**

# ============ REMOVE DEBUG LOGS ============
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ============ GENERAL KEEP RULES ============
-dontwarn java.lang.management.ManagementFactory
-dontwarn javax.management.**
-dontwarn javax.script.**
-dontwarn org.ietf.jgss.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn sun.misc.**
-dontwarn kotlin.Cloneable$DefaultImpls

# Keep your terminal classes
-keep class com.rk.terminal.** { *; }
-keep class com.rk.plugin.** { *; }

# ============ IMPORTANT: REMOVED THESE LINES ============
# -dontobfuscate     <-- BUNU SİLDİM (APK kiçilməsinə mane olur)
# -dontshrink        <-- BUNU SİLDİM (APK kiçilməsinə mane olur)