package ru.p3tr0vich.fuel;

class Const {
    static final String PREF_DISTANCE = "distance";
    static final String PREF_COST = "cost";
    static final String PREF_VOLUME = "volume";
    static final String PREF_PRICE = "price";

    static final String PREF_CONS = "consumption";
    static final String PREF_SEASON = "season";

    enum RecordAction {ADD, UPDATE, DELETE}

    enum FilterMode {CURRENT_YEAR, ALL}

    enum CalcAction {DISTANCE, COST, VOLUME}
}
