# Add project specific ProGuard rules here.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

-dontobfuscate

# Everything in the app is essential
-keep class com.nononsenseapps.** { *; }

# gradle (and stackoverflow) agree that suppressing the warning is appropriate
-dontwarn com.google.appengine.api.urlfetch.**
-dontwarn com.squareup.okhttp.**
-dontwarn org.joda.convert.**