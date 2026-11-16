# IthaGram plugins: los plugins de Python buscan las clases de Telegram por su nombre real,
# asi que no se pueden renombrar ni recortar.
-keep class org.telegram.** { *; }
-keep interface org.telegram.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn java.awt.**
