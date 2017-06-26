package org.qiyi.basecore.properties;

import org.qiyi.basecore.utils.ExceptionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class QYProperties {

    private String filePath;
    private Properties properties;

    public QYProperties(String filepath) {
        this.filePath = filepath;
        properties = new Properties();
    }

    private String getProperty(String name) {
        if (properties != null && properties.size() > 0 && properties.containsKey(name)) {
            if (properties.get(name) != null) {
                return String.valueOf(properties.get(name));
            } else {
                return "";
            }
        }
        return null;
    }

    private int getIntProperty(String name) {
        if (properties != null && properties.size() > 0 && properties.containsKey(name)) {
            if (properties.get(name) != null) {
                return Integer.parseInt(String.valueOf(properties.get(name)));
            } else {
                return 0;
            }
        }
        return -1;
    }

    public String getPropertyValue(String name) {
        String result = getProperty(name);
        if (result != null) {
            return result;
        } else {
            File file = new File(filePath);
            if (!file.exists()) {
                return "";
            }
            if (properties == null) {
                properties = new Properties();
            }
            FileInputStream s = null;
            try {
                s = new FileInputStream(file);
                properties.load(s);
                if (properties.get(name) != null) {
                    return String.valueOf(properties.get(name));
                } else {
                    return "";
                }

            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
                return "";
            } finally {
                try {
                    if (s != null)
                        s.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public int getIntPropertyValue(String name) {
        int result = getIntProperty(name);
        if (result != -1) {
            return result;
        } else {
            File file = new File(filePath);
            if (!file.exists()) {
                return 0;
            }
            if (properties == null) {
                properties = new Properties();
            }
            FileInputStream s = null;
            try {
                s = new FileInputStream(file);
                properties.load(s);
                if (properties.get(name) != null) {
                    int result2 = Integer.parseInt(String.valueOf(properties.get(name)));
                    return result2;
                } else {
                    return 0;
                }
            } catch (Exception e) {
                ExceptionUtils.printStackTrace(e);
                return 0;
            } finally {
                try {
                    if (s != null)
                        s.close();
                } catch (Exception e) {
                }
            }
        }
    }


    public void setPropertyValue(String name, String value) {
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(name, value);
        setPropertyValue(properties);
    }

    public void setPropertyValue(Properties prop) {
        if (properties == null) {
            properties = new Properties();
        }
        properties = prop;
        File file = new File(filePath);
        FileOutputStream s = null;
        try {
            s = new FileOutputStream(file, false);
            properties.store(s, "");
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
            } catch (Exception e) {
            }
        }
    }


    private static int mode;

    public static void setClientMode(int _mode) {
        mode = _mode;
    }

    public static boolean isClientPad() {
        return mode == 1;
    }

    public static boolean isClientPhone() {
        return mode != 1;
    }

}
