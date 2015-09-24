package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.Calendar;
import java.util.Date;

public class FragmentFuelingRecordChange extends Fragment implements View.OnClickListener {

    private static final String INTENT_EXTRA = "EXTRA_DATE";

    private Const.RecordAction mRecordAction;

    private Date mDate;

    private FuelingRecord mFuelingRecord;

    private Button mButtonDate;
    private EditText mEditCost;
    private EditText mEditVolume;
    private EditText mEditTotal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fueling_record_change, container, false);

        setHasOptionsMenu(true);

        mButtonDate = (Button) view.findViewById(R.id.btnDate);
        mEditCost = (EditText) view.findViewById(R.id.editCost);
        mEditVolume = (EditText) view.findViewById(R.id.editVolume);
        mEditTotal = (EditText) view.findViewById(R.id.editTotal);

        Intent intent = getActivity().getIntent();

        mRecordAction = ActivityFuelingRecordChange.getAction(intent);

        switch (mRecordAction) {
            case ADD:
                getActivity().setTitle(R.string.dialog_caption_add);

                mDate = new Date();

                SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

                mFuelingRecord = new FuelingRecord(-1, "",
                        Functions.textToFloat(sPref.getString(getString(R.string.pref_def_cost), "0")),
                        Functions.textToFloat(sPref.getString(getString(R.string.pref_def_volume), "0")),
                        sPref.getFloat(getString(R.string.pref_last_total), 0));
                break;
            case UPDATE:
                getActivity().setTitle(R.string.dialog_caption_update);

                mFuelingRecord = new FuelingRecord(intent);

                mDate = Functions.sqlDateToDate(mFuelingRecord.getSQLiteDate());
        }

        Functions.floatToText(mEditCost, mFuelingRecord.getCost(), false);
        Functions.floatToText(mEditVolume, mFuelingRecord.getVolume(), false);
        Functions.floatToText(mEditTotal, mFuelingRecord.getTotal(), false);

        if (savedInstanceState != null)
            mDate = (Date) savedInstanceState.getSerializable(INTENT_EXTRA);

        updateDate();

        mButtonDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(mDate);
                DatePickerDialog.newInstance(
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(year, monthOfYear, dayOfMonth);
                                setDate(calendar.getTime());
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                ).show(getFragmentManager(), null);
            }
        });

        view.findViewById(R.id.textCost).setOnClickListener(this);
        view.findViewById(R.id.textVolume).setOnClickListener(this);
        view.findViewById(R.id.textTotal).setOnClickListener(this);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(INTENT_EXTRA, mDate);
    }

    private void setDate(Date date) {
        mDate = date;
        updateDate();
    }

    private void updateDate() {
        mButtonDate.setText(Functions.dateToString(mDate, true));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fueling_record, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                mFuelingRecord.setSQLiteDate(Functions.dateToSQLite(mDate));
                mFuelingRecord.setCost(Functions.editTextToFloat(mEditCost));
                mFuelingRecord.setVolume(Functions.editTextToFloat(mEditVolume));
                mFuelingRecord.setTotal(Functions.editTextToFloat(mEditTotal));

                Activity activity = getActivity();

                PreferenceManager.getDefaultSharedPreferences(activity)
                        .edit()
                        .putFloat(getString(R.string.pref_last_total), mFuelingRecord.getTotal())
                        .apply();

                activity.setResult(Activity.RESULT_OK,
                        mFuelingRecord.toIntent()
                                .putExtra(ActivityFuelingRecordChange.INTENT_EXTRA_ACTION, mRecordAction.ordinal()));
                activity.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        EditText edit;
        switch (v.getId()) {
            case R.id.textCost:
                edit = mEditCost;
                break;
            case R.id.textVolume:
                edit = mEditVolume;
                break;
            case R.id.textTotal:
                edit = mEditTotal;
                break;
            default:
                return;
        }
        Functions.showKeyboard(edit);
    }
}
