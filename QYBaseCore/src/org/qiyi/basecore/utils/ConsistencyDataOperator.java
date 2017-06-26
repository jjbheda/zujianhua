package org.qiyi.basecore.utils;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.db.QiyiContentProvider;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by niejunjiang on 2017/3/14.
 */

public class ConsistencyDataOperator implements QiyiContentProvider.ITable {
    public static final String TAG = "ConsistencyDataOperator";
    /**
     * 访问该数据表下的内容，需要配置一下PREMISSION，保证某些配置数据给垂直业务独立apk使用事数据安全性问题
     */
    private static final String PERMISSION = "com.qiyi.video.SharedPreference_tabl.PERMISSION";

    private static final String TABLE_NAME = "SharedPreference_tabl";

    private static final String[] TABLE_COLUMNS = {"id", "key", "value"};

    private static final String CREATE_TABLE_SQL = new StringBuffer()
            .append("create table if not exists ")
            .append(TABLE_NAME).append("(")
            .append(TABLE_COLUMNS[0]).append(" integer primary key, ")
            .append(TABLE_COLUMNS[1]).append(" text, ")
            .append(TABLE_COLUMNS[2]).append(" text); ")
            .toString();

    public static ConsistencyDataOperator instance;

    private static final ConcurrentHashMap<String, ConsistencyContentObserver> mContentObserverMap
            = new ConcurrentHashMap<String, ConsistencyContentObserver>();

    private Handler mHandler = new Handler(Looper.myLooper() == null ? Looper.getMainLooper() : Looper.myLooper());
    private Context mContext;

    public ConsistencyDataOperator(Context context) {
        if (context != null) {
            this.mContext = context.getApplicationContext();
            QiyiContentProvider.register(context.getApplicationContext(), TABLE_NAME, this);
        }
    }

