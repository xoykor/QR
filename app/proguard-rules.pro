# Keep ML Kit & CameraX classes needed at runtime
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.zxing.** { *; }
