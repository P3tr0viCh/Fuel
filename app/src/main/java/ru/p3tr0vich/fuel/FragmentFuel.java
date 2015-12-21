package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class FragmentFuel extends Fragment implements FragmentInterface {

    private OnFragmentChangeListener mOnFragmentChangeListener;

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
            throw new ClassCastException(context.toString() +
                    " must implement OnFragmentChangeListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mOnFragmentChangeListener.onFragmentChange(this);
    }
}
