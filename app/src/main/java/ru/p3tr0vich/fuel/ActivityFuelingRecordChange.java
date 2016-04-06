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

public class ActivityFuelingRecordChange extends AppCompatActivity {

    @NonNull
    public static Intent getIntent(@NonNull Context context, @Nullable FuelingRecord fuelingRecord) {
        Intent intent = new Intent(context, ActivityFuelingRecordChange.class);

        if (fuelingRecord != null)
            fuelingRecord.toIntent(intent);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    protected void onNewIntent(Intent intent) {
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
        if (getSupportFragmentManager().getBackStackEntryCount() > 1)
            getSupportFragmentManager().popBackStack();
        else
            super.finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}