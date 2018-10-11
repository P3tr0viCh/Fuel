package ru.p3tr0vich.fuel.fragments

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.widget.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import com.melnykov.fab.FloatingActionButton
import com.pnikosis.materialishprogress.ProgressWheel
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import ru.p3tr0vich.fuel.DividerItemDecorationFueling
import ru.p3tr0vich.fuel.ImplementException
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.adapters.FuelingAdapter
import ru.p3tr0vich.fuel.factories.FragmentFactory
import ru.p3tr0vich.fuel.factories.FuelingTotalViewFactory
import ru.p3tr0vich.fuel.helpers.ContentResolverHelper
import ru.p3tr0vich.fuel.helpers.DatabaseHelper
import ru.p3tr0vich.fuel.listeners.OnRecyclerViewScrollListener
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.receivers.BroadcastReceiverLoading
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsDate
import ru.p3tr0vich.fuel.utils.UtilsFormat
import ru.p3tr0vich.fuel.utils.UtilsLog
import ru.p3tr0vich.fuel.views.FuelingTotalView
import java.util.*

class FragmentFueling : FragmentBase(FragmentFactory.Ids.FUELING), LoaderManager.LoaderCallbacks<Cursor> {

    private var mToolbarDates: Toolbar? = null
    private var mToolbarShadow: View? = null

    private var mLayoutMain: RelativeLayout? = null
    private var mTotalPanel: ViewGroup? = null

    private var mToolbarDatesVisible: Boolean = false
    private var mTotalPanelVisible: Boolean = false

    private var filter = DatabaseHelper.Filter()

    private var mFuelingAdapter: FuelingAdapter? = null

    private var mBtnDateFrom: Button? = null
    private var mBtnDateTo: Button? = null

    private var mRecyclerView: RecyclerView? = null

    private var mProgressWheel: ProgressWheel? = null
    private var mTextNoRecords: TextView? = null

    private var mFloatingActionButton: FloatingActionButton? = null

    private val mHandler = Handler()

    private var mFuelingTotalView: FuelingTotalView? = null

    private var mIdForScroll: Long = -1

    private var mSnackbar: Snackbar? = null
    private val mSnackBarCallback = object : Snackbar.Callback() {
        override fun onDismissed(snackbar: Snackbar?, event: Int) {
            // Workaround for bug

            if (event == Snackbar.Callback.DISMISS_EVENT_SWIPE)
                mFloatingActionButton!!.toggle(true, true, true)
        }
    }

    private var mDeletedFuelingRecord: FuelingRecord? = null

    private val mUndoClickListener = View.OnClickListener {
        val context = context!!

        ContentResolverHelper.insertRecord(context, mDeletedFuelingRecord!!)

        mDeletedFuelingRecord = null
    }

    private var mOnFilterChangeListener: OnFilterChangeListener? = null
    private var mOnRecordChangeListener: OnRecordChangeListener? = null

    private var mAnimationDuration: AnimationDuration? = null

    private val recyclerViewLayoutManager: LinearLayoutManager
        get() = mRecyclerView!!.layoutManager as LinearLayoutManager

    private val mRunnableShowNoRecords = Runnable { Utils.setViewVisibleAnimate(mTextNoRecords!!, true) }

    private val mRunnableShowProgressWheelFueling = Runnable { Utils.setViewVisibleAnimate(mProgressWheel!!, true) }

    private class AnimationDuration {
        val layoutTotalShow = Utils.getInteger(R.integer.animation_duration_layout_total_show)
        val layoutTotalHide = Utils.getInteger(R.integer.animation_duration_layout_total_hide)
        val startDelayFab = Utils.getInteger(R.integer.animation_start_delay_fab)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAnimationDuration = AnimationDuration()

