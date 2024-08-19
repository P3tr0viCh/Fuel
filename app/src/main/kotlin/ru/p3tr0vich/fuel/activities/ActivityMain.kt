package ru.p3tr0vich.fuel.activities

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.SyncStatusObserver
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentResultListener
import com.google.android.material.navigation.NavigationView
import ru.p3tr0vich.fuel.ContentObserverService
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.factories.FragmentFactory
import ru.p3tr0vich.fuel.fragments.FragmentCalc
import ru.p3tr0vich.fuel.fragments.FragmentDialogMessage
import ru.p3tr0vich.fuel.fragments.FragmentDialogQuestion
import ru.p3tr0vich.fuel.fragments.FragmentFueling
import ru.p3tr0vich.fuel.fragments.FragmentInterface
import ru.p3tr0vich.fuel.fragments.FragmentPreferences
import ru.p3tr0vich.fuel.helpers.ConnectivityHelper
import ru.p3tr0vich.fuel.helpers.ContactsHelper
import ru.p3tr0vich.fuel.helpers.DatabaseHelper
import ru.p3tr0vich.fuel.helpers.FragmentHelper
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.models.MapCenter
import ru.p3tr0vich.fuel.receivers.BroadcastReceiverDatabaseChanged
import ru.p3tr0vich.fuel.receivers.BroadcastReceiverLoading
import ru.p3tr0vich.fuel.receivers.BroadcastReceiverRequestSync
import ru.p3tr0vich.fuel.sync.SyncAccount
import ru.p3tr0vich.fuel.sync.SyncYandexDisk
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsDate
import ru.p3tr0vich.fuel.utils.UtilsLog

