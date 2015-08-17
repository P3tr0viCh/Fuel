package ru.p3tr0vich.fuel;
// TODO: Размер значка

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FragmentAbout extends FragmentFuel {

    public static final String TAG = "FragmentAbout";

    @Override
    public int getFragmentId() {
        return R.id.action_about;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        String versionName;
        try {
            versionName = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "0.0";
        }
        ((TextView) view.findViewById(R.id.textAboutVersion)).setText(getActivity().getString(R.string.about_version) + " " + versionName);

        ((TextView) view.findViewById(R.id.textAboutDate)).setText("(" + BuildConfig.BUILD_DATE + ")");

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Functions.LogD("FragmentAbout -- onCreate");
    }

    @Override
    public void onDestroy() {
        Functions.LogD("FragmentAbout -- onDestroy");
        super.onDestroy();
    }
}