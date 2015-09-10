package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.v4.app.Fragment;

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

    @Override
    public void onStart() {
        super.onStart();
        Functions.logD("FragmentFuel -- onStart");
        mOnFragmentChangedListener.onFragmentChanged(getFragmentId());
    }
}
