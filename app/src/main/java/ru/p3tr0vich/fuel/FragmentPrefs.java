package ru.p3tr0vich.fuel;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentPrefs extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String category = getArguments().getString(getString(R.string.pref_category));
        if (category != null) {
            if (category.equals(getString(R.string.pref_category_defaults))) {
                addPreferencesFromResource(R.xml.prefs_def);
            } else if (category.equals(getString(R.string.pref_category_cons))) {
                addPreferencesFromResource(R.xml.prefs_cons);
            }
        } else addPreferencesFromResource(R.xml.prefs_cons);
    }

    @Override
    public View onCreateView(@SuppressWarnings("NullableProblems") LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        init();

        return super.onCreateView(inflater, container, savedInstanceState);
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

    private void init() {
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); j++)
                    updatePreferenceSummary(preferenceGroup.getPreference(j));
            } else
                updatePreferenceSummary(preference);

        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferenceSummary(findPreference(key));
    }
}
