package ru.p3tr0vich.fuel.fragments

import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.activities.ActivityDialog
import ru.p3tr0vich.fuel.adapters.TextWatcherAdapter
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.helpers.SMSTextPatternHelper
import ru.p3tr0vich.fuel.utils.Utils
import ru.p3tr0vich.fuel.utils.UtilsFormat

class FragmentActivityDialogSMSTextPattern : Fragment(), ActivityDialog.ActivityDialogFragment {

    private var editSMSText: EditText? = null
    private var editSMSTextPattern: EditText? = null
    private var textResult: TextView? = null

    override val title: String
        get() = getString(R.string.pref_sms_title)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        with(inflater.inflate(R.layout.fragment_activity_dialog_sms_text_pattern, container, false)) {

            editSMSText = findViewById(R.id.edit_sms_text)
            editSMSTextPattern = findViewById(R.id.edit_sms_text_pattern)
            textResult = findViewById(R.id.text_check_result)

            if (savedInstanceState == null) {
                val preferencesHelper = PreferencesHelper.getInstance(context!!)

                editSMSText!!.setText(preferencesHelper.smsText)
                editSMSTextPattern!!.setText(preferencesHelper.smsTextPattern)

                updateResult()
            }

            val textWatcher = EditTextWatcherAdapter()

            editSMSText!!.addTextChangedListener(textWatcher)
            editSMSTextPattern!!.addTextChangedListener(textWatcher)

            return this
        }
    }

    private fun updateResult() {
        val pattern = editSMSTextPattern!!.text.toString()
        val message = editSMSText!!.text.toString()

        val patternEmpty = TextUtils.isEmpty(pattern)
        val messageEmpty = TextUtils.isEmpty(message)

        @ColorInt var color = Utils.getColor(R.color.error_text)

        var result: String

        if (patternEmpty && messageEmpty)
            result = getString(R.string.text_sms_text_result_all_empty)
        else if (patternEmpty)
            result = getString(R.string.text_sms_text_result_pattern_empty)
        else
            try {
                val value = SMSTextPatternHelper.getValue(pattern, message)

                when {
                    messageEmpty -> result = getString(R.string.text_sms_text_result_message_empty)
                    value != null -> {
                        result = getString(R.string.text_sms_text_result_ok, UtilsFormat.floatToString(value))

                        color = Utils.getColor(R.color.text_primary)
                    }
                    else -> result = getString(R.string.text_sms_text_result_no_value)
                }
            } catch (e: Exception) {
                result = getString(R.string.text_sms_text_result_wrong_pattern)
            }

        textResult!!.setTextColor(color)
        textResult!!.text = result
    }

    private inner class EditTextWatcherAdapter : TextWatcherAdapter() {
        override fun afterTextChanged(s: Editable) {
            updateResult()
        }
    }

    override fun onSaveClicked(): Boolean {
        with(PreferencesHelper.getInstance(context!!)) {
            smsText = editSMSText!!.text.toString()
            smsTextPattern = editSMSTextPattern!!.text.toString()
        }

        return true
    }

    companion object {

        fun newInstance(): Fragment {
            return FragmentActivityDialogSMSTextPattern()
        }
    }
}