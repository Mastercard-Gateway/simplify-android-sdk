# Keeping classes currently relying on gson parsing
-keep class com.simplify.android.sdk.Card { *; }
-keep class com.simplify.android.sdk.CardBrand { *; }
-keep class com.simplify.android.sdk.CardEntryMode { *; }
-keep class com.simplify.android.sdk.CardToken { *; }
-keep class com.simplify.android.sdk.Customer { *; }
-keep class com.simplify.android.sdk.FieldError { *; }
-keep class com.simplify.android.sdk.SimplifyError { *; }

# retrolambda
-dontwarn java.lang.invoke.*

## GSON ##
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Optional libraries will warn on missing classes
-dontwarn com.google.android.gms.**
-dontwarn io.reactivex.**