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

import ru.p3tr0vich.fuel.utils.UtilsLog;

public class FragmentPreferences extends FragmentPreferencesBase implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    public static final String TAG = "FragmentPreferences";

    private static final boolean LOG_ENABLED = true;

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
        void onPreferenceSyncEnabledChanged(boolean enabled);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == " + key);

        updatePreferenceSummary(key);

        if (key.equals(preferencesHelper.keys.syncEnabled)) {
            updatePreferenceSummary(preferencesHelper.keys.sync);
            mOnPreferenceSyncEnabledChangeListener.onPreferenceSyncEnabledChanged(
                    preferencesHelper.isSyncEnabled());
        } else if (key.equals(preferencesHelper.keys.smsEnabled)) {
            updatePreferenceSummary(preferencesHelper.keys.sms);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(preferencesHelper.keys.mapCenterText)) {
            mOnPreferenceClickListener.onPreferenceMapCenterClick();
        } else if (key.equals(preferencesHelper.keys.syncYandexDisk)) {
            mOnPreferenceClickListener.onPreferenceSyncYandexDiskClick();
        } else if (key.equals(preferencesHelper.keys.smsAddress)) {
            mOnPreferenceClickListener.onPreferenceSMSAddressClick();
        } else if (key.equals(preferencesHelper.keys.smsTextPattern)) {
            mOnPreferenceClickListener.onPreferenceSMSTextPatternClick();
        } else {
            return false;
        }

        return true;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onCreatePreferences");

        addPreferencesFromResource(R.xml.preferences);
        mRootPreferenceScreen = getPreferenceScreen();

        findPreference(preferencesHelper.keys.mapCenterText).setOnPreferenceClickListener(this);
        findPreference(preferencesHelper.keys.syncYandexDisk).setOnPreferenceClickListener(this);
        findPreference(preferencesHelper.keys.smsAddress).setOnPreferenceClickListener(this);
        findPreference(preferencesHelper.keys.smsTextPattern).setOnPreferenceClickListener(this);

        String keyPreferenceScreen = null;

        if (bundle == null) {
            Bundle arguments = getArguments();
            if (arguments != null)
                keyPreferenceScreen = arguments.getString(KEY_PREFERENCE_SCREEN);
        } else
            keyPreferenceScreen = bundle.getString(KEY_PREFERENCE_SCREEN);

        if (TextUtils.isEmpty(keyPreferenceScreen))
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
        mIsInRoot = preferenceScreen == null || preferenceScreen.equals(mRootPreferenceScreen);

        setPreferenceScreen(mIsInRoot ? mRootPreferenceScreen : preferenceScreen);

        mOnPreferenceScreenChangeListener.onPreferenceScreenChanged(getTitle(), mIsInRoot);
    }

    public boolean goToRootScreen() {
        if (mIsInRoot) return false;

        navigateToScreen(null);

        return true;
    }

    public void goToSyncScreen() {
        navigateToScreen((PreferenceScreen) findPreference(preferencesHelper.keys.sync));
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

    private void updatePreferenceSummary(@Nullable Preference preference) {
        if (LOG_ENABLED)
            UtilsLog.d(TAG, "updatePreferenceSummary", "preference == " + preference);

        if (preference == null) return;

        String key = preference.getKey();
        if (key == null) return;

        String text;

        if (preference instanceof EditTextPreference) {
//            text = preference.getText() not updated after sync;
            text = preferencesHelper.getString(key);

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
            if (key.equals(preferencesHelper.keys.mapCenterText)) {
                text = preferencesHelper.getMapCenterText();
            } else if (key.equals(preferencesHelper.keys.sync) ||
                    key.equals(preferencesHelper.keys.syncEnabled)) {
                text = getString(preferencesHelper.isSyncEnabled() ?
                        R.string.pref_sync_summary_on : R.string.pref_sync_summary_off);
            } else if (key.equals(preferencesHelper.keys.sms) ||
                    key.equals(preferencesHelper.keys.smsEnabled)) {
                text = getString(preferencesHelper.isSMSEnabled() ?
                        R.string.pref_sms_summary_on : R.string.pref_sms_summary_off);
            } else if (key.equals(preferencesHelper.keys.smsAddress)) {
                text = preferencesHelper.getSMSAddress();
            } else if (key.equals(preferencesHelper.keys.smsTextPattern)) {
                text = preferencesHelper.getSMSTextPattern();
                text = text.replaceAll("[\\s]+", " ");
            } else {
                text = null;
            }

            if (LOG_ENABLED)
                UtilsLog.d(TAG, "updatePreferenceSummary", "text == " + text);

            if (key.equals(preferencesHelper.keys.mapCenterText) ||
                    key.equals(preferencesHelper.keys.sync) ||
                    key.equals(preferencesHelper.keys.sms) ||
                    key.equals(preferencesHelper.keys.smsAddress) ||
                    key.equals(preferencesHelper.keys.smsTextPattern)) {
                preference.setSummary(text);
            } else if (key.equals(preferencesHelper.keys.syncEnabled) || key.equals(preferencesHelper.keys.smsEnabled)) {
                preference.setTitle(text);
            }
        }
    }

    private void init(@NonNull Preference preference) {
        updatePreferenceSummary(preference);
        if (preference instanceof PreferenceScreen || preference instanceof PreferenceGroup) {
            for (int i = 0, count = ((PreferenceGroup) preference).getPreferenceCount(); i < count; i++)
                init(((PreferenceGroup) preference).getPreference(i));
        }
    }
}