        if (savedInstanceState == null) {
            if (LOG_ENABLED) UtilsLog.d(TAG, "onCreate", "savedInstanceState == null")

            filter.mode = DatabaseHelper.Filter.MODE_CURRENT_YEAR

            filter.dateFrom = preferencesHelper.filterDateFrom
            filter.dateTo = preferencesHelper.filterDateTo
        } else {
            if (LOG_ENABLED) UtilsLog.d(TAG, "onCreate", "savedInstanceState != null")

            when (savedInstanceState.getInt(KEY_FILTER_MODE, DatabaseHelper.Filter.MODE_ALL)) {
                DatabaseHelper.Filter.MODE_CURRENT_YEAR -> filter.mode = DatabaseHelper.Filter.MODE_CURRENT_YEAR
                DatabaseHelper.Filter.MODE_YEAR -> filter.mode = DatabaseHelper.Filter.MODE_YEAR
                DatabaseHelper.Filter.MODE_DATES -> filter.mode = DatabaseHelper.Filter.MODE_DATES
                else -> filter.mode = DatabaseHelper.Filter.MODE_ALL
            }

            filter.dateFrom = savedInstanceState.getLong(KEY_FILTER_DATE_FROM)
            filter.dateTo = savedInstanceState.getLong(KEY_FILTER_DATE_TO)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onCreateView")
        }

        val isPhone = Utils.isPhone

        val view = inflater.inflate(R.layout.fragment_fueling, container, false)

        mFuelingTotalView = FuelingTotalViewFactory.getFuelingTotalView(view)

        mToolbarDates = view.findViewById(R.id.toolbar_dates)
        mToolbarShadow = view.findViewById(R.id.view_toolbar_shadow)

        mLayoutMain = view.findViewById(R.id.layout_main)

        mTotalPanel = if (isPhone) view.findViewById<View>(R.id.total_panel) as ViewGroup else null

        mRecyclerView = view.findViewById(R.id.recycler_view)
        mRecyclerView!!.setHasFixedSize(true)
        mRecyclerView!!.itemAnimator = DefaultItemAnimator()

        val itemDecoration = DividerItemDecorationFueling(context)
        if (!isPhone) itemDecoration.footerType = FuelingAdapter.TYPE_FOOTER
        mRecyclerView!!.addItemDecoration(itemDecoration)

        mRecyclerView!!.layoutManager = LinearLayoutManager(context)
        mFuelingAdapter = FuelingAdapter(View.OnClickListener { v -> doPopup(v) }, isPhone, !isPhone)
        mRecyclerView!!.adapter = mFuelingAdapter

        if (isPhone)
            mRecyclerView!!.addOnScrollListener(object : OnRecyclerViewScrollListener(
                    resources.getDimensionPixelOffset(R.dimen.recycler_view_scroll_threshold)) {

                override fun onScrollUp() {
                    setTotalAndFabVisible(false)
                }

                override fun onScrollDown() {
                    if (mSnackbar == null || !mSnackbar!!.isShown) setTotalAndFabVisible(true)
                }
            })

        mProgressWheel = view.findViewById(R.id.progress_wheel)
        mProgressWheel!!.visibility = View.GONE
        mTextNoRecords = view.findViewById(R.id.text_no_records)
        mTextNoRecords!!.visibility = View.GONE

        mFloatingActionButton = view.findViewById(R.id.fab)
        mFloatingActionButton!!.setOnClickListener { mOnRecordChangeListener!!.onRecordChange(null) }
        mFloatingActionButton!!.scaleX = 0.0f
        mFloatingActionButton!!.scaleY = 0.0f

        mBtnDateFrom = view.findViewById(R.id.btn_date_from)
        mBtnDateFrom!!.setOnClickListener { showDateDialog(true) }
        mBtnDateFrom!!.setOnLongClickListener { v ->
            doPopupDate(v, true)
            true
        }

        mBtnDateTo = view.findViewById(R.id.btn_date_to)
        mBtnDateTo!!.setOnClickListener { showDateDialog(false) }
        mBtnDateTo!!.setOnLongClickListener { v ->
            doPopupDate(v, false)
            true
        }

        Utils.setBackgroundTint(mBtnDateFrom, R.color.toolbar_title_text, R.color.primary_light)
        Utils.setBackgroundTint(mBtnDateTo, R.color.toolbar_title_text, R.color.primary_light)

        mToolbarDatesVisible = true
        setToolbarDatesVisible(filter.mode == DatabaseHelper.Filter.MODE_DATES, false)

        mTotalPanelVisible = true

        updateFilterDateButtons(true, filter.dateFrom)
        updateFilterDateButtons(false, filter.dateTo)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (LOG_ENABLED)
            UtilsLog.d(TAG, "onActivityCreated", "savedInstanceState " +
                    (if (savedInstanceState == null) "=" else "!") + "= null")

