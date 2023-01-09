# Add project specific ProGuard rules here.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# I only care about minimizing
-dontobfuscate

# Everything in the app is essential
-keep class com.nononsenseapps.** { *; }
-keep public class com.nononsenseapps.** { *; }
-keep class com.mobeta.android.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# gradle (and stackoverflow) agree that suppressing the warning is appropriate
-dontwarn org.joda.convert.**
