package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class FragmentPreference extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener, FragmentInterface {

    public static final String TAG = "FragmentPreference";

    private static final String KEY_PREFERENCE_SCREEN = "KEY_PREFERENCE_SCREEN";

    private OnPreferenceScreenChangeListener mOnPreferenceScreenChangeListener;
    private OnFragmentChangeListener mOnFragmentChangeListener;

    private boolean isInRoot;
    private PreferenceScreen rootPreferenceScreen;

    @Override
    public int getFragmentId() {
        return R.id.action_settings;
    }

    public interface OnPreferenceScreenChangeListener {
        void OnPreferenceScreenChanged(CharSequence title);
    }

    public boolean isInRoot() {
        return isInRoot;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Functions.logD("FragmentPreference -- onSharedPreferenceChanged: key == " + key);
        updatePreferenceSummary(findPreference(key));
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        rootPreferenceScreen = getPreferenceScreen();

        init(rootPreferenceScreen);

        if (bundle == null) {
            Functions.logD("FragmentPreference -- onCreatePreferences: bundle == null");
            isInRoot = true;
        } else {
            Functions.logD("FragmentPreference -- onCreatePreferences: bundle != null");
            String preferenceKey = bundle.getString(KEY_PREFERENCE_SCREEN);
            isInRoot = preferenceKey == null;
            if (!isInRoot) setPreferenceScreen((PreferenceScreen) findPreference(preferenceKey));
        }

        findPreference(getString(R.string.pref_map_center_text))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ActivityYandexMap.start(getActivity(), ActivityYandexMap.MapType.CENTER);

                        return true;
                    }
                });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_PREFERENCE_SCREEN, isInRoot ? null : getPreferenceScreen().getKey());
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
        mOnPreferenceScreenChangeListener.OnPreferenceScreenChanged(null);
        return true;
    }

    public int getTitleId() {
        if (isInRoot) return R.string.title_prefs;
        else {
            String preferenceKey = getPreferenceScreen().getKey();
            if (preferenceKey.equals(getString(R.string.pref_def_key)))
                return R.string.pref_def_header;
            else if (preferenceKey.equals(getString(R.string.pref_cons_key)))
                return R.string.pref_cons_header;
            else return -1;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Functions.logD("FragmentPreference -- onAttach (context)");
        try {
            mOnPreferenceScreenChangeListener = (OnPreferenceScreenChangeListener) context;
            mOnFragmentChangeListener = (OnFragmentChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement OnPreferenceScreenChangeListener, OnFragmentChangeListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Functions.logD("FragmentPreference -- onStart");
        mOnFragmentChangeListener.onFragmentChange(getFragmentId());
        rootPreferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        Functions.logD("FragmentPreference -- onStop");
        rootPreferenceScreen.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    private void updatePreferenceSummary(Preference preference) {
        if (preference == null) {
            Functions.logD("FragmentPreference -- updatePreferenceSummary: preference == null");
            return;
        }
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
        } else if (preference.getKey().equals(getString(R.string.pref_map_center_text))) {
            preference.setSummary(
                    PreferenceManager.getDefaultSharedPreferences(Functions.sApplicationContext)
                            .getString(getString(R.string.pref_map_center_text),
                                    YandexMapJavascriptInterface.DEFAULT_MAP_CENTER_TEXT));
        }
    }

    private void init(Preference preference) {
        if (preference instanceof PreferenceScreen || preference instanceof PreferenceGroup) {
            for (int i = 0; i < ((PreferenceGroup) preference).getPreferenceCount(); i++)
                init(((PreferenceGroup) preference).getPreference(i));
        } else
            updatePreferenceSummary(preference);
    }

    public void updateMapCenter() {
        updatePreferenceSummary(findPreference(getString(R.string.pref_map_center_text)));
    }
}
