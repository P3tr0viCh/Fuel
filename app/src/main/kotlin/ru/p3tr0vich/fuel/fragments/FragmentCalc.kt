package ru.p3tr0vich.fuel.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.annotation.IntDef
import android.text.Editable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import ru.p3tr0vich.fuel.ImplementException
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.adapters.TextWatcherAdapter
import ru.p3tr0vich.fuel.factories.FragmentFactory
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsFormat
import ru.p3tr0vich.fuel.utils.UtilsLog

class FragmentCalc : FragmentBase(FragmentFactory.Ids.CALC), AdapterView.OnItemSelectedListener, View.OnClickListener {

    private var mEditDistance: EditText? = null
    private var mEditCost: EditText? = null
    private var mEditVolume: EditText? = null
    private var mEditPrice: EditText? = null
    private var mEditCons: EditText? = null

    private var mSpinnerCons: Spinner? = null
    private var mSpinnerSeason: Spinner? = null

    private var mLayoutPriceEmpty: LinearLayout? = null
    private var mLayoutConsEmpty: LinearLayout? = null

    private var mCalculating: Boolean = false

    private var arrCons = arrayOf(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f))

    private var mOnCalcDistanceButtonClickListener: OnCalcDistanceButtonClickListener? = null

    override val titleId: Int
        get() = R.string.title_calc

    /**
     * Изменённая единица, на основе которой вычисляются другие.
     * Например, при изменении расстояния (CALC_ACTION_DISTANCE),
     * вычисляются стоимость необходимого топлива и его объём.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(CALC_ACTION_DISTANCE, CALC_ACTION_COST, CALC_ACTION_VOLUME)
    private annotation class CalcAction

    interface OnCalcDistanceButtonClickListener {
        fun onCalcDistanceButtonClick()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mOnCalcDistanceButtonClickListener = context as OnCalcDistanceButtonClickListener?
        } catch (e: ClassCastException) {
            throw ImplementException(context!!, OnCalcDistanceButtonClickListener::class.java)
        }

    }

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mCalculating = true

        val view = inflater.inflate(R.layout.fragment_calc, container, false)

        mEditDistance = view.findViewById(R.id.edit_distance)
        mEditCost = view.findViewById(R.id.edit_cost)
        mEditVolume = view.findViewById(R.id.edit_volume)
        mEditPrice = view.findViewById(R.id.edit_price)
        mEditCons = view.findViewById(R.id.edit_cons)

        mSpinnerCons = view.findViewById(R.id.spinner_cons)
        mSpinnerSeason = view.findViewById(R.id.spinner_season)

        mLayoutPriceEmpty = view.findViewById(R.id.layout_price_empty)
        mLayoutConsEmpty = view.findViewById(R.id.layout_cons_empty)

        val activity = activity!!

        val adapterCons = ArrayAdapter.createFromResource(activity,
                R.array.spinner_consumption, R.layout.spinner_item)
        adapterCons.setDropDownViewResource(R.layout.spinner_dropdown_item)
        mSpinnerCons!!.adapter = adapterCons

        val adapterSeason = ArrayAdapter.createFromResource(activity,
                R.array.spinner_season, R.layout.spinner_item)
        adapterSeason.setDropDownViewResource(R.layout.spinner_dropdown_item)
        mSpinnerSeason!!.adapter = adapterSeason

        view.findViewById<View>(R.id.btn_map).setOnClickListener { mOnCalcDistanceButtonClickListener!!.onCalcDistanceButtonClick() }

        view.findViewById<View>(R.id.text_distance).setOnClickListener(this)
        view.findViewById<View>(R.id.text_cost).setOnClickListener(this)
        view.findViewById<View>(R.id.text_volume).setOnClickListener(this)
        view.findViewById<View>(R.id.text_price).setOnClickListener(this)
        view.findViewById<View>(R.id.text_cons).setOnClickListener(this)

        loadPrefs()

        selectConsFromSpinners()
        checkTextOnEmpty(mEditPrice!!)

        mSpinnerCons!!.onItemSelectedListener = this
        mSpinnerSeason!!.onItemSelectedListener = this

        mEditDistance!!.addTextChangedListener(EditTextWatcherAdapter(mEditDistance!!))
        mEditCost!!.addTextChangedListener(EditTextWatcherAdapter(mEditCost!!))
        mEditVolume!!.addTextChangedListener(EditTextWatcherAdapter(mEditVolume!!))

        mEditPrice!!.addTextChangedListener(EditTextWatcherAdapter(mEditPrice!!))
        mEditCons!!.addTextChangedListener(EditTextWatcherAdapter(mEditCons!!))

        return view
    }

    /**
     * Изменяет поле Расстояние.
     *
     * @param distance расстояние в метрах.
     */
    fun setDistance(distance: Int) {
        UtilsFormat.floatToEditText(mEditDistance!!, (distance / 1000.0).toFloat(), false)
    }

    private fun checkTextOnEmpty(editText: EditText) {
        val linearLayout: LinearLayout?
        when (editText.id) {
            R.id.edit_price -> linearLayout = mLayoutPriceEmpty
            R.id.edit_cons -> linearLayout = mLayoutConsEmpty
            else -> return
        }
        linearLayout!!.visibility = if (TextUtils.isEmpty(editText.text)) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        mCalculating = false

        if (LOG_ENABLED) UtilsLog.d(TAG, "onResume")

        doCalculate(CALC_ACTION_COST)
    }

    override fun onDestroy() {
        savePrefs()
        super.onDestroy()
    }

    override fun onClick(v: View) {
        val edit: EditText?
        when (v.id) {
            R.id.text_price -> edit = mEditPrice
            R.id.text_cons -> edit = mEditCons
            R.id.text_distance -> edit = mEditDistance
            R.id.text_cost -> edit = mEditCost
            R.id.text_volume -> edit = mEditVolume
            else -> return
        }
        Utils.showKeyboard(edit!!)
    }

    private fun loadPrefs() {
        mEditDistance!!.setText(preferencesHelper.calcDistance)
        mEditCost!!.setText(preferencesHelper.calcCost)
        mEditVolume!!.setText(preferencesHelper.calcVolume)

        mEditPrice!!.setText(preferencesHelper.priceAsString)

        arrCons = preferencesHelper.calcCons

        mSpinnerCons!!.setSelection(preferencesHelper.calcSelectedCons)
        mSpinnerSeason!!.setSelection(preferencesHelper.calcSelectedSeason)
    }

    private fun savePrefs() {
        preferencesHelper.putCalc(
                mEditDistance!!.text.toString(),
                mEditCost!!.text.toString(),
                mEditVolume!!.text.toString(),
                mSpinnerCons!!.selectedItemPosition,
                mSpinnerSeason!!.selectedItemPosition)
    }

    private fun selectConsFromSpinners() {
        val cons = arrCons[mSpinnerSeason!!.selectedItemPosition][mSpinnerCons!!.selectedItemPosition]

        if (java.lang.Float.compare(cons, UtilsFormat.editTextToFloat(mEditCons!!)) == 0) return

        UtilsFormat.floatToEditText(mEditCons!!, cons, false)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        selectConsFromSpinners()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    private inner class EditTextWatcherAdapter internal constructor(private val mEditText: EditText) : TextWatcherAdapter() {

        override fun afterTextChanged(s: Editable) {
            if (LOG_ENABLED)
                UtilsLog.d(TAG, "afterTextChanged",
                        "mEditText id == " + resources.getResourceEntryName(mEditText.id))

            when (mEditText.id) {
                R.id.edit_price -> {
                    checkTextOnEmpty(mEditText)
                    doCalculate(CALC_ACTION_DISTANCE)
                }
                R.id.edit_cost -> doCalculate(CALC_ACTION_COST)
                R.id.edit_cons -> {
                    checkTextOnEmpty(mEditText)
                    doCalculate(CALC_ACTION_DISTANCE)
                }
                R.id.edit_distance -> doCalculate(CALC_ACTION_DISTANCE)
                R.id.edit_volume -> doCalculate(CALC_ACTION_VOLUME)
            }
        }
    }

    /**
     * Вычисляет расстояние, стоимость и объём необходимого топлива
     * при указанных стоимости единицы объёма (литра и т.п.) и расходе топлива на 100 единиц расстояния (км и т.п.).
     *
     * @param calcAction изменённая единица, на основе которой считаются две другие.
     * @see CalcAction
     */
    private fun doCalculate(@CalcAction calcAction: Int) {
        if (LOG_ENABLED) UtilsLog.d(TAG, "doCalculate", "calcAction == $calcAction")

        if (mCalculating) return

        if (LOG_ENABLED) UtilsLog.d(TAG, "doCalculate", "calc")

        mCalculating = true

        try {
            val distancePerOneFuel: Float
            val price: Float
            val cons: Float
            var distance: Float
            var cost: Float
            var volume: Float

            // Цена за "литр".
            price = UtilsFormat.editTextToFloat(mEditPrice!!)
            // Расход на 100 "км".
            cons = UtilsFormat.editTextToFloat(mEditCons!!)

            if (price == 0f || cons == 0f) return

            distance = UtilsFormat.editTextToFloat(mEditDistance!!)
            cost = UtilsFormat.editTextToFloat(mEditCost!!)
            volume = UtilsFormat.editTextToFloat(mEditVolume!!)

            // Пробег на одном "литре".
            distancePerOneFuel = 100.0f / cons

            when (calcAction) {
                CALC_ACTION_DISTANCE -> {
                    volume = distance / distancePerOneFuel
                    cost = price * volume
                    UtilsFormat.floatToEditText(mEditVolume!!, volume, true)
                    UtilsFormat.floatToEditText(mEditCost!!, cost, true)
                }
                CALC_ACTION_COST -> {
                    volume = cost / price
                    distance = distancePerOneFuel * volume
                    UtilsFormat.floatToEditText(mEditVolume!!, volume, true)
                    UtilsFormat.floatToEditText(mEditDistance!!, distance, true)
                }
                CALC_ACTION_VOLUME -> {
                    cost = price * volume
                    distance = distancePerOneFuel * volume
                    UtilsFormat.floatToEditText(mEditCost!!, cost, true)
                    UtilsFormat.floatToEditText(mEditDistance!!, distance, true)
                }
            }
        } finally {
            mCalculating = false
        }
    }

    companion object {

        private const val TAG = "FragmentCalc"

        private var LOG_ENABLED = false

        private const val CALC_ACTION_DISTANCE = 0
        private const val CALC_ACTION_COST = 1
        private const val CALC_ACTION_VOLUME = 2
    }
}