package ru.p3tr0vich.fuel.models;

import android.provider.BaseColumns;

public interface DatabaseModel {

    interface Database {
        int VERSION = 1;

        String NAME = "fuel.db";

        String CREATE_STATEMENT = TableFueling.CREATE_STATEMENT;
    }

    interface TableFueling {
        String NAME = "fueling";

        interface Columns extends BaseColumns {
            String DATETIME = "datetime";
            String COST = "cost";
            String VOLUME = "volume";
            String TOTAL = "total";
            String CHANGED = "changed";
            String DELETED = "deleted";

            String YEAR = "year";
            String MONTH = "month";

            String[] COLUMNS = new String[]{
                    _ID, DATETIME, COST, VOLUME, TOTAL
            };
            int _ID_INDEX = 0;
            int DATETIME_INDEX = 1;
            int COST_INDEX = 2;
            int VOLUME_INDEX = 3;
            int TOTAL_INDEX = 4;

            String[] COLUMNS_WITH_DELETED = new String[]{
                    _ID, DATETIME, COST, VOLUME, TOTAL, DELETED
            };
            int DELETED_INDEX = 5;

            String[] COLUMNS_YEARS = new String[]{
                    "strftime('%Y', " + DATETIME + "/1000, 'unixepoch', 'utc')" + Statement.AS + YEAR};
            int YEAR_INDEX = 0;

            String[] COLUMNS_SUM_BY_MONTHS = new String[]{
                    "SUM(" + COST + ")",
                    "strftime('%m', " + DATETIME + "/1000, 'unixepoch', 'utc')" + Statement.AS + MONTH};
            int COST_SUM_INDEX = 0;
            int MONTH_INDEX = 1;
        }

        String CREATE_STATEMENT = "CREATE TABLE " + NAME + "(" +
                Columns._ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
                Columns.DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                Columns.COST + " REAL DEFAULT 0, " +
                Columns.VOLUME + " REAL DEFAULT 0, " +
                Columns.TOTAL + " REAL DEFAULT 0, " +
                Columns.CHANGED + " INTEGER DEFAULT " + Statement.TRUE + ", " +
                Columns.DELETED + " INTEGER DEFAULT " + Statement.FALSE +
                ");";
    }

    interface Statement {
        String AND = " AND ";
        String AS = " AS ";
        String DESC = " DESC";

        String EQUAL = "=";
        int TRUE = 1;
        int FALSE = 0;
    }

    interface Where {
        String RECORD_NOT_DELETED = TableFueling.Columns.DELETED + Statement.EQUAL + Statement.FALSE;
        String BETWEEN = " BETWEEN %1$d AND %2$d";
        String LESS_OR_EQUAL = " <= %d";
    }
}