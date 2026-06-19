# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
# web3j / bouncycastle and Retrofit models: add keep rules as integrations are added.

# Reown AppKit (WalletConnect) — required keep rules (docs.reown.com/appkit/android/core/installation)
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** {
    native <methods>;
    *;
}
-keep class uniffi.** { *; }
-keepclassmembers class ** {
    public *;
    protected *;
}
-dontwarn uniffi.**
-dontwarn com.sun.jna.**
