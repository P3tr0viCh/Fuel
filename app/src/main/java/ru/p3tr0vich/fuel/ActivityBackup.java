package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;


public class ActivityBackup extends AppCompatActivity {

    public static final int REQUEST_CODE = 3710;

    public static void start(Activity parent) {
        parent.startActivityForResult(new Intent(parent, ActivityBackup.class), REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        Toolbar toolbarBackup = (Toolbar) findViewById(R.id.toolbarBackup);
        setSupportActionBar(toolbarBackup);
        toolbarBackup.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbarBackup.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Log.d("XXX", "ActivityBackup -- onCreate");
    }
}
