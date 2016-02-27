package ru.p3tr0vich.fuel;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class Const {
    public static final int ANIMATION_DURATION_TOOLBAR = 400;
    public static final int ANIMATION_DURATION_TOOLBAR_SHADOW = 100;
    public static final int ANIMATION_DURATION_LAYOUT_TOTAL_SHOW = 300;
    public static final int ANIMATION_DURATION_LAYOUT_TOTAL_HIDE = 400;
    public static final int ANIMATION_DURATION_SYNC = 1000;
    public static final int ANIMATION_DURATION_DRAWER_TOGGLE = 300;
    public static final int ANIMATION_CHART = 400;

    public static final int DELAYED_TIME_SHOW_NO_RECORDS = 400;
    public static final int DELAYED_TIME_SHOW_PROGRESS_WHEEL = 1000;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RECORD_ACTION_ADD, RECORD_ACTION_UPDATE, RECORD_ACTION_DELETE})
    public @interface RecordAction {
    }

    public static final int RECORD_ACTION_ADD = 0;
    public static final int RECORD_ACTION_UPDATE = 1;
    public static final int RECORD_ACTION_DELETE = 2;
}