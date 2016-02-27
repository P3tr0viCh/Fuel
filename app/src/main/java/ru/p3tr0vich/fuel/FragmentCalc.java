package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class FragmentCalc extends FragmentBase implements
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    public static final String TAG = "FragmentCalc";

    private EditText mEditDistance;
    private EditText mEditCost;
    private EditText mEditVolume;
    private EditText mEditPrice;
    private EditText mEditCons;

    private Spinner mSpinnerCons;
    private Spinner mSpinnerSeason;

    private LinearLayout mLayoutPriceEmpty;
    private LinearLayout mLayoutConsEmpty;

    private boolean mCalculating;

    private float[][] arrCons = {{0, 0, 0}, {0, 0, 0}};

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CALC_ACTION_DISTANCE, CALC_ACTION_COST, CALC_ACTION_VOLUME})
    private @interface CalcAction {
    }

    private static final int CALC_ACTION_DISTANCE = 0;
    private static final int CALC_ACTION_COST = 1;
    private static final int CALC_ACTION_VOLUME = 2;

    private OnCalcDistanceButtonClickListener mOnCalcDistanceButtonClickListener;

    public interface OnCalcDistanceButtonClickListener {
        void onCalcDistanceButtonClick();
    }

    @NonNull
    public static Fragment newInstance(int id) {
        return newInstance(id, new FragmentCalc());
    }

    @Override
    public int getTitleId() {
        return R.string.title_calc;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mOnCalcDistanceButtonClickListener = (OnCalcDistanceButtonClickListener) context;
        } catch (ClassCastException e) {
            throw new ImplementException(context, OnCalcDistanceButtonClickListener.class);
        }
    }

    @Nullable
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calc, container, false);

        mEditDistance = (EditText) view.findViewById(R.id.editDistance);
        mEditCost = (EditText) view.findViewById(R.id.editCost);
        mEditVolume = (EditText) view.findViewById(R.id.editVolume);
        mEditPrice = (EditText) view.findViewById(R.id.editPrice);
        mEditCons = (EditText) view.findViewById(R.id.editCons);

        mSpinnerCons = (Spinner) view.findViewById(R.id.spinnerCons);
        mSpinnerSeason = (Spinner) view.findViewById(R.id.spinnerSeason);

        Utils.setBackgroundTint(mSpinnerCons, R.color.secondary_text, R.color.accent);
        Utils.setBackgroundTint(mSpinnerSeason, R.color.secondary_text, R.color.accent);

        mLayoutPriceEmpty = (LinearLayout) view.findViewById(R.id.layoutPriceEmpty);
        mLayoutConsEmpty = (LinearLayout) view.findViewById(R.id.layoutConsEmpty);

        ArrayAdapter<CharSequence> adapterCons = ArrayAdapter.createFromResource(getActivity(),
                R.array.spinner_consumption, R.layout.spinner_item);
        adapterCons.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mSpinnerCons.setAdapter(adapterCons);

        ArrayAdapter<CharSequence> adapterSeason = ArrayAdapter.createFromResource(getActivity(),
                R.array.spinner_season, R.layout.spinner_item);
        adapterSeason.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mSpinnerSeason.setAdapter(adapterSeason);

        Button btnMaps = (Button) view.findViewById(R.id.btnMaps);
        btnMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnCalcDistanceButtonClickListener.onCalcDistanceButtonClick();
            }
        });

        Utils.setBackgroundTint(btnMaps, R.color.accent, R.color.primary_dark);

        mCalculating = true;

        loadPrefs();

        checkTextOnEmpty(mEditPrice);

        view.findViewById(R.id.textDistance).setOnClickListener(this);
        view.findViewById(R.id.textCost).setOnClickListener(this);
        view.findViewById(R.id.textVolume).setOnClickListener(this);
        view.findViewById(R.id.textPrice).setOnClickListener(this);
        view.findViewById(R.id.textCons).setOnClickListener(this);

        mSpinnerCons.setOnItemSelectedListener(this);
        mSpinnerSeason.setOnItemSelectedListener(this);

        mEditDistance.addTextChangedListener(new EditTextWatcher(mEditDistance));
        mEditCost.addTextChangedListener(new EditTextWatcher(mEditCost));
        mEditVolume.addTextChangedListener(new EditTextWatcher(mEditVolume));

        mEditPrice.addTextChangedListener(new EditTextWatcher(mEditPrice));
        mEditCons.addTextChangedListener(new EditTextWatcher(mEditCons));

        return view;
    }

    public void setDistance(int distance) {
        UtilsFormat.floatToEditText(mEditDistance, distance, false);
    }

    private void checkTextOnEmpty(EditText editText) {
        LinearLayout linearLayout;
        switch (editText.getId()) {
            case R.id.editPrice:
                linearLayout = mLayoutPriceEmpty;
                break;
            case R.id.editCons:
                linearLayout = mLayoutConsEmpty;
                break;
            default:
                return;
        }
        linearLayout.setVisibility(TextUtils.isEmpty(editText.getText()) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        mCalculating = false;
        super.onResume();
    }

    @Override
    public void onDestroy() {
        savePrefs();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        EditText edit;
        switch (v.getId()) {
            case R.id.textPrice:
                edit = mEditPrice;
                break;
            case R.id.textCons:
                edit = mEditCons;
                break;
            case R.id.textDistance:
                edit = mEditDistance;
                break;
            case R.id.textCost:
                edit = mEditCost;
                break;
            case R.id.textVolume:
                edit = mEditVolume;
                break;
            default:
                return;
        }
        Utils.showKeyboard(edit);
    }

    private void loadPrefs() {
        mEditDistance.setText(PreferenceManagerFuel.getCalcDistance());
        mEditCost.setText(PreferenceManagerFuel.getCalcCost());
        mEditVolume.setText(PreferenceManagerFuel.getCalcVolume());

        mEditPrice.setText(PreferenceManagerFuel.getCalcPrice());

        arrCons = PreferenceManagerFuel.getCalcCons();

        mSpinnerCons.setSelection(PreferenceManagerFuel.getCalcSelectedCons());
        mSpinnerSeason.setSelection(PreferenceManagerFuel.getCalcSelectedSeason());
    }

    private void savePrefs() {
        PreferenceManagerFuel.putCalc(
                mEditDistance.getText().toString(),
                mEditCost.getText().toString(),
                mEditVolume.getText().toString(),
                mEditPrice.getText().toString(),
                mSpinnerCons.getSelectedItemPosition(),
                mSpinnerSeason.getSelectedItemPosition());
    }

    private void selectConsFromSpinners() {
        UtilsFormat.floatToEditText(mEditCons,
                arrCons[mSpinnerSeason.getSelectedItemPosition()][mSpinnerCons.getSelectedItemPosition()],
                false);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectConsFromSpinners();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class EditTextWatcher implements TextWatcher {

        private final EditText mEditText;

        public EditTextWatcher(@NonNull EditText editText) {
            mEditText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            switch (mEditText.getId()) {
                case R.id.editPrice:
                    checkTextOnEmpty(mEditText);
                    doCalculate(CALC_ACTION_DISTANCE);
                    break;
                case R.id.editCost:
                    doCalculate(CALC_ACTION_COST);
                    break;
                case R.id.editCons:
                    checkTextOnEmpty(mEditText);
                case R.id.editDistance:
                    doCalculate(CALC_ACTION_DISTANCE);
                    break;
                case R.id.editVolume:
                    doCalculate(CALC_ACTION_VOLUME);
            }
        }
    }

    private void doCalculate(@CalcAction int calcAction) {

        if (mCalculating) return;

        mCalculating = true;

        try {
            float distancePerOneFuel, price, cons, distance, cost, volume;

            price = UtilsFormat.editTextToFloat(mEditPrice);
            cons = UtilsFormat.editTextToFloat(mEditCons);

            distance = UtilsFormat.editTextToFloat(mEditDistance);
            cost = UtilsFormat.editTextToFloat(mEditCost);
            volume = UtilsFormat.editTextToFloat(mEditVolume);

            if (price == 0 || cons == 0)  return;

            distancePerOneFuel = (float) (100.0 / cons);

            switch (calcAction) {
                case CALC_ACTION_DISTANCE:
                    volume = distance / distancePerOneFuel;
                    cost = price * volume;
                    UtilsFormat.floatToEditText(mEditVolume, volume, true);
                    UtilsFormat.floatToEditText(mEditCost, cost, true);
                    break;
                case CALC_ACTION_COST:
                    volume = cost / price;
                    distance = distancePerOneFuel * volume;
                    UtilsFormat.floatToEditText(mEditVolume, volume, true);
                    UtilsFormat.floatToEditText(mEditDistance, distance, true);
                    break;
                case CALC_ACTION_VOLUME:
                    cost = price * volume;
                    distance = distancePerOneFuel * volume;
                    UtilsFormat.floatToEditText(mEditCost, cost, true);
                    UtilsFormat.floatToEditText(mEditDistance, distance, true);
            }
        } finally {
            mCalculating = false;
        }
    }
}