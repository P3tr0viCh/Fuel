package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

public class ActivityAverage extends AppCompatActivity {

    public static void start(Activity parent) {
        parent.startActivity(new Intent(parent, ActivityAverage.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_average);

        Toolbar toolbarAverage = (Toolbar) findViewById(R.id.toolbarAverage);
        setSupportActionBar(toolbarAverage);
        toolbarAverage.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbarAverage.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //noinspection ConstantConditions
        Functions.addSpinnerInToolbar(getSupportActionBar(),
                new AppCompatSpinner(getSupportActionBar().getThemedContext()),
                toolbarAverage, new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d("XXX", "ActivityAverage -- onItemSelected");

/*                        Const.FilterMode filterMode;
                        switch (position) {
                            case 0:
                                filterMode = Const.FilterMode.CURRENT_YEAR;
                                break;
                            default:
                                filterMode = Const.FilterMode.ALL;
                        }
                        Toast.makeText(ActivityAverage.this, "FilterChange", Toast.LENGTH_SHORT).show();
                FragmentFueling fragmentFueling = (FragmentFueling) getFragmentManager().findFragmentById(R.id.fragmentFueling);
                fragmentFueling.setFilterMode(filterMode);*/
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
    }
}
