-dontnote com.google.vending.licensing.ILicensingService
-dontnote com.android.vending.licensing.ILicensingService

-dontnote android.net.http.**
-dontnote org.apache.http.**
-dontnote org.apache.commons.codec.**

-keepclassmembers class ru.p3tr0vich.fuel.YandexMapJavascriptInterface {
   public *;
}

-keepclassmembers class android.support.v7.widget.ListPopupWindow {
    public void setVerticalOffset(int);
    public void show();
}

-keepclassmembers class android.support.v7.view.menu.MenuPopupHelper {
    private android.support.v7.widget.ListPopupWindow mPopup;
    public void setForceShowIcon(boolean);
}

-keepclassmembers class android.support.v7.widget.PopupMenu {
    private android.support.v7.view.menu.MenuPopupHelper mPopup;
}

-dontnote android.support.v4.app.**
-dontnote android.support.v4.view.**
-dontnote android.support.v4.text.**
-dontnote android.support.v4.widget.**
-dontnote android.support.v7.view.**
-dontnote android.support.v7.widget.**
-dontnote android.support.design.widget.**

-dontnote com.melnykov.fab.**
-dontnote com.pnikosis.materialishprogress.**
-dontnote com.wdullaer.materialdatetimepicker.**

-keep class com.github.mikephil.charting.animation.ChartAnimator { *; }

-dontwarn io.realm.**

-dontnote com.github.mikephil.charting.charts.**
-dontnote com.github.mikephil.charting.animation.**

-keep class com.yandex.disk.rest.** { *; }

-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
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