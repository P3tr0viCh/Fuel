package ru.p3tr0vich.fuel;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;


public class ActivityCalc extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener, View.OnClickListener {

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

    public static void start(Activity parent) {
        parent.startActivity(new Intent(parent, ActivityCalc.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);

        Toolbar toolbarCalc = (Toolbar) findViewById(R.id.toolbarCalc);
        setSupportActionBar(toolbarCalc);
        toolbarCalc.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbarCalc.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mEditDistance = (EditText) findViewById(R.id.editDistance);
        mEditCost = (EditText) findViewById(R.id.editCost);
        mEditVolume = (EditText) findViewById(R.id.editVolume);
        mEditPrice = (EditText) findViewById(R.id.editPrice);
        mEditCons = (EditText) findViewById(R.id.editCons);

        mSpinnerCons = (Spinner) findViewById(R.id.spinnerCons);
        mSpinnerSeason = (Spinner) findViewById(R.id.spinnerSeason);

        mLayoutPriceEmpty = (LinearLayout) findViewById(R.id.layoutPriceEmpty);
        mLayoutConsEmpty = (LinearLayout) findViewById(R.id.layoutConsEmpty);

        ArrayAdapter<CharSequence> adapterCons = ArrayAdapter.createFromResource(this,
                R.array.spinner_consumption, R.layout.spinner_item);
        adapterCons.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mSpinnerCons.setAdapter(adapterCons);

        ArrayAdapter<CharSequence> adapterSeason = ArrayAdapter.createFromResource(this,
                R.array.spinner_season, R.layout.spinner_item);
        adapterSeason.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mSpinnerSeason.setAdapter(adapterSeason);

        Button btnMaps = (Button) findViewById(R.id.btnMaps);
        btnMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Functions.isInternetConnected())
                    ActivityYandexMap.start(ActivityCalc.this);
                else
                    FragmentDialogMessage.showMessage(ActivityCalc.this,
                            getString(R.string.title_message_error), getString(R.string.message_error_no_internet));
            }
        });

        mCalculating = true;

        loadPrefs();

        findViewById(R.id.textDistance).setOnClickListener(this);
        findViewById(R.id.textCost).setOnClickListener(this);
        findViewById(R.id.textVolume).setOnClickListener(this);
        findViewById(R.id.textPrice).setOnClickListener(this);
        findViewById(R.id.textCons).setOnClickListener(this);

        mSpinnerCons.setOnItemSelectedListener(this);
        mSpinnerSeason.setOnItemSelectedListener(this);

        mEditDistance.addTextChangedListener(new EditTextWatcher(mEditDistance));
        mEditCost.addTextChangedListener(new EditTextWatcher(mEditCost));
        mEditVolume.addTextChangedListener(new EditTextWatcher(mEditVolume));

        mEditPrice.addTextChangedListener(new EditTextWatcher(mEditPrice));
        mEditCons.addTextChangedListener(new EditTextWatcher(mEditCons));

        checkTextOnEmpty(mEditPrice);
        checkTextOnEmpty(mEditCons);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                ActivityPreference.start(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case ActivityYandexMap.REQUEST_CODE:
                int distance = ActivityYandexMap.getDistance(data);
                mEditDistance.setText(Integer.toString(distance));
                break;
            case ActivityPreference.REQUEST_CODE:
                loadCons(null);
                selectConsFromSpinners();
        }
    }

    private void loadPrefs() {
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(this);

        mEditDistance.setText(sPref.getString(Const.PREF_DISTANCE, ""));
        mEditCost.setText(sPref.getString(Const.PREF_COST, ""));
        mEditVolume.setText(sPref.getString(Const.PREF_VOLUME, ""));

        mEditPrice.setText(sPref.getString(Const.PREF_PRICE, ""));

        loadCons(sPref);

        mSpinnerCons.setSelection(sPref.getInt(Const.PREF_CONS, 0));
        mSpinnerSeason.setSelection(sPref.getInt(Const.PREF_SEASON, 0));
    }

    private void loadCons(SharedPreferences sPref) {
        if (sPref == null) sPref = PreferenceManager.getDefaultSharedPreferences(this);

        arrCons[0][0] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_summer_city), "0"));
        arrCons[0][1] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_summer_highway), "0"));
        arrCons[0][2] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_summer_mixed), "0"));
        arrCons[1][0] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_winter_city), "0"));
        arrCons[1][1] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_winter_highway), "0"));
        arrCons[1][2] = Functions.textToFloat(sPref.getString(this.getString(R.string.pref_winter_mixed), "0"));
    }

    private void savePrefs() {
        PreferenceManager.getDefaultSharedPreferences(this)
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
        if (editText.getText().length() == 0) linearLayout.setVisibility(View.VISIBLE);
        else linearLayout.setVisibility(View.GONE);
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
                case R.id.editCost:
                    doCalculate(Const.CalcAction.COST);
                    break;
                case R.id.editCons:
                    checkTextOnEmpty(editText);
                case R.id.editDistance:
                    doCalculate(Const.CalcAction.DISTANCE);
                    break;
                case R.id.editVolume:
                    doCalculate(Const.CalcAction.VOLUME);
            }
        }
    }

    private void doCalculate(Const.CalcAction calcAction) {

        if (mCalculating) return;

        mCalculating = true;

        try {
            float distancePerOneFuel, Price, Cons, Distance, Cost, Volume;

            Price = Functions.editTextToFloat(mEditPrice);
            Cons = Functions.editTextToFloat(mEditCons);

            Distance = Functions.editTextToFloat(mEditDistance);
            Cost = Functions.editTextToFloat(mEditCost);
            Volume = Functions.editTextToFloat(mEditVolume);

            if ((Price == 0) || (Cons == 0)) {
                mCalculating = false;
                return;
            }

            distancePerOneFuel = (float) (100.0 / Cons);

            switch (calcAction) {
                case DISTANCE:
                    Volume = Distance / distancePerOneFuel;
                    Cost = Price * Volume;
                    Functions.floatToText(mEditVolume, Volume, true);
                    Functions.floatToText(mEditCost, Cost, true);
                    break;
                case COST:
                    Volume = Cost / Price;
                    Distance = distancePerOneFuel * Volume;
                    Functions.floatToText(mEditVolume, Volume, true);
                    Functions.floatToText(mEditDistance, Distance, true);
                    break;
                case VOLUME:
                    Cost = Price * Volume;
                    Distance = distancePerOneFuel * Volume;
                    Functions.floatToText(mEditCost, Cost, true);
                    Functions.floatToText(mEditDistance, Distance, true);
            }
        } finally {
            mCalculating = false;
        }
    }
}