        doSetFilterMode(filter.mode)

        val loaderManager = loaderManager

        loaderManager.initLoader(FUELING_CURSOR_LOADER_ID, null, this)
        loaderManager.initLoader(FUELING_TOTAL_CURSOR_LOADER_ID, null, this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_FILTER_MODE, filter.mode)
        outState.putLong(KEY_FILTER_DATE_FROM, filter.dateFrom)
        outState.putLong(KEY_FILTER_DATE_TO, filter.dateTo)
    }

    override fun onDestroyView() {
        mFuelingTotalView!!.destroy()

        super.onDestroyView()
    }

    override fun onDestroy() {
        mHandler.removeCallbacks(mRunnableShowNoRecords)
        mHandler.removeCallbacks(mRunnableShowProgressWheelFueling)

        preferencesHelper.putFilterDate(filter.dateFrom, filter.dateTo)

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        setFabVisible(true)
    }

    override fun onPause() {
        setFabVisible(false)
        super.onPause()
    }

    private fun doSetFilterMode(@DatabaseHelper.Filter.Mode filterMode: Int) {
        filter.mode = filterMode

        mOnFilterChangeListener!!.onFilterChange(filterMode)
    }

    /**
     * Изменяет текущий фильтр и вызывает restartLoader в случае изменения.
     *
     * @param filterMode новый фильтр.
     * @return true, если фильтр изменился.
     */
    fun setFilterMode(@DatabaseHelper.Filter.Mode filterMode: Int): Boolean {
        if (filter.mode != filterMode) {

            setToolbarDatesVisible(filterMode == DatabaseHelper.Filter.MODE_DATES, true)

            doSetFilterMode(filterMode)

            loaderManager.restartLoader(FUELING_CURSOR_LOADER_ID, null, this)

            setTotalAndFabVisible(true)

            return true
        }

        return false
    }

    private fun isDateTimeInCurrentYear(dateTime: Long): Boolean {
        val year = UtilsDate.getCalendarInstance(dateTime).get(Calendar.YEAR)
        return year == UtilsDate.currentYear
    }

    /**
     * Проверяет, входит ли dateTime в фильтр записей filter.
     *
     * @param dateTime дата и время.
     * @return true, если dateTime входит в фильтр, можно вызвать forceLoad,
     * и false, если фильтр был изменён, при этом был вызван restartLoader.
     */
    private fun checkDateTime(dateTime: Long): Boolean {
        when (filter.mode) {
            DatabaseHelper.Filter.MODE_ALL -> return true

            DatabaseHelper.Filter.MODE_CURRENT_YEAR -> {
                if (isDateTimeInCurrentYear(dateTime))
                    return true
                if (dateTime >= filter.dateFrom && dateTime <= filter.dateTo)
                    return true
                else if (isDateTimeInCurrentYear(dateTime))
                    return !setFilterMode(DatabaseHelper.Filter.MODE_CURRENT_YEAR) // not
            }

            DatabaseHelper.Filter.MODE_DATES -> if (dateTime >= filter.dateFrom && dateTime <= filter.dateTo)
                return true
            else if (isDateTimeInCurrentYear(dateTime))
                return !setFilterMode(DatabaseHelper.Filter.MODE_CURRENT_YEAR)
        }

        return !setFilterMode(DatabaseHelper.Filter.MODE_ALL) // not
    }

    fun updateList(id: Long) {
        mIdForScroll = id

        var forceLoad = true

        if (id != -1L) {
            val context = context!!

            val fuelingRecord = ContentResolverHelper.getFuelingRecord(context, id)

            if (fuelingRecord != null) {
                forceLoad = checkDateTime(fuelingRecord.dateTime)
            }
        }

        val loaderManager: LoaderManager?
        try {
            loaderManager = getLoaderManager()
        } catch (e: Exception) {
            UtilsLog.d(TAG, "updateList", "getLoaderManager exception = " + e.message)

            return
        }

        if (loaderManager == null) {
            UtilsLog.d(TAG, "updateList", "loaderManager == null")

            return
        }

        if (forceLoad) {
            loaderManager.getLoader<FuelingCursorLoader>(FUELING_CURSOR_LOADER_ID)?.forceLoad()
        }

        loaderManager.getLoader<FuelingTotalCursorLoader>(FUELING_TOTAL_CURSOR_LOADER_ID)?.forceLoad()
    }

