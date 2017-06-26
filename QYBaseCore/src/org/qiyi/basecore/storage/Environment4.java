package org.qiyi.basecore.storage;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;

import org.qiyi.android.corejar.debug.DebugLog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * 针对某些厂商通过{@link StorageManager}来扫卡
 *
 * @author yuanzeyao
 */
class Environment4 {
    private static final String TAG = "Environment4";

    public final static String TYPE_PRIMARY = "primary";
    public final static String TYPE_INTERNAL = "internal";
    public final static String TYPE_SD = "MicroSD";
    public final static String TYPE_USB = "USB";
    public final static String TYPE_UNKNOWN = "unbekannt";

    public final static String WRITE_NONE = "none";
    public final static String WRITE_READONLY = "readonly";
    public final static String WRITE_APPONLY = "apponly";
    public final static String WRITE_FULL = "readwrite";

    private static Device[] devices, externalstorage, storage;
    private static String userDir;

    /**
     * 开始扫卡
     *
     * @param context
     * @return
     */
    public static Device[] getDevices(Context context) {
        if (devices == null) initDevices(context);
        return devices;
    }

    /**
     * 获取外置设备
     *
     * @param context
     * @return
     */
    public static Device[] getExternalStorage(Context context) {
        if (devices == null) initDevices(context);
        return externalstorage;
    }


    public static Device[] getStorage(Context context) {
        if (devices == null) initDevices(context);
        return storage;
    }


    /**
     * 启动sd卡扫描逻辑
     *
     * @param context
     */
    public static void initDevices(Context context) {
        // Userverzeichnis
        if (userDir == null) userDir = "/Android/data/" + context.getPackageName();

        // Devices einlesen
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class c = sm.getClass();
        Object[] vols;
        try {
            Method m = c.getMethod("getVolumeList");
            vols = (Object[]) m.invoke(sm);
            Device[] temp = new Device[vols.length];
            for (int i = 0; i < vols.length; i++)
                temp[i] = new Device(vols[i], context);


            Device primary = null;
            for (Device d : temp) {
                if (d.mPrimary) {
                    primary = d;
                }
            }

            if (primary == null) {
                for (Device d : temp) {
                    if (!d.mRemovable) {
                        d.mPrimary = true;
                        primary = d;
                        break;
                    }
                }
            }


            if (primary == null) {
                primary = temp[0];
                primary.mPrimary = true;
            }

            try {
                File[] files = ContextCompat.getExternalFilesDirs(context, null);
                File[] caches = ContextCompat.getExternalCacheDirs(context);
                for (Device d : temp) {
                    if (files != null) for (File f : files)
                        if (f != null && f.getAbsolutePath().startsWith(d.getAbsolutePath()))
                            d.mFiles = f;
                    if (caches != null) for (File f : caches)
                        if (f != null && f.getAbsolutePath().startsWith(d.getAbsolutePath()))
                            d.mCache = f;
                }
            }catch (NoSuchMethodError e){
                // bugfix @liuchun, NoSuchMethodError at ContextCompat.getExternalFilesDirs in some devices
                DebugLog.e(TAG, "NoSuchMethodError in ContextCompat.getExternalFilesDirs");
            }

            // die drei Listen erzeugen
            ArrayList<Device> tempDev = new ArrayList<Device>(10);
            ArrayList<Device> tempStor = new ArrayList<Device>(10);
            ArrayList<Device> tempExt = new ArrayList<Device>(10);
            for (Device d : temp) {
                tempDev.add(d);
                if (d.isAvailable(context)) {
                    tempExt.add(d);
                    tempStor.add(d);
                }
            }

            Device internal = new Device(context);
            tempStor.add(0, internal); // bei Storage-Alternativen immer
            if (!primary.mEmulated) tempDev.add(0, internal); // bei Devices nur wenn zusätzlich

            // temp in devices-Tabelle übernehmen
            devices = tempDev.toArray(new Device[tempDev.size()]);
            storage = tempStor.toArray(new Device[tempStor.size()]);
            externalstorage = tempExt.toArray(new Device[tempExt.size()]);
        } catch (Exception e) {
            // Fallback auf normale Android-Funktionen
            DebugLog.e(TAG, "getVolumeList not found, fallback");
            // TODO ist noch bei keinem Testgerät vorgekommen
        }

    }


    public static class Device extends File {
        String mUserLabel, mUuid, mState, mWriteState, mType;
        boolean mPrimary, mRemovable, mEmulated, mAllowMassStorage;
        long mMaxFileSize;
        File mFiles, mCache;

        /**
         * Erzeugen aus context.getDataDirectory(), also interner Speicher
         *
         * @param context der Context der App
         */
        Device(Context context) {
            super(Environment.getDataDirectory().getAbsolutePath());
            mState = Environment.MEDIA_MOUNTED;
            mFiles = context.getFilesDir();
            mCache = context.getCacheDir();
            mType = TYPE_INTERNAL;
            mWriteState = WRITE_APPONLY;
        }


