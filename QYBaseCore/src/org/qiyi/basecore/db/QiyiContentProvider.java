package org.qiyi.basecore.db;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider for accessing all the databases
 * Created by kangle on 2015/7/11.
 */
public class QiyiContentProvider extends ContentProvider {

    public static final String LOG_CLASS_NAME = "DBAdapter";
    private static boolean sInited;

    public static String createIndex(String tblName, String fieldName) {
        return new StringBuffer().append("CREATE INDEX ").append(fieldName).append("_").append(tblName).append(" ON ").append(tblName).append("(").append(fieldName).append(");").toString();
    }

    public static String AUTHORITY = "com.qiyi.video";
    private static Map<Integer, TableInfo> sTables = new LinkedHashMap<Integer, TableInfo>();
    private static int currentTableIndex;
    private static AppAdapter mAppAdapter;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static synchronized void register(Context context, String tableName, ITable iTable) {
        if (!sInited) {
            AUTHORITY = context.getPackageName();
        }

        TableInfo tableInfo = new TableInfo(tableName, iTable);

        if (!sTables.containsValue(tableInfo)) {
            sTables.put(++currentTableIndex, tableInfo);
            sURIMatcher.addURI(AUTHORITY, "provider/" + tableName, currentTableIndex);
        }

        if (!sInited && iTable.endRegister()) {
            onCreate(context);
            sInited = true;
        }
    }

