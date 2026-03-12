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


#############################################
# Required for Hilt / annotation metadata stability (release-only issues)
#############################################

# Keep annotation + signature metadata (Hilt + reflection-ish frameworks)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Make @Keep effective if you use it anywhere
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep *;
}


#############################################
# Hilt ViewModel map key stability (your fix)
#############################################

# Hilt ViewModel map keys are resolved from class-name strings at runtime.
# Keep ViewModel classes stable in release to avoid key collisions after shrinking.
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepnames class * extends androidx.lifecycle.ViewModel


#############################################
# Firebase / Google Play services (safe dontwarn to avoid noisy builds)
#############################################

-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore object mapping uses reflection and requires a public no-arg constructor.
# Keep no-arg constructors for app DTO/model classes used with toObject()/toObjects().
-keepclassmembers class com.example.hatchtracker.model.** {
    public <init>();
}
-keepclassmembers class com.example.hatchtracker.data.models.** {
    public <init>();
}
-keepclassmembers class com.example.hatchtracker.data.remote.models.** {
    public <init>();
}


#############################################
# Kotlin / Coroutines (avoid harmless warnings)
#############################################

-dontwarn kotlin.Metadata
-dontwarn kotlinx.coroutines.**


#############################################
# ML Kit (ONLY if you actually use/enable it; safe dontwarn)
#############################################

-dontwarn com.google.mlkit.**
-dontwarn com.google.android.odml.**

# If you later hit ML Kit *runtime* issues in release (rare but possible), enable:
# -keep class com.google.mlkit.** { *; }
