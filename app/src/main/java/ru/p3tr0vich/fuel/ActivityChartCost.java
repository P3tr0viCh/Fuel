package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class ActivityChartCost extends AppCompatActivity {

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

        toolbarChartCost.setSubtitle(R.string.title_activity_chart_cost_subtitle);
    }
}
