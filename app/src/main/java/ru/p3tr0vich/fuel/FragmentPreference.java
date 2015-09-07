package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;

public class FragmentPreference extends PreferenceFragment {

    public static final String TAG = "FragmentPreference";

    private OnPreferenceScreenChangedListener mOnPreferenceScreenChangedListener;
    private FragmentFuel.OnFragmentChangedListener mOnFragmentChangedListener;

    private boolean isInRoot;
    private PreferenceScreen rootPreferenceScreen;

    public interface OnPreferenceScreenChangedListener {
        void OnPreferenceScreenChanged(CharSequence title);
    }

    public int getFragmentId() {
        return R.id.action_settings;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        isInRoot = true;
        rootPreferenceScreen = getPreferenceScreen();
    }

    @Override
    public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != null) {
            Functions.logD("FragmentPreference -- onNavigateToScreen: " + preferenceScreen.getTitle());
            isInRoot = false;
            setPreferenceScreen(preferenceScreen);
            mOnPreferenceScreenChangedListener.OnPreferenceScreenChanged(preferenceScreen.getTitle());
        } else
            Functions.logD("FragmentPreference -- onNavigateToScreen: null");
        super.onNavigateToScreen(preferenceScreen);
    }

    public boolean goToRootScreen() {
        if (isInRoot) return false;
        isInRoot = true;
        setPreferenceScreen(rootPreferenceScreen);
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Functions.logD("FragmentPreference -- onAttach (context)");
        try {
            mOnPreferenceScreenChangedListener = (OnPreferenceScreenChangedListener) context;
            mOnFragmentChangedListener = (FragmentFuel.OnFragmentChangedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement OnPreferenceScreenChangedListener, OnFragmentChangedListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Functions.logD("FragmentPreference -- onAttach (activity)");
        try {
            mOnPreferenceScreenChangedListener = (OnPreferenceScreenChangedListener) activity;
            mOnFragmentChangedListener = (FragmentFuel.OnFragmentChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement OnPreferenceScreenChangedListener, OnFragmentChangedListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Functions.logD("FragmentPreference -- onStart");
        mOnFragmentChangedListener.onFragmentChanged(getFragmentId());
    }
}
