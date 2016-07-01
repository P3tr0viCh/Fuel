package ru.p3tr0vich.fuel;

import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.helpers.SMSTextPatternHelper;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsFormat;

public class FragmentActivityDialogSMSTextPattern extends Fragment
        implements ActivityDialog.ActivityDialogFragment {

    private EditText mEditSMSText;
    private EditText mEditSMSTextPattern;
    private TextView mTextResult;

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

        mEditSMSText = (EditText) view.findViewById(R.id.edit_sms_text);
        mEditSMSTextPattern = (EditText) view.findViewById(R.id.edit_sms_text_pattern);
        mTextResult = (TextView) view.findViewById(R.id.text_check_result);

        if (savedInstanceState == null) {
            PreferencesHelper preferencesHelper = PreferencesHelper.getInstance(getContext());

            mEditSMSText.setText(preferencesHelper.getSMSText());
            mEditSMSTextPattern.setText(preferencesHelper.getSMSTextPattern());

            updateResult();
        }

        EditTextWatcherAdapter textWatcher = new EditTextWatcherAdapter();

        mEditSMSText.addTextChangedListener(textWatcher);
        mEditSMSTextPattern.addTextChangedListener(textWatcher);

        return view;
    }

    private void updateResult() {
        String pattern = mEditSMSTextPattern.getText().toString();
        String message = mEditSMSText.getText().toString();

        boolean patternEmpty = TextUtils.isEmpty(pattern);
        boolean messageEmpty = TextUtils.isEmpty(message);

        @ColorInt int color = Utils.getColor(R.color.error_text);

        String result;

        if (patternEmpty && messageEmpty)
            result = getString(R.string.text_sms_text_result_all_empty);
        else if (patternEmpty)
            result = getString(R.string.text_sms_text_result_pattern_empty);
        else
            try {
                Float value = SMSTextPatternHelper.getValue(pattern, message);

                if (messageEmpty)
                    result = getString(R.string.text_sms_text_result_message_empty);
                else if (value != null) {
                    result = getString(R.string.text_sms_text_result_ok, UtilsFormat.floatToString(value));

                    color = Utils.getColor(R.color.text_primary);
                } else
                    result = getString(R.string.text_sms_text_result_no_value);
            } catch (Exception e) {
                result = getString(R.string.text_sms_text_result_wrong_pattern);
            }

        mTextResult.setTextColor(color);
        mTextResult.setText(result);
    }

    private class EditTextWatcherAdapter extends TextWatcherAdapter {
        @Override
        public void afterTextChanged(Editable s) {
            updateResult();
        }
    }

    @Override
    public boolean onSaveClicked() {
        PreferencesHelper.getInstance(getContext()).putSMSTextAndPattern(
                mEditSMSText.getText().toString(),
                mEditSMSTextPattern.getText().toString());

        return true;
    }
}