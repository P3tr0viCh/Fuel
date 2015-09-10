package ru.p3tr0vich.fuel;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.EditText;

public class NumberEditTextPreference extends EditTextPreference {
    private static EditText mEditText = null;

    public NumberEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mEditText = new EditText(context, attrs);
    }

    public NumberEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NumberEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberEditTextPreference(Context context) {
        super(context);
    }


}
