package ru.p3tr0vich.fuel;

class Const {
    static final String PREF_DISTANCE = "distance";
    static final String PREF_COST = "cost";
    static final String PREF_VOLUME = "volume";
    static final String PREF_PRICE = "price";

    static final String PREF_CONS = "consumption";
    static final String PREF_SEASON = "season";

    static final int TOOLBAR_SPINNER_DROPDOWN_OFFSET = 56; // Magic number

    static final int ANIMATION_DURATION_TOOLBAR = 400;
    static final int ANIMATION_DURATION_TOOLBAR_SHADOW = 100;
    static final int ANIMATION_CHART = 600;

    static final String LOG_TAG = "XXX";

    enum RecordAction {ADD, UPDATE, DELETE}

    enum CalcAction {DISTANCE, COST, VOLUME}
}
