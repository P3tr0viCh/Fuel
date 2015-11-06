# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keepclassmembers class ru.p3tr0vich.fuel.YandexMapJavascriptInterface {
   public *;
}

-keepclassmembers class ru.p3tr0vich.fuel.Functions {
    public static String sqlDateToString(String, boolean);
    public static String floatToString(float);
}

-keepclassmembers class android.support.v7.widget.PopupMenu {
    private MenuPopupHelper mPopup;
}

-keepclassmembers class android.support.v7.internal.view.menu.MenuPopupHelper {
    private ListPopupWindow mPopup;
    public void setForceShowIcon(boolean);
}

-keepclassmembers class android.support.v7.widget.ListPopupWindow {
    public void setVerticalOffset(int);
    public void show();
}

# Без этой строки не работает
-keep class !android.support.v7.widget.ListPopupWindow {*;}
