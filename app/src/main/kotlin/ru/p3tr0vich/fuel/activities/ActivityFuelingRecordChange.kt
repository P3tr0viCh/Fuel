package ru.p3tr0vich.fuel.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import ru.p3tr0vich.fuel.FragmentFuelingRecordChange
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.utils.UtilsLog

class ActivityFuelingRecordChange : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        UtilsLog.d(TAG, "onCreate")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fueling_record_change)

        val toolbarRecord = findViewById<Toolbar>(R.id.toolbar_record)
        setSupportActionBar(toolbarRecord)

        toolbarRecord?.setNavigationIcon(R.drawable.ic_close)

        toolbarRecord?.setNavigationOnClickListener {
            setResult(Activity.RESULT_CANCELED, null)
            finish()
        }

        if (savedInstanceState == null) {
            addFragment(intent)
        }
    }

    override fun onDestroy() {
        UtilsLog.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        UtilsLog.d(TAG, "onNewIntent")

        if (intent.hasExtra(EXTRA_FINISH)) {
            UtilsLog.d(TAG, "onNewIntent", "hasExtra")

            setIntent(intent)

            finish()

            return
        }

        super.onNewIntent(intent)

        addFragment(intent)
    }

    private fun addFragment(intent: Intent) {
        val fuelingRecord = if (intent.hasExtra(FuelingRecord.NAME)) FuelingRecord(intent) else null

        supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame,
                        FragmentFuelingRecordChange.newInstance(fuelingRecord),
                        FragmentFuelingRecordChange.TAG)
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .addToBackStack(null)
                .commit()
    }

    override fun finish() {
        UtilsLog.d(TAG, "finish")

        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            if (intent.hasExtra(EXTRA_FINISH)) {
                super.finish()
            } else {
                startActivity(ActivityFuelingRecordChange.getIntentForStart(this, null)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        .putExtra(EXTRA_FINISH, true))
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }

    companion object {
        private const val TAG = "ActivityFuelingRecordChange"

        private const val EXTRA_FINISH = "EXTRA_FINISH"

        @JvmStatic
        fun getIntentForStart(context: Context, fuelingRecord: FuelingRecord?): Intent {
            val intent = Intent(context, ActivityFuelingRecordChange::class.java)

            fuelingRecord?.toIntent(intent)

            return intent
        }
    }
}