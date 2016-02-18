package ru.p3tr0vich.fuel;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

interface FragmentInterface {

    String KEY_ID = "KEY_ID";

    int getFragmentId();

    @StringRes
    int getTitleId();

    @Nullable
    String getTitle();

    @StringRes
    int getSubtitleId();

    @Nullable
    String getSubtitle();

    interface OnFragmentChangeListener {
        void onFragmentChange(FragmentInterface fragment);
    }

    boolean onBackPressed();
}