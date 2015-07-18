package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

public class ActivityChartCost extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_FILTER_YEAR = "KEY_FILTER_YEAR";
    private static final String KEY_FILTER_YEARS = "KEY_FILTER_YEARS";

    private Spinner mToolbarSpinner;
    private ArrayAdapter<String> mSpinnerAdapter;

    public static void start(Activity parent) {
        parent.startActivity(new Intent(parent, ActivityChartCost.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_cost);

        Toolbar toolbarChartCost = (Toolbar) findViewById(R.id.toolbarChartCost);
        setSupportActionBar(toolbarChartCost);
        toolbarChartCost.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbarChartCost.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mSpinnerAdapter = new ArrayAdapter<>(this, R.layout.toolbar_spinner_item,
                new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.filter_years))));

        //noinspection ConstantConditions
        mToolbarSpinner = new AppCompatSpinner(getSupportActionBar().getThemedContext());

        Functions.addSpinnerInToolbar(getSupportActionBar(), toolbarChartCost,
                mToolbarSpinner, mSpinnerAdapter,
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Functions.LogD("ActivityChartCost -- onItemSelected");

                        String year;

                        switch (position) {
                            case 0:
                                year = String.valueOf(Functions.getCurrentYear());
                                break;
                            case 1:
                                year = String.valueOf(Functions.getCurrentYear() - 1);
                                break;
                            default:
                                year = (String) parent.getItemAtPosition(position);
                        }
                        ((FragmentChartCost) getFragmentManager()
                                .findFragmentById(R.id.fragmentChartCost)).setYear(year);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        if (savedInstanceState == null) {
            Functions.LogD("ActivityChartCost -- onCreate: savedInstanceState == null");

            getLoaderManager().initLoader(0, null, this);
        } else {
            Functions.LogD("ActivityChartCost -- onCreate: savedInstanceState != null");

            int[] years = savedInstanceState.getIntArray(KEY_FILTER_YEARS);

            assert years != null;

            for (int year : years) mSpinnerAdapter.add(String.valueOf(year));

            mToolbarSpinner.setSelection(savedInstanceState.getInt(KEY_FILTER_YEAR));
        }
     }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int i, j, count = mSpinnerAdapter.getCount();

        int[] years = new int[count - 2];
        for (i = 2, j = 0; i < count; i++, j++) years[j] = Integer.parseInt(mSpinnerAdapter.getItem(i));

        outState.putIntArray(KEY_FILTER_YEARS, years);
        outState.putInt(KEY_FILTER_YEAR, mToolbarSpinner.getSelectedItemPosition());

        Functions.LogD("ActivityChartCost -- onSaveInstanceState");
    }

    static class YearsCursorLoader extends CursorLoader {

        public YearsCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            Functions.LogD("ActivityChartCost -- loadInBackground");

            FuelingDBHelper dbHelper = new FuelingDBHelper();
            return dbHelper.getYears();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new YearsCursorLoader(getApplicationContext());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Functions.LogD("ActivityChartCost -- onLoadFinished");

        int currentYear = Functions.getCurrentYear();
        int prevYear = currentYear - 1;
        int year;

        if (data.moveToFirst()) do {
            year = data.getInt(0);

            Functions.LogD("ActivityChartCost -- YEAR == " + year);

            if (year == currentYear || year == prevYear) continue;

            mSpinnerAdapter.add(String.valueOf(year));
        } while (data.moveToNext());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
