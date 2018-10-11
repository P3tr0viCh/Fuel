package ru.p3tr0vich.fuel.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceGroup
import android.support.v7.preference.PreferenceScreen
import android.text.TextUtils
import ru.p3tr0vich.fuel.ImplementException
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.factories.FragmentFactory
import ru.p3tr0vich.fuel.utils.UtilsLog

class FragmentPreferences : FragmentPreferencesBase(FragmentFactory.Ids.PREFERENCES),
        SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private var onPreferenceScreenChangeListener: OnPreferenceScreenChangeListener? = null
    private var onPreferenceSyncEnabledChangeListener: OnPreferenceSyncEnabledChangeListener? = null
    private var onPreferenceClickListener: OnPreferenceClickListener? = null

    var isInRoot: Boolean = false
        private set

    private lateinit var rootPreferenceScreen: PreferenceScreen

    override val title: String
        get() = if (isInRoot) getString(R.string.title_prefs) else preferenceScreen.title as String

    interface OnPreferenceClickListener {
        fun onPreferenceMapCenterClick()

        fun onPreferenceSyncYandexDiskClick()

        fun onPreferenceSMSAddressClick()

        fun onPreferenceSMSTextPatternClick()
    }

    interface OnPreferenceScreenChangeListener {
        fun onPreferenceScreenChanged(title: CharSequence, isInRoot: Boolean)
    }

    interface OnPreferenceSyncEnabledChangeListener {
        fun onPreferenceSyncEnabledChanged(enabled: Boolean)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onSharedPreferenceChanged", "key == $key")
        }

        rootPreferenceScreen = preferenceScreen

        updatePreference(key)

        when (preferencesHelper.keys.getAsInt(key)) {
//            PRICE -> {
//                updatePreference(preferencesHelper.keys.defaultCost)
//                updatePreference(preferencesHelper.keys.defaultVolume)
//            }
//            DEFAULT_COST -> updatePreference(preferencesHelper.keys.defaultVolume)
//            DEFAULT_VOLUME -> updatePreference(preferencesHelper.keys.defaultCost)
//            SYNC_ENABLED -> {
//                updatePreference(preferencesHelper.keys.sync)
//
//                onPreferenceSyncEnabledChangeListener?.onPreferenceSyncEnabledChanged(
//                        preferencesHelper.isSyncEnabled)
//            }
//            SMS_ENABLED -> updatePreference(preferencesHelper.keys.sms)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            preferencesHelper.keys.mapCenterText -> {
                onPreferenceClickListener?.onPreferenceMapCenterClick()
                return true
            }
            preferencesHelper.keys.syncYandexDisk -> {
                onPreferenceClickListener?.onPreferenceSyncYandexDiskClick()
                return true
            }
            preferencesHelper.keys.smsAddress -> {
                onPreferenceClickListener?.onPreferenceSMSAddressClick()
                return true
            }
            preferencesHelper.keys.smsTextPattern -> {
                onPreferenceClickListener?.onPreferenceSMSTextPatternClick()
                return true
            }
            else -> return false
        }
        //todo
//        when (preferencesHelper.keys.getAsInt(preference.key)) {
//            MAP_CENTER_TEXT -> {
//                onPreferenceClickListener?.onPreferenceMapCenterClick()
//                return true
//            }
//            SYNC_YANDEX_DISK -> {
//                onPreferenceClickListener?.onPreferenceSyncYandexDiskClick()
//                return true
//            }
//            SMS_ADDRESS -> {
//                onPreferenceClickListener?.onPreferenceSMSAddressClick()
//                return true
//            }
//            SMS_TEXT_PATTERN -> {
//                onPreferenceClickListener?.onPreferenceSMSTextPatternClick()
//                return true
//            }
//            else -> return false
//        }
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onCreatePreferences")
        }

        addPreferencesFromResource(R.xml.preferences)

