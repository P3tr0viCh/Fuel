package ru.p3tr0vich.fuel;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class ActivityFuelingRecordChange extends AppCompatActivity {

    public static final String INTENT_EXTRA_ACTION = "EXTRA_ACTION";
    public static final String INTENT_EXTRA_DATA = "EXTRA_RECORD";
    public static final int REQUEST_CODE = 2309;

    public static void start(Activity parent, Const.RecordAction recordAction, FuelingRecord fuelingRecord) {
        Intent intent = new Intent(parent, ActivityFuelingRecordChange.class);

        intent.putExtra(INTENT_EXTRA_ACTION, recordAction.ordinal());
        if (recordAction == Const.RecordAction.UPDATE)
            intent.putExtra(INTENT_EXTRA_DATA, fuelingRecord);

        parent.startActivityForResult(intent, REQUEST_CODE);
    }

    public static Const.RecordAction getAction(Intent data) {
        return Functions.intToRecordAction(data.getIntExtra(INTENT_EXTRA_ACTION, -1));
    }

    public static FuelingRecord getFuelingRecord(Intent data) {
        return data.getParcelableExtra(INTENT_EXTRA_DATA);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fueling_record_change);

        Toolbar toolbarRecord = (Toolbar) findViewById(R.id.toolbarRecord);
        setSupportActionBar(toolbarRecord);
        toolbarRecord.setNavigationIcon(R.drawable.abc_ic_clear_mtrl_alpha);
        toolbarRecord.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
    }
}
