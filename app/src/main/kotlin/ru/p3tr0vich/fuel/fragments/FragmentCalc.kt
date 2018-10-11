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

    private var editDistance: EditText? = null
    private var editCost: EditText? = null
    private var editVolume: EditText? = null
    private var editPrice: EditText? = null
    private var editCons: EditText? = null

    private var spinnerCons: Spinner? = null
    private var spinnerSeason: Spinner? = null

    private var layoutPriceEmpty: LinearLayout? = null
    private var layoutConsEmpty: LinearLayout? = null

    private var calculating: Boolean = false

    private var arrCons = arrayOf(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f))

    private var onCalcDistanceButtonClickListener: OnCalcDistanceButtonClickListener? = null

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
            onCalcDistanceButtonClickListener = context as OnCalcDistanceButtonClickListener?
        } catch (e: ClassCastException) {
            throw ImplementException(context!!, OnCalcDistanceButtonClickListener::class.java)
        }

    }

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        calculating = true

        val view = inflater.inflate(R.layout.fragment_calc, container, false)

        editDistance = view.findViewById(R.id.edit_distance)
        editCost = view.findViewById(R.id.edit_cost)
        editVolume = view.findViewById(R.id.edit_volume)
        editPrice = view.findViewById(R.id.edit_price)
        editCons = view.findViewById(R.id.edit_cons)

        spinnerCons = view.findViewById(R.id.spinner_cons)
        spinnerSeason = view.findViewById(R.id.spinner_season)

        layoutPriceEmpty = view.findViewById(R.id.layout_price_empty)
        layoutConsEmpty = view.findViewById(R.id.layout_cons_empty)

        val adapterCons = ArrayAdapter.createFromResource(activity!!,
                R.array.spinner_consumption, R.layout.spinner_item)
        adapterCons.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerCons!!.adapter = adapterCons

        val adapterSeason = ArrayAdapter.createFromResource(activity!!,
                R.array.spinner_season, R.layout.spinner_item)
        adapterSeason.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerSeason!!.adapter = adapterSeason

        view.findViewById<View>(R.id.btn_map).setOnClickListener { onCalcDistanceButtonClickListener?.onCalcDistanceButtonClick() }

        view.findViewById<View>(R.id.text_distance).setOnClickListener(this)
        view.findViewById<View>(R.id.text_cost).setOnClickListener(this)
        view.findViewById<View>(R.id.text_volume).setOnClickListener(this)
        view.findViewById<View>(R.id.text_price).setOnClickListener(this)
        view.findViewById<View>(R.id.text_cons).setOnClickListener(this)

        loadPrefs()

        selectConsFromSpinners()
        checkTextOnEmpty(editPrice!!)

        spinnerCons!!.onItemSelectedListener = this
        spinnerSeason!!.onItemSelectedListener = this

        editDistance!!.addTextChangedListener(EditTextWatcherAdapter(editDistance!!))
        editCost!!.addTextChangedListener(EditTextWatcherAdapter(editCost!!))
        editVolume!!.addTextChangedListener(EditTextWatcherAdapter(editVolume!!))

        editPrice!!.addTextChangedListener(EditTextWatcherAdapter(editPrice!!))
        editCons!!.addTextChangedListener(EditTextWatcherAdapter(editCons!!))

        return view
    }

    /**
     * Изменяет поле Расстояние.
     *
     * @param distance расстояние в метрах.
     */
    fun setDistance(distance: Int) {
        UtilsFormat.floatToEditText(editDistance!!, (distance / 1000.0).toFloat(), false)
    }

    private fun checkTextOnEmpty(editText: EditText) {
        val linearLayout = when (editText.id) {
            R.id.edit_price -> layoutPriceEmpty
            R.id.edit_cons -> layoutConsEmpty
            else -> return
        }

        linearLayout!!.visibility = if (TextUtils.isEmpty(editText.text)) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()

        calculating = false

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "onResume")
        }

        doCalculate(CALC_ACTION_COST)
    }

    override fun onDestroy() {
        savePrefs()

        super.onDestroy()
    }

    override fun onClick(v: View) {
        val edit = when (v.id) {
            R.id.text_price -> editPrice
            R.id.text_cons -> editCons
            R.id.text_distance -> editDistance
            R.id.text_cost -> editCost
            R.id.text_volume -> editVolume
            else -> return
        }

        Utils.showKeyboard(edit)
    }

    private fun loadPrefs() {
        editDistance!!.setText(preferencesHelper.calcDistance)
        editCost!!.setText(preferencesHelper.calcCost)
        editVolume!!.setText(preferencesHelper.calcVolume)

        editPrice!!.setText(preferencesHelper.priceAsString)

        arrCons = preferencesHelper.calcCons

        spinnerCons!!.setSelection(preferencesHelper.calcSelectedCons)
        spinnerSeason!!.setSelection(preferencesHelper.calcSelectedSeason)
    }

    private fun savePrefs() {
        preferencesHelper.putCalc(
                editDistance!!.text.toString(),
                editCost!!.text.toString(),
                editVolume!!.text.toString(),
                spinnerCons!!.selectedItemPosition,
                spinnerSeason!!.selectedItemPosition)
    }

    private fun selectConsFromSpinners() {
        val cons = arrCons[spinnerSeason!!.selectedItemPosition][spinnerCons!!.selectedItemPosition]

        if (java.lang.Float.compare(cons, UtilsFormat.editTextToFloat(editCons!!)) == 0) {
            return
        }

        UtilsFormat.floatToEditText(editCons!!, cons, false)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        selectConsFromSpinners()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    private inner class EditTextWatcherAdapter(private val editText: EditText) : TextWatcherAdapter() {

        override fun afterTextChanged(s: Editable) {
            if (LOG_ENABLED) {
                UtilsLog.d(TAG, "afterTextChanged", "editText id == ${resources.getResourceEntryName(editText.id)}")
            }

            when (editText.id) {
                R.id.edit_price -> {
                    checkTextOnEmpty(editText)
                    doCalculate(CALC_ACTION_DISTANCE)
                }
                R.id.edit_cost -> doCalculate(CALC_ACTION_COST)
                R.id.edit_cons -> {
                    checkTextOnEmpty(editText)
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
        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "doCalculate", "calcAction == $calcAction")
        }

        if (calculating) {
            return
        }

        if (LOG_ENABLED) {
            UtilsLog.d(TAG, "doCalculate", "calc")
        }

        calculating = true

        try {
            // Цена за "литр".
            val price = UtilsFormat.editTextToFloat(editPrice!!)
            // Расход на 100 "км".
            val cons = UtilsFormat.editTextToFloat(editCons!!)


            if (price == 0f || cons == 0f) {
                return
            }

            var distance = UtilsFormat.editTextToFloat(editDistance!!)
            var cost = UtilsFormat.editTextToFloat(editCost!!)
            var volume = UtilsFormat.editTextToFloat(editVolume!!)

            // Пробег на одном "литре".
            val distancePerOneFuel = 100.0f / cons

            when (calcAction) {
                CALC_ACTION_DISTANCE -> {
                    volume = distance / distancePerOneFuel
                    cost = price * volume

                    UtilsFormat.floatToEditText(editVolume!!, volume, true)
                    UtilsFormat.floatToEditText(editCost!!, cost, true)
                }
                CALC_ACTION_COST -> {
                    volume = cost / price
                    distance = distancePerOneFuel * volume

                    UtilsFormat.floatToEditText(editVolume!!, volume, true)
                    UtilsFormat.floatToEditText(editDistance!!, distance, true)
                }
                CALC_ACTION_VOLUME -> {
                    cost = price * volume
                    distance = distancePerOneFuel * volume

                    UtilsFormat.floatToEditText(editCost!!, cost, true)
                    UtilsFormat.floatToEditText(editDistance!!, distance, true)
                }
            }
        } finally {
            calculating = false
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