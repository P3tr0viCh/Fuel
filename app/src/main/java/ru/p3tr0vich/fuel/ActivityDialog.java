package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ActivityDialog extends AppCompatActivity {

    private static final String DIALOG = "DIALOG";


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DIALOG_SMS_TEXT_PATTERN})
    public @interface Dialog {
    }

    public static final int DIALOG_SMS_TEXT_PATTERN = 0;

    public interface ActivityDialogFragment {
        String getTitle();

        boolean onSaveClicked();
    }

    public static void start(@NonNull Activity parent, @Dialog int dialog) {
        parent.startActivity(new Intent(parent, ActivityDialog.class).putExtra(DIALOG, dialog));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        Toolbar toolbarDialog = (Toolbar) findViewById(R.id.toolbarDialog);
        setSupportActionBar(toolbarDialog);

        assert toolbarDialog != null;

        toolbarDialog.setNavigationIcon(R.drawable.ic_close);

        toolbarDialog.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });

        if (savedInstanceState == null) {
            Fragment fragment;

            switch (getIntent().getIntExtra(DIALOG, -1)) {
                case DIALOG_SMS_TEXT_PATTERN:
                    fragment = FragmentActivityDialogSMSTextPattern.newInstance();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown dialog type");
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contentFrame, fragment, null)
                    .setTransition(FragmentTransaction.TRANSIT_NONE)
                    .commit();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        setTitle(((ActivityDialogFragment) fragment).getTitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_action_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ActivityDialogFragment fragment =
                (ActivityDialogFragment) getSupportFragmentManager().findFragmentById(R.id.contentFrame);

        if (fragment.onSaveClicked()) {
            setResult(Activity.RESULT_OK);
            finish();
        }

        return true;
    }
}