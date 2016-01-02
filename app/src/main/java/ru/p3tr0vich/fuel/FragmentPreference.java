package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import android.widget.Toast;

public class FragmentPreference extends PreferenceFragmentCompat implements
        FragmentInterface,
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    public static final String TAG = "FragmentPreference";
    public static final int ID = R.id.action_settings;

    public static final String KEY_PREFERENCE_SCREEN = "KEY_PREFERENCE_SCREEN";

    private OnFragmentChangeListener mOnFragmentChangeListener;
    private OnPreferenceScreenChangeListener mOnPreferenceScreenChangeListener;
    private OnPreferenceSyncEnabledChangeListener mOnPreferenceSyncEnabledChangeListener;

    private boolean mIsInRoot;
    private PreferenceScreen mRootPreferenceScreen;

    @Override
    public int getFragmentId() {
        return ID;
    }

    @Override
    public int getTitleId() {
        return -1;
    }

    @NonNull
    @Override
    public String getTitle() {
        return mIsInRoot ? getString(R.string.title_prefs) : (String) getPreferenceScreen().getTitle();
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
        return mIsInRoot;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        Functions.logD("FragmentPreference -- onSharedPreferenceChanged: key == " + key);

        updatePreferenceSummary(key);

        if (key.equals(getString(R.string.pref_sync_enabled))) {
            updatePreferenceSummary(R.string.pref_sync_key);
            mOnPreferenceSyncEnabledChangeListener.OnPreferenceSyncEnabledChanged(
                    PreferenceManagerFuel.isSyncEnabled());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(getString(R.string.pref_map_center_text))) {
            ActivityYandexMap.start(getActivity(), ActivityYandexMap.MAP_TYPE_CENTER);

            return true;
        } else if (key.equals(getString(R.string.pref_sync_yandex_disk_key))) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SyncYandexDisk.WWW_URL)));
            } catch (Exception e) {
                UtilsLog.d(TAG, "onPreferenceClick(pref_sync_yandex_disk_key)",
                        "exception == " + e.toString());

                Toast.makeText(getActivity(),
                        R.string.message_error_yandex_disk_browser_open,
                        Toast.LENGTH_SHORT).show();
            }

            return true;
        } else
            return false;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
        mRootPreferenceScreen = getPreferenceScreen();

        init(mRootPreferenceScreen);

        findPreference(getString(R.string.pref_map_center_text)).setOnPreferenceClickListener(this);

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
        mIsInRoot = preferenceScreen == null;

        setPreferenceScreen(mIsInRoot ? mRootPreferenceScreen : preferenceScreen);

        mOnPreferenceScreenChangeListener.OnPreferenceScreenChanged(getTitle(), mIsInRoot);
    }

    public boolean goToRootScreen() {
        if (mIsInRoot) return false;

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
        mRootPreferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        mRootPreferenceScreen.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    private void updatePreferenceSummary(String key) {
        updatePreferenceSummary(findPreference(key));
    }

    private void updatePreferenceSummary(@StringRes int resId) {
        updatePreferenceSummary(getString(resId));
    }

    private void updatePreferenceSummary(Preference preference) {
        if (preference == null) return;

        String key = preference.getKey();
        if (key == null) return;

        if (preference instanceof EditTextPreference) {
            EditTextPreference editPref = (EditTextPreference) preference;

            String summary, text;

            summary = (String) editPref.getSummary();
//            text = editPref.getText() not updated after sync;
            text = PreferenceManagerFuel.getString(key, "");
            editPref.setText(text);

            if (TextUtils.isEmpty(text)) text = "0";

            int i = summary.lastIndexOf(" (");
            if (i != -1) summary = summary.substring(0, i);
            summary = summary + " (" + text + ")";

            editPref.setSummary(summary);
        } else if (key.equals(getString(R.string.pref_map_center_text))) {
            preference.setSummary(PreferenceManagerFuel.getMapCenterText());
        } else if (key.equals(getString(R.string.pref_sync_key))) {
            preference.setSummary(getString(PreferenceManagerFuel.isSyncEnabled() ?
                    R.string.pref_sync_summary_on : R.string.pref_sync_summary_off));
        } else if (key.equals(getString(R.string.pref_sync_enabled))) {
            preference.setTitle(getString(PreferenceManagerFuel.isSyncEnabled() ?
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
