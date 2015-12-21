package ru.p3tr0vich.fuel;

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
    public int getTitleId() {
        return R.string.title_about;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_about, container, false);

        String versionName, buildDate;
        try {
            versionName = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "0.0";
        }
        versionName = getActivity().getString(R.string.about_version) + " " + versionName;
        ((TextView) view.findViewById(R.id.textAboutVersion)).setText(versionName);

        buildDate = "(" + BuildConfig.BUILD_DATE + ")";
        ((TextView) view.findViewById(R.id.textAboutDate)).setText(buildDate);

        return view;
    }
}