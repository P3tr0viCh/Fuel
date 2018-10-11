package ru.p3tr0vich.fuel.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.activities.ActivityMain

class FragmentDialogQuestion : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)

        @StringRes var textId: Int?

        arguments?.getInt(TITLE)?.let {
            with(activity!!.layoutInflater
                    .inflate(R.layout.apptheme_dialog_title, null, false) as TextView) {

                this.setText(it)

                builder.setCustomTitle(this)
            }
        }

        builder.setMessage(arguments?.getInt(MESSAGE) ?: -1)

        //todo
        textId = arguments!!.getInt(POSITIVE_BUTTON_TEXT)
        if (textId == 0) textId = R.string.dialog_btn_ok

        builder.setPositiveButton(textId) { _, _ ->
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

        textId = arguments!!.getInt(NEGATIVE_BUTTON_TEXT)
        if (textId == 0) textId = R.string.dialog_btn_cancel

        builder.setNegativeButton(textId, null)

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

        fun show(parent: Fragment,
                 requestCode: Int,
                 @StringRes titleId: Int?,
                 @StringRes messageId: Int,
                 @StringRes positiveButtonTextId: Int?,
                 @StringRes negativeButtonTextId: Int?) {
            val dialogQuestion = newInstance(titleId, messageId,
                    positiveButtonTextId, negativeButtonTextId)

            dialogQuestion.setTargetFragment(parent, requestCode)

            val fragmentManager = parent.fragmentManager!!

            dialogQuestion.show(fragmentManager, TAG)
        }

        fun show(parent: AppCompatActivity,
                 requestCode: Int,
                 @StringRes titleId: Int?,
                 @StringRes messageId: Int,
                 @StringRes positiveButtonTextId: Int?,
                 @StringRes negativeButtonTextId: Int?) {
            val dialogQuestion = newInstance(titleId, messageId,
                    positiveButtonTextId, negativeButtonTextId)

            dialogQuestion.setTargetFragment(null, requestCode)
            dialogQuestion.show(parent.supportFragmentManager, TAG)
        }
    }
}