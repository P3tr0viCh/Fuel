package ru.p3tr0vich.fuel.models

import android.provider.BaseColumns

interface DatabaseModel {

    interface Database {
        companion object {
            const val VERSION = 1

            const val NAME = "fuel.db"

            val CREATE_STATEMENT = TableFueling.CREATE_STATEMENT
        }
    }

    interface TableFueling {

        interface Columns : BaseColumns {
            companion object {
                const val ID = "_id"

                const val DATETIME = "datetime"
                const val COST = "cost"
                const val VOLUME = "volume"
                const val TOTAL = "total"
                const val CHANGED = "changed"
                const val DELETED = "deleted"

                const val YEAR = "year"
                const val MONTH = "month"

                val COLUMNS = arrayOf(BaseColumns._ID, DATETIME, COST, VOLUME, TOTAL)

                const val ID_INDEX = 0
                const val DATETIME_INDEX = 1
                const val COST_INDEX = 2
                const val VOLUME_INDEX = 3
                const val TOTAL_INDEX = 4

                val COLUMNS_WITH_DELETED = arrayOf(BaseColumns._ID, DATETIME, COST, VOLUME, TOTAL, DELETED)

                const val DELETED_INDEX = 5

                val COLUMNS_YEARS = arrayOf("strftime('%Y', " + DATETIME + "/1000, 'unixepoch', 'utc')" + Statement.AS + YEAR)

                const val YEAR_INDEX = 0

                val COLUMNS_SUM_BY_MONTHS = arrayOf("SUM($COST)", "strftime('%m', " + DATETIME + "/1000, 'unixepoch', 'utc')" + Statement.AS + MONTH)

                const val COST_SUM_INDEX = 0
                const val MONTH_INDEX = 1
            }
        }

        companion object {
            const val NAME = "fueling"

            const val CREATE_STATEMENT = "CREATE TABLE " + NAME + "(" +
                    Columns.ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, " +
                    Columns.DATETIME + " INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                    Columns.COST + " REAL DEFAULT 0, " +
                    Columns.VOLUME + " REAL DEFAULT 0, " +
                    Columns.TOTAL + " REAL DEFAULT 0, " +
                    Columns.CHANGED + " INTEGER DEFAULT " + Statement.TRUE + ", " +
                    Columns.DELETED + " INTEGER DEFAULT " + Statement.FALSE +
                    ");"
        }
    }

    interface Statement {
        companion object {
            const val AND = " AND "
            const val AS = " AS "
            const val DESC = " DESC"

            const val EQUAL = "="
            const val TRUE = 1
            const val FALSE = 0
        }
    }

    interface Where {
        companion object {
            const val RECORD_NOT_DELETED = TableFueling.Columns.DELETED + Statement.EQUAL + Statement.FALSE
            const val BETWEEN = " BETWEEN %1\$d AND %2\$d"
            const val LESS_OR_EQUAL = " <= %d"
        }
    }
}