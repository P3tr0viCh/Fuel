package ru.p3tr0vich.fuel;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.UtilsLog;

public class ActivityFuelingRecordChange extends AppCompatActivity {

    private static final String TAG = "ActivityFuelingRecordChange";

    private static final String EXTRA_FINISH = "EXTRA_FINISH";

    @NonNull
    public static Intent getIntentForStart(@NonNull Context context, @Nullable FuelingRecord fuelingRecord) {
        Intent intent = new Intent(context, ActivityFuelingRecordChange.class);

        if (fuelingRecord != null)
            fuelingRecord.toIntent(intent);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UtilsLog.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fueling_record_change);

        Toolbar toolbarRecord = (Toolbar) findViewById(R.id.toolbarRecord);
        setSupportActionBar(toolbarRecord);

        assert toolbarRecord != null;

        toolbarRecord.setNavigationIcon(R.drawable.ic_close);

        toolbarRecord.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        if (savedInstanceState == null)
            addFragment(getIntent());
    }

    @Override
    protected void onDestroy() {
        UtilsLog.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        UtilsLog.d(TAG, "onNewIntent");

        if (intent.hasExtra(EXTRA_FINISH)) {
            UtilsLog.d(TAG, "onNewIntent", "hasExtra");

            setIntent(intent);

            finish();

            return;
        }

        super.onNewIntent(intent);

        addFragment(intent);
    }

    private void addFragment(@NonNull Intent intent) {
        FuelingRecord fuelingRecord =
                intent.hasExtra(FuelingRecord.NAME) ? new FuelingRecord(intent) : null;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentFrame,
                        FragmentFuelingRecordChange.getInstance(fuelingRecord),
                        FragmentFuelingRecordChange.TAG)
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void finish() {
        UtilsLog.d(TAG, "finish");

        if (getSupportFragmentManager().getBackStackEntryCount() > 1)
            getSupportFragmentManager().popBackStack();
        else {
            if (getIntent().hasExtra(EXTRA_FINISH))
                super.finish();
            else
                startActivity(ActivityFuelingRecordChange.getIntentForStart(this, null)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        .putExtra(EXTRA_FINISH, true));
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}