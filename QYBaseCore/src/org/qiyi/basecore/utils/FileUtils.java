package org.qiyi.basecore.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import org.json.JSONObject;
import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.storage.StorageCheckor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPInputStream;

public class FileUtils {
    // All locks for read and write
    private static ConcurrentMap<String, ReentrantReadWriteLock> sCurrentLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();

    private static String TAG = FileUtils.class.toString();

    public final static String FILE_EXTENSION_SEPARATOR = ".";

    /**
     * 获取对应参数在System CacheDir目录下创建的dirName文件夹下的fileName文件 System
     * CacheDir/dirName/fileName
     *
     * @param context
     * @param dirName
     * @param fileName
     * @return
     */
    public static File getFile(Context context, String dirName, String fileName) {
        if (context == null) {
            return null;
        }
        File cacheDir = new File(context.getCacheDir(), dirName);
        return new File(cacheDir, fileName);
    }

    /**
     * 文本文件转换为指定编码的字符串
     *
     * @param file     文本文件
     * @param encoding 编码类型
     * @return 转换后的字符串
     */
    public static String file2String(File file, String encoding) {
        String result = "";
        if (file == null || !file.exists()) {
            return result;
        }
        InputStreamReader reader = null;
        StringWriter writer = new StringWriter();

        ReentrantReadWriteLock lock;
        //理论上这里不应该出现NullPointerException，但是确实统计出有
        try {
            lock = createOrGetLock(file.getAbsolutePath());
            lock.readLock().lock();
        } catch (Exception e) {
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
                throw new RuntimeException(e);
            }
            return result;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            if (TextUtils.isEmpty(encoding)) {
                reader = new InputStreamReader(fis);
            } else {
                reader = new InputStreamReader(fis,encoding);
            }
            // 将输入流写入输出流
            char[] buffer = new char[4096];
            int n = 0;
            while (-1 != (n = reader.read(buffer))) {
                writer.write(buffer, 0, n);
            }
            result = writer.toString();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (reader != null) {
                silentlyCloseCloseable(reader);
            }
            if (writer != null) {
                silentlyCloseCloseable(writer);
            }
            if( fis != null) {
                silentlyCloseCloseable(fis);
            }

            lock.readLock().unlock();
            tryToRemoveLock(file.getAbsolutePath());
        }
        return result;
    }

    /**
     * 重命名文件
     *
     * @param oldFilePath 旧路径
     * @param newFilePath 新路径
     * @param overwrite   是否自动覆盖 true则会覆盖
     * @return 成功标记
     */
    public static boolean renameFile(String oldFilePath, String newFilePath, boolean overwrite) {
        if (StringUtils.isEmpty(oldFilePath) || StringUtils.isEmpty(newFilePath)) {
            return false;
        }
        return renameFile(new File(oldFilePath), new File(newFilePath), overwrite);
    }

    /**
     * 重命名文件
     *
     * @param oldFile   旧文件
     * @param newFile   新文件
     * @param overwrite 是否自动覆盖 true则会覆盖
     * @return 成功标记
     */
    public static boolean renameFile(File oldFile, File newFile, boolean overwrite) {
        boolean result = true;
        ReentrantReadWriteLock lock = createOrGetLock(newFile.getPath());
        lock.writeLock().lock();
        try {
            if (!newFile.getParentFile().exists()) {
                result = newFile.getParentFile().mkdirs();
            }
            if (overwrite && newFile.exists()) {
                result &= newFile.delete();
            }
            result &= oldFile.renameTo(newFile);
        } catch (Exception e) {
            // do nothing
        } finally {
            lock.writeLock().unlock();
            tryToRemoveLock(newFile.getPath());
        }
        return result;
    }

    /**
     * 将字符串写入指定文件(当指定的父路径中文件夹不存在时，会最大限度去创建，以保证保存成功！)
     *
     * @param res      原字符串
     * @param filePath 文件路径
     * @return 成功标记
     */
    public static boolean string2File(String res, String filePath) {
        return string2File(res, filePath, false);
    }

    public static boolean string2File(String res, String filePath, boolean append) {
        boolean flag = true;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;

        if (res == null) {
            return false;
        }

        ReentrantReadWriteLock lock = createOrGetLock(filePath);
        lock.writeLock().lock();

        FileWriter fileWriter = null;
        try {
            File distFile = new File(filePath);
            if (!distFile.getParentFile().exists()) {
                distFile.getParentFile().mkdirs();
            }
            fileWriter = new FileWriter(distFile, append);
            bufferedReader = new BufferedReader(new StringReader(res));
            bufferedWriter = new BufferedWriter(fileWriter);
            char buf[] = new char[1024 * 4]; // 字符缓冲区
            int len;
            while ((len = bufferedReader.read(buf)) != -1) {
                bufferedWriter.write(buf, 0, len);
            }
            bufferedWriter.flush();
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
            flag = false;
        } finally {
            if (bufferedReader != null) {
                silentlyCloseCloseable(bufferedReader);
            }
            if (bufferedWriter != null) {
                silentlyCloseCloseable(bufferedWriter);
            }
            if(fileWriter != null) {
                silentlyCloseCloseable(fileWriter);
            }

            lock.writeLock().unlock();
            tryToRemoveLock(filePath);
        }
        return flag;
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 成功标记
     */
   /* public static boolean deleteFile(Context mContext, String filePath, String fileName) {
        String fileP = FileUtils.getFile(mContext, filePath, fileName).getPath();
        ReentrantReadWriteLock lock = createOrGetLock(fileP);
        lock.writeLock().lock();
        try {
            File file = FileUtils.getFile(mContext, filePath, fileName);
            DebugLog.v("CrashHandler", "file.delete() " + file.exists());
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {

        } finally {
            lock.writeLock().unlock();
            tryToRemoveLock(filePath);
        }
        return true;
    }*/



    /**
     * Get or create ReentrantReadWriteLock for given file path
     *
     * @param path fully file path
     * @return lock for the file under this path
     */
    private static ReentrantReadWriteLock createOrGetLock(String path) {
        synchronized (sCurrentLocks) {
            if (sCurrentLocks.containsKey(path)) {
                return sCurrentLocks.get(path);
            } else {
                if (!sCurrentLocks.containsKey(path)) {
                    sCurrentLocks.put(path, new ReentrantReadWriteLock());
                }
                return sCurrentLocks.get(path);
            }
        }
    }

    /**
     * Try to release ReentrantReadWriteLock by judge lock's hold count
     *
     * @param path the file path for this lock
     */
    private static void tryToRemoveLock(String path) {
        synchronized (sCurrentLocks) {
            if (sCurrentLocks.containsKey(path)) {
                ReentrantReadWriteLock lock = sCurrentLocks.get(path);
                if (!lock.hasQueuedThreads() && lock.getReadHoldCount() == 0 && lock.getWriteHoldCount() == 0) {
                    sCurrentLocks.remove(path);
                }
            }
        }

    }

    /**
     * 将byte[]写到本地文件
     *
     * @param bytes
     * @param filePath
     * @return
     */
    public static boolean bytesToFile(byte[] bytes, String filePath) {

        boolean ret = true;
        BufferedOutputStream bos = null;
        File file = new File(filePath);

        ReentrantReadWriteLock lock;
        //理论上这里不应该出现NullPointerException，但是确实统计出有
        try {
            lock = createOrGetLock(filePath);
            lock.writeLock().lock();
        } catch (Exception e) {
            if (DebugLog.isDebug()) {
                ExceptionUtils.printStackTrace(e);
                throw new RuntimeException(e);
            }
            return false;
        }
        FileOutputStream fis = null;
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            fis = new FileOutputStream(file);
            bos = new BufferedOutputStream(fis);
            bos.write(bytes);
            bos.flush();
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
            ret = false;
        } finally {

            if (bos != null) {
                silentlyCloseCloseable(bos);
            }

            if(fis != null) {
                silentlyCloseCloseable(fis);
            }

            lock.writeLock().unlock();
            tryToRemoveLock(filePath);
        }

        return ret;
    }

    /**
     * 将文件转成bytes[]
     *
     * @param file
     * @return
     */
    public static byte[] fileToBytes(File file) {

        if (file == null || !file.exists()) {
            return null;
        }

        ReentrantReadWriteLock lock = createOrGetLock(file.getAbsolutePath());
        lock.readLock().lock();

        BufferedInputStream buf = null;
        byte[] bytes = new byte[(int) file.length()];

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            buf = new BufferedInputStream(fis);
            buf.read(bytes, 0, bytes.length);
        } catch (Exception e) {
            bytes = null;
            ExceptionUtils.printStackTrace(e);
        } finally {

            if (buf != null) {
                silentlyCloseCloseable(buf);
            }

            if(fis != null) {
                silentlyCloseCloseable(fis);
            }

            lock.readLock().unlock();
            tryToRemoveLock(file.getAbsolutePath());
        }

        return bytes;
    }

    /**
     * Copy data from a source to destFile. Return true if succeed, return false if failed.
     *
     * @param sourceFile source file
     * @param destFile   destFile
     * @return success return true
     */
    public static boolean copyToFile(File sourceFile, File destFile) {
        DebugLog.v(TAG, "CopyToFile from " + sourceFile + " to " + destFile);
        if (sourceFile == null || destFile == null || !sourceFile.exists()) {
            return false;
        }

        InputStream inputStream = null;
        FileOutputStream out = null;
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            inputStream = new FileInputStream(sourceFile);
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096]; // SUPPRESS CHECKSTYLE
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
            DebugLog.v(TAG, "Copy " + sourceFile + " success!");
            return true;
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
            DebugLog.w(TAG, "Copy " + sourceFile + " failed!");
            return false;
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.getFD().sync();
                }
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }

            if (out != null) {
                silentlyCloseCloseable(out);
            }
            if (inputStream != null) {
                silentlyCloseCloseable(inputStream);
            }
        }
    }

    /**
     * 从本地 row 取得数据
     *
     * @return
     */
    public static String readFileFromRow(Context context, String filename) {
        InputStream is = null;
        String ret = null;
        int num = -1;
        try {
            is = context.getResources().openRawResource(ResourcesTool.getResourceIdForRaw(filename));
            byte[] buffer = new byte[is.available()];
            num = is.read(buffer);
            DebugLog.log(TAG,"read num = " + num);
            ret = new String(buffer);
        } catch (Exception e) {
            DebugLog.d("HomePageDataController", "readDataFromLocalFile e :" + e);
        } finally {
           if(is != null) {
               silentlyCloseCloseable(is);
           }
        }
        return ret;
    }

    /**
     * 从row 里面读取gzip压缩，string数据
     *
     * @param context
     * @param rowName
     * @return
     */
    public static String readGzipDataFromRowFile(Context context, String rowName) {
        byte[] bytes = readGzipFromRowFile(context, rowName);
        if (bytes != null && bytes.length > 0) {
            return new String(bytes);
        } else {
            return null;
        }
    }

    /**
     * 从row 里面读取gzip压缩，string数据
     *
     * @param context
     * @param rowName
     * @return
     */
    public static byte[] readGzipFromRowFile(Context context, String rowName) {
        long time = 0;
        if (DebugLog.isDebug()) {
            time = SystemClock.uptimeMillis();
        }
        InputStream is = null;
        byte[] ret = null;
        GZIPInputStream zipInputStream = null;
        try {
            is = context.getResources().openRawResource(ResourcesTool.getResourceIdForRaw(rowName));
            zipInputStream = new GZIPInputStream(is);
            byte[] buffer = new byte[8 * 1024];
            int len;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while ((len = zipInputStream.read(buffer)) != -1) {
                bout.write(buffer, 0, len);
            }
            bout.flush();
            ret = bout.toByteArray();
            DebugLog.e(TAG, "readGzipDataFromRowFile cost time  :" + (SystemClock.uptimeMillis() - time));
        } catch (Exception e) {
            DebugLog.e(TAG, "readGzipDataFromRowFile e :" + e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return ret;
    }

    public static byte[] File2byte(String filePath) {
        byte[] buffer = null;
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            File file = new File(filePath);
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            buffer = bos.toByteArray();
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (fis != null) {
                silentlyCloseCloseable(fis);
            }
            if (bos != null) {
                silentlyCloseCloseable(bos);
            }
        }
        return buffer;
    }

    /**
     * Closes closeable and swallows IOException.
     *
     * @param closeable
     */
    public static void silentlyCloseCloseable(@NonNull Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        }
    }

    /**
     * TODO KANGLE 只有在非internal storage中的apk才能安装
     *
     * @param context
     * @param apkFile
     */
    public static void installApkFile(Context context, File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(FileUtils.getFileProviderUriFormFile(context, apkFile), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    /**
     * 通过FileProvider获取文件的Uri，以content://开头
     *
     * @param ctx
     * @param file
     * @return
     */
    public static Uri getFileProviderUriFormFile(Context ctx, File file) {

        if (file == null || !file.exists()) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                String s=ctx.getPackageName();
                s+=".fileprovider";
                return FileProvider.getUriForFile(ctx, s, file);
            } catch (IllegalArgumentException e) {
                ExceptionUtils.printStackTrace(e);
                return Uri.fromFile(file);
            }
        } else {
            return Uri.fromFile(file);
        }
    }

    /**
     * 通过FileProvider获取文件的Uri，以content://开头
     *
     * @param ctx
     * @param pathName
     * @return
     */
    public static Uri getFileProviderUriFormPathName(Context ctx, String pathName) {

        if (StringUtils.isEmpty(pathName)) {
            return null;
        }
        if(Build.VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(ctx, "com.qiyi.video.fileprovider", new File(pathName));
        }else {
            return Uri.fromFile(new File(pathName));
        }
    }

    /**
     * 申请外部应用访问本应用文件Uri的权限
     *
     * @param ctx
     * @param intent
     * @param avatarUri
     */
    public static void applyUriPermission(Context ctx, Intent intent, Uri avatarUri) {
        List<ResolveInfo> resInfoList = ctx.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            ctx.grantUriPermission(packageName, avatarUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    public static String getFileNameWithoutExtension(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }

        int extenPosi = filePath.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        int filePosi = filePath.lastIndexOf(File.separator);
        if (filePosi == -1) {
            return (extenPosi == -1 ? filePath : filePath.substring(0, extenPosi));
        }
        if (extenPosi == -1) {
            return filePath.substring(filePosi + 1);
        }
        return (filePosi < extenPosi ? filePath.substring(filePosi + 1, extenPosi) : filePath.substring(filePosi + 1));
    }

    /**
     * get suffix of file from path
     * <p/>
     * <pre>
     *      getFileExtension(null)               =   ""
     *      getFileExtension("")                 =   ""
     *      getFileExtension("   ")              =   "   "
     *      getFileExtension("a.mp3")            =   "mp3"
     *      getFileExtension("a.b.rmvb")         =   "rmvb"
     *      getFileExtension("abc")              =   ""
     *      getFileExtension("c:\\")              =   ""
     *      getFileExtension("c:\\a")             =   ""
     *      getFileExtension("c:\\a.b")           =   "b"
     *      getFileExtension("c:a.txt\\a")        =   ""
     *      getFileExtension("/home/admin")      =   ""
     *      getFileExtension("/home/admin/a.txt/b")  =   ""
     *      getFileExtension("/home/admin/a.txt/b.mp3")  =   "mp3"
     * </pre>
     *
     * @param filePath
     * @return
     */
    public static String getFileExtension(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }

        int extenPosi = filePath.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        int filePosi = filePath.lastIndexOf(File.separator);
        if (extenPosi == -1) {
            return "";
        }
        return (filePosi >= extenPosi) ? "" : filePath.substring(extenPosi + 1);
    }


    /**
     * Indicates if this file represents a file on the underlying file system.
     *
     * @param filePath
     * @return
     */
    public static boolean isFileExist(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return false;
        }

        File file = new File(filePath);
        return (file.exists() && file.isFile());
    }

    /**
     * delete file or directory
     * <ul>
     * <li>if path is null or empty, return true</li>
     * <li>if path not exist, return true</li>
     * <li>if path exist, delete recursion. return true</li>
     * <ul>
     *
     * @param file
     * @return
     */
    public static boolean deleteFiles(File file) {

        if (file==null) {
            return true;
        }

        if(!file.exists()){
            return true;
        }

        if (file.isFile()) {
            return file.delete();
        }

        if (!file.isDirectory()) {
            return false;
        }

        for (File f : file.listFiles()) {
            if (f.isFile()) {
                boolean deleted = f.delete();
                if (!deleted) {
                    return false;
                }
            } else if (f.isDirectory()) {
                deleteFiles(f);
            }
        }
        return file.delete();
    }


    /**
     * 删除文件
     *
     * @param file 待删除文件
     * @return 成功标记
     */
    public static boolean deleteFile(File file) {

        boolean ret = false;

        if (file != null) {
            String filePath = file.getPath();
            ReentrantReadWriteLock lock = createOrGetLock(filePath);
            lock.writeLock().lock();
            try {
                if (file.exists()) {
                    ret = file.delete();
                }
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            } finally {
                lock.writeLock().unlock();
                tryToRemoveLock(filePath);
            }
        }

        return ret;
    }

    /**
     * get file size
     * <ul>
     * <li>if path is null or empty, return -1</li>
     * <li>if path exist and it is a file, return file size, else return -1</li>
     * <ul>
     *
     * @param path
     * @return
     */
    public static long getFileSize(String path) {
        if (TextUtils.isEmpty(path)) {
            return -1;
        }

        File file = new File(path);
        return (file.exists() && file.isFile() ? file.length() : -1);
    }

    /**
     * Creates the directory named by the trailing filename of this file,
     * including the complete directory path required to create this directory. <br/>
     * <br/>
     * <ul>
     * <strong>Attentions:</strong>
     * <li>makeDirs("C:\\Users\\Trinea") can only create users folder</li>
     * <li>makeFolder("C:\\Users\\Trinea\\") can create Trinea folder</li>
     * </ul>
     *
     * @param filePath
     * @return true if the necessary directories have been created or the target
     * directory already exists, false one of the directories can not be
     * created.
     * <ul>
     * <li>if {@link FileUtils#getFolderName(String)} return null,
     * return false</li>
     * <li>if target directory already exists, return true</li>
     * <li>return {@link File#mkdirs()}</li>
     * </ul>
     */
    public static boolean makeDirs(String filePath) {
        String folderName = getFolderName(filePath);
        if (TextUtils.isEmpty(folderName)) {
            return false;
        }

        File folder = new File(folderName);
        return (folder.exists() && folder.isDirectory()) ? true : folder.mkdirs();
    }

    /**
     * get folder name from path
     * <p/>
     * <pre>
     *      getFolderName(null)               =   null
     *      getFolderName("")                 =   ""
     *      getFolderName("   ")              =   ""
     *      getFolderName("a.mp3")            =   ""
     *      getFolderName("a.b.rmvb")         =   ""
     *      getFolderName("abc")              =   ""
     *      getFolderName("c:\\")              =   "c:"
     *      getFolderName("c:\\a")             =   "c:"
     *      getFolderName("c:\\a.b")           =   "c:"
     *      getFolderName("c:a.txt\\a")        =   "c:a.txt"
     *      getFolderName("c:a\\b\\c\\d.txt")    =   "c:a\\b\\c"
     *      getFolderName("/home/admin")      =   "/home"
     *      getFolderName("/home/admin/a.txt/b.mp3")  =   "/home/admin/a.txt"
     * </pre>
     *
     * @param filePath
     * @return
     */
    public static String getFolderName(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }

        int filePosi = filePath.lastIndexOf(File.separator);
        return (filePosi == -1) ? "" : filePath.substring(0, filePosi);
    }

    /**
     * 将一个序列化对象写入文件
     *
     * @param object
     * @return
     */
    public static void writeSerObjectToFile(Object object, String fileName) {
        ObjectOutputStream out = null;
        FileOutputStream fos = null;
        try {
            makeDIRAndCreateFile(fileName);
            fos = new FileOutputStream(fileName);
            out = new ObjectOutputStream(fos);
            out.writeObject(object);
        } catch (FileNotFoundException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (out != null) {
                silentlyCloseCloseable(out);
            }
            if(fos != null) {
                silentlyCloseCloseable(fos);
            }
        }
    }

    /**
     * 从文件中直接读一个对象
     *
     * @param fileName
     * @return
     */
    public static Object readSerObjectFromFile(String fileName) {
        Object b = null;
        ObjectInputStream in = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            in = new ObjectInputStream(fis);
            b = in.readObject();
        } catch (Exception e) {
            ;
        } finally {
            if (in != null) {
               silentlyCloseCloseable(in);
            }
            if(fis != null ){
                silentlyCloseCloseable(fis);
            }
        }
        return b;
    }

    /**
     * 创建目录和文件， 如果目录或文件不存在，则创建出来
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static File makeDIRAndCreateFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.isDirectory()) {
            file.mkdirs();
            return file;
        }

        String parent = file.getParent();
        File parentFile = new File(parent);
        if (!parentFile.exists()) {
            if (parentFile.mkdirs()) {
                file.createNewFile();
            } else {
                throw new IOException("创建目录失败！");
            }
        } else {
            if (!file.exists()) {
                file.createNewFile();
            }
        }
        return file;
    }

    /**
     * 字符串转成文件
     *
     * @date:2014-11-5
     * @time:上午10:19:40
     */
    public static void stringToFile(Context context, String filePath,
                                    String text) {


        File distFile = new File(filePath);

        if (!distFile.exists()) {
            try {
                distFile.createNewFile();
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(distFile);
            byte[] bytes = text.getBytes();
            fout.write(bytes);
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } finally {

            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }

    }

    // 读取文本文件中的内容
    public static String fileToString(String strFilePath) {
        File file = new File(strFilePath);

        if(!file.exists()){
            return "";
        }

        return file2String(file, null);
    }


    /***********************************************************************************************
     * Start CoreJar FilsUtils
     **********************************************************************************************/
    public static final String DOWNLOAD_ERROR_CODE_FILE_NAME = "downloadError.txt";
    public static final String CUBE_ERROR_FILE_NAME = "cubeError.txt";
    public static final String APP_CRASH_LOG_FILE = "crashlog.txt";
    public static final String APP_MOBILE_PLAY_KEY_EVENT = "mobilePlay.txt";

    // 把map转化成json然后存储文件,json不是动态，固定json格式参数
    public static void storeJson2File(String filePath, String fileName,
                                      String isnew) {
        File distFile = new File(filePath + fileName);
        FileOutputStream fileOutputStream = null;
        try {
            if (!distFile.exists()) {
                distFile.createNewFile();
            } else {
                distFile.delete();
                distFile.createNewFile();
            }
            fileOutputStream = new FileOutputStream(distFile);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("isnew", isnew);
            byte[] buffer = jsonObject.toString().getBytes("UTF-8");
            fileOutputStream.write(buffer);
            fileOutputStream.flush();

        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }

    // json转化成map
    public static HashMap<String, String> getJson2File(String filePath,
                                                       String fileName) {
        File distFile = new File(filePath + fileName);
        FileInputStream fileInputStream = null;
        String content = null;
        try {
            if (!distFile.exists()) {
                return null;
            }
            fileInputStream = new FileInputStream(distFile);
            byte[] buffer = new byte[fileInputStream.available()];
            if (fileInputStream.read(buffer) != -1) {
                content = new String(buffer, "UTF-8");
                if (!TextUtils.isEmpty(content)) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    JSONObject jsonObject = new JSONObject(content);
                    map.put("isnew", jsonObject.optString("isnew"));
                    return map;
                }
            }

        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
        return null;
    }

    private static String selectDir(@NonNull Context context) {
        boolean haveSdCard = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        String filePath = null;

        if (haveSdCard) {
            File path = null;
            try {
                path = StorageCheckor.getStoragePublicDir(context, null);
            }catch (Exception e){
                ExceptionUtils.printStackTrace(e);
            }
            if(path!=null){
                filePath = path.getPath() + "/.qiyi/";
                path = new File(filePath);
                if (!path.exists()) {
                    path.mkdir();
                }
            }else{
                filePath = context.getCacheDir().getPath();
                filePath = filePath + "/.qiyi/";
                File f = new File(filePath);
                if (!f.exists()) {
                    f.mkdir();
                }
            }

        } else {
            filePath = context.getCacheDir().getPath();
            filePath = filePath + "/.qiyi/";
            File f = new File(filePath);
            if (!f.exists()) {
                f.mkdir();
            }
        }

        return filePath;
    }

    // 存储唯一标示符
    public static void storeOnlySign2File(Context context, String isnew) {
        String filePath = selectDir(context);
        String fileName = "qiyi_only.data";
        storeJson2File(filePath, fileName, isnew);
    }

    // 获取唯一标示符
    public static HashMap<String, String> getOnlySign(Context context) {
        String filePath = selectDir(context);
        String fileName = "qiyi_only.data";
        return getJson2File(filePath, fileName);
    }

    /**
     * 下载错误码保存到文件
     *
     * @param fileName
     * @param content
     */
    public static void errorCodeToAccessFile(String fileName, String content) {
        RandomAccessFile randomFile = null;
        try {
            // 打开一个随机访问文件流，按读写方式
            randomFile = new RandomAccessFile(fileName, "rw");
            // 文件长度，字节数
            long fileLength = randomFile.length();
            if (fileLength >= 99 * 1024) {
                // 不能大于100K,将文件指针移到文件头
                randomFile.seek(0);
            } else {
                // 将写文件指针移到文件尾。
                randomFile.seek(fileLength);
            }

            randomFile.writeBytes(content);

        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (randomFile != null) {
                try {
                    randomFile.close();
                } catch (IOException e) {
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }



    public static final long CUBE_FEEDBACK_SEEK_LENGTH = 20 * 1024 * 8L;

    public static String readRandomAccessFile(String fileName, long cutLength) {

        RandomAccessFile randomFile = null;

        StringBuilder sb = new StringBuilder();

        try
        {

            File file = new File(fileName);
            if(!file.exists()){
                return "";
            }

            randomFile = new RandomAccessFile(fileName, "r");
            long fileLength = randomFile.length();
            DebugLog.log(TAG, "fileLength = " + fileLength);
            long start = 0;
            if (fileLength > cutLength) {
                start = fileLength - cutLength;
            }
            randomFile.seek(start);
            String line = "";
            while ((line = randomFile.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            try {
                if (randomFile != null) {
                    randomFile.close();
                }
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        return sb.toString();

    }


    /**
     * File path split-char.
     */
    public static final String ROOT_FILE_PATH = "/";

    /**
     * 根据用户设置偏好，获得视频下载路径 外置sd卡（为了兼容Android 4.4 外置sd卡的write权限控制）的路径需要额外额外处理
     *
     * @param context
     * @param fileName
     * @return
     */
    public static String getDownloadVideoErrorCodePath(Context context,
                                                       String fileName) {

        File externalFile = StorageCheckor.getInternalStorageFilesDir(context,null);

        String downloadPath = SharedPreferencesFactory.get(context, SharedPreferencesConstants.OFFLINE_DOWNLOAD_DIR, "");
        if (!TextUtils.isEmpty(downloadPath)) {
            downloadPath = downloadPath + "Android/data/"
                    + context.getPackageName() + "/files" + ROOT_FILE_PATH
                    + fileName;
        } else {
            downloadPath = externalFile.getAbsolutePath() + "/" + ROOT_FILE_PATH
                    + fileName;
        }


        if (!TextUtils.isEmpty(downloadPath)) {
            try {
                File file = new File(downloadPath);
                if (!file.exists()) {
                    // 这句话对于4.4的外置sd卡很重要
                    StorageCheckor.getInternalStorageFilesDir(context, null);
                    file.createNewFile();
                }
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
        DebugLog.v("download_error", downloadPath);
        return downloadPath;
    }
    /***********************************************************************************************
     * END CoreJar FilsUtils
     **********************************************************************************************/


    /***********************************************************************************************
     * Start cardExtra FilsUtils
     **********************************************************************************************/
    public static long getSDCardFreeSpace() {
        long result = 0;
     /*   String state = Environment.getExternalStorageState();
        long result = 0;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            long blockSize = sf.getBlockSize();
            long availCount = sf.getAvailableBlocks();

            result = availCount * blockSize;

//			Debug.logd("availave block size = " + availCount + " available size = " + availCount
//					* blockSize / 1024 /1024 + " MB");
        }*/
        return result;
    }

    public static int getDiskCacheSize() {
        int size = (int) getSDCardFreeSpace() / 10;
        //200MB
        if (size < 200000000) {
            size = 200000000;
        }
        return size;
    }

    public static ArrayList<String> getFilelist(String path) {
        ArrayList<String> list = new ArrayList<String>();
        if (StringUtils.isEmptyArray(path)) {
            return list;
        }
        File file = new File(path);
        File[] files = file.listFiles();
        if (null != files) {
            for (File a : files) {
                list.add(a.getAbsolutePath());
                DebugLog.v("Feed", "getFilelist:" + a.getAbsolutePath());
            }
        }
        return list;
    }


    /**
     *移动网络播放关键事件记录
     * @param mContext
     * @param eventStr
     */
    public static void mobilePlayEventToFile (Context mContext, String eventStr){

        RandomAccessFile file = null;
        String fileName = mContext.getFilesDir().getAbsolutePath() + "/" + FileUtils.APP_MOBILE_PLAY_KEY_EVENT;
        try {
            file = new RandomAccessFile(fileName, "rw");
            long fileLength = file.length();
            if (fileLength > 99 * 1024){
                file.seek(0);
            }else {
                file.seek(fileLength);
            }
            file.writeBytes(eventStr);

        }catch (Exception e){
            ExceptionUtils.printStackTrace(e);

        }finally {
            if (file != null){
                try {
                    file.close();
                }catch (IOException e){
                    ExceptionUtils.printStackTrace(e);
                }
            }
        }
    }

    /***********************************************************************************************
     * end cardExtra FilsUtils
     **********************************************************************************************/


}
