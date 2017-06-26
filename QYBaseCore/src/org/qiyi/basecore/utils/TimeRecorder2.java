package org.qiyi.basecore.utils;

import java.util.HashMap;
import java.util.Map;

import org.qiyi.android.corejar.debug.DebugLog;

public class TimeRecorder2 {

    private static Map<String, Long> mTagStart = new HashMap<>();
    private static Map<String, Long> mTagEnd = new HashMap<>();
    private static Map<String, Long> mTagCost = new HashMap<>();

    public static void onTaskStart(String tag, String logTag) {
        if (!DebugLog.isDebug()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        mTagStart.put(tag, startTime);
    }

    public static long onTaskEnd(String tag, String logTag) {
        if (!DebugLog.isDebug()) {
            return 0;
        }

        long endTime = System.currentTimeMillis();
        Long lStart = mTagStart.get(tag);
        long startTime = lStart == null ? 0 : lStart;
        long duration = endTime - startTime;
        mTagEnd.put(tag, endTime);
        mTagCost.put(tag, duration);

        DebugLog.v(logTag, tag + ": cost " + duration);
        return duration;
    }

    public static String getTagByUrl(String url) {
        String tag = "";
        if (!StringUtils.isEmpty(url)) {
            if (url.contains("player_tabs?")) {
                int s = url.indexOf("&page_part=") + 11;
                tag = "ReflactionPage" + url.substring(s, s + 1);
            } else if (url.contains("card_view?")) {
                tag = "ReflactionFullEpisode";
            }
        }

        return tag;
    }

    public static long getCost(String tag) {
        Long value = mTagCost.get(tag);
        return value == null ? 0 : value;
    }

    public static long getStart(String tag) {
        Long value = mTagStart.get(tag);
        return value == null ? 0 : value;
    }

    public static long getEnd(String tag) {
        Long value = mTagEnd.get(tag);
        return value == null ? 0 : value;
    }
}