//        rootPreferenceScreen = preferenceScreen

        findPreference(preferencesHelper.keys.mapCenterText).onPreferenceClickListener = this
        findPreference(preferencesHelper.keys.syncYandexDisk).onPreferenceClickListener = this
        findPreference(preferencesHelper.keys.smsAddress).onPreferenceClickListener = this
        findPreference(preferencesHelper.keys.smsTextPattern).onPreferenceClickListener = this

        val keyPreferenceScreen: String? =
                bundle?.getString(KEY_PREFERENCE_SCREEN)
                        ?: arguments?.getString(KEY_PREFERENCE_SCREEN)

        if (TextUtils.isEmpty(keyPreferenceScreen)) {
            isInRoot = true
        } else {
            navigateToScreen(findPreference(keyPreferenceScreen) as PreferenceScreen)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(KEY_PREFERENCE_SCREEN, if (isInRoot) null else preferenceScreen.key)
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        navigateToScreen(preferenceScreen)

        super.onNavigateToScreen(preferenceScreen)
    }

    private fun navigateToScreen(preferenceScreen: PreferenceScreen?) {
        isInRoot = preferenceScreen == null || preferenceScreen == rootPreferenceScreen

        setPreferenceScreen(if (isInRoot) rootPreferenceScreen else preferenceScreen)

        onPreferenceScreenChangeListener?.onPreferenceScreenChanged(title, isInRoot)
    }

    fun goToRootScreen(): Boolean {
        if (isInRoot) return false

        navigateToScreen(null)

        return true
    }

    fun goToSyncScreen() {
        navigateToScreen(findPreference(preferencesHelper.keys.sync) as PreferenceScreen)
    }

    override fun onBackPressed(): Boolean {
        return goToRootScreen()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            onPreferenceScreenChangeListener = context as OnPreferenceScreenChangeListener?
            onPreferenceSyncEnabledChangeListener = context as OnPreferenceSyncEnabledChangeListener?
            onPreferenceClickListener = context as OnPreferenceClickListener?
        } catch (e: ClassCastException) {
            throw ImplementException(context!!, arrayOf(OnPreferenceScreenChangeListener::class.java, OnPreferenceSyncEnabledChangeListener::class.java, OnPreferenceClickListener::class.java))
        }

    }

    override fun onStart() {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onStart")
        }

        super.onStart()

        init(rootPreferenceScreen)

        rootPreferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onStop")
        }

        rootPreferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    private fun getValue(value: String?, empty: String): String? {
        return if (TextUtils.isEmpty(value)) empty else value
    }

    private fun updatePreference(key: String) {
        updatePreference(rootPreferenceScreen.findPreference(key))
    }

    @SuppressLint("SwitchIntDef")
    private fun updatePreference(preference: Preference?) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "updatePreference", "preference == " + preference!!)
        }

        if (preference == null) {
            return
        }

        val key = preference.key ?: return

        var title: String? = null
        var summary: String? = null

        val intKey = preferencesHelper.keys.getAsInt(key)

//        when (intKey) {
//            PRICE -> summary = getValue(preferencesHelper.priceAsString, getString(R.string.pref_price_empty))
//            DEFAULT_COST, DEFAULT_VOLUME -> {
//                val cost = preferencesHelper.defaultCost
//                val volume = preferencesHelper.defaultVolume
//
//                if (cost == 0f && volume == 0f)
//                    summary = if (intKey == DEFAULT_COST)
//                        getString(R.string.pref_def_cost_empty)
//                    else
//                        getString(R.string.pref_def_volume_empty)
//                else {
//                    val price = preferencesHelper.price
//
//                    if (intKey == DEFAULT_COST) {
//                        if (cost == 0f) {
//                            summary = getString(R.string.pref_def_cost_calc)
//
//                            if (price != 0f) {
//                                summary += " (" + UtilsFormat.floatToString(volume * price) + ")"
//                            }
//
//                        }
//                    } else {
//                        if (volume == 0f) {
//                            summary = getString(R.string.pref_def_volume_calc)
//
//                            if (price != 0f) {
//                                summary += " (" + UtilsFormat.floatToString(cost / price) + ")"
//                            }
//                        }
//                    }
//                }
//
//                if (TextUtils.isEmpty(summary)) {
//                    summary = preferencesHelper.getString(key)
//                }
//            }
//            MAP_CENTER_TEXT -> summary = preferencesHelper.mapCenterText
//            SYNC -> summary = getString(if (preferencesHelper.isSyncEnabled)
//                R.string.pref_sync_summary_on
//            else
//                R.string.pref_sync_summary_off)
//            SYNC_ENABLED -> title = getString(if (preferencesHelper.isSyncEnabled)
//                R.string.pref_sync_summary_on
//            else
//                R.string.pref_sync_summary_off)
//            SMS -> summary = getString(if (preferencesHelper.isSMSEnabled)
//                R.string.pref_sms_summary_on
//            else
//                R.string.pref_sms_summary_off)
//            SMS_ENABLED -> title = getString(if (preferencesHelper.isSMSEnabled)
//                R.string.pref_sms_summary_on
//            else
//                R.string.pref_sms_summary_off)
//            SMS_ADDRESS -> summary = preferencesHelper.smsAddress
//            SMS_TEXT_PATTERN -> {
//                summary = preferencesHelper.smsTextPattern
//                summary = summary.replace("[\\s]+".toRegex(), " ")
//            }
//            else -> if (preference is EditTextPreference) {
//                var text: String? = preferencesHelper.getString(key)
//
//                preference.text = text
//
//                text = getValue(text, "0")
//
//                summary = preference.summary.toString()
//
//                val i = summary.lastIndexOf(" (")
//
//                if (i != -1) {
//                    summary = summary.substring(0, i)
//                }
//
//                summary += " ($text)"
//            }
//        }

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "updatePreference", "title == $title, summary == $summary")
        }

        if (title != null) {
            preference.title = title
        }
        if (summary != null) {
            preference.summary = summary
        }
    }

    private fun init(preference: Preference) {
        updatePreference(preference)
        if (preference is PreferenceScreen || preference is PreferenceGroup) {
            var i = 0
            val count = (preference as PreferenceGroup).preferenceCount
            while (i < count) {
                init(preference.getPreference(i))
                i++
            }
        }
    }

    companion object {
        private const val TAG = "FragmentPreferences"

        private var LOG_ENABLED = false

        const val KEY_PREFERENCE_SCREEN = "KEY_PREFERENCE_SCREEN"
    }
}