package org.qiyi.basecore.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 时间格式有关的操作
 */
public class TimeUtils {

    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 格式化时间
     * @return
     */
    public static String formatDate(){

        SimpleDateFormat DEFAULT_FOMATTER = new SimpleDateFormat(DEFAULT_DATE_PATTERN, Locale.getDefault());

        return DEFAULT_FOMATTER.format(new java.util.Date());
    }

    /**
     * 格式化时间
     * @param pattern
     * @return
     */
    public static String formatDate(String pattern){

        SimpleDateFormat sdf = null;
        if (StringUtils.isEmpty(pattern)) {
            SimpleDateFormat DEFAULT_FOMATTER = new SimpleDateFormat(DEFAULT_DATE_PATTERN, Locale.getDefault());
            sdf = DEFAULT_FOMATTER;
        }else {
            sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        }

        return sdf.format(new java.util.Date());
    }

    /**
     * 获取时区信息
     * @return
     */
    public static String getTimeArea() {

        Date date = new Date();
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        TimeZone zone = dateFormat.getTimeZone();
        int timeArea = zone.getOffset(date.getTime()) / 1000;

        return timeArea + "";
    }

    /**
     * 获取时长
     * @param duration
     * @return
     */
    public static String getDuration(String duration) {
        if (StringUtils.isEmpty(duration))
            return "00:00";
        if (duration.contains(":")) {
            if (duration.startsWith("00:"))
                return duration.substring("00:".length());
            return duration;
        }
        long s = StringUtils.toLong(duration, 0L);
        String str = convertSecondsToDuration(s);
        if (str.startsWith("00:"))
            return str.substring("00:".length());
        return str;
    }

    /**
     * 把秒转化为时长
     * @param seconds
     * @return
     */
    public static String convertSecondsToDuration(long seconds) {
        long days = seconds / (60 * 60 * 24);
        seconds -= days * (60 * 60 * 24);
        long hours = seconds / (60 * 60);
        seconds -= hours * (60 * 60);
        long minutes = seconds / 60;
        seconds -= minutes * (60);

        StringBuffer sb = new StringBuffer();
        if (hours < 10) {
            sb.append("0");
        }
        sb.append(hours);
        sb.append(":");
        if (minutes < 10) {
            sb.append("0");
        }
        sb.append(minutes);
        sb.append(":");
        if (seconds < 10) {
            sb.append("0");
        }
        sb.append(seconds);

        if (days > 0) {
            return "" + days + "d " + sb.toString();
        } else {
            return "" + sb.toString();
        }
    }
}
