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

# Lifecycle 2.8 falls back to reflection when it looks for Compose's own
# LocalLifecycleOwner (it targets
# androidx.compose.ui.platform.AndroidCompositionLocals_androidKt#getLocalLifecycleOwner
# by name so it can stay compatible with Compose 1.6). The rule shipped with that
# artifact declares the wrong return type and therefore never matches, so R8 renames
# the accessor, the lookup fails, and the app crashes while building its first frame
# with "CompositionLocal LocalLifecycleOwner not present".
-keep class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt { *; }
