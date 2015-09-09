package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class FragmentPreference extends PreferenceFragment {

    public static final String TAG = "FragmentPreference";

    private OnPreferenceScreenChangeListener mOnPreferenceScreenChangeListener;
    private FragmentFuel.OnFragmentChangedListener mOnFragmentChangedListener;

    private boolean isInRoot;
    private PreferenceScreen rootPreferenceScreen;

    public interface OnPreferenceScreenChangeListener {
        void OnPreferenceScreenChanged(CharSequence title);
    }

    @SuppressWarnings("SameReturnValue")
    private int getFragmentId() {
        return R.id.action_settings;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        isInRoot = true;
        rootPreferenceScreen = getPreferenceScreen();

        rootPreferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                        updatePreferenceSummary(findPreference(key));
                    }
                });

        init(rootPreferenceScreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout preferences = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

        assert preferences != null;

        FrameLayout listContainer = (FrameLayout) preferences.findViewById(R.id.list_container);

        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_preference, container, false);
        LinearLayout prefContainer = (LinearLayout) root.findViewById(R.id.prefContainer);

        preferences.removeAllViews();
        prefContainer.addView(listContainer);
        preferences.addView(root);

        return preferences;
    }

    @Override
    public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != null) {
            isInRoot = false;
            setPreferenceScreen(preferenceScreen);
            mOnPreferenceScreenChangeListener.OnPreferenceScreenChanged(preferenceScreen.getTitle());
        }
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
        onAttach((Activity) context);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Functions.logD("FragmentPreference -- onAttach (activity)");
        try {
            mOnPreferenceScreenChangeListener = (OnPreferenceScreenChangeListener) activity;
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

    private void updatePreferenceSummary(Preference preference) {
        if (preference instanceof EditTextPreference) {
            EditTextPreference editPref = (EditTextPreference) preference;

            String summary, text;

            summary = (String) editPref.getSummary();
            text = editPref.getText();

            if (TextUtils.isEmpty(text)) text = "0";

            int i = summary.lastIndexOf(" (");
            if (i != -1) summary = summary.substring(0, i);
            summary = summary + " (" + text + ")";

            editPref.setSummary(summary);
        }
    }

    private void init(Preference preference) {
        if (preference instanceof PreferenceScreen || preference instanceof PreferenceGroup) {
            for (int i = 0; i < ((PreferenceGroup) preference).getPreferenceCount(); i++)
                init(((PreferenceGroup) preference).getPreference(i));
        } else
            updatePreferenceSummary(preference);
    }
}
