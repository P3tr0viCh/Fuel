package ru.p3tr0vich.fuel;

class Const {
    static final String PREF_DISTANCE = "distance";
    static final String PREF_COST = "cost";
    static final String PREF_VOLUME = "volume";
    static final String PREF_PRICE = "price";

    static final String PREF_CONS = "consumption";
    static final String PREF_SEASON = "season";

    static final int TOOLBAR_SPINNER_DROPDOWN_OFFSET = 56; // TODO: Magic number

    static final String LOG_TAG = "XXX";

    enum RecordAction {ADD, UPDATE, DELETE}

    enum FilterMode {CURRENT_YEAR, ALL}

    enum CalcAction {DISTANCE, COST, VOLUME}
}