    private fun markRecordAsDeleted(fuelingRecord: FuelingRecord): Boolean {
        val id = fuelingRecord.id

        val context = context!!

        if (ContentResolverHelper.markRecordAsDeleted(context, id)) {
            return true
        } else {
            Utils.toast(R.string.message_error_delete_record)

            return false
        }
    }

    private fun isItemVisible(position: Int): Boolean {
        val firstVisibleItem = recyclerViewLayoutManager
                .findFirstCompletelyVisibleItemPosition()
        val lastVisibleItem = recyclerViewLayoutManager
                .findLastCompletelyVisibleItemPosition()

        return firstVisibleItem != RecyclerView.NO_POSITION &&
                lastVisibleItem != RecyclerView.NO_POSITION &&
                position > firstVisibleItem && position < lastVisibleItem
    }

    private fun scrollToPosition(position: Int) {
        var position = position
        if (position < 0) return

        if (mFuelingAdapter!!.isShowHeader && position == FuelingAdapter.HEADER_POSITION + 1)
            position = FuelingAdapter.HEADER_POSITION


        if (isItemVisible(position)) return

        recyclerViewLayoutManager.scrollToPositionWithOffset(position, 0)
    }

    private fun setFilterDate(dateFrom: Long, dateTo: Long) {
        filter.dateFrom = dateFrom
        filter.dateTo = dateTo

        loaderManager.restartLoader(FUELING_CURSOR_LOADER_ID, null, this)
    }

    private fun setFilterDate(setDateFrom: Boolean, date: Long) {
        if (setDateFrom) {
            filter.dateFrom = date
        } else {
            filter.dateTo = date
        }

        loaderManager.restartLoader(FUELING_CURSOR_LOADER_ID, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return when (id) {
            FUELING_CURSOR_LOADER_ID -> FuelingCursorLoader(context!!, filter)
            FUELING_TOTAL_CURSOR_LOADER_ID -> FuelingTotalCursorLoader(context!!)
            else -> throw IllegalArgumentException("Wrong Loader ID")
        }
    }

    private fun swapRecords(data: Cursor?) {
        mHandler.removeCallbacks(mRunnableShowNoRecords)

        val records = DatabaseHelper.getFuelingRecords(data)

        if (records == null || records.isEmpty())
            mHandler.postDelayed(mRunnableShowNoRecords, Utils.getInteger(R.integer.delayed_time_show_no_records).toLong())
        else
            mTextNoRecords!!.visibility = View.GONE

        mFuelingAdapter!!.showYear = filter.mode != DatabaseHelper.Filter.MODE_CURRENT_YEAR
        mFuelingAdapter!!.swapRecords(records)

        if (mIdForScroll != -1L) {
            scrollToPosition(mFuelingAdapter!!.findPositionById(mIdForScroll))
            mIdForScroll = -1
        }

        mFuelingTotalView!!.onFuelingRecordsChanged(records)
    }

    private fun updateTotal(data: Cursor?) {
        val records = DatabaseHelper.getFuelingRecords(data)
        mFuelingTotalView!!.onLastFuelingRecordsChanged(records)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onLoadFinished")

        when (loader.id) {
            FUELING_CURSOR_LOADER_ID -> swapRecords(data)
            FUELING_TOTAL_CURSOR_LOADER_ID -> updateTotal(data)
        }

    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "onLoaderReset")

        when (loader.id) {
            FUELING_CURSOR_LOADER_ID -> swapRecords(null)
            FUELING_TOTAL_CURSOR_LOADER_ID -> updateTotal(null)
        }
    }

    private fun doPopup(v: View) {
        val activity = activity!!

        val popupMenu = PopupMenu(activity, v)
        popupMenu.inflate(R.menu.menu_fueling)

        var menuHelper: Any? = null
        try {
            val fMenuHelper = PopupMenu::class.java.getDeclaredField("mPopup")
            fMenuHelper.isAccessible = true
            menuHelper = fMenuHelper.get(popupMenu)
            menuHelper!!.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType!!).invoke(menuHelper, true)
        } catch (e: Exception) {
            //
        }

