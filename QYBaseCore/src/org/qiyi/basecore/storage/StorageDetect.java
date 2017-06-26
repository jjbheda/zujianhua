package org.qiyi.basecore.storage;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.constant.BaseCoreSPConstants;
import org.qiyi.basecore.utils.CommonUtils;
import org.qiyi.basecore.utils.ExceptionUtils;
import org.qiyi.basecore.utils.SharedPreferencesFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class StorageDetect {
    public static final String TAG = "CHECKSD";

    public static final String FINGERPRINT_DIRECTORY = File.separator + ".fingerprintqiyi" + File.separator;

    public static final String VFAT_TYPE = "vfat";
    public static final String EXTFAT_TYPE = "extfat";
    public static final String EXT4_TYPE = "ext4";
    public static final String FUSE_TYPE = "fuse";
    public static final String SDCARDFS_TYPE = "sdcardfs";
    public static final String TEXFAT_TYPE = "texfat";
    public static final String EXFAT_TYPE = "exfat";


    public static final int STORAGE_TYPE = 0; //"存储卡";
    public static final int INTERNAL_STORAGE_TYPE = 1; //"内置存储卡";
    public static final int EXTERNAL_STORAGE_TYPE = 2; //"外置存储卡";
    public static final int UDISK_TYPE = 3; //"U盘存储"

    public static final String FILE_TYPE_ARRAY[] = {VFAT_TYPE, EXTFAT_TYPE, EXT4_TYPE, FUSE_TYPE, SDCARDFS_TYPE, TEXFAT_TYPE, EXFAT_TYPE};

    public static int FILE_TYPE_MAX_STR_LEN = 0;
    public static int FILE_TYPE_MIN_STR_LEN = 0;

    private static String getStoragePath(String[] paramArrayOfString) {
        int len = paramArrayOfString.length;
        for (int i = 0; i < len; i++) {
            String str1 = paramArrayOfString[i];
            DebugLog.v(TAG, "str1:" + str1);
            String str2 = str1.toLowerCase();
            if (str2.equals("/storage/emulated")) {
                DebugLog.v(TAG, "path is /storage/emulated so return the inner SD path!!");
                return getExternalPath();
            }
            boolean cond = false;
            if (!(str2.contains("sd"))) {
                if ((str2.contains("emmc"))
                        || (str2.contains("ext_card"))
                        || (str2.contains("external"))
                        || (str2.contains("storage"))
                        )
                    cond = true;
            } else {
                if ((!(str2.contains("extrasd_bind")))
                        || (str2.contains("emmc"))
                        || (str2.contains("ext_card"))
                        || (str2.contains("external"))
                        || (str2.contains("storage"))
                        )
                    cond = true;
            }

            if (cond) {
                String str3 = SperatorStr(str1);
                DebugLog.v(TAG, "str3 :" + str3);
                String str4 = getExternalPath();
                DebugLog.v(TAG, "str4 :" + str4);
                String str5 = SperatorStr(str4);
                DebugLog.v(TAG, "str5 " + str5);
                if (str3.equals(str5)) {
                    DebugLog.v(TAG, "str3 == str5");
                    return str1;
                }
                if (str3.equals(str4)) {
                    DebugLog.v(TAG, "str3 == str4");
                    return str1;
                }
                if (str3.equals("/storage/")) {
                    DebugLog.v(TAG, "str3 == /storage/");
                    return str1;
                }
                if (str3.equals("/storage/removable/")) {
                    DebugLog.v(TAG, "str3 == /storage/removable/");
                    return str1;
                }

            }

            if (str1.equals("/mnt/sdcard"))
                return str1;
            if (str1.equals("/mnt/sdcard/external_sd"))
                return str1;
            if (str1.equals("/mnt/ext_sdcard"))
                return str1;
//			if(str1.equals("/udisk"))
//				return str1;	
//			if(str1.equals("/HWUserData"))	
//				return str1;				
        }
        return null;
    }

//	private static String getStoragePath2(String[] paramArrayOfString)
//	{
//		String[] filterString = { "udisk", "usbotg", "disk1", "disk2", "disk3", "disk4", "usbdrivea", "usbdriveb", "usbdrivec", "usbdrived" };
//		int filter_len = filterString.length;
//		int arr_len = paramArrayOfString.length;
//		for(int i = 0; i < arr_len; i++)
//		{
//			String str = paramArrayOfString[i];
//			String lower_str = str.toLowerCase();
//			for(int j = 0; j < filter_len; j++){
//				String filter = filterString[j];
//				if(lower_str.contains(filter))
//					return str;
//			}
//		}
//		return null;
//	}

    private static String getFileType(String[] paramArrayOfString) {
        int array_len = paramArrayOfString.length;
        for (int i = 0; i < array_len; i++) {
            String str = paramArrayOfString[i];
            int len = str.length();
            if ((len >= FILE_TYPE_MIN_STR_LEN) && (len <= FILE_TYPE_MAX_STR_LEN)) {
                int file_type_size = FILE_TYPE_ARRAY.length;

                for (int j = 0; j < file_type_size; j++) {
                    String type_str = FILE_TYPE_ARRAY[j];
                    if (str.equals(type_str))
                        return type_str;
                }
            }
        }
        return null;
    }


    private static String getExternalPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    }

    private static String SperatorStr(String paramString) {
        if ((paramString != null) && (paramString.length() > 0)) {
            String str = paramString.substring(0, paramString.length() - 1);
            if (str != null)
                return str.substring(0, str.lastIndexOf(File.separator) + 1);
        }
        return "";
    }

    private static int getSdkVer() {
        return Build.VERSION.SDK_INT;
    }

    private static boolean checkExternalSd(ArrayList<StorageItem> paramArrayList) {
        Iterator<StorageItem> iter = paramArrayList.iterator();
        StorageItem item;
        String external_sd = getExternalPath();
        while (iter.hasNext()) {
            item = iter.next();
            if (item != null) {
                String str = item.path;
                if (str.equals(external_sd))
                    return true;
            }
        }
        return false;
    }

    private static boolean checksize(long size) {
        long l1 = size / 1000000000L;
        long l2;
        byte[] arrayOfByte;
        boolean found = false;
        if (l1 % 2L == 0) {
            l2 = l1;
        } else
            l2 = l1 + 1L;

        String binaryStr = Integer.toBinaryString((int) l2);
        if (binaryStr == null)
            return false;
        arrayOfByte = binaryStr.getBytes();
        if (arrayOfByte == null)
            return false;

        int len = arrayOfByte.length;
        int i;
        for (i = 1; i < len; i++) {
            if (arrayOfByte[i] != 0x30) {
                found = true;
                break;
            }
        }

        if (found || (size <= 0))
            return false;
        else {
            double d1 = ((double) (l2 * 1073741824L)) / ((double) size);
            DebugLog.v(TAG, "isPhySize real_diff = " + d1);
            if ((d1 >= 1.063741824D) && (d1 <= 1.098741824D))
                return true;
            else
                return false;
        }
    }

    private static boolean checksize(String path, long size) {
        try {
            if (!checksize(size)) {
                boolean ret = (Environment.getExternalStorageDirectory().getAbsoluteFile().getCanonicalPath() + "/").equals(path);
                if (ret)
                    return true;
                else
                    return false;
            }
            return false;
        } catch (IOException localIOException) {

        }
        return false;
    }

    private static ArrayList<StorageItem> processStorageList(ArrayList<StorageItem> paramArrayList) {
        ArrayList<StorageItem> localArrayList = new ArrayList<StorageItem>();
        StorageItem item;
        Iterator<StorageItem> iter = paramArrayList.iterator();
        int priority = 0;
        while (iter.hasNext()) {
            item = iter.next();
            if (priority == 0) {
                //long total_size1 = item.totalsize;
                //checksize(total_size1);
                localArrayList.add(item);
                priority = item.priority;
            } else {
                int priority2 = item.priority;
                if (priority2 >= priority) {
                    String name = item.path;
                    long size = item.getTotalSize();
                    boolean ret = checksize(name, size);
                    if (!ret) {
                        localArrayList.add(item);
                        continue;
                    }
                }
                localArrayList.add(0, item);
                priority = item.priority;
            }
        }

        return localArrayList;
    }

    private static void createfingerprint(Context context) {

        String fingerName = SharedPreferencesFactory.get(context, "default_sd_fingerprint", "", BaseCoreSPConstants.COMMON_SP);
        DebugLog.v(TAG, "createfingerprint fingerName = " + fingerName);
        String whole_path = "";
        try {
            whole_path = context.getExternalFilesDir("").getAbsolutePath() + FINGERPRINT_DIRECTORY + fingerName;
        } catch (Exception e) {
            whole_path = getExternalPath() + "Android/data/" + context.getPackageName() + "/files" + FINGERPRINT_DIRECTORY + fingerName;
            ExceptionUtils.printStackTrace(e);
        }
        DebugLog.v(TAG, "createfingerprint finger whole_path:" + whole_path);
        File fingerFile = new File(whole_path);
        String fingerNameNew;
        boolean res = false;
        if ((TextUtils.isEmpty(fingerName)) || (!fingerFile.exists())) {
            fingerNameNew = String.valueOf(System.currentTimeMillis());
            DebugLog.v(TAG, "createfingerprint fingerprint2 = " + fingerNameNew);
            try {
                try {
                    whole_path = context.getExternalFilesDir("").getAbsolutePath() + FINGERPRINT_DIRECTORY + fingerNameNew;
                } catch (Exception e) {
                    whole_path = getExternalPath() + "Android/data/" + context.getPackageName() + "/files" + FINGERPRINT_DIRECTORY + fingerNameNew;
                }

                File fingerFileNew = new File(whole_path);
                File fingerFileNewParentDir = fingerFileNew.getParentFile();
                String fingerFileNewParentPath = fingerFileNewParentDir.getAbsolutePath();
                DebugLog.v(TAG, "createfingerprint fingerFileNewParentPath = " + fingerFileNewParentPath);
                if ((fingerFileNewParentDir.exists()) && (!(fingerFileNewParentDir.isDirectory()))) {
                    DebugLog.v(TAG, "createfingerprint delete " + fingerFileNewParentPath);
                    fingerFileNewParentDir.delete();
                }
                if (!fingerFileNewParentDir.exists()) {
                    context.getExternalFilesDir("");
                    res = fingerFileNewParentDir.mkdirs();
                    DebugLog.v(TAG, "createfingerprint mkdirs " + fingerFileNewParentPath + " " + fingerFileNewParentDir.exists());
                }
                res = fingerFileNew.createNewFile();
                DebugLog.v(TAG, "createfingerprint createNewFile " + whole_path + " " + res);
                SharedPreferencesFactory.set(context, "default_sd_fingerprint", fingerNameNew, BaseCoreSPConstants.COMMON_SP);
                return;
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            } catch (NullPointerException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
    }

    private static void filterStorageList(ArrayList<StorageItem> paramArrayList, Context context) {

        String fingerprint = SharedPreferencesFactory.get(context, "default_sd_fingerprint", "", BaseCoreSPConstants.COMMON_SP);
        DebugLog.v(TAG, "filterStorageList fingerprint = " + fingerprint);
        String external_path = getExternalPath();
        for (int i = 0; i < paramArrayList.size(); i++) {
            StorageItem item = paramArrayList.get(i);
            String path = item.path;
            DebugLog.v(TAG, "filterStorageList " + i + ": path = " + path);
            String whole_path = path + "Android/data/" + context.getPackageName() + "/files" + FINGERPRINT_DIRECTORY + fingerprint;
            DebugLog.v(TAG, "whole_path:" + whole_path);
            if ((!(path.equals(external_path)) && ((new File(whole_path)).exists())) || (path.equals(external_path) && getRepeatCount(paramArrayList, external_path) > 1)) {
                DebugLog.v(TAG, "path: " + path + " is duplicated");
                paramArrayList.remove(i);
                i--;
            }
        }


    }

    public static int getRepeatCount(List<StorageItem> sdItems, String path) {
        List<StorageItem> tmpItems = new ArrayList<StorageItem>(sdItems);
        int result = 0;
        for (StorageItem item : tmpItems) {
            if (item.path.equals(path)) {
                result++;
            }
        }
        return result;
    }

    private static boolean checkPath(String line) {
        String[] arrayOfString = new String[5];
        arrayOfString[0] = "sd";
        arrayOfString[1] = "emmc";
        arrayOfString[2] = "ext_card";
        arrayOfString[3] = "external";
        arrayOfString[4] = "storage";

        String[] excludeString = new String[5];
        excludeString[0] = "secure";
        excludeString[1] = "asec";
        excludeString[2] = "firmware";
        excludeString[3] = "obb";
        excludeString[4] = "tmpfs";

//  arrayOfString[2] = "hwuserdata";		
//		arrayOfString[3] = "udisk";
//		arrayOfString[6] = "usbotg";
//		arrayOfString[7] = "disk1";
//		arrayOfString[8] = "disk2";
//		arrayOfString[9] = "disk3";
//		arrayOfString[10] = "disk4";
//		arrayOfString[11] = "usbdrivea";
//		arrayOfString[12] = "usbdriveb";
//		arrayOfString[13] = "usbdrivec";
//		arrayOfString[14] = "usbdrived";

        int len = excludeString.length;
        for (int k = 0; k < len; k++) {
            if (line.contains(excludeString[k])) {
                DebugLog.v(TAG, "exclude path contains " + excludeString[k] + ",so exclude it!!!");
                return false;
            }
        }

        len = arrayOfString.length;
        for (int k = 0; k < len; k++) {
            if (line.contains(arrayOfString[k])) {
                DebugLog.v(TAG, "include  path contain " + arrayOfString[k] + ",so include it!!");
                return true;
            }
        }
        return false;
    }

    private static void computeTypeStrRange() {

        int max_len = 0, min_len = 0;
        int size = FILE_TYPE_ARRAY.length;

        for (int i = 0; i < size; i++) {
            String type = FILE_TYPE_ARRAY[i];
            int len = type.length();
            if ((len > 0) && ((len < min_len) || (min_len == 0))) {
                min_len = len;
            } else if (len > max_len) {
                max_len = len;
            }
        }
        FILE_TYPE_MAX_STR_LEN = max_len;
        FILE_TYPE_MIN_STR_LEN = min_len;
        DebugLog.v(TAG, "FILE_TYPE_MAX_STR_LEN = " + FILE_TYPE_MAX_STR_LEN + " FILE_TYPE_MIN_STR_LEN = " + FILE_TYPE_MIN_STR_LEN);
    }

    /**
     * 通过反射StorageManager和StorageVolume的隐藏API获取SD卡路径
     * 该调用会返回所有可能的卡槽路径，包含挂载和未挂载的卡，由{@link StorageCheckor}
     * 进行筛选和过滤，返回给上层应用层
     * @param mContext
     * @return
     */
    private static ArrayList<StorageItem> scanSDAPI(Context mContext) {
        ArrayList<StorageItem> sdPaths = new ArrayList<StorageItem>();
        try {
            Environment4.initDevices(mContext);
            Environment4.Device[] devices = Environment4.getDevices(mContext);
            for (Environment4.Device device : devices) {
                StorageItem item = new StorageItem(device.getPath() + "/", FUSE_TYPE, -100);
                item.totalsize = device.getTotalSpace();
                item.availsize = device.getFreeSpace();

                item.mPrimary = device.mPrimary;
                item.mRemovable = device.mRemovable;
                item.mState = device.mState;
                //创建隐藏文件作为sd卡的指纹
                if (device.getState(mContext).equals("mounted") && item.canWrite(mContext)) {
                    item.createHideFile(mContext);
                    //
                    if(device.isPrimary()){
                        sdPaths.add(0, item);
                    }else {
                        sdPaths.add(item);
                    }
                }
            }

        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        }
        if (sdPaths.size() == 0) {
            sdPaths = scanSDMount(mContext);
        }
        return sdPaths;
    }

    private static ArrayList<StorageItem> scanSDMount(Context mContext) {
        int priority_level1 = -100;
        int priority_level2 = -1;
        int priority_level3 = 0;
        ArrayList<StorageItem> localArrayList1 = new ArrayList<StorageItem>();
        ArrayList<StorageItem> localArrayList2 = new ArrayList<StorageItem>();
        StorageItem item = null;
        int priority = 0;

        createfingerprint(mContext);
        computeTypeStrRange();
        BufferedReader localBufferedReader = null;
        try {
            localBufferedReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("mount").getInputStream()));
            while (true) {
                String line = localBufferedReader.readLine();

                if (line == null) {
                    DebugLog.v(TAG, "line: is null!");
                    int sdk = getSdkVer();
                    if ((sdk >= Build.VERSION_CODES.JELLY_BEAN_MR1) && !checkExternalSd(localArrayList1)) {
                        String external_sd = getExternalPath();
                        StorageItem item1 = new StorageItem(external_sd, FUSE_TYPE, priority_level1);
                        item1.mState = Environment.MEDIA_MOUNTED;
                        if (item1.getTotalSize() > 0) {
                            DebugLog.v(TAG, "add ExternalPath: " + external_sd);
                            localArrayList1.add(item1);
                        }
                    }
                    localArrayList2 = processStorageList(localArrayList1);
                    int size = localArrayList2.size();

                    for (int i = 0; i < size; i++) {
                        StorageItem card = localArrayList2.get(i);
                        if (i == 0) {
                            if (size != 1)
                                card.type = INTERNAL_STORAGE_TYPE;
                            else
                                card.type = STORAGE_TYPE;
                        } else {
                            card.type = EXTERNAL_STORAGE_TYPE;
                        }
                    }

//					if(item != null){
//						if(item.totalsize > 0){
//							item.type = UDISK_TYPE;
//							localArrayList2.add(item);
//						}
//					}
                    filterStorageList(localArrayList2, mContext);
                    return localArrayList2;
                } else {
                    DebugLog.v(TAG, "line: " + line);

                    if (!checkPath(line.toLowerCase()))
                        continue;

                    DebugLog.v(TAG, "after checkPath: " + line);

                    String[] arrayOfString;
                    arrayOfString = line.split("\\s+");
                    if (arrayOfString == null)
                        continue;
                    String path = getStoragePath(arrayOfString);
                    DebugLog.v(TAG, "getStoragePath: " + path);

                    if (TextUtils.isEmpty(path)) {
//						path = getStoragePath2(arrayOfString);
//						Log.e(TAG, "getStoragePath2: " + path);
//
//						if(TextUtils.isEmpty(path))
//							continue;
//						String type = getFileType(arrayOfString);
//						if(type == null){
//							Log.e(TAG, "getFileType fails");
//							continue;
//						}
//						Log.e(TAG, "getFileType: " + type);
//
//						item = new StorageItem(path + File.separator, type, 1025);
                        continue;
                    } else {
                        String type = getFileType(arrayOfString);
                        if (type == null) {
                            DebugLog.v(TAG, "getFileType fails");
                            continue;
                        }
                        DebugLog.v(TAG, "getFileType: " + type);

                        if (type.equals(VFAT_TYPE) || type.equals(EXTFAT_TYPE) || type.equals(TEXFAT_TYPE)) {
                            if (arrayOfString == null || arrayOfString.length <= 0)
                                priority = priority_level3;
                            else {
                                String str = arrayOfString[0];
                                if (TextUtils.isEmpty(str)) {
                                    priority = priority_level3;
                                }

                                String str2 = str.replaceFirst("/dev/block/vold/", "");
                                DebugLog.v(TAG, "replaceFirst vold : " + str2);
                                if (!TextUtils.isEmpty(str2)) {
                                    String[] arrayOfString2 = str2.split(":");
                                    if ((arrayOfString2 != null) && (arrayOfString2.length >= 2)) {
                                        priority = Integer.parseInt(arrayOfString2[1]);
                                        DebugLog.v(TAG, "split: priority = " + priority);
                                    } else
                                        priority = priority_level3;
                                } else
                                    priority = priority_level2;
                            }
                        } else
                            priority = priority_level1;

                        StorageItem item2 = new StorageItem(path + File.separator, type, priority);
                        item2.mState = Environment.MEDIA_MOUNTED;
                        if (item2.getTotalSize() > 0) {
                            DebugLog.v(TAG, "add path:" + item2.path);
                            localArrayList1.add(item2);
                        } else {
                            DebugLog.v(TAG, item2.path + " is not add!");
                        }
                        continue;
                    }
                }

            }
        } catch (IOException localIOException) {
            return localArrayList2;
        } finally {
            if (localBufferedReader != null) {
                try {
                    localBufferedReader.close();
                } catch (Exception e) {
                    DebugLog.v(TAG, "close file failed");
                }
            }
        }
    }


    public static ArrayList<StorageItem> getStorageList(Context mContext)  //需要在异步线程调用
    {
        if (!CommonUtils.scanSDDoubleAndMerge(mContext)) {
            DebugLog.v(TAG, "getStorageList use api...");
            return scanSDAPI(mContext);
        } else {
            DebugLog.v(TAG, "getStorageList use two way...");
            ArrayList<StorageItem> listOne = scanSDAPI(mContext);
            ArrayList<StorageItem> listTwo = scanSDMount(mContext);

            ArrayList<StorageItem> results = new ArrayList<StorageItem>();
            results.addAll(listOne);
            boolean repeat;
            for (int i = 0; i < listOne.size(); i++) {
                StorageItem item = listOne.get(i);
                StorageItem itemCheck = null;
                repeat = false;
                for (int j = 0; j < listTwo.size(); j++) {
                    itemCheck = listTwo.get(j);
                    if (itemCheck.path.equals(item.path)) {
                        //路径名相同
                        DebugLog.v(TAG, "getStorageList path equals->repeat:" + itemCheck.path);
                        repeat = true;
                        break;
                    }

                    if ((itemCheck.getAvailSize() == item.getAvailSize() && itemCheck.getTotalSize() == item.getTotalSize())) {
                        if (itemCheck.checkHideFileExist(mContext)) {
                            DebugLog.v(TAG, "getStorageList file equals->repeat:" + itemCheck.path);
                            repeat = true;
                        }
                    }

                }
                if (!repeat && itemCheck != null) {
                    results.add(itemCheck);
                }
            }

            return results;
        }
    }


}

