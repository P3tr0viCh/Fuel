package ru.p3tr0vich.fuel.fragments

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
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

    private var toolbarDates: Toolbar? = null
    private var toolbarShadow: View? = null

    private var layoutMain: RelativeLayout? = null
    private var totalPanel: ViewGroup? = null

    private var toolbarDatesVisible: Boolean = false
    private var totalPanelVisible: Boolean = false

    private var filter = DatabaseHelper.Filter()

    private var fuelingAdapter: FuelingAdapter? = null

    private var btnDateFrom: Button? = null
    private var btnDateTo: Button? = null

    private var recyclerView: RecyclerView? = null

    private var progressWheel: ProgressWheel? = null
    private var textNoRecords: TextView? = null

    private var floatingActionButton: FloatingActionButton? = null

    private val handler = Handler()

    private var fuelingTotalView: FuelingTotalView? = null

    private var idForScroll: Long = -1

    private var snackbar: Snackbar? = null
    private val snackBarCallback = object : Snackbar.Callback() {
        override fun onDismissed(snackbar: Snackbar?, event: Int) {
            // Workaround for bug

            if (event == Snackbar.Callback.DISMISS_EVENT_SWIPE)
                floatingActionButton?.toggle(true, true, true)
        }
    }

    private var deletedFuelingRecord: FuelingRecord? = null

    private val undoClickListener = View.OnClickListener {
        ContentResolverHelper.insertRecord(context!!, deletedFuelingRecord)

        deletedFuelingRecord = null
    }

    private var onFilterChangeListener: OnFilterChangeListener? = null
    private var onRecordChangeListener: OnRecordChangeListener? = null

    private var animationDuration: AnimationDuration? = null

    private val recyclerViewLayoutManager: LinearLayoutManager
        get() = recyclerView!!.layoutManager as LinearLayoutManager

    private val runnableShowNoRecords = Runnable { Utils.setViewVisibleAnimate(textNoRecords, true) }

    private val runnableShowProgressWheelFueling = Runnable { Utils.setViewVisibleAnimate(progressWheel, true) }

    private class AnimationDuration {
        val layoutTotalShow = Utils.getInteger(R.integer.animation_duration_layout_total_show)
        val layoutTotalHide = Utils.getInteger(R.integer.animation_duration_layout_total_hide)
        val startDelayFab = Utils.getInteger(R.integer.animation_start_delay_fab)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        animationDuration = AnimationDuration()

        if (savedInstanceState == null) {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "onCreate", "savedInstanceState == null")
            }

            filter.mode = DatabaseHelper.Filter.MODE_CURRENT_YEAR

            filter.dateFrom = preferencesHelper.filterDateFrom
            filter.dateTo = preferencesHelper.filterDateTo
        } else {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "onCreate", "savedInstanceState != null")
            }

            //todo: parcelize
            filter.mode = savedInstanceState.getInt(KEY_FILTER_MODE, DatabaseHelper.Filter.MODE_ALL)

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

        fuelingTotalView = FuelingTotalViewFactory.getFuelingTotalView(view)

        toolbarDates = view.findViewById(R.id.toolbar_dates)
        toolbarShadow = view.findViewById(R.id.view_toolbar_shadow)

        layoutMain = view.findViewById(R.id.layout_main)

        totalPanel = if (isPhone) view.findViewById<View>(R.id.total_panel) as ViewGroup else null

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.itemAnimator = DefaultItemAnimator()

        val itemDecoration = DividerItemDecorationFueling(context)
        if (!isPhone) itemDecoration.footerType = FuelingAdapter.TYPE_FOOTER
        recyclerView!!.addItemDecoration(itemDecoration)

        recyclerView!!.layoutManager = LinearLayoutManager(context)
        fuelingAdapter = FuelingAdapter(View.OnClickListener { v -> doPopup(v) }, isPhone, !isPhone)
        recyclerView!!.adapter = fuelingAdapter

        if (isPhone)
            recyclerView!!.addOnScrollListener(object : OnRecyclerViewScrollListener(
                    resources.getDimensionPixelOffset(R.dimen.recycler_view_scroll_threshold)) {

                override fun onScrollUp() {
                    setTotalAndFabVisible(false)
                }

                override fun onScrollDown() {
                    //todo: check
                    if (snackbar?.isShown != true) setTotalAndFabVisible(true)
                }
            })

        progressWheel = view.findViewById(R.id.progress_wheel)
        progressWheel!!.visibility = View.GONE
        textNoRecords = view.findViewById(R.id.text_no_records)
        textNoRecords!!.visibility = View.GONE

        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton!!.setOnClickListener { onRecordChangeListener?.onRecordChange(null) }
        floatingActionButton!!.scaleX = 0.0f
        floatingActionButton!!.scaleY = 0.0f

        btnDateFrom = view.findViewById(R.id.btn_date_from)
        btnDateFrom!!.setOnClickListener { showDateDialog(btnDateFrom) }
        btnDateFrom!!.setOnLongClickListener { v ->
            doPopupDate(v, btnDateFrom)
            true
        }

        btnDateTo = view.findViewById(R.id.btn_date_to)
        btnDateTo!!.setOnClickListener { showDateDialog(btnDateTo) }
        btnDateTo!!.setOnLongClickListener { v ->
            doPopupDate(v, btnDateTo)
            true
        }

        Utils.setBackgroundTint(btnDateFrom, R.color.toolbar_title_text, R.color.primary_light)
        Utils.setBackgroundTint(btnDateTo, R.color.toolbar_title_text, R.color.primary_light)

        toolbarDatesVisible = true
        setToolbarDatesVisible(filter.mode == DatabaseHelper.Filter.MODE_DATES, false)

        totalPanelVisible = true

        updateFilterDateButton(btnDateFrom, filter.dateFrom)
        updateFilterDateButton(btnDateTo, filter.dateTo)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onActivityCreated", "savedInstanceState " +
                    (if (savedInstanceState == null) "=" else "!") + "= null")
        }

        doSetFilterMode(filter.mode)

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
        fuelingTotalView?.destroy()

        super.onDestroyView()
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnableShowNoRecords)
        handler.removeCallbacks(runnableShowProgressWheelFueling)

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

        onFilterChangeListener?.onFilterChange(filterMode)
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
        return UtilsDate.getCalendarInstance(dateTime).get(Calendar.YEAR) == UtilsDate.currentYear
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
        idForScroll = id

        var forceLoad = true

        if (id != -1L) {
            val fuelingRecord = ContentResolverHelper.getFuelingRecord(context!!, id)

            if (fuelingRecord != null) {
                forceLoad = checkDateTime(fuelingRecord.dateTime)
            }
        }

        if (forceLoad) {
            loaderManager.getLoader<FuelingCursorLoader>(FUELING_CURSOR_LOADER_ID)?.forceLoad()
        }

        loaderManager.getLoader<FuelingTotalCursorLoader>(FUELING_TOTAL_CURSOR_LOADER_ID)?.forceLoad()
    }

    private fun markRecordAsDeleted(fuelingRecord: FuelingRecord): Boolean {
        val id = fuelingRecord.id

        return if (ContentResolverHelper.markRecordAsDeleted(context!!, id)) {
            true
        } else {
            Utils.toast(R.string.message_error_delete_record)

            false
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
        if (position < 0) return

        var pos = position

        if (fuelingAdapter!!.isShowHeader && position == FuelingAdapter.HEADER_POSITION + 1) {
            pos = FuelingAdapter.HEADER_POSITION
        }

        if (isItemVisible(pos)) {
            return
        }

        recyclerViewLayoutManager.scrollToPositionWithOffset(pos, 0)
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
        handler.removeCallbacks(runnableShowNoRecords)

        val records = DatabaseHelper.getFuelingRecords(data)

        if (records?.isEmpty() == true) {
            handler.postDelayed(runnableShowNoRecords, Utils.getInteger(R.integer.delayed_time_show_no_records).toLong())
        } else {
            textNoRecords!!.visibility = View.GONE
        }

        fuelingAdapter!!.showYear = filter.mode != DatabaseHelper.Filter.MODE_CURRENT_YEAR
        fuelingAdapter!!.swapRecords(records)

        if (idForScroll != -1L) {
            scrollToPosition(fuelingAdapter!!.findPositionById(idForScroll))
            idForScroll = -1
        }

        fuelingTotalView!!.onFuelingRecordsChanged(records)
    }

    private fun updateTotal(data: Cursor?) {
        fuelingTotalView!!.onLastFuelingRecordsChanged(DatabaseHelper.getFuelingRecords(data))
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onLoadFinished")
        }

        when (loader.id) {
            FUELING_CURSOR_LOADER_ID -> swapRecords(data)
            FUELING_TOTAL_CURSOR_LOADER_ID -> updateTotal(data)
        }

    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onLoaderReset")
        }

        when (loader.id) {
            FUELING_CURSOR_LOADER_ID -> swapRecords(null)
            FUELING_TOTAL_CURSOR_LOADER_ID -> updateTotal(null)
        }
    }

    private fun doPopup(v: View) {
        val popupMenu = PopupMenu(activity!!, v)
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
                    val fuelingRecord = ContentResolverHelper.getFuelingRecord(context!!, id)

                    if (fuelingRecord == null) {
                        UtilsLog.d(TAG, "onMenuItemClick",
                                "ContentProviderHelper.getFuelingRecord() == null")

                        updateList(-1)

                        return@OnMenuItemClickListener true
                    }

                    when (item.itemId) {
                        R.id.action_fueling_update -> {
                            onRecordChangeListener?.onRecordChange(fuelingRecord)
                            true
                        }
                        R.id.action_fueling_delete -> {
                            deletedFuelingRecord = fuelingRecord

                            if (markRecordAsDeleted(fuelingRecord)) {
                                snackbar = Snackbar
                                        .make(layoutMain!!, R.string.message_record_deleted,
                                                Snackbar.LENGTH_LONG)
                                        .setAction(R.string.dialog_btn_cancel, undoClickListener)
                                        .addCallback(snackBarCallback)

                                snackbar!!.show()
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
        handler.removeCallbacks(runnableShowProgressWheelFueling)

        if (loading) {
            handler.postDelayed(runnableShowProgressWheelFueling, Utils.getInteger(R.integer.delayed_time_show_progress_wheel).toLong())
        } else {
            Utils.setViewVisibleAnimate(progressWheel, false)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onFilterChangeListener = context as OnFilterChangeListener?
            onRecordChangeListener = context as OnRecordChangeListener?
        } catch (e: ClassCastException) {
            throw ImplementException(context,
                    arrayOf(OnFilterChangeListener::class.java, OnRecordChangeListener::class.java))
        }
    }

    private fun setToolbarDatesVisible(visible: Boolean, animate: Boolean) {
        if (toolbarDatesVisible == visible) {
            return
        }

        toolbarDatesVisible = visible

        val toolbarDatesTopHidden = -Utils.getSupportActionBarSize(context!!) // minus!
        val toolbarShadowHeight = resources.getDimensionPixelSize(R.dimen.toolbar_shadow_height)

        if (animate) {
            val valueAnimatorShadowShow = ValueAnimator.ofInt(0, toolbarShadowHeight)
            valueAnimatorShadowShow
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar_shadow).toLong())
                    .addUpdateListener { animation -> Utils.setViewHeight(toolbarShadow!!, animation.animatedValue as Int) }

            val valueAnimatorShadowHide = ValueAnimator.ofInt(toolbarShadowHeight, 0)
            valueAnimatorShadowHide
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar_shadow).toLong())
                    .addUpdateListener { animation -> Utils.setViewHeight(toolbarShadow!!, animation.animatedValue as Int) }

            val valueAnimatorToolbar = ValueAnimator.ofInt(
                    if (visible) toolbarDatesTopHidden else 0, if (visible) 0 else toolbarDatesTopHidden)
            valueAnimatorToolbar
                    .setDuration(Utils.getInteger(R.integer.animation_duration_toolbar).toLong())
                    .addUpdateListener { animation -> Utils.setViewTopMargin(toolbarDates!!, animation.animatedValue as Int) }

            with(AnimatorSet()) {
                playSequentially(valueAnimatorShadowShow,
                        valueAnimatorToolbar, valueAnimatorShadowHide)

                start()
            }
        } else {
            Utils.setViewTopMargin(toolbarDates, if (visible) 0 else toolbarDatesTopHidden)
        }
    }

    private fun setTotalPanelVisible(visible: Boolean) {
        if (totalPanelVisible == visible) return

        totalPanelVisible = visible

        val valueAnimator = ValueAnimator.ofInt(
                totalPanel!!.translationY.toInt(), if (visible) 0 else totalPanel!!.height)

        valueAnimator
                .setDuration((if (visible)
                    animationDuration!!.layoutTotalShow
                else
                    animationDuration!!.layoutTotalHide).toLong())

                .addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                    var translationY: Int = 0

                    override fun onAnimationUpdate(animation: ValueAnimator) {
                        translationY = animation.animatedValue as Int

                        totalPanel!!.translationY = translationY.toFloat()

                        Utils.setViewTopMargin(totalPanel, -translationY)
                    }
                })

        valueAnimator.start()
    }

    private fun setTotalAndFabVisible(visible: Boolean) {
        floatingActionButton?.toggle(visible, true)
        setTotalPanelVisible(visible)
    }

    fun setFabVisible(visible: Boolean) {
        val value = if (visible) 1.0f else 0.0f

        floatingActionButton?.animate()?.setStartDelay(animationDuration!!.startDelayFab.toLong())?.scaleX(value)?.scaleY(value)
    }

    private fun updateFilterDateButton(button: Button?, date: Long) {
        button?.text = UtilsFormat.dateTimeToString(date, true, Utils.isPhoneInPortrait)
    }

    private fun setPopupFilterDate(button: Button?, menuId: Int) {
        when (menuId) {
            R.id.action_dates_start_of_year,
            R.id.action_dates_end_of_year -> {
                val calendar = UtilsDate.getCalendarInstance(if (button == btnDateFrom) filter.dateFrom else filter.dateTo)

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

                updateFilterDateButton(button, date)

                setFilterDate(button == btnDateFrom, date)
            }
            R.id.action_dates_winter, R.id.action_dates_summer, R.id.action_dates_curr_year, R.id.action_dates_prev_year -> {
                val calendarFrom = Calendar.getInstance()
                val calendarTo = Calendar.getInstance()

                var year = 0

                when (menuId) {
                    R.id.action_dates_winter, R.id.action_dates_summer -> {
                        calendarFrom.timeInMillis = if (button == btnDateFrom) filter.dateFrom else filter.dateTo

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

                updateFilterDateButton(btnDateFrom, dateFrom)
                updateFilterDateButton(btnDateTo, dateTo)

                setFilterDate(dateFrom, dateTo)
            }
        }
    }

    private fun doPopupDate(v: View, button: Button?) {
        val popupMenu = PopupMenu(activity!!, v)
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
            setPopupFilterDate(button, item.itemId)
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

    private fun showDateDialog(button: Button?) {
        val calendar = UtilsDate.getCalendarInstance(if (button == btnDateFrom) filter.dateFrom else filter.dateTo)

        DatePickerDialog.newInstance(
                { _, year, monthOfYear, dayOfMonth ->
                    calendar.set(year, monthOfYear, dayOfMonth)

                    val date = calendar.timeInMillis

                    updateFilterDateButton(button, date)

                    setFilterDate(button == btnDateFrom, date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show(fragmentManager!!, null)
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

    private class FuelingCursorLoader(context: Context, private val filter: DatabaseHelper.Filter) : CursorLoader(context) {

        override fun loadInBackground(): Cursor? {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "FuelingCursorLoader", "loadInBackground")
            }

            BroadcastReceiverLoading.send(context, true)

            try {
                return ContentResolverHelper.getAll(context, filter)
            } finally {
                BroadcastReceiverLoading.send(context, false)
            }
        }
    }

    private class FuelingTotalCursorLoader(context: Context) : CursorLoader(context) {

        override fun loadInBackground(): Cursor? {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "FuelingTotalCursorLoader", "loadInBackground")
            }

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