package ru.p3tr0vich.fuel.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.activities.ActivityMain

class FragmentDialogQuestion : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)

        arguments?.getInt(TITLE)?.let {
            val view = activity!!.layoutInflater
                    .inflate(R.layout.apptheme_dialog_title, null, false) as TextView

            view.setText(it)

            builder.setCustomTitle(view)
        }

        builder.setMessage(arguments?.getInt(MESSAGE) ?: -1)

        arguments?.getInt(POSITIVE_BUTTON_TEXT)?.let {
            builder.setPositiveButton(if (it == 0) R.string.dialog_btn_ok else it) { _, _ ->
                val fragment = targetFragment
                if (fragment != null) {
                    fragment.onActivityResult(targetRequestCode, Activity.RESULT_OK, null)
                } else {
                    if (activity is ActivityMain)
                    // TODO: use listener?
                        (activity as ActivityMain)
                                .onActivityResult(targetRequestCode, Activity.RESULT_OK, null)
                }
            }
        }

        arguments?.getInt(NEGATIVE_BUTTON_TEXT)?.let {
            builder.setNegativeButton(if (it == 0) R.string.dialog_btn_cancel else it, null)
        }

        return builder.setCancelable(true).create()
    }

    companion object {

        const val TAG = "FragmentDialogQuestion"

        private const val TITLE = "title"
        private const val MESSAGE = "message"
        private const val POSITIVE_BUTTON_TEXT = "positive_button_text"
        private const val NEGATIVE_BUTTON_TEXT = "negative_button_text"

        private fun newInstance(@StringRes titleId: Int?,
                                @StringRes messageId: Int,
                                @StringRes positiveButtonTextId: Int?,
                                @StringRes negativeButtonTextId: Int?): FragmentDialogQuestion {
            val args = Bundle()

            if (titleId != null) args.putInt(TITLE, titleId)
            args.putInt(MESSAGE, messageId)
            if (positiveButtonTextId != null) args.putInt(POSITIVE_BUTTON_TEXT, positiveButtonTextId)
            if (negativeButtonTextId != null) args.putInt(NEGATIVE_BUTTON_TEXT, negativeButtonTextId)

            val dialogQuestion = FragmentDialogQuestion()
            dialogQuestion.arguments = args

            return dialogQuestion
        }

        //todo: delete
        fun show(parent: Fragment,
                 requestCode: Int,
                 @StringRes titleId: Int?,
                 @StringRes messageId: Int,
                 @StringRes positiveButtonTextId: Int?,
                 @StringRes negativeButtonTextId: Int?) {
            val dialogQuestion = newInstance(titleId, messageId, positiveButtonTextId, negativeButtonTextId)

            dialogQuestion.setTargetFragment(parent, requestCode)

            dialogQuestion.show(parent.fragmentManager!!, TAG)
        }

        fun show(parent: AppCompatActivity,
                 requestCode: Int,
                 @StringRes titleId: Int?,
                 @StringRes messageId: Int,
                 @StringRes positiveButtonTextId: Int?,
                 @StringRes negativeButtonTextId: Int?) {
            val dialogQuestion = newInstance(titleId, messageId, positiveButtonTextId, negativeButtonTextId)

            dialogQuestion.setTargetFragment(null, requestCode)

            dialogQuestion.show(parent.supportFragmentManager, TAG)
        }
    }
}