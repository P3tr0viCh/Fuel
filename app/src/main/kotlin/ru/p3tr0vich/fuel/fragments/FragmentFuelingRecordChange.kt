package ru.p3tr0vich.fuel.fragments

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.EditText
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.R.id.edit_cost
import ru.p3tr0vich.fuel.adapters.TextWatcherAdapter
import ru.p3tr0vich.fuel.helpers.ContentResolverHelper
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsDate
import ru.p3tr0vich.fuel.utils.UtilsFormat
import java.util.*

class FragmentFuelingRecordChange : Fragment(), View.OnClickListener {

    private var mFuelingRecord: FuelingRecord? = null

    private var mButtonDate: Button? = null
    private var mEditCost: EditText? = null
    private var mEditVolume: EditText? = null
    private var mEditTotal: EditText? = null

    private var mPrice: Float = 0.toFloat()

    private lateinit var preferencesHelper: PreferencesHelper

    /**
     * Шаг ожидания ввода.
     * В случае изменения старой записи или если цена меньше или равна нулю
     * вычисление отключено (`CALC_STEP_DISABLED`).
     * Иначе, при первом открытии фрагмента, шаг устанавливается на выбор
     * редактируемого и вычисляемого полей (`CALC_STEP_SELECT`).
     * При последующих пересозданиях фрагмента загружается предыдущее значение.
     */
    private var mCalcStep: Int = 0

    /**
     * Проверка способа изменения поля:
     * true, если поле изменяется в результате ручного ввода,
     * false, если изменение происходит в результате вызова функции setText.
     */
    private var mManualInput = true

    private var mCostTextWatcher: TextWatcher? = null
    private var mVolumeTextWatcher: TextWatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = context!!

