# Keep MSAL
-keep class com.microsoft.identity.** { *; }
-dontwarn com.microsoft.identity.**

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
