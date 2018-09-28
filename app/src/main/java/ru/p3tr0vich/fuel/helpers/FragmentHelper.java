package ru.p3tr0vich.fuel.helpers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import ru.p3tr0vich.fuel.FragmentCalc;
import ru.p3tr0vich.fuel.FragmentFueling;
import ru.p3tr0vich.fuel.FragmentInterface;
import ru.p3tr0vich.fuel.FragmentPreferences;
import ru.p3tr0vich.fuel.R;
import ru.p3tr0vich.fuel.factories.FragmentFactory;

public class FragmentHelper {
    private final FragmentActivity mFragmentActivity;

    public FragmentHelper(@NonNull FragmentActivity fragmentActivity) {
        mFragmentActivity = fragmentActivity;
    }

    @Nullable
    public Fragment getFragment(@FragmentFactory.Ids.Id int fragmentId) {
        return mFragmentActivity.getSupportFragmentManager().findFragmentByTag(
                FragmentFactory.fragmentIdToTag(fragmentId));
    }

    @NonNull
    public FragmentInterface getCurrentFragment() {
        FragmentInterface fragmentInterface =
                (FragmentInterface) mFragmentActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.content_frame);

        assert fragmentInterface != null;

        return fragmentInterface;

    }

    @Nullable
    public FragmentFueling getFragmentFueling() {
        return (FragmentFueling) getFragment(FragmentFactory.Ids.FUELING);
    }

    @Nullable
    public FragmentCalc getFragmentCalc() {
        return (FragmentCalc) getFragment(FragmentFactory.Ids.CALC);
    }

    @Nullable
    public FragmentPreferences getFragmentPreferences() {
        return (FragmentPreferences) getFragment(FragmentFactory.Ids.PREFERENCES);
    }

    public void addMainFragment() {
        mFragmentActivity.getSupportFragmentManager().beginTransaction()
                .add(R.id.content_frame,
                        FragmentFactory.getFragmentNewInstance(FragmentFactory.Ids.MAIN),
                        FragmentFactory.fragmentIdToTag(FragmentFactory.Ids.MAIN))
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .commit();
    }

    public void replaceFragment(@FragmentFactory.Ids.Id int fragmentId, @Nullable Bundle args) {
        mFragmentActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame,
                        FragmentFactory.getFragmentNewInstance(fragmentId, args),
                        FragmentFactory.fragmentIdToTag(fragmentId))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }
}