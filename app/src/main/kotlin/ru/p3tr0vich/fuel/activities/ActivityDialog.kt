package ru.p3tr0vich.fuel.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import androidx.fragment.app.FragmentTransaction
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.fragments.FragmentActivityDialogSMSTextPattern

class ActivityDialog : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog)

        val toolbarDialog = findViewById<Toolbar>(R.id.toolbar_dialog)

        setSupportActionBar(toolbarDialog)

        toolbarDialog?.setNavigationIcon(R.drawable.ic_close)

        toolbarDialog?.setNavigationOnClickListener {
            setResult(Activity.RESULT_CANCELED, null)
            finish()
        }

        supportFragmentManager.addFragmentOnAttachListener(object : FragmentOnAttachListener {
            override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
                title = (fragment as ActivityDialogFragment).title
            }
        })

        if (savedInstanceState == null) {
            val fragment: Fragment

            when (intent.getIntExtra(DIALOG, -1)) {
                DIALOG_SMS_TEXT_PATTERN -> fragment =
                    FragmentActivityDialogSMSTextPattern.newInstance()

                else -> throw IllegalArgumentException("Unknown dialog type")
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment, null)
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_action_save, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.content_frame) as ActivityDialogFragment?

        if (fragment != null && fragment.onSaveClicked()) {
            setResult(Activity.RESULT_OK)
            finish()
        }

        return true
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(DIALOG_SMS_TEXT_PATTERN)
    annotation class Dialog

    interface ActivityDialogFragment {
        val title: String

        fun onSaveClicked(): Boolean
    }

    companion object {
        private const val DIALOG = "DIALOG"

        const val DIALOG_SMS_TEXT_PATTERN = 0

        @JvmStatic
        fun start(parent: Activity, @Dialog dialog: Int) {
            parent.startActivity(
                Intent(parent, ActivityDialog::class.java).putExtra(
                    DIALOG,
                    dialog
                )
            )
        }
    }
}