# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ===== KOTLINX SERIALIZATION RULES =====
# Keep serialization classes and their generated serializers for JSON backup
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializers referenced via @Serializable annotation
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `Companion` object fields of serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}

# Preserve JSON backup DTOs to ensure cross-version serialization compatibility and readable stack traces
-keep @kotlinx.serialization.Serializable class dev.tuandoan.tasktracker.data.backup.dto.** { *; }
-keepclassmembers class dev.tuandoan.tasktracker.data.backup.dto.** { *; }

# ===== ROOM DATABASE RULES =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class **_Impl { *; }

# ===== KOTLIN COROUTINES WARNINGS =====
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# ===== GOOGLE PLAY IN-APP REVIEW =====
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# ===== STACK TRACES & DEOBFUSCATION =====
# Preserve line numbers and source file attributes for Firebase Crashlytics mapping
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== SERIALIZABLE =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
