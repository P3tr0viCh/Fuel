package ru.p3tr0vich.fuel;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;

public class FragmentDialogDate extends DialogFragment {

    public static final int REQUEST_CODE = 4517;

    private static final String DIALOG_TAG = "DialogDatePicker";
    private static final String KEY_DATE = "KEY_DATE";

    private Dialog mDialog;

    private Calendar mCalendar;

    public static void show(Fragment parent, Date date) {
        Bundle args = new Bundle();

        args.putSerializable(KEY_DATE, date);

        DialogFragment dialogDate = new FragmentDialogDate();
        dialogDate.setArguments(args);
        dialogDate.setTargetFragment(parent, FragmentDialogDate.REQUEST_CODE);
        dialogDate.show(parent.getFragmentManager(), DIALOG_TAG);
    }

    public static Date getDate(Intent data) {
        return (Date) data.getSerializableExtra(KEY_DATE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_date, null, false);

        Date date = (Date) getArguments().getSerializable(KEY_DATE);

        mCalendar = Calendar.getInstance();

        mCalendar.setTime(date);

        DatePicker mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);

        mDatePicker.init(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        updateTitle(year, monthOfYear, dayOfMonth);
                    }
                });

        builder.setView(view);
        builder.setPositiveButton(R.string.dialog_btn_done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent();

                Date date = mCalendar.getTime();
                intent.putExtra(KEY_DATE, date);

                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
            }
        });

        builder.setNegativeButton(R.string.dialog_btn_cancel, null);
        builder.setCancelable(true);

        builder.setTitle("");

        mDialog = builder.create();

        updateTitle(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));

        return mDialog;
    }

    private void updateTitle(int year, int month, int day) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);

        String title = DateUtils.formatDateTime(getActivity(),
                mCalendar.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);

        title = Character.toString(title.charAt(0)).toUpperCase() + title.substring(1);

        mDialog.setTitle(title);
    }
}