    private static void onCreate(Context context) {
        mAppAdapter = new AppAdapter(context);
        mAppAdapter.openWithWriteMethod();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;
        String tableName = sTables.get(sURIMatcher.match(uri)).tableName;
        if (tableName != null) {
            cursor = mAppAdapter.query(tableName, projection, selection, selectionArgs, sortOrder);
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        long rowID = -1;
        int updatedRowId = 0;

        TableInfo tableInfo = getTableInfo(uri);
        if (tableInfo != null) {

            String tableName = tableInfo.tableName;
            String selectionForUpdate = tableInfo.iTable.getSelectionForUpdate(values);
            String[] selectionArgsForUpdate = tableInfo.iTable.getSelectionArgsForUpdate(values);

            if (selectionForUpdate != null) {
                synchronized (tableInfo) {
                    //先尝试update
                    updatedRowId = update(uri,
                            values, selectionForUpdate, selectionArgsForUpdate);
                    //更新0项，说明要做插入
                    if (updatedRowId == 0) {
                        rowID = mAppAdapter.insert(tableName, values);
                    }
                }
            } else if (tableName != null) {
                rowID = mAppAdapter.insert(tableName, values);
            }
        }

        return ContentUris.withAppendedId(uri, (rowID == -1 && updatedRowId != 0) ? updatedRowId : rowID);
    }

    @Nullable
    private TableInfo getTableInfo(Uri uri) {
        TableInfo tableInfo = sTables.get(sURIMatcher.match(uri));
        if (DebugLog.isDebug() && tableInfo == null) {
            throw new RuntimeException("QiyiContentProvider#getTableInfo: No corresponding TableInfo");
        }
        return tableInfo;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int deletedNum;
        String tableName = sTables.get(sURIMatcher.match(uri)).tableName;
        deletedNum = mAppAdapter.delete(tableName, selection, selectionArgs);
        return deletedNum;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String tableName = sTables.get(sURIMatcher.match(uri)).tableName;
        int affected = mAppAdapter.update(tableName, values, selection, selectionArgs);
        return affected;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        final int numOperations = operations.size();
        final ContentProviderResult[] results = new ContentProviderResult[numOperations];
        mAppAdapter.beginTransaction();
        for (int i = 0; i < numOperations; i++) {
            results[i] = operations.get(i).apply(this, results, i);
        }
        mAppAdapter.setTransactionSuccessful();
        mAppAdapter.endTransaction();
        return results;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    public static Uri createUri(String tableName) {
        return Uri.parse("content://" + AUTHORITY + "/provider/" + tableName);
    }

    public static class AppAdapter {

        public static final String DB_NANE = "qyvideo.db";
        public static final int DATABASE_VERSION = 71;
        private static final String LOG_CLASS_NAME = "DBAdapter";

        private BaseDBHelper _helper;

        private Context _context;

        private SQLiteDatabase _db;

        public AppAdapter(Context context) {
            this._context = context;
            DebugLog.log(QiyiContentProvider.LOG_CLASS_NAME, "初始化DataBase目录数据库helper ");
            _helper = new BaseDBHelper(_context, DB_NANE, null, DATABASE_VERSION);
        }

        public boolean isOpen() {
            if (null == _db) {
                open(false);
            }
            return null != _db && _db.isOpen();
        }

        protected void open(boolean isRead) {
            try {
                _db = isRead ? _helper.getReadableDatabase() : _helper.getWritableDatabase();
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
                _db = null;
            }
        }

        /**
         * 开启数据库事物
         */
        protected void beginTransaction() {
            try {
                if (_db != null) {
                    _db.beginTransaction();
                }
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }

        }

        /**
         * 设置事物处理成功，不设置时会回滚
         */
        protected void setTransactionSuccessful() {
            try {
                if (_db != null) {
                    _db.setTransactionSuccessful();
                }
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }

        }

        /**
         * 事物处理完毕
         */
        protected void endTransaction() {
            try {
                if (_db != null) {
                    _db.endTransaction();
                }
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }

        }

/*    public void dropTable(String tableName) {
        if (!_db.isOpen())
            return;
        if (null != _db && null != tableName) {
            try {
                _db.execSQL("DROP TABLE IF EXISTS " + tableName);
            } catch (Exception e) {

            }
        }
    }*/

        public long insert(String tableName, ContentValues map) {
            long ret = -1;
            if (null == _db || !_db.isOpen())
                return ret;

            try {
                ret = _db.insertWithOnConflict(tableName, null, map, SQLiteDatabase.CONFLICT_REPLACE);
            } catch (Exception e) {
                DebugLog.d(QiyiContentProvider.LOG_CLASS_NAME, "Exception in insert: " + e);
                ExceptionUtils.printStackTrace(e);
            }
            return ret;
        }

        public int update(String tableName, ContentValues map, String whereClause, String[] whereArgs) {
            int ret = 0;
            if (null == _db || !_db.isOpen()) {
                return ret;
            }

            if (null != tableName && null != map && map.size() > 0) {
                try {
                    ret = _db.update(tableName, map, whereClause, whereArgs);
                } catch (Exception e) {
                    DebugLog.d(LOG_CLASS_NAME, "Exception in update: " + e);
                    ExceptionUtils.printStackTrace(e);
                }
            }
            return ret;
        }

        public int delete(String tableName, String selection, String[] selectionArgs) {
            int ret = 0;
            if (null == _db || !_db.isOpen())
                return ret;
            if (null != tableName) {
                try {
                    ret = _db.delete(tableName, selection, selectionArgs);
                } catch (Exception e) {
                    DebugLog.d(LOG_CLASS_NAME, "Exception in delete: " + e);
                    ExceptionUtils.printStackTrace(e);
                }
            }
            return ret;
        }

        public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String orderBy) {
            if (null == _db || !_db.isOpen())
                return null;
            Cursor cursor = null;
            try {
                cursor = _db.query(true, table, columns, selection, selectionArgs, null, null, orderBy, null);
            } catch (Exception e) {
                DebugLog.d(LOG_CLASS_NAME, "Exception in query: " + e);
                ExceptionUtils.printStackTrace(e);
            }
            return cursor;
        }

        public void close() {
            if (null == _db || !_db.isOpen())
                return;
            try {
                _db.close();
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }

        public class BaseDBHelper extends SQLiteOpenHelper {

            public BaseDBHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory, int version) {
                super(context, dbName, factory, version);
            }

            public void execSQL(SQLiteDatabase db, String sql) {
                execSQL(db, sql, null);
            }

            public void execSQL(SQLiteDatabase db, String sql, String indexSql) {
                if (null == db)
                    return;
                if (!StringUtils.isEmpty(sql))
                    db.execSQL(sql);
                if (!StringUtils.isEmpty(indexSql))
                    db.execSQL(indexSql);
                DebugLog.log(LOG_CLASS_NAME, "BaseDBHelper exec sql:" + sql + (!StringUtils.isEmpty(indexSql) ? "\n" + indexSql : ""));
            }

            @Override
            public void onOpen(final SQLiteDatabase db) {
                super.onOpen(db);
            }

            /**
             * 在这里新加的table记得一定要在onUpgrade中也加一下
             */
            @Override
            public void onCreate(SQLiteDatabase db) {
                DebugLog.log(LOG_CLASS_NAME, "BaseDBHelper onCreate start...");
                for (Map.Entry<Integer, TableInfo> entry : sTables.entrySet()) {
                    entry.getValue().iTable.onCreate(db, this);
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                DebugLog.log(QiyiContentProvider.LOG_CLASS_NAME, "BaseDBHelper onUpgrade from version " + oldVersion + " to " + newVersion);
                for (Map.Entry<Integer, TableInfo> entry : sTables.entrySet()) {
                    entry.getValue().iTable.onUpgrade(db, oldVersion, newVersion, this);
                }
            }
        }

        /**
         * 检查table中是否存在某一column
         *
         * @param inDatabase
         * @param inTable
         * @param columnToCheck
         * @return
         */
        public static boolean existsColumnInTable(SQLiteDatabase inDatabase, String inTable, String columnToCheck) {
            Cursor mCursor = null;
            try {
                // Query 1 row
                mCursor = inDatabase.rawQuery("SELECT * FROM " + inTable + " LIMIT 0", null);

                // getColumnIndex() gives us the index (0 to ...) of the column - otherwise we get a -1
                if (mCursor.getColumnIndex(columnToCheck) != -1)
                    return true;
                else
                    return false;

            } catch (Exception Exp) {
                // Something went wrong. Missing the database? The table?
                //Log.d("... - existsColumnInTable", "When checking whether a column exists in the table, an error occurred: " + Exp.getMessage());
                return false;
            } finally {
                if (mCursor != null) mCursor.close();
            }
        }

        /**
         * 获取当前数据库版本号
         */
/*    public int getDbVersion() {
        int version = 0;
        DebugLog.log(LOG_CLASS_NAME, "dabaseVersion = " + _db.getVersion());
        version = _db.getVersion();
        return version;
    }*/
        public void openWithWriteMethod() {
            open(false);
        }

        public void release(Cursor cursor) {
            release(cursor, true);
        }

        public void release(Cursor cursor, boolean closeDb) {
            if (null != cursor && !cursor.isClosed()) {
                cursor.close();
            }
            if (closeDb)
                close();
        }
    }

    /**
     * 封装业务数据库表信息
     */
    private static class TableInfo {

        /**
         * 表名称
         */
        private final String tableName;

        /**
         * 业务数据库表与ContentProvider的交互
         */
        public ITable iTable;

        public TableInfo(String tableName, ITable iTable) {
            this.tableName = tableName;
            this.iTable = iTable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TableInfo tableInfo = (TableInfo) o;

            return tableName.equals(tableInfo.tableName);
        }

        @Override
        public int hashCode() {
            return tableName.hashCode();
        }
    }

    /**
     * 封装业务数据库表与ContentProvider的交互行为
     */
    public interface ITable {

        /**
         * @param values
         * @return 做更新操作的Selection
         */
        String getSelectionForUpdate(ContentValues values);

        /**
         * @param values
         * @return 做更新操作的SelectionArgs
         */
        String[] getSelectionArgsForUpdate(ContentValues values);


        /**
         * 将{@link SQLiteOpenHelper#onUpgrade(SQLiteDatabase, int, int)}代理给子类处理
         * 为了不影响其他数据库表的更新，实现的时候最好加上try-catch
         *
         * @param db
         * @param oldVersion
         * @param newVersion
         * @param baseDBHelper
         */
        void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion, AppAdapter.BaseDBHelper baseDBHelper);

        /**
         * 将{@link SQLiteOpenHelper#onCreate(SQLiteDatabase)}代理给子类处理
         *
         * @param db
         * @param baseDBHelper
         */
        void onCreate(SQLiteDatabase db, AppAdapter.BaseDBHelper baseDBHelper);

        /**
         * 最后一个注册数据库的Operator需要返回true
         *
         * @return 是否结束注册（开始初始化数据库）
         */
        boolean endRegister();
    }
}
