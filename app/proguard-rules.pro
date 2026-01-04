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

# Room and Kotlin
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class kotlin.coroutines.Continuation

# Hilt ProGuard Rules
# Keep all Hilt-generated classes
#-keep class dagger.hilt.** { *; }
#-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager
#-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager
#-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager
#-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
#-keep class * extends dagger.hilt.android.internal.managers.ServiceComponentManager
#
## CRITICAL: Force keep Application class (referenced in AndroidManifest.xml)
#-keep,allowshrinking,allowobfuscation class dev.tuandoan.tasktracker.TaskTrackerApplication
#-keep class dev.tuandoan.tasktracker.TaskTrackerApplication {
#    <init>();
#    <methods>;
#    <fields>;
#}
#
## CRITICAL: Force keep MainActivity class (referenced in AndroidManifest.xml)
#-keep,allowshrinking,allowobfuscation class dev.tuandoan.tasktracker.MainActivity
#-keep class dev.tuandoan.tasktracker.MainActivity {
#    <init>();
#    <methods>;
#    <fields>;
#}
#
## CRITICAL: Keep all Hilt-generated versions unconditionally
#-keep class dev.tuandoan.tasktracker.Hilt_TaskTrackerApplication {
#    <init>();
#    <methods>;
#    <fields>;
#}
#-keep class dev.tuandoan.tasktracker.Hilt_MainActivity {
#    <init>();
#    <methods>;
#    <fields>;
#}
#
## Suppress warnings for Hilt-generated classes
#-dontwarn dev.tuandoan.tasktracker.Hilt_MainActivity
#-dontwarn dev.tuandoan.tasktracker.Hilt_TaskTrackerApplication
#
## Keep all classes annotated with Hilt annotations
#-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
#-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
#-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
#
## Keep all Hilt modules and their methods
#-keep @dagger.Module class * { *; }
#-keep @dagger.hilt.InstallIn class * { *; }
#-keepclassmembers class * {
#    @dagger.Provides *;
#    @dagger.Binds *;
#    @javax.inject.Inject <init>(...);
#}
#
## Keep ViewModels with @HiltViewModel
#-keep class * extends androidx.lifecycle.ViewModel {
#    <init>(...);
#}
#-keep class * {
#    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
#}
#
## Keep dependency injection related classes
#-keep class javax.inject.** { *; }
#-keep class javax.annotation.** { *; }
#-keep class dagger.** { *; }
#
## Prevent obfuscation of classes that use field injection
#-keepclassmembers class * {
#    @javax.inject.Inject <fields>;
#    @javax.inject.Inject <init>(...);
#}
#
## Keep all generated Hilt components and classes
#-keep class **_HiltComponents$* { *; }
#-keep class **_Factory { *; }
#-keep class **_MembersInjector { *; }
#-keep class **Hilt_* { *; }
#
## Keep all classes that extend generated Hilt classes
#-keep class * extends **Hilt_* { *; }
#
## Additional conditional keeps for Hilt-generated classes
#-if class * {
#    @dagger.hilt.android.HiltAndroidApp <methods>;
#}
#-keep,allowshrinking,allowobfuscation class <1>_** { *; }
#
#-if class * {
#    @dagger.hilt.android.AndroidEntryPoint <methods>;
#}
#-keep,allowshrinking,allowobfuscation class <1>_** { *; }
#
## DEBUGGING: Print what's happening to Application classes
## -printseeds
## -verbose
#
## CRITICAL: Keep all classes in our main package (this should fix everything)
#-keep class dev.tuandoan.tasktracker.** { *; }
#
## CRITICAL: Additional explicit keeps for troubleshooting
#-keepattributes *Annotation*
#-keepattributes Signature
#-keepattributes InnerClasses
#-keepattributes EnclosingMethod
#
## CRITICAL: Ensure Android system can find our Application
#-keepnames class dev.tuandoan.tasktracker.TaskTrackerApplication
#-keepnames class dev.tuandoan.tasktracker.MainActivity
#
## CRITICAL: Keep everything needed for Application instantiation
#-keep class android.app.Application
#-keep class * extends android.app.Application {
#    <init>();
#    void attachBaseContext(android.content.Context);
#    void onCreate();
#}
#
## CRITICAL: Keep everything needed for Activity instantiation
#-keep class * extends androidx.activity.ComponentActivity {
#    <init>();
#    void onCreate(android.os.Bundle);
#}