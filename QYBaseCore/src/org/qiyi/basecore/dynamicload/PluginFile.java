package org.qiyi.basecore.dynamicload;

import java.io.File;

/**
 * Wrap class for file which contains file type
 * 
 * @author huangbo
 *
 */
public class PluginFile {
    /**
     * Plugin file type for jar or dex 
     */
    public static final int FILE_TYPE_DEX = 0;
    /**
     * Plugin file type for so 
     */
    public static final int FILE_TYPE_SO = FILE_TYPE_DEX + 1;
    /**
     * Plugin file type for unknown
     */
    public static final int FILE_TYPE_UNKNOWN = FILE_TYPE_SO + 1;

    /**
     * Target file which need inject
     */
    public File mFile;

    /**
     * Target file's type {@link #FILE_TYPE_DEX}, {@link #FILE_TYPE_SO}
     */
    public int mFileType;

    public PluginFile(File file, int type) {
        mFile = file;
        if (type < FILE_TYPE_DEX || type > FILE_TYPE_SO) {
            mFileType = FILE_TYPE_DEX;
        } else {
            mFileType = type;
        }
    }
}
