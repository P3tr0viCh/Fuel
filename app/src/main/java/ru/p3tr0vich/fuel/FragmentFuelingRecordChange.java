package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
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

    private static final String STATE_KEY_DATE = "STATE_KEY_DATE";
    private static final String STATE_KEY_CALC_ENABLED = "STATE_KEY_CALC_ENABLED";

    private FuelingRecord mFuelingRecord;

    private Button mButtonDate;
    private EditText mEditCost;
    private EditText mEditVolume;
    private EditText mEditTotal;

    private float mDefaultCost;
    private float mDefaultVolume;
    private float mOneCUVolumeCost; // Стоимость одного литра
    private boolean mCalcEnabled;

    private CostTextWatcherAdapter mCostTextWatcher = null;
    private VolumeTextWatcherAdapter mVolumeTextWatcher = null;

    private PreferencesHelper mPreferencesHelper;

    @NonNull
    public static Fragment newInstance(@Nullable FuelingRecord fuelingRecord) {
        FragmentFuelingRecordChange fragment = new FragmentFuelingRecordChange();

        if (fuelingRecord != null)
            fragment.setArguments(fuelingRecord.toBundle());

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferencesHelper = PreferencesHelper.getInstance(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fueling_record_change, container, false);

        setHasOptionsMenu(true);

        mButtonDate = (Button) view.findViewById(R.id.btn_date);
        mEditCost = (EditText) view.findViewById(R.id.edit_cost);
        mEditVolume = (EditText) view.findViewById(R.id.edit_volume);
        mEditTotal = (EditText) view.findViewById(R.id.edit_total);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(FuelingRecord.NAME))
            mFuelingRecord = new FuelingRecord(bundle);
        else {
            float cost = mPreferencesHelper.getDefaultCost();
            float volume = mPreferencesHelper.getDefaultVolume();
            float price = mPreferencesHelper.getPrice();

            if (price != 0) {
                if (cost == 0 && volume != 0) cost = volume * price;
                else if (volume == 0 && cost != 0) volume = cost / price;
            }

            mFuelingRecord = new FuelingRecord(cost, volume, mPreferencesHelper.getLastTotal());
        }

        getActivity().setTitle(mFuelingRecord.getId() != 0 ?
                R.string.dialog_caption_update :
                R.string.dialog_caption_add);

        if (savedInstanceState == null) {
            UtilsFormat.floatToEditText(mEditCost, mFuelingRecord.getCost(), false);
            UtilsFormat.floatToEditText(mEditVolume, mFuelingRecord.getVolume(), false);
            UtilsFormat.floatToEditText(mEditTotal, mFuelingRecord.getTotal(), false);
        } else
            mFuelingRecord.setDateTime(savedInstanceState.getLong(STATE_KEY_DATE));

        updateDate();

        mButtonDate.setOnClickListener(this);

        view.findViewById(R.id.text_cost).setOnClickListener(this);
        view.findViewById(R.id.text_volume).setOnClickListener(this);
        view.findViewById(R.id.text_total).setOnClickListener(this);

        mCalcEnabled = mFuelingRecord.getId() == 0 &&
                (savedInstanceState == null ||
                        savedInstanceState.getBoolean(STATE_KEY_CALC_ENABLED));
        if (mCalcEnabled) {
            mDefaultCost = mPreferencesHelper.getDefaultCost();
            mDefaultVolume = mPreferencesHelper.getDefaultVolume();

            if (mDefaultCost != 0 && mDefaultVolume != 0)
                mOneCUVolumeCost = mDefaultCost / mDefaultVolume;
            else
                mOneCUVolumeCost = 0;

            mCalcEnabled = mOneCUVolumeCost != 0;
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mCalcEnabled) {
            if (mCostTextWatcher == null) {
                mCostTextWatcher = new CostTextWatcherAdapter();
                mEditCost.addTextChangedListener(mCostTextWatcher);
            }
            if (mVolumeTextWatcher == null) {
                mVolumeTextWatcher = new VolumeTextWatcherAdapter();
                mEditVolume.addTextChangedListener(mVolumeTextWatcher);
            }
        }
    }

    private class CostTextWatcherAdapter extends TextWatcherAdapter {
        @Override
        public void afterTextChanged(Editable s) {
            final float cost = UtilsFormat.editTextToFloat(mEditCost);

            final float volume =
                    cost == 0 ? 0 : cost == mDefaultCost ? mDefaultVolume : cost / mOneCUVolumeCost;

            mCalcEnabled = false;
            UtilsFormat.floatToEditText(mEditVolume, volume, false);
            mCalcEnabled = true;
        }
    }

    private class VolumeTextWatcherAdapter extends TextWatcherAdapter {
        @Override
        public void afterTextChanged(Editable s) {
            if (mCalcEnabled) {
                mCalcEnabled = false;

                mEditVolume.removeTextChangedListener(mVolumeTextWatcher);
                mEditCost.removeTextChangedListener(mCostTextWatcher);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(STATE_KEY_DATE, mFuelingRecord.getDateTime());
        outState.putBoolean(STATE_KEY_CALC_ENABLED, mCalcEnabled);
    }

    private void updateDate() {
        mButtonDate.setText(UtilsFormat.dateToString(mFuelingRecord.getDateTime(), true));
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

                mPreferencesHelper.putLastTotal(mFuelingRecord.getTotal());

                Activity activity = getActivity();

                activity.setResult(Activity.RESULT_OK);

                activity.finish();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == mButtonDate) {
            final Calendar calendar = UtilsDate.getCalendarInstance(mFuelingRecord.getDateTime());

            DatePickerDialog.newInstance(
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                            calendar.set(year, monthOfYear, dayOfMonth);

                            mFuelingRecord.setDateTime(calendar.getTimeInMillis());

                            updateDate();
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show(getFragmentManager(), null);
        } else {
            final EditText edit;
            switch (v.getId()) {
                case R.id.text_cost:
                    edit = mEditCost;
                    break;
                case R.id.text_volume:
                    edit = mEditVolume;
                    break;
                case R.id.text_total:
                    edit = mEditTotal;
                    break;
                default:
                    return;
            }
            Utils.showKeyboard(edit);
        }
    }
}