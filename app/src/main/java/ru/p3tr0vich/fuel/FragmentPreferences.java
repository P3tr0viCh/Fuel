package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import ru.p3tr0vich.fuel.utils.UtilsFormat;
import ru.p3tr0vich.fuel.utils.UtilsLog;

import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.DEFAULT_COST;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.DEFAULT_VOLUME;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.MAP_CENTER_TEXT;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.PRICE;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.SMS;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.SMS_ADDRESS;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.SMS_ENABLED;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.SMS_TEXT_PATTERN;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.SYNC;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.SYNC_ENABLED;
import static ru.p3tr0vich.fuel.helpers.PreferencesHelper.Keys.SYNC_YANDEX_DISK;

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

    @SuppressLint("SwitchIntDef")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == " + key);

        updatePreference(key);

        switch (preferencesHelper.keys.getAsInt(key)) {
            case PRICE:
                updatePreference(preferencesHelper.keys.defaultCost);
                updatePreference(preferencesHelper.keys.defaultVolume);
                break;
            case DEFAULT_COST:
                updatePreference(preferencesHelper.keys.defaultVolume);
                break;
            case DEFAULT_VOLUME:
                updatePreference(preferencesHelper.keys.defaultCost);
                break;
            case SYNC_ENABLED:
                updatePreference(preferencesHelper.keys.sync);
                mOnPreferenceSyncEnabledChangeListener.onPreferenceSyncEnabledChanged(
                        preferencesHelper.isSyncEnabled());
                break;
            case SMS_ENABLED:
                updatePreference(preferencesHelper.keys.sms);
        }
    }

    @SuppressLint("SwitchIntDef")
    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preferencesHelper.keys.getAsInt(preference.getKey())) {
            case MAP_CENTER_TEXT:
                mOnPreferenceClickListener.onPreferenceMapCenterClick();
                return true;
            case SYNC_YANDEX_DISK:
                mOnPreferenceClickListener.onPreferenceSyncYandexDiskClick();
                return true;
            case SMS_ADDRESS:
                mOnPreferenceClickListener.onPreferenceSMSAddressClick();
                return true;
            case SMS_TEXT_PATTERN:
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

    private String getValue(@Nullable String value, @NonNull String empty) {
        return TextUtils.isEmpty(value) ? empty : value;
    }

    private String getValue(@Nullable String value, @StringRes int emptyId) {
        return getValue(value, getString(emptyId));
    }

    private void updatePreference(String key) {
        updatePreference(mRootPreferenceScreen.findPreference(key));
    }

    @SuppressLint("SwitchIntDef")
    private void updatePreference(@Nullable Preference preference) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "updatePreference", "preference == " + preference);

        if (preference == null) return;

        String key = preference.getKey();
        if (key == null) return;

        String title = null;
        String summary = null;

        int intKey = preferencesHelper.keys.getAsInt(key);

        switch (intKey) {
            case PRICE:
                summary = getValue(preferencesHelper.getPriceAsString(), R.string.pref_price_empty);
                break;
            case DEFAULT_COST:
            case DEFAULT_VOLUME:
                float cost = preferencesHelper.getDefaultCost();
                float volume = preferencesHelper.getDefaultVolume();

                if (cost == 0 && volume == 0)
                    summary = intKey == DEFAULT_COST ?
                            getString(R.string.pref_def_cost_empty) :
                            getString(R.string.pref_def_volume_empty);
                else {
                    float price = preferencesHelper.getPrice();

                    if (intKey == DEFAULT_COST) {
                        if (cost == 0) {
                            summary = getString(R.string.pref_def_cost_calc);

                            if (price != 0)
                                summary += " (" +  UtilsFormat.floatToString(volume * price) + ")";

                        }
                    } else {
                        if (volume == 0) {
                            summary = getString(R.string.pref_def_volume_calc);

                            if (price != 0)
                                summary += " (" + UtilsFormat.floatToString(cost / price) + ")";
                        }
                    }
                }
                if (TextUtils.isEmpty(summary)) summary = preferencesHelper.getString(key);
                break;
            case MAP_CENTER_TEXT:
                summary = preferencesHelper.getMapCenterText();
                break;
            case SYNC:
                summary = getString(preferencesHelper.isSyncEnabled() ?
                        R.string.pref_sync_summary_on : R.string.pref_sync_summary_off);
                break;
            case SYNC_ENABLED:
                title = getString(preferencesHelper.isSyncEnabled() ?
                        R.string.pref_sync_summary_on : R.string.pref_sync_summary_off);
                break;
            case SMS:
                summary = getString(preferencesHelper.isSMSEnabled() ?
                        R.string.pref_sms_summary_on : R.string.pref_sms_summary_off);
                break;
            case SMS_ENABLED:
                title = getString(preferencesHelper.isSMSEnabled() ?
                        R.string.pref_sms_summary_on : R.string.pref_sms_summary_off);
                break;
            case SMS_ADDRESS:
                summary = preferencesHelper.getSMSAddress();
                break;
            case SMS_TEXT_PATTERN:
                summary = preferencesHelper.getSMSTextPattern();
                summary = summary.replaceAll("[\\s]+", " ");
                break;
            default:
                if (preference instanceof EditTextPreference) {
                    String text = preferencesHelper.getString(key);

                    ((EditTextPreference) preference).setText(text);

                    text = getValue(text, "0");

                    summary = preference.getSummary().toString();

                    int i = summary.lastIndexOf(" (");
                    if (i != -1) summary = summary.substring(0, i);
                    summary += " (" + text + ")";
                }
        }

        if (LOG_ENABLED)
            UtilsLog.d(TAG, "updatePreference", "title == " + title + ", summary == " + summary);

        if (title != null) preference.setTitle(title);
        if (summary != null) preference.setSummary(summary);
    }

    private void init(@NonNull Preference preference) {
        updatePreference(preference);
        if (preference instanceof PreferenceScreen || preference instanceof PreferenceGroup) {
            for (int i = 0, count = ((PreferenceGroup) preference).getPreferenceCount(); i < count; i++)
                init(((PreferenceGroup) preference).getPreference(i));
        }
    }
}