        preferencesHelper = PreferencesHelper.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_fueling_record_change, container, false)

        setHasOptionsMenu(true)

        mButtonDate = view.findViewById(R.id.btn_date)
        mEditCost = view.findViewById(edit_cost)
        mEditVolume = view.findViewById(R.id.edit_volume)
        mEditTotal = view.findViewById(R.id.edit_total)

        val bundle = arguments

        if (bundle != null && bundle.containsKey(FuelingRecord.NAME)) {
            mFuelingRecord = FuelingRecord(bundle)
            mPrice = 0f
        } else {
            var cost = preferencesHelper!!.defaultCost
            var volume = preferencesHelper!!.defaultVolume
            mPrice = preferencesHelper!!.price

            if (mPrice != 0f) {
                if (cost == 0f && volume != 0f)
                    cost = volume * mPrice
                else if (volume == 0f && cost != 0f) volume = cost / mPrice
            }

            mFuelingRecord = FuelingRecord(cost, volume, preferencesHelper!!.lastTotal)
        }

        val activity = activity!!

        activity.setTitle(if (mFuelingRecord!!.id != 0L)
            R.string.dialog_caption_update
        else
            R.string.dialog_caption_add)

        if (savedInstanceState == null) {
            UtilsFormat.floatToEditText(mEditCost!!, mFuelingRecord!!.cost, false)
            UtilsFormat.floatToEditText(mEditVolume!!, mFuelingRecord!!.volume, false)
            UtilsFormat.floatToEditText(mEditTotal!!, mFuelingRecord!!.total, false)
        } else
            mFuelingRecord!!.dateTime = savedInstanceState.getLong(STATE_KEY_DATE)

        updateDate()

        mButtonDate!!.setOnClickListener(this)

        view.findViewById<View>(R.id.text_cost).setOnClickListener(this)
        view.findViewById<View>(R.id.text_volume).setOnClickListener(this)
        view.findViewById<View>(R.id.text_total).setOnClickListener(this)

        if (savedInstanceState == null)
            mCalcStep = if (mFuelingRecord!!.id == 0L && mPrice > 0) CALC_STEP_SELECT else CALC_STEP_DISABLED
        else
            mCalcStep = savedInstanceState.getInt(STATE_KEY_CALC_STEP, CALC_STEP_DISABLED)

        return view
    }

    override fun onStart() {
        super.onStart()

        if (mCalcStep != CALC_STEP_DISABLED) {
            if (mCostTextWatcher == null) {
                mCostTextWatcher = EditTextWatcherAdapter(mEditCost, mEditVolume,
                        CalcVolume(), CALC_STEP_COST)
                mEditCost!!.addTextChangedListener(mCostTextWatcher)
            }
            if (mVolumeTextWatcher == null) {
                mVolumeTextWatcher = EditTextWatcherAdapter(mEditVolume, mEditCost,
                        CalcCost(), CALC_STEP_VOLUME)
                mEditVolume!!.addTextChangedListener(mVolumeTextWatcher)
            }
        }
    }

    /**
     * @param edited     редактируемое поле.
     * @param calculated вычисляемое поле.
     * @param calculator метод вычисления.
     * @param thisStep   редактируемое поле (`CALC_STEP_COST или CALC_STEP_VOLUME`).
     */
    private inner class EditTextWatcherAdapter(
            private val edited: EditText?,
            private val calculated: EditText?,
            private val calculator: Calculator,
            private val thisStep: Int) : TextWatcherAdapter() {

        override fun afterTextChanged(s: Editable) {
            // Изначально шаг установлен на выбор.
            // После первого изменения в одном из полей, это поле назначается редактируемым,
            // а другое -- вычисляемым.

            if (mCalcStep == CALC_STEP_SELECT) {
                mCalcStep = thisStep
            }

            if (mCalcStep == thisStep) {
                // Если значение изменяется в редактируемом поле,
                // вычисляется значение для вычисляемого поля.

                val enteredValue = UtilsFormat.editTextToFloat(edited)

                val calculatedValue = calculator.calc(enteredValue, mPrice)

                mManualInput = false
                UtilsFormat.floatToEditText(calculated, calculatedValue, false)
                mManualInput = true
            } else {
                // Иначе, если было произведено ручное изменение в вычисляемом поле,
                // вычисление отключается.

                if (mManualInput) {
                    mCalcStep = CALC_STEP_DISABLED

                    mEditCost?.removeTextChangedListener(mCostTextWatcher)
                    mEditVolume?.removeTextChangedListener(mVolumeTextWatcher)
                }
            }
        }
    }

    /**
     * Метод вычисления.
     * См. [CalcCost], [CalcVolume]
     */
    private interface Calculator {
        fun calc(value: Float, price: Float): Float
    }

    /**
     * Вычисляет объём на основе стоимости.
     */
    private class CalcVolume : Calculator {
        override fun calc(value: Float, price: Float): Float {
            return if (value == 0f || price == 0f) 0f else value / price
        }
    }

    /**
     * Вычисляет стоимость на основе объёма.
     */
    private class CalcCost : Calculator {
        override fun calc(value: Float, price: Float): Float {
            return if (value == 0f || price == 0f) 0f else value * price
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong(STATE_KEY_DATE, mFuelingRecord!!.dateTime)
        outState.putInt(STATE_KEY_CALC_STEP, mCalcStep)
    }

    private fun updateDate() {
        mButtonDate!!.text = UtilsFormat.dateToString(mFuelingRecord!!.dateTime, true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_fueling_record, menu)
    }

    /**
     * Выполняет добавление новой или изменение уже существующей записи.
     *
     * @return Истина, если сохранение успешно.
     */
    private fun saveRecord(): Boolean {
        val activity = activity!!

        Utils.hideKeyboard(activity)

        mFuelingRecord!!.cost = UtilsFormat.editTextToFloat(mEditCost!!)
        mFuelingRecord!!.volume = UtilsFormat.editTextToFloat(mEditVolume!!)
        mFuelingRecord!!.total = UtilsFormat.editTextToFloat(mEditTotal!!)

        val context = context!!

        if (mFuelingRecord!!.id != 0L) {
            if (ContentResolverHelper.updateRecord(context, mFuelingRecord!!)) {
                return true
            } else {
                Utils.toast(R.string.message_error_update_record)

                return false
            }
        } else {
            if (ContentResolverHelper.insertRecord(context, mFuelingRecord!!)) {
                return true
            } else {
                Utils.toast(R.string.message_error_insert_record)

                return false
            }
        }
    }

    private fun onSaveClicked(): Boolean {
        if (saveRecord()) {
            preferencesHelper!!.putLastTotal(mFuelingRecord!!.total)

            val activity = activity!!

            activity.setResult(Activity.RESULT_OK)

            activity.finish()

            return true
        } else {
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_save -> return onSaveClicked()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        if (v === mButtonDate) {
            val calendar = UtilsDate.getCalendarInstance(mFuelingRecord!!.dateTime)

            val fragmentManager = fragmentManager!!

            DatePickerDialog.newInstance(
                    { view, year, monthOfYear, dayOfMonth ->
                        calendar.set(year, monthOfYear, dayOfMonth)

                        mFuelingRecord!!.dateTime = calendar.timeInMillis

                        updateDate()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show(fragmentManager, null)
        } else {
            val edit = when (v.id) {
                R.id.text_cost -> mEditCost
                R.id.text_volume -> mEditVolume
                R.id.text_total -> mEditTotal
                else -> return
            }

            Utils.showKeyboard(edit)
        }
    }

    companion object {

        val TAG = "FragmentFuelingRecordChange"

        private val STATE_KEY_DATE = "STATE_KEY_DATE"
        private val STATE_KEY_CALC_STEP = "STATE_KEY_CALC_STEP"

        fun newInstance(fuelingRecord: FuelingRecord?): Fragment {
            val fragment = FragmentFuelingRecordChange()

            if (fuelingRecord != null)
                fragment.arguments = fuelingRecord.toBundle()

            return fragment
        }

        /**
         * Выбор редактируемого и вычисляемого полей.
         */
        private val CALC_STEP_SELECT = 0
        /**
         * Редактируемое поле -- Стоимость, вычисляемое поле -- Объём.
         */
        private val CALC_STEP_COST = 1
        /**
         * Редактируемое поле -- Объём, вычисляемое -- Стоимость.
         */
        private val CALC_STEP_VOLUME = 2
        /**
         * Вычисление отключено.
         */
        private val CALC_STEP_DISABLED = 3
    }
}