# Keep kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class com.vivichi.app.data.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.vivichi.app.data.**$$serializer { *; }
-keepclassmembers class com.vivichi.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.vivichi.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
