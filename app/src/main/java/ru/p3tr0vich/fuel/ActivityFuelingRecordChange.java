package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class ActivityFuelingRecordChange extends AppCompatActivity {

    public static final String INTENT_EXTRA_ACTION = "EXTRA_ACTION";

    public static void start(@NonNull Activity parent,
                             int requestCode,
                             @Const.RecordAction int recordAction,
                             @Nullable FuelingRecord fuelingRecord) {
        Intent intent = new Intent(parent, ActivityFuelingRecordChange.class);

        intent.putExtra(INTENT_EXTRA_ACTION, recordAction);
        if (recordAction == Const.RECORD_ACTION_UPDATE && fuelingRecord != null)
            fuelingRecord.toIntent(intent);

        parent.startActivityForResult(intent, requestCode);
    }

    @Const.RecordAction
    public static int getAction(Intent data) {
        return Utils.intToRecordAction(data.getIntExtra(INTENT_EXTRA_ACTION, -1));
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
    }
}