package ru.p3tr0vich.fuel.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.widget.TextView
import ru.p3tr0vich.fuel.R

class FragmentDialogMessage : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(TITLE)

        val builder = AlertDialog.Builder(activity!!)

        if (!TextUtils.isEmpty(title)) {
            @SuppressLint("InflateParams")
            val customTitle = activity!!.layoutInflater
                    .inflate(R.layout.apptheme_dialog_title, null, false) as TextView

            customTitle.text = title

            builder.setCustomTitle(customTitle)
        }

        builder.setMessage(arguments?.getString(MESSAGE))

        builder.setPositiveButton(R.string.dialog_btn_ok) { _, _ -> dismiss() }

        return builder.setCancelable(true).create()
    }

    companion object {

        private const val TAG = "FragmentDialogMessage"

        private const val TITLE = "title"
        private const val MESSAGE = "message"

        private fun newInstance(title: String?, message: String): FragmentDialogMessage {
            val args = Bundle()
            args.putString(TITLE, title)
            args.putString(MESSAGE, message)

            val dialogMessage = FragmentDialogMessage()
            dialogMessage.arguments = args

            return dialogMessage
        }

        fun show(parent: FragmentActivity, title: String?, message: String) {
            newInstance(title, message).show(parent.supportFragmentManager, TAG)
        }

        fun show(parent: FragmentActivity, @StringRes titleId: Int?, @StringRes messageId: Int) {
            show(parent, if (titleId == null) null else parent.getString(titleId), parent.getString(messageId))
        }
    }
}