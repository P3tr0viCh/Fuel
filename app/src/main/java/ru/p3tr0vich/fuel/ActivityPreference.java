package ru.p3tr0vich.fuel;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;

public class ActivityPreference extends PreferenceActivity {

    public static final int REQUEST_CODE = 1471;

    public static void start(Activity parent) {
        parent.startActivityForResult(new Intent(parent, ActivityPreference.class), ActivityPreference.REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prepareLayout();
    }

    private void prepareLayout() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        View content = root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_prefs, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        Toolbar toolbarPrefs = (Toolbar) toolbarContainer.findViewById(R.id.toolbarPrefs);
        toolbarPrefs.setTitle(getTitle());
        toolbarPrefs.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbarPrefs.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void finish() {
        setResult(RESULT_OK);
        super.finish();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.prefs_headers, target);
    }

    @TargetApi( Build.VERSION_CODES.KITKAT )
    @Override
    protected boolean isValidFragment( String fragmentName ) {
        return fragmentName.equals(FragmentPrefs.class.getName());
    }
}
