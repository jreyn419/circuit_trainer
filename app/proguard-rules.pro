# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.github.jreyn419.circuittrainer.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.jreyn419.circuittrainer.** {
    kotlinx.serialization.KSerializer serializer(...);
}