        val id = v.tag as Long

        popupMenu.setOnMenuItemClickListener(
                PopupMenu.OnMenuItemClickListener { item ->
                    val context = context!!

                    val fuelingRecord = ContentResolverHelper.getFuelingRecord(context, id)

                    if (fuelingRecord == null) {
                        UtilsLog.d(TAG, "onMenuItemClick",
                                "ContentProviderHelper.getFuelingRecord() == null")
                        updateList(-1)
                        return@OnMenuItemClickListener true
                    }

                    when (item.itemId) {
                        R.id.action_fueling_update -> {
                            mOnRecordChangeListener!!.onRecordChange(fuelingRecord)
                            true
                        }
                        R.id.action_fueling_delete -> {
                            mDeletedFuelingRecord = fuelingRecord

                            if (markRecordAsDeleted(fuelingRecord)) {
                                mSnackbar = Snackbar
                                        .make(mLayoutMain!!, R.string.message_record_deleted,
                                                Snackbar.LENGTH_LONG)
                                        .setAction(R.string.dialog_btn_cancel, mUndoClickListener)
                                        .addCallback(mSnackBarCallback)
                                mSnackbar!!.show()
                            }

                            true
                        }
                        else -> false
                    }
                }
        )

        popupMenu.show()

        try {
            if (menuHelper == null) return

            val fListPopup = menuHelper.javaClass.getDeclaredField("mPopup")
            fListPopup.isAccessible = true
            val listPopup = fListPopup.get(menuHelper)
            val listPopupClass = listPopup.javaClass

            // Magic number
            listPopupClass.getDeclaredMethod("setVerticalOffset", Int::class.javaPrimitiveType!!).invoke(listPopup, -v.height - 8)

            listPopupClass.getDeclaredMethod("show").invoke(listPopup)
        } catch (e: Exception) {
            //
        }

    }

    fun setLoading(loading: Boolean) {
        mHandler.removeCallbacks(mRunnableShowProgressWheelFueling)
        if (loading) {
            mHandler.postDelayed(mRunnableShowProgressWheelFueling,
                    Utils.getInteger(R.integer.delayed_time_show_progress_wheel).toLong())
        } else
            Utils.setViewVisibleAnimate(mProgressWheel!!, false)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mOnFilterChangeListener = context as OnFilterChangeListener?
            mOnRecordChangeListener = context as OnRecordChangeListener?
        } catch (e: ClassCastException) {
            throw ImplementException(context!!,
                    arrayOf(OnFilterChangeListener::class.java, OnRecordChangeListener::class.java))
        }

    }

    private fun setToolbarDatesVisible(visible: Boolean, animate: Boolean) {
        if (mToolbarDatesVisible == visible) {
            return
        }

        mToolbarDatesVisible = visible

        val context = context!!

        val toolbarDatesTopHidden = -Utils.getSupportActionBarSize(context) // minus!
        val toolbarShadowHeight = resources.getDimensionPixelSize(R.dimen.toolbar_shadow_height)

        if (animate) {
            val valueAnimatorShadowShow = ValueAnimator.ofInt(0, toolbarShadowHeight)
            valueAnimatorShadowShow
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar_shadow).toLong())
                    .addUpdateListener { animation -> Utils.setViewHeight(mToolbarShadow!!, animation.animatedValue as Int) }

            val valueAnimatorShadowHide = ValueAnimator.ofInt(toolbarShadowHeight, 0)
            valueAnimatorShadowHide
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar_shadow).toLong())
                    .addUpdateListener { animation -> Utils.setViewHeight(mToolbarShadow!!, animation.animatedValue as Int) }

            val valueAnimatorToolbar = ValueAnimator.ofInt(
                    if (visible) toolbarDatesTopHidden else 0, if (visible) 0 else toolbarDatesTopHidden)
            valueAnimatorToolbar
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar).toLong())
                    .addUpdateListener { animation -> Utils.setViewTopMargin(mToolbarDates!!, animation.animatedValue as Int) }

            val animatorSet = AnimatorSet()
            animatorSet.playSequentially(valueAnimatorShadowShow,
                    valueAnimatorToolbar, valueAnimatorShadowHide)
            animatorSet.start()
        } else
            Utils.setViewTopMargin(mToolbarDates!!, if (visible) 0 else toolbarDatesTopHidden)
    }

    private fun setTotalPanelVisible(visible: Boolean) {
        if (mTotalPanelVisible == visible) return

        mTotalPanelVisible = visible

        val valueAnimator = ValueAnimator.ofInt(
                mTotalPanel!!.translationY.toInt(), if (visible) 0 else mTotalPanel!!.height)
        valueAnimator
                .setDuration((if (visible)
                    mAnimationDuration!!.layoutTotalShow
                else
                    mAnimationDuration!!.layoutTotalHide).toLong())
                .addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                    internal var translationY: Int = 0

                    override fun onAnimationUpdate(animation: ValueAnimator) {
                        translationY = animation.animatedValue as Int

                        mTotalPanel!!.translationY = translationY.toFloat()
                        Utils.setViewTopMargin(mTotalPanel!!, -translationY)
                    }
                })
        valueAnimator.start()
    }

    private fun setTotalAndFabVisible(visible: Boolean) {
        mFloatingActionButton!!.toggle(visible, true)
        setTotalPanelVisible(visible)
    }

    fun setFabVisible(visible: Boolean) {
        val value = if (visible) 1.0f else 0.0f
        mFloatingActionButton!!.animate()
                .setStartDelay(mAnimationDuration!!.startDelayFab.toLong()).scaleX(value).scaleY(value)
    }

    private fun updateFilterDateButtons(dateFrom: Boolean, date: Long) {
        (if (dateFrom) mBtnDateFrom else mBtnDateTo)!!.text = UtilsFormat.dateTimeToString(date, true, Utils.isPhoneInPortrait)
    }

    private fun setPopupFilterDate(setDateFrom: Boolean, menuId: Int) {
        when (menuId) {
            R.id.action_dates_start_of_year, R.id.action_dates_end_of_year -> {
                val calendar = UtilsDate.getCalendarInstance(if (setDateFrom) filter.dateFrom else filter.dateTo)

                when (menuId) {
                    R.id.action_dates_start_of_year -> {
                        calendar.set(Calendar.MONTH, Calendar.JANUARY)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                    }
                    R.id.action_dates_end_of_year -> {
                        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                        calendar.set(Calendar.DAY_OF_MONTH, 31)
                    }
                }

                val date = calendar.timeInMillis

                updateFilterDateButtons(setDateFrom, date)
                setFilterDate(setDateFrom, date)
            }
            R.id.action_dates_winter, R.id.action_dates_summer, R.id.action_dates_curr_year, R.id.action_dates_prev_year -> {
                val calendarFrom = Calendar.getInstance()
                val calendarTo = Calendar.getInstance()

                var year = 0

                when (menuId) {
                    R.id.action_dates_winter, R.id.action_dates_summer -> {
                        calendarFrom.timeInMillis = if (setDateFrom) filter.dateFrom else filter.dateTo

                        year = calendarFrom.get(Calendar.YEAR)
                    }
                    R.id.action_dates_curr_year, R.id.action_dates_prev_year -> {
                        year = UtilsDate.currentYear
                        if (menuId == R.id.action_dates_prev_year) year--
                    }
                }

                when (menuId) {
                    R.id.action_dates_winter -> {
                        // TODO: добавить выбор -- зима начала года и зима конца года
                        calendarFrom.set(year - 1, Calendar.DECEMBER, 1)
                        calendarTo.set(Calendar.YEAR, year)
                        calendarTo.set(Calendar.MONTH, Calendar.FEBRUARY)
                        calendarTo.set(Calendar.DAY_OF_MONTH,
                                calendarTo.getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    R.id.action_dates_summer -> {
                        calendarFrom.set(year, Calendar.JUNE, 1)
                        calendarTo.set(year, Calendar.AUGUST, 31)
                    }
                    R.id.action_dates_curr_year, R.id.action_dates_prev_year -> {
                        calendarFrom.set(year, Calendar.JANUARY, 1)
                        calendarTo.set(year, Calendar.DECEMBER, 31)
                    }
                }

                UtilsDate.setStartOfDay(calendarFrom)
                UtilsDate.setEndOfDay(calendarTo)

                val dateFrom = calendarFrom.timeInMillis
                val dateTo = calendarTo.timeInMillis

                updateFilterDateButtons(true, dateFrom)
                updateFilterDateButtons(false, dateTo)

                setFilterDate(dateFrom, dateTo)
            }
        }
    }

    private fun doPopupDate(v: View, dateFrom: Boolean) {
        val activity = activity!!

        val popupMenu = PopupMenu(activity, v)
        popupMenu.inflate(R.menu.menu_dates)

        var menuHelper: Any? = null
        try {
            val fMenuHelper = PopupMenu::class.java.getDeclaredField("mPopup")
            fMenuHelper.isAccessible = true
            menuHelper = fMenuHelper.get(popupMenu)
        } catch (e: Exception) {
            //
        }

        popupMenu.setOnMenuItemClickListener { item ->
            setPopupFilterDate(dateFrom, item.itemId)
            true
        }

        popupMenu.show()

        try {
            if (menuHelper == null) return

            val fListPopup = menuHelper.javaClass.getDeclaredField("mPopup")
            fListPopup.isAccessible = true
            val listPopup = fListPopup.get(menuHelper)
            val listPopupClass = listPopup.javaClass

            // Magic number
            listPopupClass.getDeclaredMethod("setVerticalOffset", Int::class.javaPrimitiveType!!).invoke(listPopup, -v.height - 8)

            listPopupClass.getDeclaredMethod("show").invoke(listPopup)
        } catch (e: Exception) {
            //
        }

    }

    private fun showDateDialog(dateFrom: Boolean) {
        val calendar = UtilsDate.getCalendarInstance(if (dateFrom) filter.dateFrom else filter.dateTo)

        val fragmentManager = fragmentManager!!

        DatePickerDialog.newInstance(
                { view, year, monthOfYear, dayOfMonth ->
                    calendar.set(year, monthOfYear, dayOfMonth)

                    val date = calendar.timeInMillis

                    updateFilterDateButtons(dateFrom, date)

                    setFilterDate(dateFrom, date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show(fragmentManager, null)
    }

    /**
     * Вызывается при изменении фильтра.
     * Используется в главной активности для обновления списка в тулбаре.
     */
    interface OnFilterChangeListener {
        /**
         * @param filterMode текущий фильтр.
         */
        fun onFilterChange(@DatabaseHelper.Filter.Mode filterMode: Int)
    }

    /**
     * Вызывается при добавлении или обновлении записи.
     * Используется в главной активности для вызова активности изменения записи.
     */
    interface OnRecordChangeListener {
        /**
         * @param fuelingRecord если null -- добавляется новая запись,
         * иначе -- запись обновляется.
         */
        fun onRecordChange(fuelingRecord: FuelingRecord?)
    }

    private class FuelingCursorLoader internal constructor(context: Context, private val mFilter: DatabaseHelper.Filter) : CursorLoader(context) {

        override fun loadInBackground(): Cursor? {
            if (LOG_ENABLED) UtilsLog.d(TAG, "FuelingCursorLoader", "loadInBackground")

            BroadcastReceiverLoading.send(context, true)

            try {
                return ContentResolverHelper.getAll(context, mFilter)
            } finally {
                BroadcastReceiverLoading.send(context, false)
            }
        }
    }

    private class FuelingTotalCursorLoader internal constructor(context: Context) : CursorLoader(context) {

        override fun loadInBackground(): Cursor? {
            if (LOG_ENABLED) UtilsLog.d(TAG, "FuelingTotalCursorLoader", "loadInBackground")

            return ContentResolverHelper.getTwoLastRecords(context)
        }
    }

    companion object {

        private const val TAG = "FragmentFueling"

        private var LOG_ENABLED = false

        private const val KEY_FILTER_MODE = "KEY_FILTER_MODE"
        private const val KEY_FILTER_DATE_FROM = "KEY_FILTER_DATE_FROM"
        private const val KEY_FILTER_DATE_TO = "KEY_FILTER_DATE_TO"

        private const val FUELING_CURSOR_LOADER_ID = 0
        private const val FUELING_TOTAL_CURSOR_LOADER_ID = 1
    }
}