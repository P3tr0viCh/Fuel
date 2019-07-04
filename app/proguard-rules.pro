-dontnote com.google.vending.licensing.ILicensingService
-dontnote com.android.vending.licensing.ILicensingService

-dontnote android.net.http.**
-dontnote org.apache.http.**
-dontnote org.apache.commons.codec.**

-keepclassmembers class ru.p3tr0vich.fuel.YandexMapJavascriptInterface {
   public *;
}

-dontnote androidx.appcompat.app.**
-dontnote androidx.appcompat.view.**
-dontnote androidx.appcompat.text.**
-dontnote androidx.appcompat.widget.**
-dontnote androidx.appcompat.view.**
-dontnote androidx.appcompat.widget.**
-dontnote com.google.android.material.**

-dontnote com.pnikosis.materialishprogress.**

-keep class com.github.mikephil.charting.animation.ChartAnimator { *; }

-dontwarn io.realm.**

-dontnote com.github.mikephil.charting.charts.**
-dontnote com.github.mikephil.charting.animation.**

-keep class com.yandex.disk.rest.** { *; }

-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes *Annotation*

-keep class com.squareup.retrofit.** { *; }

-keepclasseswithmembers class * {
    @retrofit.http.* <methods>;
}

-keep class retrofit.http.** { *; }

-keep class com.squareup.okhttp.** { *; }

-keep interface com.squareup.okhttp.** { *; }

-keep class com.squareup.okio.** { *; }

-keep class org.slf4j.** { *; }

-dontwarn com.squareup.retrofit.**
-dontwarn rx.**
-dontwarn com.google.appengine.api.urlfetch.**
-dontwarn com.squareup.okio.**
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn com.squareup.okhttp.**
-dontwarn org.slf4j.**

-dontnote com.squareup.okhttp.**
-dontnote com.google.gson.internal.**
-dontnote retrofit.**
-dontnote okio.**