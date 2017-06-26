package org.qiyi.basecore.filedownload;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author kangle 封装文件下载中的数据库操作
 */
public class FileDownloadDBHelper extends SQLiteOpenHelper {

    private static final String TAG = FileDownloadDBHelper.class.getSimpleName();
    private final Object lockObj = new Object();

    /**
     * 假如以后对数据库的表结构有所修改，需要修改 CURRENT_DATABASE_VERSION
     */
    private static int CURRENT_DATABASE_VERSION = 1;

    public FileDownloadDBHelper(Context context) {
        super(context, FileDownloadEntry.TABLE_NAME, null, CURRENT_DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FileDownloadEntry.SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DebugLog.v(TAG, "onUpgrade in " + TAG);
/*        db.execSQL("alter table "+ FileDownloadEntry.TABLE_NAME +" add column " + FileDownloadEntry.COLUMN_NAME_SERIALIZABLE
            + FileDownloadEntry.SPACE_SEP +FileDownloadEntry.BOLB_TYPE);*/
    }


    private static abstract class FileDownloadEntry implements BaseColumns {
        private static String TABLE_NAME = "db_file_download";

        public static final String COLUMN_NAME_ENTRY_ID = "file_download_id";
        public static final String COLUMN_NAME_SERIALIZABLE = "serializable";

        private static final String TEXT_TYPE = "TEXT";
        private static final String BOLB_TYPE = "BOLB";

        private static final String COMMA_SEP = ",";

        private static final String SPACE_SEP = " ";

        private static final String SQL_CREATE_ENTRIES = "CREATE TABLE "
                + FileDownloadEntry.TABLE_NAME + " (" + FileDownloadEntry._ID + " INTEGER PRIMARY KEY,"
                + FileDownloadEntry.COLUMN_NAME_ENTRY_ID + SPACE_SEP + TEXT_TYPE + COMMA_SEP
                + FileDownloadEntry.COLUMN_NAME_SERIALIZABLE + SPACE_SEP + BOLB_TYPE  + " )";

    }

    public void insertOrUpdate(FileDownloadStatus fileDownloadStatus, DBHelperCallback dbHelperCallback) {
        List<FileDownloadStatus> list = new ArrayList<FileDownloadStatus>();
        list.add(fileDownloadStatus);
        insertOrUpdate(list, dbHelperCallback);
    }

    public void insertOrUpdate(final List<FileDownloadStatus> list, final DBHelperCallback dbHelperCallback) {

        if(StringUtils.isEmptyList(list)){
            return;
        }

        synchronized (lockObj) {
            for (Iterator<FileDownloadStatus> iterator = list.iterator(); iterator.hasNext();) {
                FileDownloadStatus fileDownloadStatus = iterator.next();
                if(!fileDownloadStatus.needPersistant()){
                    iterator.remove();
                }
            }
        }

        DebugLog.v(TAG, "insertOrUpdate: " + list);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Thread.currentThread().setName("FileDownloadDBHelper.insertOrUpdate");
                synchronized (FileDownloadDBHelper.class) {
                    SQLiteDatabase db = null;
                    try {
                        db = FileDownloadDBHelper.this.getWritableDatabase();

                        if(db.isOpen()){

                            db.beginTransaction();
                            for (FileDownloadStatus fileDownloadStatus : list) {

                                String id = fileDownloadStatus.getId();

                                String sqlStr = "insert or replace into " + FileDownloadEntry.TABLE_NAME +" ("
                                        +FileDownloadEntry._ID + FileDownloadEntry.COMMA_SEP
                                        +FileDownloadEntry.COLUMN_NAME_ENTRY_ID + FileDownloadEntry.COMMA_SEP
                                        +FileDownloadEntry.COLUMN_NAME_SERIALIZABLE  + ") values ("
                                                + "(select "+ FileDownloadEntry._ID +" from "+ FileDownloadEntry.TABLE_NAME +" where "
                                                + FileDownloadEntry.COLUMN_NAME_ENTRY_ID +"= "+"'"+id+"'"+"),"
                                                + "'"+id+"'" +FileDownloadEntry.COMMA_SEP + " ? );";

                                SQLiteStatement insertStmt = db.compileStatement(sqlStr);
                                insertStmt.clearBindings();
                                insertStmt.bindBlob(1, getBytes(fileDownloadStatus));
                                insertStmt.execute();

//                                DebugLog.d(TAG, "insertOrUpdate: " + fileDownloadStatus);
                            }

                            db.setTransactionSuccessful();
                        }

                    } catch (Exception e) {
                        if(DebugLog.isDebug()){
                            ExceptionUtils.printStackTrace(e);
                        }
                    }finally {
                        if(db != null && db.inTransaction()){
                            try {
                                db.endTransaction();
                            } catch (Exception e) {
                                ExceptionUtils.printStackTrace(e);
                            }
                        }
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if(dbHelperCallback != null){
                    dbHelperCallback.onInsertOrUpdateCompleted();
                }
            }

        }.execute();
    }

    public void query(final DBHelperCallback dbHelperCallback) {

        new AsyncTask<Void, Void, List<FileDownloadStatus>>(){

            @Override
            protected List<FileDownloadStatus> doInBackground(Void... params) {
                Thread.currentThread().setName("FileDownloadDBHelper.query");
                List<FileDownloadStatus> list = null;

                synchronized (FileDownloadDBHelper.class) {
                    Cursor cursor = null;
                    try {

                        SQLiteDatabase db = FileDownloadDBHelper.this.getReadableDatabase();

                        if(db.isOpen()) {

                            String[] projection = {FileDownloadEntry.COLUMN_NAME_SERIALIZABLE};

                            cursor = db.query(FileDownloadEntry.TABLE_NAME, // The table to query
                                    projection, // The columns to return
                                    null, // The columns for the WHERE clause
                                    null, // The values for the WHERE clause
                                    null, // don't group the rows
                                    null, // don't filter by row groups
                                    null // The sort order
                                    );

                            list = new ArrayList<FileDownloadStatus>();

                            while (cursor.moveToNext()) {

                                FileDownloadStatus fileDownloadStatus = (FileDownloadStatus) getSerializable(cursor.getBlob(0));

                                if(fileDownloadStatus != null){
                                    list.add(fileDownloadStatus);
                                }

                                DebugLog.v(TAG, "query: " + fileDownloadStatus);

                            }
                        }

                    } catch (Exception e) {
                        if(DebugLog.isDebug()){
                            ExceptionUtils.printStackTrace(e);
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }

                return list;
            }

            @Override
            protected void onPostExecute(List<FileDownloadStatus> result) {
                if(dbHelperCallback != null && result != null){
                    dbHelperCallback.onQueryCompleted(result);
                }
            }

        }.execute();
    }

    public void delete(final List<FileDownloadStatus> list) {

        if(StringUtils.isEmptyList(list)){
            return;
        }

        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                Thread.currentThread().setName("FileDownloadDBHelper.delete");
                synchronized (lockObj) {
                    for (Iterator<FileDownloadStatus> iterator = list.iterator(); iterator.hasNext(); ) {
                        FileDownloadStatus fileDownloadStatus = iterator.next();

                        //删除文件
                        File toDeleteFile = fileDownloadStatus.getDownloadedFile();
                        if (toDeleteFile != null) {
                            File fileRenameTo = new File(toDeleteFile.getAbsolutePath() + "_to_delete");
                            toDeleteFile.renameTo(fileRenameTo);
                            if (fileRenameTo.exists()) {
                                fileRenameTo.delete();
                            }
                        }

                        if (!fileDownloadStatus.needPersistant()) {
                            iterator.remove();
                        }
                    }
                }

                synchronized (FileDownloadDBHelper.class) {
                    try {

                        SQLiteDatabase db = FileDownloadDBHelper.this.getWritableDatabase();

                        if(db.isOpen()){
                            String[] ids = new String[list.size()];

                            int i = 0;
                            for (FileDownloadStatus status : list) {

                                ids[i++] = "'" + status.getId() + "'";

                                DebugLog.v(TAG, "delete: " + status);
                            }

                            String args = TextUtils.join(", ", ids);

                            db.execSQL(String.format("DELETE FROM " + FileDownloadEntry.TABLE_NAME + " WHERE "
                                    + FileDownloadEntry.COLUMN_NAME_ENTRY_ID + " IN (%s);", args));
                        }

                    } catch (Exception e) {
                        if(DebugLog.isDebug()){
                            ExceptionUtils.printStackTrace(e);
                        }
                    }
                }

                return null;
            }}.execute();

    }

    private static byte[] getBytes(Serializable serializableObj) {
        byte[] yourBytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(serializableObj);
            yourBytes = bos.toByteArray();
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                bos.close();

            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return yourBytes;
    }

    private static Serializable getSerializable(final byte[] blob) {

        Serializable serializableObj = null;

        ByteArrayInputStream bis = new ByteArrayInputStream(blob);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            serializableObj = (Serializable)in.readObject();
        } catch (final Exception e) {
/*            InteractTool.randomReportException(new DebugLog.IGetLog() {
                @Override
                public String getLog() {
                    return "blob: " + (blob == null ? "null" : blob.length) + " " + e.getMessage();
                }
            });*/
            ExceptionUtils.printStackTrace(e);
        } finally {
            try {
                bis.close();
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }

        return serializableObj;
    }

    public interface DBHelperCallback{
        void onInsertOrUpdateCompleted();

        void onQueryCompleted(List<FileDownloadStatus> result);
    }
}

