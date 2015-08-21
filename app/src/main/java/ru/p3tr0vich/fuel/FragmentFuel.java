package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

public abstract class FragmentFuel extends Fragment {

    private OnFragmentChangedListener mOnFragmentChangedListener;

    protected abstract int getFragmentId();

    public interface OnFragmentChangedListener {
        void onFragmentChanged(int fragmentId);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Functions.logD("FragmentFuel -- onAttach (context)");
        try {
            mOnFragmentChangedListener = (OnFragmentChangedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement OnFragmentChangedListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Functions.logD("FragmentFuel -- onAttach (activity)");
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
        Functions.logD("FragmentFuel -- onStart");
        mOnFragmentChangedListener.onFragmentChanged(getFragmentId());
    }
}
