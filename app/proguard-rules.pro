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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Ktor / Supabase on Android: JVM-only classes referenced by optional debug code (R8 would otherwise fail the release build)
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn io.netty.**
# Keep kotlinx.serialization generated serializers for app models
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.numberone.numberone.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
