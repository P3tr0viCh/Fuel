package ru.p3tr0vich.fuel;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class FragmentBase extends Fragment implements FragmentInterface {

    private int mFragmentId = -1;

    private OnFragmentChangeListener mOnFragmentChangeListener;

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
        super.onCreate(savedInstanceState);

        if (getArguments() != null) mFragmentId = getArguments().getInt(KEY_ID, -1);

        if (mFragmentId == -1)
            throw new IllegalArgumentException(getString(R.string.exception_fragment_no_id));
    }

    @Override
    public final int getFragmentId() {
        return mFragmentId;
    }

    @Override
    public int getTitleId() {
        return -1;
    }

    @Nullable
    @Override
    public String getTitle() {
        int id = getTitleId();
        return id != -1 ? getString(id) : null;
    }

    @Override
    public int getSubtitleId() {
        return -1;
    }

    @Nullable
    @Override
    public String getSubtitle() {
        int id = getSubtitleId();
        return id != -1 ? getString(id) : null;
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

    @Override
    public boolean onBackPressed() {
        return false;
    }
}