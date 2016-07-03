package ru.p3tr0vich.fuel;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;

import ru.p3tr0vich.fuel.helpers.PreferencesHelper;

public abstract class FragmentPreferencesBase extends PreferenceFragmentCompat
        implements FragmentInterface {

    private int mFragmentId = -1;

    private OnFragmentChangeListener mOnFragmentChangeListener;

    protected PreferencesHelper preferencesHelper;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected static Fragment newInstance(int id, @NonNull Fragment fragment) {
        Bundle args = new Bundle();
        args.putInt(KEY_ID, id);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        preferencesHelper = PreferencesHelper.getInstance(getContext());

        super.onCreate(savedInstanceState);

        if (getArguments() != null) mFragmentId = getArguments().getInt(KEY_ID, -1);

        if (mFragmentId == -1)
            throw new IllegalArgumentException(getString(R.string.exception_fragment_no_id));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDivider(null);

        getListView().addItemDecoration(new DividerItemDecorationPreferences(getContext()));
    }

    @Override
    public final int getFragmentId() {
        return mFragmentId;
    }

    @Override
    public int getTitleId() {
        return -1;
    }

    @Override
    public int getSubtitleId() {
        return -1;
    }

    @Nullable
    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnFragmentChangeListener = (OnFragmentChangeListener) context;
        } catch (ClassCastException e) {
            throw new ImplementException(context, OnFragmentChangeListener.class);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mOnFragmentChangeListener.onFragmentChange(this);
    }
}