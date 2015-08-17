package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.app.Fragment;

public abstract class FragmentFuel extends Fragment {

    private OnFragmentChangedListener mOnFragmentChangedListener;

    protected abstract int getFragmentId();

    public interface OnFragmentChangedListener {
        void onFragmentChanged(int fragmentId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mOnFragmentChangedListener = (OnFragmentChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement OnFragmentChangedListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mOnFragmentChangedListener.onFragmentChanged(getFragmentId());
    }
}
