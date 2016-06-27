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

import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.utils.UtilsLog;

public class FragmentPreferences extends FragmentPreferencesBase implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    public static final String TAG = "FragmentPreferences";

    private static final boolean LOG_ENABLED = false;

    public static final String KEY_PREFERENCE_SCREEN = "KEY_PREFERENCE_SCREEN";

    private OnPreferenceScreenChangeListener mOnPreferenceScreenChangeListener;
    private OnPreferenceSyncEnabledChangeListener mOnPreferenceSyncEnabledChangeListener;
    private OnPreferenceClickListener mOnPreferenceClickListener;

    private boolean mIsInRoot;
    private PreferenceScreen mRootPreferenceScreen;

    public interface OnPreferenceClickListener {
        void onPreferenceMapCenterClick();

        void onPreferenceSyncYandexDiskClick();

        void onPreferenceSMSAddressClick();

        void onPreferenceSMSTextPatternClick();
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
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == " + key);

        updatePreferenceSummary(key);

        switch (key) {
            case PreferencesHelper.PREF_SYNC_ENABLED:
                updatePreferenceSummary(PreferencesHelper.PREF_SYNC);
                mOnPreferenceSyncEnabledChangeListener.onPreferenceSyncEnabledChanged(
                        PreferencesHelper.isSyncEnabled());
                break;
            case PreferencesHelper.PREF_SMS_ENABLED:
                updatePreferenceSummary(PreferencesHelper.PREF_SMS);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // add in onCreatePreferences

        String key = preference.getKey();

        switch (key) {
            case PreferencesHelper.PREF_MAP_CENTER_TEXT:
                mOnPreferenceClickListener.onPreferenceMapCenterClick();

                return true;
            case PreferencesHelper.PREF_SYNC_YANDEX_DISK:
                mOnPreferenceClickListener.onPreferenceSyncYandexDiskClick();

                return true;
            case PreferencesHelper.PREF_SMS_ADDRESS:
                mOnPreferenceClickListener.onPreferenceSMSAddressClick();

                return true;
            case PreferencesHelper.PREF_SMS_TEXT_PATTERN:
                mOnPreferenceClickListener.onPreferenceSMSTextPatternClick();

                return true;
            default:
                return false;
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onCreatePreferences");

        addPreferencesFromResource(R.xml.preferences);
        mRootPreferenceScreen = getPreferenceScreen();

        findPreference(PreferencesHelper.PREF_MAP_CENTER_TEXT).setOnPreferenceClickListener(this);
        findPreference(PreferencesHelper.PREF_SYNC_YANDEX_DISK).setOnPreferenceClickListener(this);
        findPreference(PreferencesHelper.PREF_SMS_ADDRESS).setOnPreferenceClickListener(this);
        findPreference(PreferencesHelper.PREF_SMS_TEXT_PATTERN).setOnPreferenceClickListener(this);

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
        navigateToScreen((PreferenceScreen) findPreference(PreferencesHelper.PREF_SYNC));
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
            mOnPreferenceClickListener = (OnPreferenceClickListener) context;
        } catch (ClassCastException e) {
            throw new ImplementException(context, new Class[]{
                    OnPreferenceScreenChangeListener.class,
                    OnPreferenceSyncEnabledChangeListener.class,
                    OnPreferenceClickListener.class});
        }
    }

    @Override
    public void onStart() {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onStart");

        super.onStart();

        init(mRootPreferenceScreen);

        mRootPreferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onStop");

        mRootPreferenceScreen.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        super.onStop();
    }

    private void updatePreferenceSummary(String key) {
        updatePreferenceSummary(mRootPreferenceScreen.findPreference(key));
    }

    private void updatePreferenceSummary(Preference preference) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "updatePreferenceSummary", "preference == " + preference);

        if (preference == null) return;

        String key = preference.getKey();
        if (key == null) return;

        String text;

        if (preference instanceof EditTextPreference) {
//            text = preference.getText() not updated after sync;
            text = PreferencesHelper.getString(key);

            if (LOG_ENABLED)
                UtilsLog.d(TAG, "updatePreferenceSummary", "text == " + text);

            ((EditTextPreference) preference).setText(text);

            if (TextUtils.isEmpty(text)) text = "0";

            String summary = preference.getSummary().toString();

            int i = summary.lastIndexOf(" (");
            if (i != -1) summary = summary.substring(0, i);
            summary += " (" + text + ")";

            preference.setSummary(summary);
        } else {
            switch (key) {
                case PreferencesHelper.PREF_MAP_CENTER_TEXT:
                    text = PreferencesHelper.getMapCenterText();
                    break;
                case PreferencesHelper.PREF_SYNC:
                case PreferencesHelper.PREF_SYNC_ENABLED:
                    text = getString(PreferencesHelper.isSyncEnabled() ?
                            R.string.pref_sync_summary_on : R.string.pref_sync_summary_off);
                    break;
                case PreferencesHelper.PREF_SMS:
                case PreferencesHelper.PREF_SMS_ENABLED:
                    text = getString(PreferencesHelper.isSMSEnabled() ?
                            R.string.pref_sms_summary_on : R.string.pref_sms_summary_off);
                    break;
                case PreferencesHelper.PREF_SMS_ADDRESS:
                    text = PreferencesHelper.getSMSAddress();
                    break;
                case PreferencesHelper.PREF_SMS_TEXT_PATTERN:
                    text = PreferencesHelper.getSMSTextPattern();
                    text = text.replaceAll("[\\s]+", " ");
                    break;
                default:
                    text = null;
            }

            if (LOG_ENABLED)
                UtilsLog.d(TAG, "updatePreferenceSummary", "text == " + text);

            switch (key) {
                case PreferencesHelper.PREF_MAP_CENTER_TEXT:
                case PreferencesHelper.PREF_SYNC:
                case PreferencesHelper.PREF_SMS:
                case PreferencesHelper.PREF_SMS_ADDRESS:
                case PreferencesHelper.PREF_SMS_TEXT_PATTERN:
                    preference.setSummary(text);
                    break;
                case PreferencesHelper.PREF_SYNC_ENABLED:
                case PreferencesHelper.PREF_SMS_ENABLED:
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
}