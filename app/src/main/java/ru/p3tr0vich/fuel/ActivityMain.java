package ru.p3tr0vich.fuel;
// TODO: change color of selected item in spinners and popup menu

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class ActivityMain extends AppCompatActivity implements
        FragmentFueling.FilterChangeListener,
        FragmentFueling.RecordChangeListener {

    private static final String ACTION_LOADING = "ru.p3tr0vich.fuel.ACTION_LOADING";
    private static final String EXTRA_LOADER_ID = "ru.p3tr0vich.fuel.EXTRA_LOADER_ID";
    private static final String EXTRA_LOADING = "ru.p3tr0vich.fuel.EXTRA_LOADING";

    private Spinner mToolbarSpinner;

    private BroadcastReceiver mLoadingStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbarMain);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mToolbarSpinner = new AppCompatSpinner(getSupportActionBar().getThemedContext());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filter_years, R.layout.toolbar_spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        mToolbarSpinner.setAdapter(adapter);

        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics()));

        mToolbarSpinner.setDropDownVerticalOffset(-px);

        toolbarMain.addView(mToolbarSpinner);

        mToolbarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("XXX", "ActivityMain -- onItemSelected");

                Const.FilterMode filterMode;
                switch (position) {
                    case 0:
                        filterMode = Const.FilterMode.CURRENT_YEAR;
                        break;
                    default:
                        filterMode = Const.FilterMode.ALL;
                }
                FragmentFueling fragmentFueling = (FragmentFueling) getFragmentManager().findFragmentById(R.id.fragmentFueling);
                fragmentFueling.setFilterMode(filterMode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Functions.sApplicationContext = this.getApplicationContext();

        mLoadingStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int loaderId = intent.getIntExtra(EXTRA_LOADER_ID, 0);
                boolean visible = intent.getBooleanExtra(EXTRA_LOADING, false);

                FragmentFueling fragmentFueling = (FragmentFueling) getFragmentManager().findFragmentById(R.id.fragmentFueling);
                fragmentFueling.setProgressBarVisible(loaderId, visible);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mLoadingStatusReceiver,
                new IntentFilter(ACTION_LOADING));

        Log.d("XXX", "ActivityMain -- onCreate");
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLoadingStatusReceiver);
        super.onDestroy();
    }

    public static Intent getLoadingBroadcast(int loaderId, boolean startLoading) {
        Intent intent = new Intent(ACTION_LOADING);
        intent.putExtra(EXTRA_LOADER_ID, loaderId);
        intent.putExtra(EXTRA_LOADING, startLoading);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_calc:
                ActivityCalc.start(this);
                return true;
            case R.id.action_backup:
                ActivityBackup.start(this);
                return true;
            case R.id.action_settings:
                ActivityPreference.start(this);
                return true;
            case R.id.action_about:
                FragmentDialogAbout.show(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRecordChange(Const.RecordAction recordAction, FuelingRecord fuelingRecord) {
        if (recordAction != Const.RecordAction.DELETE)
            // ADD, UPDATE
            ActivityFuelingRecordChange.start(this, recordAction, fuelingRecord);
        else
            FragmentDialogDeleteRecord.show(this, fuelingRecord);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        FuelingRecord fuelingRecord;
        FragmentFueling fragmentFueling = (FragmentFueling) getFragmentManager().findFragmentById(R.id.fragmentFueling);

        switch (requestCode) {
            case ActivityFuelingRecordChange.REQUEST_CODE:
                Const.RecordAction action = ActivityFuelingRecordChange.getAction(data);

                fuelingRecord = ActivityFuelingRecordChange.getFuelingRecord(data);

                switch (action) {
                    case ADD:
                        fragmentFueling.addRecord(fuelingRecord);
                        break;
                    case UPDATE:
                        fragmentFueling.updateRecord(fuelingRecord);
                        break;
                }
                break;
            case FragmentDialogDeleteRecord.REQUEST_CODE:
                fuelingRecord = FragmentDialogDeleteRecord.getFuelingRecord(data);

                fragmentFueling.deleteRecord(fuelingRecord);
                break;
            case ActivityBackup.REQUEST_CODE:
                fragmentFueling.updateAfterChange();
                break;
        }
    }

    @Override
    public void onFilterChange(Const.FilterMode filterMode) {
        Log.d("XXX", "ActivityMain -- onFilterChange");

        int position;
        switch (filterMode) {
            case CURRENT_YEAR:
                position = 0;
                break;
            default:
                position = 1;
        }
        if (position != mToolbarSpinner.getSelectedItemPosition())
            mToolbarSpinner.setSelection(position);
    }
}
