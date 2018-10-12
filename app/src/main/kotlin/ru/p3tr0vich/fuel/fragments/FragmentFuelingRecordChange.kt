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

    private var fuelingRecord: FuelingRecord? = null

    private var buttonDate: Button? = null
    private var editCost: EditText? = null
    private var editVolume: EditText? = null
    private var editTotal: EditText? = null

    private var price = 0f

    private lateinit var preferencesHelper: PreferencesHelper

    /**
     * Шаг ожидания ввода.
     * В случае изменения старой записи или если цена меньше или равна нулю
     * вычисление отключено (`CALC_STEP_DISABLED`).
     * Иначе, при первом открытии фрагмента, шаг устанавливается на выбор
     * редактируемого и вычисляемого полей (`CALC_STEP_SELECT`).
     * При последующих пересозданиях фрагмента загружается предыдущее значение.
     */
    private var calcStep: Int = 0

    /**
     * Проверка способа изменения поля:
     * true, если поле изменяется в результате ручного ввода,
     * false, если изменение происходит в результате вызова функции setText.
     */
    private var manualInput = true

    private var costTextWatcher: TextWatcher? = null
    private var volumeTextWatcher: TextWatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesHelper = PreferencesHelper.getInstance(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_fueling_record_change, container, false)

        setHasOptionsMenu(true)

        buttonDate = view.findViewById(R.id.btn_date)
        editCost = view.findViewById(edit_cost)
        editVolume = view.findViewById(R.id.edit_volume)
        editTotal = view.findViewById(R.id.edit_total)

        if (arguments != null && arguments!!.containsKey(FuelingRecord.NAME)) {
            fuelingRecord = FuelingRecord(arguments!!)
            price = 0f
        } else {
            var cost = preferencesHelper.defaultCost
            var volume = preferencesHelper.defaultVolume
            price = preferencesHelper.price

            if (price != 0f) {
                if (cost == 0f && volume != 0f)
                    cost = volume * price
                else if (volume == 0f && cost != 0f) volume = cost / price
            }

            fuelingRecord = FuelingRecord(cost, volume, preferencesHelper.lastTotal)
        }

        activity!!.setTitle(if (fuelingRecord!!.id != 0L)
            R.string.dialog_caption_update
        else
            R.string.dialog_caption_add)

        if (savedInstanceState == null) {
            UtilsFormat.floatToEditText(editCost, fuelingRecord!!.cost, false)
            UtilsFormat.floatToEditText(editVolume, fuelingRecord!!.volume, false)
            UtilsFormat.floatToEditText(editTotal, fuelingRecord!!.total, false)
        } else {
            fuelingRecord!!.dateTime = savedInstanceState.getLong(STATE_KEY_DATE)
        }

        updateDate()

        buttonDate!!.setOnClickListener(this)

        view.findViewById<View>(R.id.text_cost).setOnClickListener(this)
        view.findViewById<View>(R.id.text_volume).setOnClickListener(this)
        view.findViewById<View>(R.id.text_total).setOnClickListener(this)

        calcStep = savedInstanceState?.getInt(STATE_KEY_CALC_STEP, CALC_STEP_DISABLED) ?:
                if (fuelingRecord!!.id == 0L && price > 0) CALC_STEP_SELECT else CALC_STEP_DISABLED

        return view
    }

    override fun onStart() {
        super.onStart()

        if (calcStep != CALC_STEP_DISABLED) {
            if (costTextWatcher == null) {
                costTextWatcher = EditTextWatcherAdapter(editCost, editVolume,
                        CalcVolume(), CALC_STEP_COST)
                editCost!!.addTextChangedListener(costTextWatcher)
            }

            if (volumeTextWatcher == null) {
                volumeTextWatcher = EditTextWatcherAdapter(editVolume, editCost,
                        CalcCost(), CALC_STEP_VOLUME)
                editVolume!!.addTextChangedListener(volumeTextWatcher)
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

            if (calcStep == CALC_STEP_SELECT) {
                calcStep = thisStep
            }

            if (calcStep == thisStep) {
                // Если значение изменяется в редактируемом поле,
                // вычисляется значение для вычисляемого поля.

                val enteredValue = UtilsFormat.editTextToFloat(edited)

                val calculatedValue = calculator.calc(enteredValue, price)

                manualInput = false
                UtilsFormat.floatToEditText(calculated, calculatedValue, false)
                manualInput = true
            } else {
                // Иначе, если было произведено ручное изменение в вычисляемом поле,
                // вычисление отключается.

                if (manualInput) {
                    calcStep = CALC_STEP_DISABLED

                    editCost?.removeTextChangedListener(costTextWatcher)
                    editVolume?.removeTextChangedListener(volumeTextWatcher)
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

        outState.putLong(STATE_KEY_DATE, fuelingRecord!!.dateTime)
        outState.putInt(STATE_KEY_CALC_STEP, calcStep)
    }

    private fun updateDate() {
        buttonDate!!.text = UtilsFormat.dateToString(fuelingRecord!!.dateTime, true)
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
        Utils.hideKeyboard(activity)

        fuelingRecord!!.cost = UtilsFormat.editTextToFloat(editCost)
        fuelingRecord!!.volume = UtilsFormat.editTextToFloat(editVolume)
        fuelingRecord!!.total = UtilsFormat.editTextToFloat(editTotal)

        if (fuelingRecord!!.id != 0L) {
            return if (ContentResolverHelper.updateRecord(context!!, fuelingRecord!!)) {
                true
            } else {
                Utils.toast(R.string.message_error_update_record)

                false
            }
        } else {
            return if (ContentResolverHelper.insertRecord(context!!, fuelingRecord)) {
                true
            } else {
                Utils.toast(R.string.message_error_insert_record)

                false
            }
        }
    }

    private fun onSaveClicked(): Boolean {
        return if (saveRecord()) {
            preferencesHelper.putLastTotal(fuelingRecord!!.total)

            activity!!.setResult(Activity.RESULT_OK)

            activity!!.finish()

            true
        } else {
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_save -> return onSaveClicked()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        if (v === buttonDate) {
            val calendar = UtilsDate.getCalendarInstance(fuelingRecord!!.dateTime)

            DatePickerDialog.newInstance(
                    { _, year, monthOfYear, dayOfMonth ->
                        calendar.set(year, monthOfYear, dayOfMonth)

                        fuelingRecord!!.dateTime = calendar.timeInMillis

                        updateDate()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show(fragmentManager, null)
        } else {
            val edit = when (v.id) {
                R.id.text_cost -> editCost
                R.id.text_volume -> editVolume
                R.id.text_total -> editTotal
                else -> return
            }

            Utils.showKeyboard(edit)
        }
    }

    companion object {

        const val TAG = "FragmentFuelingRecordChange"

        private const val STATE_KEY_DATE = "STATE_KEY_DATE"
        private const val STATE_KEY_CALC_STEP = "STATE_KEY_CALC_STEP"

        fun newInstance(fuelingRecord: FuelingRecord?): Fragment {
            val fragment = FragmentFuelingRecordChange()

            fragment.arguments = fuelingRecord?.toBundle()

            return fragment
        }

        /**
         * Выбор редактируемого и вычисляемого полей.
         */
        private const val CALC_STEP_SELECT = 0
        /**
         * Редактируемое поле -- Стоимость, вычисляемое поле -- Объём.
         */
        private const val CALC_STEP_COST = 1
        /**
         * Редактируемое поле -- Объём, вычисляемое -- Стоимость.
         */
        private const val CALC_STEP_VOLUME = 2
        /**
         * Вычисление отключено.
         */
        private const val CALC_STEP_DISABLED = 3
    }
}