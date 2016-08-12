package ru.p3tr0vich.fuel.factories;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.p3tr0vich.fuel.FragmentAbout;
import ru.p3tr0vich.fuel.FragmentBackup;
import ru.p3tr0vich.fuel.FragmentBase;
import ru.p3tr0vich.fuel.FragmentCalc;
import ru.p3tr0vich.fuel.FragmentChartCost;
import ru.p3tr0vich.fuel.FragmentFueling;
import ru.p3tr0vich.fuel.FragmentInterface;
import ru.p3tr0vich.fuel.FragmentPreferences;
import ru.p3tr0vich.fuel.R;

public class FragmentFactory {

    private final FragmentActivity mFragmentActivity;

    public static final class Ids {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({BAD_ID,
                FUELING,
                CALC,
                CHART_COST,
                BACKUP,
                PREFERENCES,
                ABOUT})
        public @interface Id {
        }

        public static final int BAD_ID = -1;
        public static final int FUELING = 0;
        public static final int CALC = 1;
        public static final int CHART_COST = 2;
        public static final int BACKUP = 3;
        public static final int PREFERENCES = 4;
        public static final int ABOUT = 5;

        private Ids() {
        }
    }

    public static final class Tags {
        public static final String FUELING = "FragmentFueling";
        public static final String CALC = "FragmentCalc";
        public static final String CHART_COST = "FragmentChartCost";
        public static final String PREFERENCES = "FragmentPreferences";
        public static final String BACKUP = "FragmentBackup";
        public static final String ABOUT = "FragmentAbout";

        private Tags() {
        }
    }

    public FragmentFactory(@NonNull FragmentActivity fragmentActivity) {
        mFragmentActivity = fragmentActivity;
    }

    @NonNull
    public Fragment getFragmentNewInstance(@Ids.Id int fragmentId) {
        switch (fragmentId) {
            case Ids.CALC:
                return FragmentBase.newInstance(fragmentId, new FragmentCalc());
            case Ids.CHART_COST:
                return FragmentBase.newInstance(fragmentId, new FragmentChartCost());
            case Ids.PREFERENCES:
                return FragmentBase.newInstance(fragmentId, new FragmentPreferences());
            case Ids.BACKUP:
                return FragmentBase.newInstance(fragmentId, new FragmentBackup());
            case Ids.ABOUT:
                return FragmentBase.newInstance(fragmentId, new FragmentAbout());
            case Ids.FUELING:
                return FragmentBase.newInstance(fragmentId, new FragmentFueling());
            case Ids.BAD_ID:
            default:
                throw new IllegalArgumentException(mFragmentActivity.getString(R.string.exception_fragment_bad_id));
        }
    }

    @Ids.Id
    public static int intToFragmentId(int id) {
        return id;
    }

    @NonNull
    public String fragmentIdToTag(@Ids.Id int id) {
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
                throw new IllegalArgumentException(mFragmentActivity.getString(R.string.exception_fragment_bad_id));
        }
    }

    @Nullable
    private Fragment findFragmentByTag(@NonNull String fragmentTag) {
        return mFragmentActivity.getSupportFragmentManager().findFragmentByTag(fragmentTag);
    }

    @Nullable
    public FragmentInterface findFragmentById(@Ids.Id int fragmentId) {
        FragmentInterface fragment = getCurrentFragment();
        return fragment.getFragmentId() == fragmentId ? fragment : null;
    }

    @NonNull
    public FragmentInterface getCurrentFragment() {
        return (FragmentInterface) mFragmentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);

    }

    @Nullable
    public FragmentFueling getFragmentFueling() {
        return (FragmentFueling) findFragmentByTag(Tags.FUELING);
    }

    @Nullable
    public FragmentCalc getFragmentCalc() {
        return (FragmentCalc) findFragmentByTag(Tags.CALC);
    }

    @Nullable
    public FragmentPreferences getFragmentPreferences() {
        return (FragmentPreferences) findFragmentByTag(Tags.PREFERENCES);
    }
}