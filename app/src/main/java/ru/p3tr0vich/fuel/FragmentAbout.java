package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.p3tr0vich.fuel.utils.UtilsFormat;

public class FragmentAbout extends FragmentBase {

    @Override
    public int getTitleId() {
        return R.string.title_about;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_about, container, false);

        String versionName;

        Activity activity = getActivity();
        assert activity != null;

        try {
            versionName = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "0.0";
        }

        versionName = activity.getString(R.string.about_version, versionName);
        ((TextView) view.findViewById(R.id.text_app_version)).setText(versionName);

        ((TextView) view.findViewById(R.id.text_app_build_date)).setText(
                UtilsFormat.dateToString(BuildConfig.BUILD_DATE, true));

        return view;
    }
}