        @SuppressWarnings("NullArgumentToVariableArgMethod")
        Device(Object storage, Context context) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            super((String) storage.getClass().getMethod("getPath").invoke(storage));
            for (Method m : storage.getClass().getMethods()) {
                if (m.getName().equals("getUserLabel") && m.getParameterTypes().length == 0 && m.getReturnType() == String.class)
                    mUserLabel = (String) m.invoke(storage); // ab Android 4.4
                if (m.getName().equals("getUuid") && m.getParameterTypes().length == 0 && m.getReturnType() == String.class)
                    mUuid = (String) m.invoke(storage); // ab Android 4.4
                if (m.getName().equals("getState") && m.getParameterTypes().length == 0 && m.getReturnType() == String.class)
                    mState = (String) m.invoke(storage); // ab Android 4.4
                if (m.getName().equals("isRemovable") && m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class)
                    mRemovable = (Boolean) m.invoke(storage); // ab Android 4.0
                if (m.getName().equals("isPrimary") && m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class)
                    mPrimary = (Boolean) m.invoke(storage); // ab Android 4.2
                if (m.getName().equals("isEmulated") && m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class)
                    mEmulated = (Boolean) m.invoke(storage); // ab Android 4.0
                if (m.getName().equals("allowMassStorage") && m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class)
                    mAllowMassStorage = (Boolean) m.invoke(storage); // ab Android 4.0
                if (m.getName().equals("getMaxFileSize") && m.getParameterTypes().length == 0 && m.getReturnType() == long.class)
                    mMaxFileSize = (Long) m.invoke(storage); // ab Android 4.0
                // getDescription (ab 4.1 mit context) liefert keine sinnvollen Werte
                // getPathFile (ab 4.2) liefert keine sinnvollen Werte
                // getMtpReserveSpace (ab 4.0) für diese Zwecke unwichtig
                // getStorageId (ab 4.0) für diese Zwecke unwichtig
            }
            if (mState == null) mState = getState(context);

            if (mPrimary)
                mType = TYPE_PRIMARY;
            else {
                String n = getAbsolutePath().toLowerCase();
                if (n.indexOf("sd") > 0)
                    mType = TYPE_SD;
                else if (n.indexOf("usb") > 0)
                    mType = TYPE_USB;
                else
                    mType = TYPE_UNKNOWN + " " + getAbsolutePath();
            }
        }


        public String getType() {
            return mType;
        }


        public String getAccess() {
            if (mWriteState == null) {
                try {
                    mWriteState = WRITE_NONE;
                    File[] root = listFiles();
                    if (root == null || root.length == 0)
                        throw new IOException("root empty/unreadable");
                    mWriteState = WRITE_READONLY;
                    File t = File.createTempFile("jow", null, getFilesDir());
                    //noinspection ResultOfMethodCallIgnored
                    t.delete();
                    mWriteState = WRITE_APPONLY;
                    t = File.createTempFile("jow", null, this);
                    //noinspection ResultOfMethodCallIgnored
                    t.delete();
                    mWriteState = WRITE_FULL;
                } catch (IOException ignore) {
                    DebugLog.v(TAG, "test " + getAbsolutePath() + " ->" + mWriteState + "<- " + ignore.getMessage());
                }
            }
            return mWriteState;
        }


        public boolean isAvailable(Context context) {
            String s = getState(context);
            return (
                    Environment.MEDIA_MOUNTED.equals(s) ||
                            Environment.MEDIA_MOUNTED_READ_ONLY.equals(s)
            );
            // MEDIA_SHARED: als USB freigegeben; bitte Handy auf MTP umstellen
        }


        public String getState(Context context) {
            try {
                if (mRemovable || mState == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        // Android 5.0? Da gibts was neues
                        mState = Environment.getExternalStorageState(this);
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                        // Android 4.4? Dann dort nachfragen
                        mState = Environment.getStorageState(this);
                    else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                        // Android 4.0-4.4, StorageManager.getVolumeState(String)
                        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                        try{
                            Method m = sm.getClass().getMethod("getVolumeState", String.class);
                            m.setAccessible(true);

                            mState = (String)m.invoke(sm, getAbsolutePath());
                        }catch (Exception e){
                            // failed to reflect StorageManager.getVolumeState
                            DebugLog.w(TAG, "StorageManager-->getVolumeState not found, reflection failed");

                            if (canRead() && getTotalSpace() > 0)
                                // lesbar und Größe vorhanden => gibt es
                                mState = Environment.MEDIA_MOUNTED;
                            else if (mState == null || Environment.MEDIA_MOUNTED.equals(mState))
                                // nicht lesbar, keine Größe aber noch MOUNTED || oder ungesetzt => UNKNOWN
                                mState = EnvironmentCompat.MEDIA_UNKNOWN;
                        }
                    }
                }
            }catch (NoSuchMethodError e){
                // bugfix @liuchun, NoSuchMethodError at Environment.getStorageState in some devices
                DebugLog.e(TAG, "NoSuchMethodError in Environment.getStorageState");
                mState = EnvironmentCompat.MEDIA_UNKNOWN;
            }

            return mState;
        }

        public File getFilesDir() {
            if (mFiles == null) {
                mFiles = new File(this, userDir + "/files");
                if (!mFiles.isDirectory())
                    //noinspection ResultOfMethodCallIgnored
                    mFiles.mkdirs();
            }
            return mFiles;
        }

        public File getCacheDir() {
            if (mCache == null) {
                mCache = new File(this, userDir + "/cache");
                if (!mCache.isDirectory())
                    //noinspection ResultOfMethodCallIgnored
                    mCache.mkdirs();
            }
            return mCache;
        }


        public boolean isPrimary() {
            return mPrimary;
        }


        public boolean isRemovable() {
            return mRemovable;
        }


        public boolean isEmulated() {
            return mEmulated;
        }

        public boolean isAllowMassStorage() {
            return mAllowMassStorage;
        }

        public long getMaxFileSize() {
            return mMaxFileSize;
        }

        public String getUserLabel() {
            return mUserLabel;
        }

        public String getUuid() {
            return mUuid;
        }
    }
}
