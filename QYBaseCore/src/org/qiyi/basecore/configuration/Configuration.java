package org.qiyi.basecore.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Properties;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.FileUtils;
import org.qiyi.basecore.utils.StringUtils;

/**
 * @author liuzm
 */
public class Configuration {
    private final static String TAG = "Configuration";

    private static Properties defaultProperty;

    static {
        defaultProperty = new Properties();
    }

    /**
     * load()
     *
     * @param path
     * @return
     */
    public static boolean loadProperties(String path) {
        boolean result = false;
        FileInputStream inputStream = null;
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                inputStream = new FileInputStream(file);
                defaultProperty.load(inputStream);
                result = true;
            }
        } catch (Exception ignore) {
            DebugLog.e(TAG, ignore.getMessage());
        } finally {
            if (inputStream != null) {
                FileUtils.silentlyCloseCloseable(inputStream);
            }
        }

        return result;
    }

    /**
     * load()
     *
     * @param is
     * @return
     */
    public static boolean loadProperties(InputStream is) {
        try {
            defaultProperty.load(is);
            return true;
        } catch (Exception ignore) {
            DebugLog.e(TAG, ignore.getMessage());
        }
        return false;
    }

    public static void setProperty(String vKey, String vValue) {
        defaultProperty.setProperty(vKey, vValue);
    }

    /**
     * boolean
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public static boolean getBoolean(String name, boolean defaultValue) {
        String value = getProperty(name);
        try {
            if (!StringUtils.isEmpty(value)) {

                return Boolean.valueOf(value);
            } else {

                return defaultValue;
            }
        } catch (Exception e) {

            return defaultValue;
        }
    }

    /**
     * int
     *
     * @param name
     * @param fallbackValue
     * @return
     */
    public static int getIntProperty(String name, int fallbackValue) {
        String value = getProperty(name);
        try {
            if (!StringUtils.isEmpty(value)) {

                return Integer.parseInt(value);
            } else {

                return fallbackValue;
            }
        } catch (Exception nfe) {

            return fallbackValue;
        }
    }

    /**
     * long
     *
     * @param name
     * @return
     */
    public static long getLongProperty(String name) {
        String value = getProperty(name);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    /**
     * String 鏁版嵁
     *
     * @param name
     * @return
     */
    public static String getProperty(String name) {
        return getProperty(name, null);
    }

    public static String getProperty(String name, String fallbackValue) {
        String value = null;
        try {
            value = defaultProperty.getProperty(name);
        } catch (AccessControlException ace) {
            value = fallbackValue;
        }
        return replace(value);
    }

    private static String replace(String value) {
        if (null == value) {
            return value;
        }
        String newValue = value;
        int openBrace = 0;
        if (-1 != (openBrace = value.indexOf("{", openBrace))) {
            int closeBrace = value.indexOf("}", openBrace);
            if (closeBrace > (openBrace + 1)) {
                String name = value.substring(openBrace + 1, closeBrace);
                if (name.length() > 0) {
                    newValue = value.substring(0, openBrace) + getProperty(name) + value.substring(closeBrace + 1);

                }
            }
        }

        if (newValue.equals(value)) {
            return value;
        } else {
            return replace(newValue);
        }
    }
}
