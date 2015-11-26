-dontnote com.google.vending.licensing.ILicensingService
-dontnote com.android.vending.licensing.ILicensingService

-keepclassmembers class ru.p3tr0vich.fuel.YandexMapJavascriptInterface {
   public *;
}

-keepclassmembers class ru.p3tr0vich.fuel.Functions {
    public static java.lang.String sqlDateToString(java.lang.String, boolean);
    public static java.lang.String floatToString(float);
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

-keep class com.github.mikephil.charting.animation.ChartAnimator { *; }