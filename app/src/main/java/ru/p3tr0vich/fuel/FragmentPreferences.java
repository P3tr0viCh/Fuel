package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

public class FragmentPreferences extends FragmentPreferencesBase implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    public static final String TAG = "FragmentPreferences";

    public static final String KEY_PREFERENCE_SCREEN = "KEY_PREFERENCE_SCREEN";

    private OnPreferenceScreenChangeListener mOnPreferenceScreenChangeListener;
    private OnPreferenceSyncEnabledChangeListener mOnPreferenceSyncEnabledChangeListener;
    private OnPreferenceMapCenterClickListener mOnPreferenceMapCenterClickListener;

    private boolean mIsInRoot;
    private PreferenceScreen mRootPreferenceScreen;

    public interface OnPreferenceMapCenterClickListener {
        void onPreferenceMapCenterClick();
    }

    public interface OnPreferenceScreenChangeListener {
        void onPreferenceScreenChanged(@NonNull CharSequence title, boolean isInRoot);
    }

    public interface OnPreferenceSyncEnabledChangeListener {
        void onPreferenceSyncEnabledChanged(final boolean enabled);
    }

    @NonNull
    public static Fragment newInstance(int id) {
        return newInstance(id, new FragmentPreferences());
    }

    @NonNull
    @Override
    public String getTitle() {
        return mIsInRoot ? getString(R.string.title_prefs) : (String) getPreferenceScreen().getTitle();
    }

    public boolean isInRoot() {
        return mIsInRoot;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == " + key);

        updatePreferenceSummary(key);

        switch (key) {
            case PreferenceManagerFuel.PREF_SYNC_ENABLED:
                updatePreferenceSummary(PreferenceManagerFuel.PREF_SYNC_KEY);
                mOnPreferenceSyncEnabledChangeListener.onPreferenceSyncEnabledChanged(
                        PreferenceManagerFuel.isSyncEnabled());
                break;
            case PreferenceManagerFuel.PREF_SMS_ENABLED:
                updatePreferenceSummary(PreferenceManagerFuel.PREF_SMS_KEY);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(PreferenceManagerFuel.PREF_MAP_CENTER_TEXT)) {
            mOnPreferenceMapCenterClickListener.onPreferenceMapCenterClick();

            return true;
        } else if (key.equals(getString(R.string.pref_sync_yandex_disk_key))) {
            Utils.openUrl(getContext(), SyncYandexDisk.WWW_URL,
                    getString(R.string.message_error_yandex_disk_browser_open));

            return true;
        } else
            return false;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        mRootPreferenceScreen = getPreferenceScreen();

        init(mRootPreferenceScreen);

        findPreference(PreferenceManagerFuel.PREF_MAP_CENTER_TEXT).setOnPreferenceClickListener(this);

        findPreference(getString(R.string.pref_sync_yandex_disk_key)).setOnPreferenceClickListener(this);

        String keyPreferenceScreen = null;

        if (bundle == null) {
            Bundle arguments = getArguments();
            if (arguments != null)
                keyPreferenceScreen = arguments.getString(KEY_PREFERENCE_SCREEN);
        } else
            keyPreferenceScreen = bundle.getString(KEY_PREFERENCE_SCREEN);

        if (keyPreferenceScreen == null)
            mIsInRoot = true;
        else
            navigateToScreen((PreferenceScreen) findPreference(keyPreferenceScreen));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_PREFERENCE_SCREEN, mIsInRoot ? null : getPreferenceScreen().getKey());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout preferences = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

        assert preferences != null;

        FrameLayout listContainer = (FrameLayout) preferences.findViewById(R.id.list_container);

        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_preferences, container, false);
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
        mIsInRoot = preferenceScreen == null;

        setPreferenceScreen(mIsInRoot ? mRootPreferenceScreen : preferenceScreen);

        mOnPreferenceScreenChangeListener.onPreferenceScreenChanged(getTitle(), mIsInRoot);
    }

    public boolean goToRootScreen() {
        if (mIsInRoot) return false;

        navigateToScreen(null);

        return true;
    }

    public void goToSyncScreen() {
        navigateToScreen((PreferenceScreen) findPreference(PreferenceManagerFuel.PREF_SYNC_KEY));
    }

    @Override
    public boolean onBackPressed() {
        return goToRootScreen();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnPreferenceScreenChangeListener = (OnPreferenceScreenChangeListener) context;
            mOnPreferenceSyncEnabledChangeListener = (OnPreferenceSyncEnabledChangeListener) context;
            mOnPreferenceMapCenterClickListener = (OnPreferenceMapCenterClickListener) context;
        } catch (ClassCastException e) {
            throw new ImplementException(context, new Class[]{
                    OnPreferenceScreenChangeListener.class,
                    OnPreferenceSyncEnabledChangeListener.class,
                    OnPreferenceMapCenterClickListener.class});
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mRootPreferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        mRootPreferenceScreen.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    private void updatePreferenceSummary(String key) {
        updatePreferenceSummary(mRootPreferenceScreen.findPreference(key));
    }

    private void updatePreferenceSummary(Preference preference) {
//        UtilsLog.d(TAG, "updatePreferenceSummary", "preference == " + preference);

        if (preference == null) return;

        String key = preference.getKey();
        if (key == null) return;

        if (preference instanceof EditTextPreference) {
            EditTextPreference editPref = (EditTextPreference) preference;
//            text = editPref.getText() not updated after sync;
            String text = PreferenceManagerFuel.getString(key);

//            UtilsLog.d(TAG, "updatePreferenceSummary", "text == " + text);

            editPref.setText(text);

            String summary = (String) editPref.getSummary();

            if (TextUtils.isEmpty(text)) text = "0";

            int i = summary.lastIndexOf(" (");
            if (i != -1) summary = summary.substring(0, i);
            summary += " (" + text + ")";

            editPref.setSummary(summary);
        } else {
            String text;

            switch (key) {
                case PreferenceManagerFuel.PREF_MAP_CENTER_TEXT:
                    text = PreferenceManagerFuel.getMapCenterText();
                    break;
                case PreferenceManagerFuel.PREF_SYNC_KEY:
                case PreferenceManagerFuel.PREF_SYNC_ENABLED:
                    text = getString(PreferenceManagerFuel.isSyncEnabled() ?
                            R.string.pref_sync_summary_on : R.string.pref_sync_summary_off);
                    break;
                case PreferenceManagerFuel.PREF_SMS_KEY:
                case PreferenceManagerFuel.PREF_SMS_ENABLED:
                    text = getString(PreferenceManagerFuel.isSMSEnabled() ?
                            R.string.pref_sms_summary_on : R.string.pref_sms_summary_off);
                    break;
                default:
                    text = null;
            }

            switch (key) {
                case PreferenceManagerFuel.PREF_MAP_CENTER_TEXT:
                case PreferenceManagerFuel.PREF_SYNC_KEY:
                case PreferenceManagerFuel.PREF_SMS_KEY:
                    preference.setSummary(text);
                    break;
                case PreferenceManagerFuel.PREF_SYNC_ENABLED:
                case PreferenceManagerFuel.PREF_SMS_ENABLED:
                    preference.setTitle(text);
            }
        }
    }

    private void init(Preference preference) {
        updatePreferenceSummary(preference);
        if (preference instanceof PreferenceScreen || preference instanceof PreferenceGroup) {
            for (int i = 0, count = ((PreferenceGroup) preference).getPreferenceCount(); i < count; i++)
                init(((PreferenceGroup) preference).getPreference(i));
        }
    }

    public void updateMapCenter() {
        updatePreferenceSummary(PreferenceManagerFuel.PREF_MAP_CENTER_TEXT);
    }
}