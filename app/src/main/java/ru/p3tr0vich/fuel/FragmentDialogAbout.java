package ru.p3tr0vich.fuel;
// TODO: Размер значка

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FragmentDialogAbout extends DialogFragment implements View.OnClickListener {

    private static final String DIALOG_TAG = "DialogAbout";

    public static void show(Activity parent) {
        FragmentDialogAbout dialog = new FragmentDialogAbout();
        dialog.show(parent.getFragmentManager(), DIALOG_TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        @SuppressLint("InflateParams")
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_about, null, false);
        builder.setView(rootView);

        RelativeLayout ltMain = (RelativeLayout) rootView.findViewById(R.id.ltMain);
        ltMain.setOnClickListener(this);

        TextView tv = (TextView) rootView.findViewById(R.id.textAboutVersion);
        String versionName;
        try {
            versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "0.0";
        }
        tv.setText(getActivity().getString(R.string.about_version) + " " + versionName);

        tv = (TextView) rootView.findViewById(R.id.textAboutDate);
        tv.setText("(" + BuildConfig.BUILD_DATE + ")");

        return builder.create();
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }
}