package ru.p3tr0vich.fuel;

interface FragmentInterface {
    int getFragmentId();

    interface OnFragmentChangeListener {
        void onFragmentChange(int fragmentId);
    }
}
