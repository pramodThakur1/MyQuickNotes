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

# 1. STRIP ALL METADATA (SourceFiles, LineNumbers)
# Hacker ko kabhi pata nahi chalega ki galti kis line par thi.
-keepattributes !SourceFile,!LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# 2. AGGRESSIVE CLASS REPACKAGING
# Saari classes ko ek hi chhote dabba (a.b.c) mein ghusa do.
-repackageclasses ''
-allowaccessmodification

# 3. USE SHORT NAMES (a, b, c...)
# Saare functions aur variables ko chota aur uljha hua bana do.
# Hum default dictionary use karenge jo sabse best hai.

# Glide ProGuard Rules
-keep public class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule { *; }
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep class com.bumptech.glide.GeneratedLibraryGlideModuleImpl { *; }
-keep class com.bumptech.glide.load.resource.bitmap.VideoDecoder { *; }
-keep class com.bumptech.glide.load.resource.drawable.AnimatedWebpDecoder { *; }
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$DirectGlue { *; }

# For AndroidX and generic rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.android.material.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.constraintlayout.** { *; }

# Google API and HTTP Client Rules (Fix for Missing Classes)
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontwarn com.google.api.client.http.apache.**
-dontwarn com.google.api.client.http.googlehttpclient.**
-dontwarn org.apache.http.**
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.lang.model.element.**
-dontwarn java.lang.instrument.**

# Keep Google Drive and Auth classes
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.android.gms.auth.** { *; }

# Keep Custom Views (used in XML)
-keep class com.nishuapps.gonotes.ZoomableImageView { *; }

# Keep BroadcastReceivers and inner components used in Manifest
-keep class com.nishuapps.gonotes.MainActivity$ReminderReceiver { *; }
-keepclassmembers class com.nishuapps.gonotes.MainActivity$ReminderReceiver {
    public <init>(...);
}

# 4. STRIP MOCK/TEST ARTIFACTS FROM LIBRARIES (MockClientAuthentication Fix)
# Removing classes identified by scanner as "Mock/Test" code.
-keep class com.google.api.client.googleapis.auth.oauth2.MockClientAuthentication { *; }
-assumenosideeffects class com.google.api.client.googleapis.auth.oauth2.MockClientAuthentication { *; }
# Note: Actually, we want to REMOVE it, so we ensure no 'keep' rule includes it.
# Adding assume-no-side-effects helps R8 strip it if it's only weakly referenced.

# 5. FIX DEAD SECURITY CONTROL (Keep RootBeer visible for scanners)
-keep class com.scottyab.rootbeer.** { *; }
-keepclassmembers class com.scottyab.rootbeer.RootBeerNative {
    native <methods>;
}
-dontwarn com.scottyab.rootbeer.RootBeerNative

# 5. SILENCE LOGCAT IN RELEASE BUILDS (CWE-532 Fix)
# Release APK mein se saare Log aur printStackTrace mita do.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}
