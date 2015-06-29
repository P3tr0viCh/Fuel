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
import android.widget.TextView;

public class FragmentDialogAbout extends DialogFragment implements View.OnClickListener {

    private static final String DIALOG_TAG = "DialogAbout";

    public static void show(Activity parent) {
        new FragmentDialogAbout().show(parent.getFragmentManager(), DIALOG_TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_about, null, false);

        view.findViewById(R.id.ltMain).setOnClickListener(this);

        String versionName;
        try {
            versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "0.0";
        }
        ((TextView) view.findViewById(R.id.textAboutVersion)).setText(getActivity().getString(R.string.about_version) + " " + versionName);

        ((TextView) view.findViewById(R.id.textAboutDate)).setText("(" + BuildConfig.BUILD_DATE + ")");

        return new AlertDialog.Builder(getActivity()).setView(view).create();
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    @Override
    public void onResume() {
        Functions.setDialogWidth(getDialog(), 300);
        super.onResume();
    }
}