class ActivityMain : AppCompatActivity(),
    SyncStatusObserver,
    FragmentFueling.OnFilterChangeListener,
    FragmentFueling.OnRecordChangeListener,
    FragmentCalc.OnCalcDistanceButtonClickListener,
    FragmentInterface.OnFragmentChangeListener,
    FragmentPreferences.OnPreferenceScreenChangeListener,
    FragmentPreferences.OnPreferenceSyncEnabledChangeListener,
    FragmentPreferences.OnPreferenceClickListener {

    private var toolbarMain: Toolbar? = null
    private var toolbarSpinner: Spinner? = null

    private var drawerLayout: DrawerLayout? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var navigationView: NavigationView? = null

    private var syncAccount: SyncAccount? = null
    private var syncMonitor: Any? = null

    private var imgSync: ImageView? = null
    private var animationSync = RotateAnimation(
        360.0f, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
    )

    private var btnSync: TextView? = null

    private lateinit var broadcastReceiverLoading: BroadcastReceiverLoading
    private lateinit var broadcastReceiverDatabaseChanged: BroadcastReceiverDatabaseChanged
    private lateinit var broadcastReceiverRequestSync: BroadcastReceiverRequestSync

    private lateinit var preferencesHelper: PreferencesHelper

    private lateinit var fragmentHelper: FragmentHelper

    @FragmentFactory.Ids.Id
    private var currentFragmentId = 0
    private var clickedMenuId = -1
    private var openPreferenceSync = false

    init {
        animationSync.interpolator = LinearInterpolator()
        animationSync.duration = Utils.getInteger(R.integer.animation_duration_sync).toLong()
        animationSync.repeatCount = Animation.INFINITE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        UtilsLog.d(TAG, "onCreate")

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        syncAccount = SyncAccount(this)

        preferencesHelper = PreferencesHelper.getInstance(this)

        fragmentHelper = FragmentHelper(this)

        syncMonitor =
            ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this)

        initToolbar()
        initToolbarSpinner()
        initDrawer()

        initSyncViews()

        updateSyncStatus()

        initLoadingStatusReceiver()
        initDatabaseChangedReceiver()
        initRequestSyncReceiver()

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportFragmentManager.setFragmentResultListener(
            DIALOG_REQUEST_KEY,
            this,
            fragmentResultListener
        )

        if (savedInstanceState == null) {
            fragmentHelper.addMainFragment()

            ContentObserverService.requestSync(this)
        } else {
            currentFragmentId = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT_ID)

            if (currentFragmentId == FragmentFactory.Ids.PREFERENCES) {
                fragmentHelper.fragmentPreferences?.let {
                    drawerToggle?.isDrawerIndicatorEnabled = it.isInRoot
                }
            }
        }
    }

    private fun initToolbar() {
        toolbarMain = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbarMain)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initToolbarSpinner() {
        toolbarSpinner = AppCompatSpinner(supportActionBar!!.themedContext)

        Utils.setBackgroundTint(toolbarSpinner, R.color.toolbar_title_text, R.color.primary_light)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.filter_dates,
            R.layout.toolbar_spinner_item
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        toolbarSpinner!!.adapter = adapter

        toolbarSpinner!!.dropDownVerticalOffset = -Utils.getSupportActionBarSize(this)

        toolbarMain!!.addView(toolbarSpinner)

        toolbarSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                fragmentHelper.fragmentFueling?.let {
                    if (it.isVisible) {
                        it.setFilterMode(positionToFilterMode(position))
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)

        drawerToggle = object : ActionBarDrawerToggle(
            this,
            drawerLayout, toolbarMain, R.string.app_name, R.string.app_name
        ) {

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)

                fragmentHelper.fragmentFueling?.let {
                    if (it.isVisible) {
                        it.setFabVisible(false)
                    }
                }

                Utils.hideKeyboard(this@ActivityMain)

                updateSyncStatus()
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)

                fragmentHelper.fragmentFueling?.let {
                    if (it.isVisible) {
                        it.setFabVisible(true)
                    }
                }

                selectItem(clickedMenuId)
            }
        }

        drawerLayout!!.addDrawerListener(drawerToggle!!)
        drawerToggle!!.syncState()
        drawerToggle!!.toolbarNavigationClickListener =
            View.OnClickListener { fragmentHelper.currentFragment.onBackPressed() }

        navigationView = findViewById(R.id.drawer_navigation_view)
        navigationView!!.setNavigationItemSelectedListener { menuItem ->
            clickedMenuId = menuItem.itemId
            openPreferenceSync = false
            // Если текущий фрагмент -- настройки, может отображаться стрелка влево.
            // Если нажат другой пункт меню, показывается значок меню.
            if (currentFragmentId == FragmentFactory.Ids.PREFERENCES && currentFragmentId != menuIdToFragmentId(
                    clickedMenuId
                )
            ) {
                drawerToggle!!.isDrawerIndicatorEnabled = true
            }

            drawerLayout!!.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun initSyncViews() {
        imgSync = findViewById(R.id.image_sync)

        btnSync = findViewById(R.id.btn_sync)

        btnSync!!.setOnClickListener {
            if (preferencesHelper.isSyncEnabled) {
                ContentObserverService.requestSync(this@ActivityMain)
            } else {
                clickedMenuId = R.id.action_preferences

                openPreferenceSync = true

                if (drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout!!.closeDrawer(GravityCompat.START)
                } else {
                    selectItem(clickedMenuId)
                }
            }
        }
    }

    // TODO: move to FragmentFueling
    private fun initLoadingStatusReceiver() {
        broadcastReceiverLoading = object : BroadcastReceiverLoading() {
            override fun onReceive(loading: Boolean) {
                val fragmentFueling = fragmentHelper.fragmentFueling

                if (fragmentFueling != null)
                    fragmentFueling.setLoading(loading)
                else
                    UtilsLog.d(TAG, "broadcastReceiverLoading.onReceive", "fragmentFueling == null")
            }
        }
        broadcastReceiverLoading.register(this)
    }

    private fun initDatabaseChangedReceiver() {
        broadcastReceiverDatabaseChanged = object : BroadcastReceiverDatabaseChanged() {
            override fun onReceive(id: Long) {
                fragmentHelper.fragmentFueling?.updateList(id)
            }
        }
        broadcastReceiverDatabaseChanged.register(this)
    }

    private fun initRequestSyncReceiver() {
        broadcastReceiverRequestSync = object : BroadcastReceiverRequestSync() {
            override fun onReceive(resultCode: Int) {
                UtilsLog.d(BroadcastReceiverRequestSync::class.simpleName, resultCode.toString())

                when (resultCode) {
                    ContentObserverService.RESULT_INTERNET_DISCONNECTED -> {
                        FragmentDialogMessage.show(
                            this@ActivityMain,
                            null,
                            getString(R.string.message_error_no_internet)
                        )
                    }

                    ContentObserverService.RESULT_TOKEN_EMPTY -> {
                        showDialogNeedAuth()
                    }

                    ContentObserverService.RESULT_SYNC_DISABLED -> {
                        UtilsLog.d(TAG, "RESULT_SYNC_DISABLED")
                    }

                    ContentObserverService.RESULT_REQUEST_DONE -> {
                        UtilsLog.d(TAG, "RESULT_REQUEST_DONE")
                    }
                }
            }
        }

        broadcastReceiverRequestSync.register(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_CURRENT_FRAGMENT_ID, currentFragmentId)
    }

    @FragmentFactory.Ids.Id
    private fun menuIdToFragmentId(menuId: Int): Int {
        return when (menuId) {
            R.id.action_fueling -> FragmentFactory.Ids.FUELING
            R.id.action_calc -> FragmentFactory.Ids.CALC
            R.id.action_chart_cost -> FragmentFactory.Ids.CHART_COST
            R.id.action_preferences -> FragmentFactory.Ids.PREFERENCES
            R.id.action_backup -> FragmentFactory.Ids.BACKUP
            R.id.action_about -> FragmentFactory.Ids.ABOUT
            else -> throw IllegalArgumentException("Bad menu id == $menuId")
        }
    }

    private fun fragmentIdToMenuId(@FragmentFactory.Ids.Id fragmentId: Int): Int {
        return when (fragmentId) {
            FragmentFactory.Ids.ABOUT -> R.id.action_about
            FragmentFactory.Ids.BACKUP -> R.id.action_backup
            FragmentFactory.Ids.CALC -> R.id.action_calc
            FragmentFactory.Ids.CHART_COST -> R.id.action_chart_cost
            FragmentFactory.Ids.FUELING -> R.id.action_fueling
            FragmentFactory.Ids.PREFERENCES -> R.id.action_preferences
            FragmentFactory.Ids.BAD_ID -> throw IllegalArgumentException("Bad fragment id == $fragmentId")
            else -> throw IllegalArgumentException("Bad fragment id == $fragmentId")
        }
    }

    private fun selectItem(menuId: Int) {
        if (menuId == -1) return

        clickedMenuId = -1

        val fragmentId = menuIdToFragmentId(menuId)

        if (currentFragmentId == fragmentId) {
            if (currentFragmentId == FragmentFactory.Ids.PREFERENCES) {
                fragmentHelper.fragmentPreferences?.let {
                    if (openPreferenceSync) {
                        it.goToSyncScreen()
                    } else {
                        it.goToRootScreen()
                    }
                }
            }

            return
        }

        fragmentHelper.getFragment(FragmentFactory.Ids.MAIN)?.let {
            if (!it.isVisible) {
                supportFragmentManager.popBackStack()
            }
        }

        if (fragmentId == FragmentFactory.Ids.MAIN) {
            return
        }

        val bundle = Bundle()

        if (openPreferenceSync) {
            bundle.putString(
                FragmentPreferences.KEY_PREFERENCE_SCREEN,
                preferencesHelper.keys.sync
            )

            openPreferenceSync = false
        }

        fragmentHelper.replaceFragment(fragmentId, bundle)
    }

    private val fragmentResultListener =
        FragmentResultListener { _, result ->
            when (result.getInt(DIALOG_TYPE)) {
                DIALOG_TYPE_YANDEX_AUTH ->
                    Utils.openUrl(this@ActivityMain, SyncYandexDisk.URL.AUTH, null)
            }
        }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (drawerLayout!!.isDrawerVisible(GravityCompat.START)) {
                drawerLayout!!.closeDrawer(GravityCompat.START)
                return
            }

            if (fragmentHelper.currentFragment.onBackPressed()) {
                return
            }

            if (supportFragmentManager.backStackEntryCount == 0) {
                finishAffinity()
            } else {
                supportFragmentManager.popBackStack()
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle!!.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        ContentResolver.removeStatusChangeListener(syncMonitor)

        broadcastReceiverDatabaseChanged.unregister(this)
        broadcastReceiverLoading.unregister(this)
        broadcastReceiverRequestSync.unregister(this)

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return false
    }

    override fun onRecordChange(fuelingRecord: FuelingRecord?) {
        startActivity(ActivityFuelingRecordChange.getIntentForStart(this, fuelingRecord))
    }

    override fun onFilterChange(@DatabaseHelper.Filter.Mode filterMode: Int) {
        val position = filterModeToPosition(filterMode)

        if (position != toolbarSpinner?.selectedItemPosition) {
            toolbarSpinner?.setSelection(position)
        }
    }

    override fun onFragmentChange(fragment: FragmentInterface) {
        currentFragmentId = fragment.fragmentId

        title = fragment.title
        toolbarMain?.subtitle = fragment.subtitle

        toolbarSpinner?.visibility =
            if (currentFragmentId == FragmentFactory.Ids.FUELING) View.VISIBLE else View.GONE

        navigationView?.menu?.findItem(fragmentIdToMenuId(currentFragmentId))?.isChecked = true
    }

    override fun setTitle(title: CharSequence?) {
        val actionBar = supportActionBar ?: return

        if (title != null) {
            actionBar.title = title
            actionBar.setDisplayShowTitleEnabled(true)
        } else {
            actionBar.title = null
            actionBar.setDisplayShowTitleEnabled(false)
        }
    }

    override fun onPreferenceScreenChanged(title: CharSequence, isInRoot: Boolean) {
        setTitle(title)
        toggleDrawer(drawerToggle, drawerLayout, !isInRoot)
    }

    private fun toggleDrawer(
        actionBarDrawerToggle: ActionBarDrawerToggle?, drawerLayout: DrawerLayout?,
        showArrow: Boolean
    ) {
        if (drawerToggle == null || drawerToggle?.isDrawerIndicatorEnabled != showArrow) return

        val start: Float
        val end: Float

        if (showArrow) {
            start = 0f
            end = 1f
        } else {
            start = 1f
            end = 0f
        }

        val anim = ValueAnimator.ofFloat(start, end)

        anim.addUpdateListener { valueAnimator ->
            drawerLayout?.let {
                actionBarDrawerToggle?.onDrawerSlide(
                    it,
                    valueAnimator.animatedValue as Float
                )
            }
        }
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (!showArrow) {
                    drawerToggle?.isDrawerIndicatorEnabled = true
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                if (showArrow) {
                    drawerToggle?.isDrawerIndicatorEnabled = false
                }
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
        anim.interpolator = DecelerateInterpolator()
        anim.duration = Utils.getInteger(R.integer.animation_duration_drawer_toggle).toLong()

        anim.start()
    }

    private fun updateSyncStatus() {
        if (syncAccount!!.isSyncActive) {
            btnSync!!.text = getString(R.string.sync_in_process)
            imgSync!!.setImageResource(R.drawable.ic_sync)

            imgSync!!.startAnimation(animationSync)
        } else {
            if (preferencesHelper.isSyncEnabled) {
                if (syncAccount!!.yandexDiskToken.isNullOrEmpty()) {
                    btnSync!!.text = getString(R.string.sync_no_token)
                    imgSync!!.setImageResource(R.drawable.ic_sync_off)
                } else {
                    if (preferencesHelper.lastSyncHasError) {
                        btnSync!!.text = getString(R.string.sync_error)
                        imgSync!!.setImageResource(R.drawable.ic_sync_alert)
                    } else {
                        val dateTime = preferencesHelper.lastSyncDateTime

                        btnSync!!.text = if (dateTime != PreferencesHelper.SYNC_NONE)
                            getString(
                                R.string.sync_done,
                                UtilsDate.getRelativeDateTime(this, dateTime)
                            )
                        else
                            getString(R.string.sync_not_performed)
                        imgSync!!.setImageResource(R.drawable.ic_sync)
                    }
                }
            } else {
                btnSync!!.text = getString(R.string.sync_disabled)
                imgSync!!.setImageResource(R.drawable.ic_sync_off)
            }

            imgSync!!.clearAnimation()
        }
    }

    override fun onPreferenceSyncEnabledChanged(enabled: Boolean) {
        syncAccount!!.setIsSyncable(enabled)

        updateSyncStatus()

        if (enabled && syncAccount!!.yandexDiskToken.isNullOrEmpty()) {
            showDialogNeedAuth()
        }
    }

    override fun onStatusChanged(which: Int) {
        runOnUiThread { updateSyncStatus() }
    }

    private fun showDialogNeedAuth() {
        FragmentDialogQuestion.show(
            this,
            DIALOG_TYPE_YANDEX_AUTH,
            R.string.dialog_caption_auth,
            R.string.message_dialog_auth,
            R.string.dialog_btn_agree, R.string.dialog_btn_disagree
        )
    }

    private val activityYandexMap =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                val data = activityResult.data

                when (data?.getIntExtra(ActivityYandexMap.EXTRA_TYPE, 0)) {
                    ActivityYandexMap.MAP_TYPE_DISTANCE -> {
                        data.getIntExtra(ActivityYandexMap.EXTRA_DISTANCE, 0).let {
                            fragmentHelper.fragmentCalc?.setDistance(it)
                        }
                    }

                    ActivityYandexMap.MAP_TYPE_CENTER -> {
                        preferencesHelper.mapCenter = MapCenter(data)
                    }
                }
            }
        }

    private fun startYandexMap(@ActivityYandexMap.MapType mapType: Int) {
        val connectedState = ConnectivityHelper.getConnectedState(applicationContext)

        UtilsLog.d(TAG, "startYandexMap", "connectedState == $connectedState")

        if (connectedState != ConnectivityHelper.DISCONNECTED) {
            activityYandexMap.launch(
                Intent(this, ActivityYandexMap::class.java)
                    .putExtra(ActivityYandexMap.EXTRA_TYPE, mapType)
            )
        } else {
            FragmentDialogMessage.show(
                this, null,
                parent.getString(R.string.message_error_no_internet)
            )
        }
    }

    override fun onCalcDistanceButtonClick() {
        startYandexMap(ActivityYandexMap.MAP_TYPE_DISTANCE)
    }

    override fun onPreferenceMapCenterClick() {
        startYandexMap(ActivityYandexMap.MAP_TYPE_CENTER)
    }

    override fun onPreferenceSyncYandexDiskClick() {
        Utils.openUrl(
            this,
            SyncYandexDisk.URL.WWW,
            getString(R.string.message_error_yandex_disk_browser_open)
        )
    }

    override fun onPreferenceSMSAddressClick() {
        val intent = ContactsHelper.intent

        if (intent.resolveActivity(packageManager) != null) {
            getActivityContacts.launch(intent)
        } else {
            Utils.toast(R.string.message_error_no_contacts_activity)
        }
    }

    private val getActivityContacts =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val address = ContactsHelper.getPhoneNumber(this, it.data)

                if (address != null) {
                    preferencesHelper.smsAddress = address
                }
            }
        }

    override fun onPreferenceSMSTextPatternClick() {
        ActivityDialog.start(this, ActivityDialog.DIALOG_SMS_TEXT_PATTERN)
    }

    companion object {
        private const val TAG = "ActivityMain"

        private const val KEY_CURRENT_FRAGMENT_ID = "KEY_CURRENT_FRAGMENT_ID"

        const val DIALOG_REQUEST_KEY = "DIALOG_REQUEST_KEY"
        const val DIALOG_TYPE = "DIALOG_TYPE"
        const val DIALOG_TYPE_YANDEX_AUTH = 100

        @DatabaseHelper.Filter.Mode
        private fun positionToFilterMode(position: Int): Int {
            return when (position) {
                0 -> DatabaseHelper.Filter.MODE_CURRENT_YEAR
                1 -> DatabaseHelper.Filter.MODE_DATES
                else -> DatabaseHelper.Filter.MODE_ALL
            }
        }

        private fun filterModeToPosition(@DatabaseHelper.Filter.Mode filterMode: Int): Int {
            return when (filterMode) {
                DatabaseHelper.Filter.MODE_YEAR, DatabaseHelper.Filter.MODE_DATES -> 1
                DatabaseHelper.Filter.MODE_ALL -> 2
                DatabaseHelper.Filter.MODE_CURRENT_YEAR, DatabaseHelper.Filter.MODE_TWO_LAST_RECORDS -> 0
                else -> 0
            }
        }
    }
}