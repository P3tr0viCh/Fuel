package ru.p3tr0vich.fuel;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ActivityCalc extends AppCompatActivity {

    public static void start(Activity parent) {
        parent.startActivity(new Intent(parent, ActivityCalc.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);

        Toolbar toolbarCalc = (Toolbar) findViewById(R.id.toolbarCalc);
        setSupportActionBar(toolbarCalc);
        toolbarCalc.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbarCalc.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                ActivityPreference.start(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case ActivityYandexMap.REQUEST_CODE:
                ((FragmentCalc) getFragmentManager().findFragmentById(R.id.fragmentCalc))
                        .setDistance(ActivityYandexMap.getDistance(data));
                break;
            case ActivityPreference.REQUEST_CODE:
                ((FragmentCalc) getFragmentManager().findFragmentById(R.id.fragmentCalc))
                        .prefsChanged();
        }
    }
}
