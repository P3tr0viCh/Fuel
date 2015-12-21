package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
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

    public static final String KEY_PREFERENCE_SCREEN = "KEY_PREFERENCE_SCREEN";

    private OnFragmentChangeListener mOnFragmentChangeListener;
    private OnPreferenceScreenChangeListener mOnPreferenceScreenChangeListener;
    private OnPreferenceSyncEnabledChangeListener mOnPreferenceSyncEnabledChangeListener;

    private boolean isInRoot;
    private PreferenceScreen rootPreferenceScreen;

    @Override
    public int getFragmentId() {
        return R.id.action_settings;
    }

    @Override
    public int getTitleId() {
        return -1;
    }

    @Override
    @NonNull
    public String getTitle() {
        return isInRoot ? getString(R.string.title_prefs) : (String) getPreferenceScreen().getTitle();
    }

    @Override
    public int getSubtitleId() {
        return -1;
    }

    @Nullable
    @Override
    public String getSubtitle() {
        return null;
    }

    public interface OnPreferenceScreenChangeListener {
        void OnPreferenceScreenChanged(@NonNull CharSequence title, boolean isInRoot);
    }

    public interface OnPreferenceSyncEnabledChangeListener {
        void OnPreferenceSyncEnabledChanged(final boolean enabled);
    }

    public boolean isInRoot() {
        return isInRoot;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Functions.logD("FragmentPreference -- onSharedPreferenceChanged: key == " + key);

        updatePreferenceSummary(key);

        if (key.equals(getString(R.string.pref_sync_enabled))) {
            updatePreferenceSummary(R.string.pref_sync_key);
            mOnPreferenceSyncEnabledChangeListener.OnPreferenceSyncEnabledChanged(
                    FuelingPreferenceManager.isSyncEnabled());
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        rootPreferenceScreen = getPreferenceScreen();

        init(rootPreferenceScreen);

        findPreference(getString(R.string.pref_map_center_text))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ActivityYandexMap.start(getActivity(), ActivityYandexMap.MAP_TYPE_CENTER);

                        return true;
                    }
                });

        String preferenceKey = null;

        if (bundle == null) {
            Bundle arguments = getArguments();
            if (arguments != null)
                preferenceKey = arguments.getString(KEY_PREFERENCE_SCREEN);
        } else
            preferenceKey = bundle.getString(KEY_PREFERENCE_SCREEN);

        isInRoot = preferenceKey == null;
        if (!isInRoot) navigateToScreen((PreferenceScreen) findPreference(preferenceKey));
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
        navigateToScreen(preferenceScreen);

        super.onNavigateToScreen(preferenceScreen);
    }

    private void navigateToScreen(@Nullable PreferenceScreen preferenceScreen) {
        isInRoot = preferenceScreen == null;

        setPreferenceScreen(isInRoot ? rootPreferenceScreen : preferenceScreen);

        mOnPreferenceScreenChangeListener.OnPreferenceScreenChanged(getTitle(), isInRoot);
    }

    public boolean goToRootScreen() {
        if (isInRoot) return false;

        navigateToScreen(null);

        return true;
    }

    public void goToSyncScreen() {
        navigateToScreen((PreferenceScreen) findPreference(getString(R.string.pref_sync_key)));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnFragmentChangeListener = (OnFragmentChangeListener) context;
            mOnPreferenceScreenChangeListener = (OnPreferenceScreenChangeListener) context;
            mOnPreferenceSyncEnabledChangeListener = (OnPreferenceSyncEnabledChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement OnPreferenceScreenChangeListener, OnFragmentChangeListener, " +
                    "OnPreferenceSyncEnabledChangeListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mOnFragmentChangeListener.onFragmentChange(this);
        rootPreferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        rootPreferenceScreen.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    private void updatePreferenceSummary(String key) {
        updatePreferenceSummary(findPreference(key));
    }

    private void updatePreferenceSummary(@StringRes int resId) {
        updatePreferenceSummary(getString(resId));
    }

    private void updatePreferenceSummary(Preference preference) {
        if (preference == null) {
//            Functions.logD("FragmentPreference -- updatePreferenceSummary: preference == null");
            return;
        }

        String key = preference.getKey();
        if (key == null) {
//            Functions.logD("FragmentPreference -- updatePreferenceSummary: key == null");
            return;
        }

//        Functions.logD("FragmentPreference -- updatePreferenceSummary: preference == " + key);

        if (preference instanceof EditTextPreference) {
            EditTextPreference editPref = (EditTextPreference) preference;

            String summary, text;

            summary = (String) editPref.getSummary();
//            text = editPref.getText() not updated after sync;
            text = FuelingPreferenceManager.getString(key, "");
            editPref.setText(text);

            if (TextUtils.isEmpty(text)) text = "0";

            int i = summary.lastIndexOf(" (");
            if (i != -1) summary = summary.substring(0, i);
            summary = summary + " (" + text + ")";

            editPref.setSummary(summary);
        } else if (key.equals(getString(R.string.pref_map_center_text))) {
            preference.setSummary(FuelingPreferenceManager.getMapCenterText());
        } else if (key.equals(getString(R.string.pref_sync_key))) {
            preference.setSummary(getString(FuelingPreferenceManager.isSyncEnabled() ?
                    R.string.pref_sync_summary_on : R.string.pref_sync_summary_off));
        } else if (key.equals(getString(R.string.pref_sync_enabled))) {
            preference.setTitle(getString(FuelingPreferenceManager.isSyncEnabled() ?
                    R.string.pref_sync_summary_on : R.string.pref_sync_summary_off));
        }
    }

    private void init(Preference preference) {
        updatePreferenceSummary(preference);
        if (preference instanceof PreferenceScreen || preference instanceof PreferenceGroup) {
            for (int i = 0; i < ((PreferenceGroup) preference).getPreferenceCount(); i++)
                init(((PreferenceGroup) preference).getPreference(i));
        }
    }

    public void updateMapCenter() {
        updatePreferenceSummary(R.string.pref_map_center_text);
    }
}
