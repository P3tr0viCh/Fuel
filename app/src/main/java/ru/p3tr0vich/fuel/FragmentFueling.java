package ru.p3tr0vich.fuel;
// TODO: Скакать при изменении если не видно

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FragmentFueling extends Fragment implements
        View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_FILTER_MODE = "KEY_FILTER_MODE";

    private static final int LOADER_LIST_ID = 0;

    private FuelingDBHelper db;
    private FuelingCursorAdapter mFuelingCursorAdapter;

    private Const.FilterMode mFilterMode;

    private ListView mListViewFueling;

    private View mListViewHeader;
    private View mListViewFooter;

    private ProgressWheel mProgressWheelFueling;

    private TextView mTextAverage;
    private TextView mTextSumCost;

    private long mSelectedId = 0; // TODO: HACK

    private FilterChangeListener mFilterChangeListener;
    private RecordChangeListener mRecordChangeListener;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(Const.LOG_TAG, "FragmentFueling -- onCreateView");

        View view = inflater.inflate(R.layout.fueling_listview, container, false);

        mListViewFueling = (ListView) view.findViewById(R.id.listViewFueling);

        mListViewHeader = inflater.inflate(R.layout.fueling_listview_header, null, false);
        mListViewFooter = inflater.inflate(R.layout.fueling_listview_footer, null, false);

        mProgressWheelFueling = (ProgressWheel) view.findViewById(R.id.progressWheelFueling);

        mTextAverage = (TextView) view.findViewById(R.id.tvAverage);
        mTextSumCost = (TextView) view.findViewById(R.id.tvSumCost);

        FloatingActionButton floatingActionButton = (FloatingActionButton) view.findViewById(R.id.floatingActionButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecordChangeListener.onRecordChange(Const.RecordAction.ADD, null);
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_FILTER_MODE, mFilterMode);

        Log.d(Const.LOG_TAG, "FragmentFueling -- onSaveInstanceState");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        db = new FuelingDBHelper(getActivity());

        String[] from = {
                FuelingDBHelper.COLUMN_DATETIME,
                FuelingDBHelper.COLUMN_COST,
                FuelingDBHelper.COLUMN_VOLUME,
                FuelingDBHelper.COLUMN_TOTAL};
        int[] to = {R.id.tvDate, R.id.tvCost, R.id.tvVolume, R.id.tvDate};

        mListViewFueling.addHeaderView(mListViewHeader, null, false);
        mListViewFueling.addFooterView(mListViewFooter, null, false);

        mFuelingCursorAdapter = new FuelingCursorAdapter(this, getActivity(), from, to);
        mListViewFueling.setAdapter(mFuelingCursorAdapter);

        if (savedInstanceState == null) {
            Log.d(Const.LOG_TAG, "FragmentFueling -- onActivityCreated: savedInstanceState == null");

            doSetFilterMode(Const.FilterMode.CURRENT_YEAR);
        } else {
            Log.d(Const.LOG_TAG, "FragmentFueling -- onActivityCreated: savedInstanceState != null");

            doSetFilterMode((Const.FilterMode) savedInstanceState.getSerializable(KEY_FILTER_MODE));
        }

        getLoaderManager().initLoader(LOADER_LIST_ID, null, this);
    }

    private void doSetFilterMode(Const.FilterMode filterMode) {
        mFilterMode = filterMode;
        mFuelingCursorAdapter.showYear = (filterMode != Const.FilterMode.CURRENT_YEAR);

        mFilterChangeListener.onFilterChange(filterMode);
    }

    public boolean setFilterMode(Const.FilterMode filterMode) {
        Log.d(Const.LOG_TAG, "FragmentFueling -- setFilterMode");
        if (mFilterMode != filterMode) {
            Log.d(Const.LOG_TAG, "FragmentFueling -- setFilterMode: mFilterMode != filterMode");

            doSetFilterMode(filterMode);

            getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);

            return false;
        } else return true;
    }

    private void checkFilterMode(FuelingRecord fuelingRecord) {
        // Вызов после добавления или изменения записи.
        // Если год в дате записи не текущий, то нужно установить фильтр -> "все записи".
        // Если год в записи текущий, то needUpdate == true.
        // Если год не текущий, то вызывается needUpdate == setFilterMode.
        // В setFilterMode проверяется текущий фильтр. Если он уже "все записи", то needUpdate == true,
        // иначе needUpdate == false и вызывается рестарт лоадер.
        // Если needUpdate == true вызывается форс лоад.

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Functions.sqliteToDate(fuelingRecord.getSQLiteDate()));

        boolean needUpdate = calendar.get(Calendar.YEAR) == Functions.getCurrentYear() || setFilterMode(Const.FilterMode.ALL);

        if (needUpdate) updateAfterChange();
    }

    static class FuelingCursorLoader extends CursorLoader {

        private final Context mContext;
        private final Const.FilterMode mFilterMode;

        public FuelingCursorLoader(Context context, Const.FilterMode filterMode) {
            super(context);
            mContext = context;
            mFilterMode = filterMode;
        }

        @Override
        public Cursor loadInBackground() {
            Log.d(Const.LOG_TAG, "FragmentFueling -- loadInBackground");

            LocalBroadcastManager.getInstance(mContext).sendBroadcast(ActivityMain.getLoadingBroadcast(true));

            FuelingDBHelper dbHelper = new FuelingDBHelper(mContext);
            dbHelper.setFilterMode(mFilterMode);
            try {
                return dbHelper.getAllCursor();
            } finally {
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(ActivityMain.getLoadingBroadcast(false));
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LIST_ID:
                return new FuelingCursorLoader(getActivity().getApplicationContext(), mFilterMode);
            default:
                return null;
        }
    }

    class VolumeAndTotal {
        final float volume;
        final float total;

        VolumeAndTotal(float v, float t) {
            volume = v;
            total = t;
        }
    }

    private void calcTotal(Cursor data) {
        float costSum = 0, volumeSum = 0;
        float volume, total, firstTotal = 0, lastTotal = 0, average;

        int averageCount;

        int columnCost = data.getColumnIndex(FuelingDBHelper.COLUMN_COST);
        int columnVolume = data.getColumnIndex(FuelingDBHelper.COLUMN_VOLUME);
        int columnTotal = data.getColumnIndex(FuelingDBHelper.COLUMN_TOTAL);

        boolean completeData = true; // Во всех записях указаны объём заправки и текущий пробег

        List<VolumeAndTotal> volumeAndTotals = new ArrayList<>();

        if (data.moveToFirst()) do {
            costSum += data.getFloat(columnCost);

            volume = data.getFloat(columnVolume);
            total = data.getFloat(columnTotal);

            volumeAndTotals.add(new VolumeAndTotal(volume, total));

            if (completeData) {
                if (volume == 0 || total == 0) completeData = false;
                else {
                    // Сортировка записей по дате в обратном порядке
                    // isFirst -- последняя заправка
                    if (data.isFirst()) lastTotal = total;
                    else {
                        // Последний (isFirst) объём заправки не нужен -- неизвестно, сколько на ней будет пробег
                        volumeSum += volume;
                        if (data.isLast()) firstTotal = total;
                    }
                }
            }
        } while (data.moveToNext());

        if (completeData)
            average = (volumeSum / (lastTotal - firstTotal)) * 100;
        else {
            average = 0;
            averageCount = 0;
            for (int i = 0; i < volumeAndTotals.size() - 1; i++) {
                lastTotal = volumeAndTotals.get(i).total;
                if (lastTotal != 0) {
                    volume = volumeAndTotals.get(i + 1).volume;
                    total = volumeAndTotals.get(i + 1).total;
                    if (volume != 0 && total != 0) {
                        average += (volume / (lastTotal - total)) * 100;
                        averageCount++;
                    }
                }
            }

            average = averageCount != 0 ? average / averageCount : 0;
        }

        mTextAverage.setText(Functions.floatToString(average));
        mTextSumCost.setText(Functions.floatToString(costSum));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_LIST_ID:
                mFuelingCursorAdapter.swapCursor(data);
                selectItemById(mSelectedId);

                Log.d(Const.LOG_TAG, "FragmentFueling -- onLoadFinished: LOADER_LIST_ID");

                calcTotal(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_LIST_ID:
                mFuelingCursorAdapter.swapCursor(null);
        }
    }

    public void updateAfterChange() {
        getLoaderManager().getLoader(LOADER_LIST_ID).forceLoad();
    }

    public void addRecord(FuelingRecord fuelingRecord) {
        mSelectedId = db.insertRecord(fuelingRecord);
        if (mSelectedId > 0) checkFilterMode(fuelingRecord);
    }

    public void updateRecord(FuelingRecord fuelingRecord) {
        if (db.updateRecord(fuelingRecord) > 0) checkFilterMode(fuelingRecord);
    }

    public void deleteRecord(FuelingRecord fuelingRecord) {
        if (db.deleteRecord(fuelingRecord) > 0)
            updateAfterChange();
    }

    private void doPopup(View v) {

        final long id = (long) v.getTag();

        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.inflate(R.menu.menu_fueling);

        Object menuHelper = null;
        try {
            Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
            fMenuHelper.setAccessible(true);
            menuHelper = fMenuHelper.get(popupMenu);
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuHelper, true);
        } catch (Exception e) {
            Log.d(Const.LOG_TAG, "Error setForceShowIcon: " + e.getMessage());
        }

        popupMenu.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        FuelingRecord fuelingRecord = db.getFuelingRecord(id);
                        switch (item.getItemId()) {
                            case R.id.action_fueling_update:
                                mRecordChangeListener.onRecordChange(Const.RecordAction.UPDATE, fuelingRecord);
                                return true;
                            case R.id.action_fueling_delete:
                                mRecordChangeListener.onRecordChange(Const.RecordAction.DELETE, fuelingRecord);
                                return true;
                            default:
                                return false;
                        }
                    }
                }
        );

        popupMenu.show();

        try {
            if (menuHelper == null) return;

            Field fListPopup = menuHelper.getClass().getDeclaredField("mPopup");
            fListPopup.setAccessible(true);
            Object listPopup = fListPopup.get(menuHelper);
            Class<?> listPopupClass = listPopup.getClass();

            // Magic number
            listPopupClass.getDeclaredMethod("setVerticalOffset", int.class).invoke(listPopup, -v.getHeight() - 8);

            listPopupClass.getDeclaredMethod("show").invoke(listPopup);
        } catch (Exception e) {
            //
        }
    }

    @Override
    public void onClick(View v) {
        doPopup(v);
    }

    private void selectItemById(long id) {
        if (mSelectedId == 0) return;
        for (int i = 0; i < mListViewFueling.getCount(); i++) {
            if (mListViewFueling.getItemIdAtPosition(i) == id) {
                mListViewFueling.setSelection(i);
                break;
            }
        }
        mSelectedId = 0;
    }

    public void setProgressBarVisible(boolean visible) {
        Functions.setProgressWheelVisible(mProgressWheelFueling, visible);
    }

    public interface FilterChangeListener {
        // to Toolbar Spinner
        void onFilterChange(Const.FilterMode filterMode);
    }

    public interface RecordChangeListener {
        void onRecordChange(Const.RecordAction recordAction, FuelingRecord fuelingRecord);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mFilterChangeListener = (FilterChangeListener) activity;
            mRecordChangeListener = (RecordChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement FilterChangeListener, RecordChangeListener");
        }
    }
}
