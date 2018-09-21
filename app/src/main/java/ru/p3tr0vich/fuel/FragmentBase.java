package ru.p3tr0vich.fuel;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import ru.p3tr0vich.fuel.factories.FragmentFactory;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;

import static ru.p3tr0vich.fuel.factories.FragmentFactory.Ids.BAD_ID;

public abstract class FragmentBase extends Fragment implements FragmentInterface {

    private static final String KEY_ID = "FRAGMENT_BASE_KEY_ID";

    @FragmentFactory.Ids.Id
    private int mFragmentId = BAD_ID;

    private OnFragmentChangeListener mOnFragmentChangeListener;

    protected PreferencesHelper preferencesHelper;

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static Fragment newInstance(@FragmentFactory.Ids.Id int id, @NonNull Fragment fragment,
                                       @Nullable Bundle args) {
        if (args == null) {
            args = new Bundle();
        }

        args.putInt(KEY_ID, id);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mFragmentId = FragmentFactory.intToFragmentId(getArguments().getInt(KEY_ID, BAD_ID));
        }

        if (mFragmentId == BAD_ID) {
            throw new IllegalArgumentException("Fragment must have ID");
        }

        Context context = getContext();

        assert context != null;

        preferencesHelper = PreferencesHelper.getInstance(context);
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