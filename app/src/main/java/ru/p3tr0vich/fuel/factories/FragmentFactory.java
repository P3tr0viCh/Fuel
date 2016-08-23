package ru.p3tr0vich.fuel.factories;

import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.p3tr0vich.fuel.FragmentAbout;
import ru.p3tr0vich.fuel.FragmentBackup;
import ru.p3tr0vich.fuel.FragmentBase;
import ru.p3tr0vich.fuel.FragmentCalc;
import ru.p3tr0vich.fuel.FragmentChartCost;
import ru.p3tr0vich.fuel.FragmentFueling;
import ru.p3tr0vich.fuel.FragmentPreferences;

public class FragmentFactory {

    public interface Ids {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({BAD_ID,
                FUELING,
                CALC,
                CHART_COST,
                BACKUP,
                PREFERENCES,
                ABOUT})
        @interface Id {
        }

        int BAD_ID = -1;
        int FUELING = 0;
        int CALC = 1;
        int CHART_COST = 2;
        int BACKUP = 3;
        int PREFERENCES = 4;
        int ABOUT = 5;
    }

    private interface Tags {
        String FUELING = FragmentFueling.class.getSimpleName();
        String CALC = FragmentCalc.class.getSimpleName();
        String CHART_COST = FragmentChartCost.class.getSimpleName();
        String PREFERENCES = FragmentPreferences.class.getSimpleName();
        String BACKUP = FragmentBackup.class.getSimpleName();
        String ABOUT = FragmentAbout.class.getSimpleName();
    }

    public interface MainFragment {
        int ID = Ids.FUELING;
        String TAG = Tags.FUELING;
    }

    private FragmentFactory() {
    }

    @NonNull
    public static Fragment getFragmentNewInstance(@Ids.Id int fragmentId, @Nullable Bundle args) {
        Fragment fragment;

        switch (fragmentId) {
            case Ids.CALC:
                fragment = new FragmentCalc();
                break;
            case Ids.CHART_COST:
                fragment = new FragmentChartCost();
                break;
            case Ids.PREFERENCES:
                fragment = new FragmentPreferences();
                break;
            case Ids.BACKUP:
                fragment = new FragmentBackup();
                break;
            case Ids.ABOUT:
                fragment = new FragmentAbout();
                break;
            case Ids.FUELING:
                fragment = new FragmentFueling();
                break;
            case Ids.BAD_ID:
            default:
                throw new IllegalArgumentException("Fragment bad ID");
        }

        return FragmentBase.newInstance(fragmentId, fragment, args);
    }

    @NonNull
    public static Fragment getFragmentNewInstance(@Ids.Id int fragmentId) {
        return getFragmentNewInstance(fragmentId, null);
    }

    @Ids.Id
    public static int intToFragmentId(int id) {
        return id;
    }

    @NonNull
    public static String fragmentIdToTag(@Ids.Id int id) {
        switch (id) {
            case Ids.ABOUT:
                return Tags.ABOUT;
            case Ids.BACKUP:
                return Tags.BACKUP;
            case Ids.CALC:
                return Tags.CALC;
            case Ids.CHART_COST:
                return Tags.CHART_COST;
            case Ids.FUELING:
                return Tags.FUELING;
            case Ids.PREFERENCES:
                return Tags.PREFERENCES;
            case Ids.BAD_ID:
            default:
                throw new IllegalArgumentException("Fragment bad ID");
        }
    }
}