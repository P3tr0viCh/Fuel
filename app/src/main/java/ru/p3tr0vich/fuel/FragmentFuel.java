package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.v4.app.Fragment;

public abstract class FragmentFuel extends Fragment implements FragmentInterface {

    private OnFragmentChangeListener mOnFragmentChangeListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        Functions.logD("FragmentFuel -- onAttach (context)");
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
//        Functions.logD("FragmentFuel -- onStart");
        mOnFragmentChangeListener.onFragmentChange(getFragmentId());
    }
}
