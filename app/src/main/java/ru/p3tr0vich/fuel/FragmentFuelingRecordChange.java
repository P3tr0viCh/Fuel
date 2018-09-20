package ru.p3tr0vich.fuel;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
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

import ru.p3tr0vich.fuel.helpers.ContentResolverHelper;
import ru.p3tr0vich.fuel.helpers.PreferencesHelper;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.Utils;
import ru.p3tr0vich.fuel.utils.UtilsDate;
import ru.p3tr0vich.fuel.utils.UtilsFormat;

import static ru.p3tr0vich.fuel.R.id.edit_cost;

public class FragmentFuelingRecordChange extends Fragment implements View.OnClickListener {

    public static final String TAG = "FragmentFuelingRecordChange";

    private static final String STATE_KEY_DATE = "STATE_KEY_DATE";
    private static final String STATE_KEY_CALC_STEP = "STATE_KEY_CALC_STEP";

    private FuelingRecord mFuelingRecord;

    private Button mButtonDate;
    private EditText mEditCost;
    private EditText mEditVolume;
    private EditText mEditTotal;

    private float mPrice;

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
        mEditCost = (EditText) view.findViewById(edit_cost);
        mEditVolume = (EditText) view.findViewById(R.id.edit_volume);
        mEditTotal = (EditText) view.findViewById(R.id.edit_total);

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(FuelingRecord.NAME)) {
            mFuelingRecord = new FuelingRecord(bundle);
            mPrice = 0;
        } else {
            float cost = mPreferencesHelper.getDefaultCost();
            float volume = mPreferencesHelper.getDefaultVolume();
            mPrice = mPreferencesHelper.getPrice();

            if (mPrice != 0) {
                if (cost == 0 && volume != 0) cost = volume * mPrice;
                else if (volume == 0 && cost != 0) volume = cost / mPrice;
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

        if (savedInstanceState == null)
            mCalcStep = mFuelingRecord.getId() == 0 && mPrice > 0 ? CALC_STEP_SELECT : CALC_STEP_DISABLED;
        else
            mCalcStep = savedInstanceState.getInt(STATE_KEY_CALC_STEP, CALC_STEP_DISABLED);

        return view;
    }

    /**
     * Шаг ожидания ввода.
     * В случае изменения старой записи или если цена меньше или равна нулю
     * вычисление отключено ({@code CALC_STEP_DISABLED}).
     * Иначе, при первом открытии фрагмента, шаг устанавливается на выбор
     * редактируемого и вычисляемого полей ({@code CALC_STEP_SELECT}).
     * При последующих пересозданиях фрагмента загружается предыдущее значение.
     */
    private int mCalcStep;

    /**
     * Выбор редактируемого и вычисляемого полей.
     */
    private static final int CALC_STEP_SELECT = 0;
    /**
     * Редактируемое поле -- Стоимость, вычисляемое поле -- Объём.
     */
    private static final int CALC_STEP_COST = 1;
    /**
     * Редактируемое поле -- Объём, вычисляемое -- Стоимость.
     */
    private static final int CALC_STEP_VOLUME = 2;
    /**
     * Вычисление отключено.
     */
    private static final int CALC_STEP_DISABLED = 3;

    /**
     * Проверка способа изменения поля:
     * true, если поле изменяется в результате ручного ввода,
     * false, если изменение происходит в результате вызова функции setText.
     */
    private boolean mManualInput = true;

    private TextWatcher mCostTextWatcher = null;
    private TextWatcher mVolumeTextWatcher = null;

    @Override
    public void onStart() {
        super.onStart();

        if (mCalcStep != CALC_STEP_DISABLED) {
            if (mCostTextWatcher == null) {
                mCostTextWatcher = new EditTextWatcherAdapter(mEditCost, mEditVolume,
                        new CalcVolume(), CALC_STEP_COST);
                mEditCost.addTextChangedListener(mCostTextWatcher);
            }
            if (mVolumeTextWatcher == null) {
                mVolumeTextWatcher = new EditTextWatcherAdapter(mEditVolume, mEditCost,
                        new CalcCost(), CALC_STEP_VOLUME);
                mEditVolume.addTextChangedListener(mVolumeTextWatcher);
            }
        }
    }

    private class EditTextWatcherAdapter extends TextWatcherAdapter {
        private final EditText mEdited;
        private final EditText mCalculated;

        private final Calculator mCalculator;

        private final int mThisStep;

        /**
         * @param edited     редактируемое поле.
         * @param calculated вычисляемое поле.
         * @param calculator метод вычисления.
         * @param thisStep   редактируемое поле ({@code CALC_STEP_COST или CALC_STEP_VOLUME}).
         */
        public EditTextWatcherAdapter(EditText edited, EditText calculated, Calculator calculator, int thisStep) {
            mEdited = edited;
            mCalculated = calculated;
            mCalculator = calculator;
            mThisStep = thisStep;
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Изначально шаг установлен на выбор.
            // После первого изменения в одном из полей, это поле назначается редактируемым,
            // а другое -- вычисляемым.

            if (mCalcStep == CALC_STEP_SELECT) {
                mCalcStep = mThisStep;
            }

            if (mCalcStep == mThisStep) {
                // Если значение изменяется в редактируемом поле,
                // вычисляется значение для вычисляемого поля.

                float enteredValue = UtilsFormat.editTextToFloat(mEdited);

                float calculatedValue = mCalculator.calc(enteredValue, mPrice);

                mManualInput = false;
                UtilsFormat.floatToEditText(mCalculated, calculatedValue, false);
                mManualInput = true;
            } else {
                // Иначе, если было произведено ручное изменение в вычисляемом поле,
                // вычисление отключается.

                if (mManualInput) {
                    mCalcStep = CALC_STEP_DISABLED;
                    mEditCost.removeTextChangedListener(mCostTextWatcher);
                    mEditVolume.removeTextChangedListener(mVolumeTextWatcher);
                }
            }
        }
    }

    /**
     * Метод вычисления.
     * См. {@link CalcCost}, {@link CalcVolume}
     */
    private interface Calculator {
        float calc(float value, float price);
    }

    /**
     * Вычисляет объём на основе стоимости.
     */
    private static class CalcVolume implements Calculator {
        public float calc(float cost, float price) {
            if (cost == 0 || price == 0) return 0;
            return cost / price;
        }
    }

    /**
     * Вычисляет стоимость на основе объёма.
     */
    private static class CalcCost implements Calculator {
        public float calc(float volume, float price) {
            if (volume == 0 || price == 0) return 0;
            return volume * price;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(STATE_KEY_DATE, mFuelingRecord.getDateTime());
        outState.putInt(STATE_KEY_CALC_STEP, mCalcStep);
    }

    private void updateDate() {
        mButtonDate.setText(UtilsFormat.dateToString(mFuelingRecord.getDateTime(), true));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fueling_record, menu);
    }

    /**
     * Выполняет добавление новой или изменение уже существующей записи.
     *
     * @return Истина, если сохранение успешно.
     */
    private boolean saveRecord() {
        Utils.hideKeyboard(getActivity());

        mFuelingRecord.setCost(UtilsFormat.editTextToFloat(mEditCost));
        mFuelingRecord.setVolume(UtilsFormat.editTextToFloat(mEditVolume));
        mFuelingRecord.setTotal(UtilsFormat.editTextToFloat(mEditTotal));

        if (mFuelingRecord.getId() != 0) {
            if (ContentResolverHelper.updateRecord(getContext(), mFuelingRecord)) {
                return true;
            } else {
                Utils.toast(R.string.message_error_update_record);

                return false;
            }
        } else {
            if (ContentResolverHelper.insertRecord(getContext(), mFuelingRecord)) {
                return true;
            } else {
                Utils.toast(R.string.message_error_insert_record);

                return false;
            }
        }
    }

    private boolean onSaveClicked() {
        if (saveRecord()) {
            mPreferencesHelper.putLastTotal(mFuelingRecord.getTotal());

            Activity activity = getActivity();

            activity.setResult(Activity.RESULT_OK);

            activity.finish();

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                return onSaveClicked();
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