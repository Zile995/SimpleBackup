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

# Kotlin serialization looks up the generated serializer classes through a function on companion
# objects. The companions are looked up reflectively so we need to explicitly keep these functions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.stefan.simplebackup.data.model.**$$serializer { *; }
-keepclassmembers class com.stefan.simplebackup.data.model.* {
    *** Companion;
}
-keepclasseswithmembers class com.stefan.simplebackup.data.model.* {
     kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers,allowoptimization,allowobfuscation class com.stefan.simplebackup.databinding.** {
    public <methods>;
    public <fields>;
}