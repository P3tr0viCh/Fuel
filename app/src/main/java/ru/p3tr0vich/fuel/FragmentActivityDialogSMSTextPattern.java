package ru.p3tr0vich.fuel;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import ru.p3tr0vich.fuel.helpers.PreferencesHelper;

public class FragmentActivityDialogSMSTextPattern extends Fragment
        implements ActivityDialog.ActivityDialogFragment {

    private EditText mEditSMSText;
    private EditText mEditSMSTextPattern;

    @NonNull
    public static Fragment newInstance() {
        return new FragmentActivityDialogSMSTextPattern();
    }

    @Override
    public String getTitle() {
        return getContext().getString(R.string.pref_sms_title);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_dialog_sms_text_pattern, container, false);

        mEditSMSText = (EditText) view.findViewById(R.id.editSMSText);
        mEditSMSTextPattern = (EditText) view.findViewById(R.id.editSMSTextPattern);

        mEditSMSText.setText(PreferencesHelper.getSMSText());
        mEditSMSTextPattern.setText(PreferencesHelper.getSMSTextPattern());

        return view;
    }

    @Override
    public boolean onSaveClicked() {
        PreferencesHelper.putSMSTextAndPattern(
                mEditSMSText.getText().toString(),
                mEditSMSTextPattern.getText().toString());

        return true;
    }
}