package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import ru.p3tr0vich.fuel.helpers.ContentProviderHelper;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsDate;
import ru.p3tr0vich.fuel.utils.UtilsFormat;

public class FragmentFuelingRecordChange extends Fragment implements View.OnClickListener {

    public static final String TAG = "FragmentFuelingRecordChange";

    private static final String INTENT_EXTRA_DATE = "EXTRA_DATE";

    private long mDateTime;

    private FuelingRecord mFuelingRecord;

    private Button mButtonDate;
    private EditText mEditCost;
    private EditText mEditVolume;
    private EditText mEditTotal;

    public static Fragment getInstance(@Nullable FuelingRecord fuelingRecord) {
        FragmentFuelingRecordChange fragment = new FragmentFuelingRecordChange();

        if (fuelingRecord != null)
            fragment.setArguments(fuelingRecord.toBundle());

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fueling_record_change, container, false);

        setHasOptionsMenu(true);

        mButtonDate = (Button) view.findViewById(R.id.btnDate);
        mEditCost = (EditText) view.findViewById(R.id.editCost);
        mEditVolume = (EditText) view.findViewById(R.id.editVolume);
        mEditTotal = (EditText) view.findViewById(R.id.editTotal);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(FuelingRecord.NAME))
            mFuelingRecord = new FuelingRecord(bundle);
        else
            mFuelingRecord = new FuelingRecord(
                    PreferencesHelper.getDefaultCost(),
                    PreferencesHelper.getDefaultVolume(),
                    PreferencesHelper.getLastTotal());

        getActivity().setTitle(mFuelingRecord.getId() != 0 ?
                R.string.dialog_caption_update :
                R.string.dialog_caption_add);

        UtilsFormat.floatToEditText(mEditCost, mFuelingRecord.getCost(), false);
        UtilsFormat.floatToEditText(mEditVolume, mFuelingRecord.getVolume(), false);
        UtilsFormat.floatToEditText(mEditTotal, mFuelingRecord.getTotal(), false);

        if (savedInstanceState == null)
            mDateTime = mFuelingRecord.getDateTime();
        else
            mDateTime = savedInstanceState.getLong(INTENT_EXTRA_DATE);

        updateDate();

        mButtonDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = UtilsDate.getCalendarInstance(mDateTime);

                DatePickerDialog.newInstance(
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                                calendar.set(year, monthOfYear, dayOfMonth);

                                mDateTime = calendar.getTimeInMillis();

                                updateDate();
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

        outState.putLong(INTENT_EXTRA_DATE, mDateTime);
    }

    private void updateDate() {
        mButtonDate.setText(UtilsFormat.dateToString(mDateTime, true));
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
                mFuelingRecord.setDateTime(mDateTime);
                mFuelingRecord.setCost(UtilsFormat.editTextToFloat(mEditCost));
                mFuelingRecord.setVolume(UtilsFormat.editTextToFloat(mEditVolume));
                mFuelingRecord.setTotal(UtilsFormat.editTextToFloat(mEditTotal));

                if (mFuelingRecord.getId() != 0) {
                    if (ContentProviderHelper.updateRecord(getContext(), mFuelingRecord) == 0) {
                        Utils.toast(R.string.message_error_update_record);

                        return false;
                    }
                } else {
                    if (ContentProviderHelper.insertRecord(getContext(), mFuelingRecord) == -1) {
                        Utils.toast(R.string.message_error_insert_record);

                        return false;
                    }
                }

                PreferencesHelper.putLastTotal(mFuelingRecord.getTotal());

                Activity activity = getActivity();

                activity.setResult(Activity.RESULT_OK);

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
        Utils.showKeyboard(edit);
    }
}