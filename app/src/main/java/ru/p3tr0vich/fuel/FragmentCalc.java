package ru.p3tr0vich.fuel;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class FragmentCalc extends FragmentFuel implements
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

    private final float[][] arrCons = {{0, 0, 0}, {0, 0, 0}};

    private enum CalcAction {DISTANCE, COST, VOLUME}

    @Override
    protected int getFragmentId() {
        return R.id.action_calc;
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

        view.findViewById(R.id.btnMaps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Functions.isInternetConnected())
                    ActivityYandexMap.start(getActivity());
                else
                    FragmentDialogMessage.showMessage(getActivity(),
                            getString(R.string.title_message_error), getString(R.string.message_error_no_internet));
            }
        });

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
        mEditDistance.setText(Integer.toString(distance));
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
        Functions.showKeyboard(edit);
    }

    private void loadPrefs() {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mEditDistance.setText(sPref.getString(Const.PREF_DISTANCE, ""));
        mEditCost.setText(sPref.getString(Const.PREF_COST, ""));
        mEditVolume.setText(sPref.getString(Const.PREF_VOLUME, ""));

        mEditPrice.setText(sPref.getString(Const.PREF_PRICE, ""));

        loadCons(sPref);

        mSpinnerCons.setSelection(sPref.getInt(Const.PREF_CONS, 0));
        mSpinnerSeason.setSelection(sPref.getInt(Const.PREF_SEASON, 0));
    }

    private void loadCons(SharedPreferences sPref) {
        if (sPref == null) sPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        arrCons[0][0] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_summer_city), "0"));
        arrCons[0][1] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_summer_highway), "0"));
        arrCons[0][2] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_summer_mixed), "0"));
        arrCons[1][0] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_winter_city), "0"));
        arrCons[1][1] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_winter_highway), "0"));
        arrCons[1][2] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_winter_mixed), "0"));
    }

    private void savePrefs() {
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()

                .putString(Const.PREF_DISTANCE, mEditDistance.getText().toString())
                .putString(Const.PREF_COST, mEditCost.getText().toString())
                .putString(Const.PREF_VOLUME, mEditVolume.getText().toString())

                .putString(Const.PREF_PRICE, mEditPrice.getText().toString())

                .putInt(Const.PREF_CONS, mSpinnerCons.getSelectedItemPosition())
                .putInt(Const.PREF_SEASON, mSpinnerSeason.getSelectedItemPosition())

                .apply();
    }

    private void selectConsFromSpinners() {
        Functions.floatToText(mEditCons,
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
        final EditText editText;

        public EditTextWatcher(EditText e) {
            this.editText = e;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            switch (editText.getId()) {
                case R.id.editPrice:
                    checkTextOnEmpty(editText);
                    doCalculate(CalcAction.DISTANCE);
                    break;
                case R.id.editCost:
                    doCalculate(CalcAction.COST);
                    break;
                case R.id.editCons:
                    checkTextOnEmpty(editText);
                case R.id.editDistance:
                    doCalculate(CalcAction.DISTANCE);
                    break;
                case R.id.editVolume:
                    doCalculate(CalcAction.VOLUME);
            }
        }
    }

    private void doCalculate(CalcAction calcAction) {

        if (mCalculating) return;

        mCalculating = true;

        try {
            float distancePerOneFuel, price, cons, distance, cost, volume;

            price = Functions.editTextToFloat(mEditPrice);
            cons = Functions.editTextToFloat(mEditCons);

            distance = Functions.editTextToFloat(mEditDistance);
            cost = Functions.editTextToFloat(mEditCost);
            volume = Functions.editTextToFloat(mEditVolume);

            if (price == 0 || cons == 0) {
                mCalculating = false;
                return;
            }

            distancePerOneFuel = (float) (100.0 / cons);

            switch (calcAction) {
                case DISTANCE:
                    volume = distance / distancePerOneFuel;
                    cost = price * volume;
                    Functions.floatToText(mEditVolume, volume, true);
                    Functions.floatToText(mEditCost, cost, true);
                    break;
                case COST:
                    volume = cost / price;
                    distance = distancePerOneFuel * volume;
                    Functions.floatToText(mEditVolume, volume, true);
                    Functions.floatToText(mEditDistance, distance, true);
                    break;
                case VOLUME:
                    cost = price * volume;
                    distance = distancePerOneFuel * volume;
                    Functions.floatToText(mEditCost, cost, true);
                    Functions.floatToText(mEditDistance, distance, true);
            }
        } finally {
            mCalculating = false;
        }
    }
}
