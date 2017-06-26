package org.qiyi.basecore.jobquequ;

/**
 * @author zhongshan
 * @date 2016-08-08.
 */
public class Priority {
    //APP_START 仅仅供程序启动初始化使用的优先级，其他接口请勿使用！！！
    public static final int APP_START = Integer.MAX_VALUE;

    public static final int PLAYER_LOGIC_MIN = 1001;
    public static final int PLAYER_LOGIC_MAX = 10000;

    public static final int HIGH_MIN = 501;
    public static final int HIGH_MAX = 1000;

    public static final int MID_MIN = 201;
    public static final int MID_MAX = 500;

    public static final int LOW_MIN = 1;
    public static final int LOW_MAX = 200;
}