    public String get(String key, String defaultVale) {
        if (!TextUtils.isEmpty(key)) {
            Cursor cursor = null;
            try {
                String where = TABLE_COLUMNS[1] + "='" + key + "'";
                cursor = mContext.getContentResolver().query(QiyiContentProvider.createUri(TABLE_NAME), TABLE_COLUMNS, where, null, TABLE_COLUMNS[0] + " desc limit 1");
                cursor.moveToFirst();
                defaultVale = cursor2Value(cursor, defaultVale);
                DebugLog.v(TAG, "get:" + key + " success");
            } catch (Exception e) {
                DebugLog.d(TAG, "get failed");
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return defaultVale;
    }

    public int put(String key, String value) {
        int affect = 0;
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
            try {
                ContentValues contentValues = toContentValue(key, value);
                if (contentValues != null) {
                    ContentProviderOperation operation = ContentProviderOperation.newInsert(QiyiContentProvider.createUri(TABLE_NAME)).
                            withValues(contentValues).build();
                    ArrayList<ContentProviderOperation> list = new ArrayList<ContentProviderOperation>();
                    list.add(operation);
                    ContentProviderResult[] results = mContext.getContentResolver().applyBatch(QiyiContentProvider.AUTHORITY, list);
                    if (results != null) {
                        affect = results.length;
                    }
                    mContext.getContentResolver().notifyChange(QiyiContentProvider.createUri(TABLE_NAME + "/" + key), null);
                    DebugLog.v(TAG, "put:" + key + " success");
                }
            } catch (Exception e) {
                DebugLog.d(TAG, "put failed");
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return affect;
    }

    public int put(Map<String, String> keyValues) {
        int affect = 0;
        if (keyValues != null) {
            try {
                ArrayList<ContentProviderOperation> list = new ArrayList<ContentProviderOperation>();
               /* for (String key : keyValues.keySet()) {
                    ContentValues contentValues = toContentValue(key, keyValues.get(key));
                    if (contentValues != null) {
                        ContentProviderOperation operation = ContentProviderOperation.newInsert(QiyiContentProvider.createUri(TABLE_NAME)).
                                withValues(contentValues).build();
                        list.add(operation);
                    }
                }*/

                for (Map.Entry<String,String> entry : keyValues.entrySet()) {
                    ContentValues contentValues = toContentValue(entry.getKey(), entry.getValue());
                    if (contentValues != null) {
                        ContentProviderOperation operation = ContentProviderOperation.newInsert(QiyiContentProvider.createUri(TABLE_NAME)).
                                withValues(contentValues).build();
                        list.add(operation);
                    }
                }

                ContentProviderResult[] results = mContext.getContentResolver().applyBatch(QiyiContentProvider.AUTHORITY, list);
                if (results != null) {
                    affect = results.length;
                }
                for (String key : keyValues.keySet()) {
                    mContext.getContentResolver().notifyChange(QiyiContentProvider.createUri(TABLE_NAME + "/" + key), null);
                    DebugLog.v(TAG, "put:" + key + " success");
                }
            } catch (Exception e) {
                DebugLog.d(TAG, "put failed");
                if (DebugLog.isDebug()) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return affect;
    }

    public int reMove(String key) {
        int affect = 0;
        try {
            if (!TextUtils.isEmpty(key)) {
                String where = TABLE_COLUMNS[1] + "='" + key + "'";
                affect = mContext.getContentResolver().delete(QiyiContentProvider.createUri(TABLE_NAME), where, null);
                mContext.getContentResolver().notifyChange(QiyiContentProvider.createUri(TABLE_NAME + "/" + key), null);
                DebugLog.v(TAG, "remove:" + key + " success");
            }
        } catch (Exception e) {
            DebugLog.d(TAG, "remove failed");
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return affect;
    }

    private String cursor2Value(Cursor cursor, String defaultValue) {
        String ret = defaultValue;
        try {
            ret = cursor.getString(cursor.getColumnIndex(TABLE_COLUMNS[2]));
        } catch (Exception e) {
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
            }
            DebugLog.v(TAG, "no record");
        }
        return ret;
    }

    private ContentValues toContentValue(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(TABLE_COLUMNS[1], key);
        values.put(TABLE_COLUMNS[2], value);
        return values;
    }

    @Override
    public String getSelectionForUpdate(ContentValues values) {
        return TABLE_COLUMNS[1] + "=?";
    }

    @Override
    public String[] getSelectionArgsForUpdate(ContentValues values) {
        return new String[]{(String) values.get(TABLE_COLUMNS[1])};
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion, QiyiContentProvider.AppAdapter.BaseDBHelper baseDBHelper) {
        if (oldVersion <= 68) {
            baseDBHelper.execSQL(db, CREATE_TABLE_SQL);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db, QiyiContentProvider.AppAdapter.BaseDBHelper baseDBHelper) {
        baseDBHelper.execSQL(db, CREATE_TABLE_SQL);
    }

    @Override
    public boolean endRegister() {
        return false;
    }

    public void registerContentObserver(final String observerKey, final ConsistencyContentObserver.ICrossProcessDataChangeListener listener) {
        if (listener != null && !TextUtils.isEmpty(observerKey)) {
            ConsistencyContentObserver observer = mContentObserverMap.get(observerKey);
            if (observer == null) {
                DebugLog.v(TAG, "observer == null");
                observer = new ConsistencyContentObserver(observerKey, mHandler);
                observer.addListener(listener);
                mContentObserverMap.put(observerKey, observer);
                mContext.getContentResolver().registerContentObserver(QiyiContentProvider.createUri(TABLE_NAME + "/" + observerKey), false, observer);
            } else {
                observer.addListener(listener);
                DebugLog.v(TAG, "observer != null");
            }
        }
    }

    public void unregisterContentObserver(String observerKey) {
        if (!TextUtils.isEmpty(observerKey)) {
            ConsistencyContentObserver observer = mContentObserverMap.get(observerKey);
            if (observer != null) {
                DebugLog.v(TAG, "unregisterContentObserver:" + observerKey);
                mContext.getContentResolver().unregisterContentObserver(observer);
                mContentObserverMap.remove(observerKey);
                observer.clearListener();
            } else {
                DebugLog.v(TAG, "unregisterContentObserver:" + null);
            }
        }
    }

    public void unregisterContentObserver(String observerKey, ConsistencyContentObserver.ICrossProcessDataChangeListener listener) {
        if (!TextUtils.isEmpty(observerKey)) {
            ConsistencyContentObserver observer = mContentObserverMap.get(observerKey);
            if (observer != null) {
                DebugLog.v(TAG, "unregisterContentObserver:" + observerKey);
                observer.removeListener(listener);
            }
        }
    }
}
