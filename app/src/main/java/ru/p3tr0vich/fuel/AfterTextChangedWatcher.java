package ru.p3tr0vich.fuel;

import android.text.TextWatcher;

abstract class AfterTextChangedWatcher implements TextWatcher {
    @Override
    public final void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public final void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}