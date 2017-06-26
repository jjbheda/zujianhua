package org.qiyi.basecore.utils;

/**
 * Created by niejunjiang on 2016/4/28.
 * 兼容以前的sharedPreferenceFactory，用来存放SharePreferenced的key值
 * 建议以key 值来标识不同的业务，且key 值由业务方在各自的业务中定义，请不要在该类添加
 */
@Deprecated
public class SharedPreferencesConstants {

    /**
     * ——————————————————————————————————
     * sharedpreferences.xml name start
     * ——————————————————————————————————
     */
    public static final String DEFAULT_SHAREPREFERENCE_NAME = "default_sharePreference";
    public static final String LAUNCH_SHAREPREFERENCE_NAME = "launch_sharePreference";
    public static final String DEFAULT_PLAY_STAT_NAME = "DEFAULT_PLAY_STAT_NAME";

    //7.5前SharedPreferenceHelper 的sharedpreferencesName
    public static final String DEFAULT_DOWNLOAD_PREFERENCE_NAME = "song_download";

    //记录广告位弹窗次数的sharedpreferencesName
    public static final String KEY_AD_TIMES = "KEY_AD_TIMES";

    //用了记录第一次启动的SharePreferenceName 同时也是key 值
    public static final String KEY_STARTED_TIMES = "KEY_STARTED_TIMES";

    /**
     * Default cache folder for content local disk cache
     */
    public static final String DEFAULT_CACHE_FOLDER = "content_cache";

    /**
     * ——————————————————————————————————————————————
     * sharedpreferences.xml name  end
     * ——————————————————————————————————————————————
     */

    /**
     * ——————————————————————————————————————————————
     * sharedpreferences key      start
     * ——————————————————————————————————————————————
     */

    /**
     * push uuid
     */
    public final static String KEY_IQIYI_PUSH_UUID = "key_iqiyi_push_uuid";

    /**
     * 下载状态默认文件名（来自以前的sharepreferenceHelper）
     */

    public static final String DOWNLOADED_VIDEO = "DOWNLOADED_VIDEO";
    public static final String DOWNLOADING_VIDEO = "DOWNLOADING_VIDEO";
    public static final String DOWNLOADED_ALBUM = "DOWNLOADED_ALBUM";
    public static final String DOWNLOADING_ALBUM = "DOWNLOADING_ALBUM";
    public static final String DOWNLOAD_STAT_LAST_TIME = "DOWNLOAD_STAT_LAST_TIME";

    /**
     * 是否是第一次开启扫一扫帮助
     */
    public static final String IS_FIRST_TIME_SCAN_HELP_LAUCH = "IS_FIRST_TIME_SCAN_HELP_LAUCH";

    /**
     * The key for my setting if skip title or tail of movie
     * 我的奇艺-设置-跳过片头片尾 在SharedPreferences中KEY值  comment by weizheng
     */
    public static final String KEY_SETTING_SKIP = "KEY_SETTING_SKIP";

    /**
     * The value for my setting if skip title or tail of movie
     * 我的奇艺-设置-跳过片头片尾 在SharedPreferences中Value 值  comment by weizheng
     */
    public static final String VALUE_SETTING_SKIP = "1";

    /**
     * The key for my setting if allow download when 2G/3G
     */
    public static final String KEY_SETTING_ALLOW = "KEY_SETTING_ALLOW";

    /**
     * The value for my setting if allow download when 2G/3G
     */
    public static final String VALUE_SETTING_ALLOW = "1";

    /**
     * The key for my setting how to remain the network status
     */
    public static final String KEY_SETTING_REMIND = "KEY_SETTING_REMIND";

    /**
     * The key for my settings to remind every time.
     */
    public static final String VALUE_SETTING_REMIND_EACH = "2";

    /**
     * The key for my settings to remind only once.
     */
    public static final String VALUE_SETTING_REMIND_ONCE = "1";

    /**
     * 设置首页的列表样式
     */
    public static final String KEY_SETTING_MODE = "KEY_SETTING_MODE";

    /**
     * 是否启动的时候显示切换海报模式的弹框
     * added by lixueyong
     */
    public static final String KEY_CHANGE_MODE = "KEY_CHANGE_MODE";

    /**
     * The key for my settings to list mode
     */
    public static final String VALUE_SETTING_MODE_LIST = "1";

    /**
     * The key for my settings to grid mode
     */
    public static final String VALUE_SETTING_MODE_GRID = "2";

    /**
     * The key for my settings  never to remind .
     */
    public static final String VALUE_SETTING_REMIND_NEVER = "3";

    /**
     * 自选服务
     */
    public static final String KEY_SETTING_CUSTOM_SERVICE = "KEY_SETTING_CUSTOM_SERVICE";
    public static final String ID_QIXIU = "1016";
    public static final String ID_GAMECENTER = "8005";
    public static final String ID_APPSTORE = "8003";
    /**
     * 默认服务
     */
    public static final String VALUE_SETTING_DEFAULT_SERVICE = "0";

    /**
     * 自选服务
     */
    public static final String VALUE_SETTING_CUSTOM_SERVICE = "1";

    public static final String KEY_LAST_CUSTOM_SERVICE_PINGBACK_TIME = "LAST_CUSTOM_SERVICE_PINGBACK_TIME";

    public static final String KEY_CUSTOM_SERVICE_PWD = "KEY_CUSTOM_SERVICE_PWD";

    public static final String KEY_SETTING_FLOATING_SHOW = "KEY_SETTING_FLOATING_SHOW";

    public static final String KEY_SETTING_PUSH_MSG_OFF = "KEY_SETTING_PUSH_MSG_OFF";
    public static final String VALUE_PUSH_MSG_OFF = "1";

    //跨屏购
    public static final String KEY_SETTING_KUAPINGGOU = "KEY_SETTING_KUAPINGGOU";
    public static final String VALUE_KUAPINGGOU_OFF = "1";
    public static final String KEY_SETTING_GPS_LOC_OFF = "KEY_SETTING_PUSH_MSG_OFF";

    /**
     * The key for if created short cut
     */
    public static final String KEY_CREATE_SHORT = "DIALOG_CREATE_SHORT";

    /**
     * The key for if version upgrade
     */
    public static final String KEY_VERSION_UPGRADE = "KEY_VERSION_UPGRADE";

    public static final String DEFAULT_VALUE_VERSION_UPGRADE = "3.0";

    public static final String VALUE_CREATE_SHORT = "1";

    /**
     * 升级版本号
     */
    public static final String KEY_NEW_UPDATA_VERSION = "KEY_NEW_UPDATA_VERSION";

    /**
     * 底部tips显示控制标志
     */
    public static final String KEY_BOTTOM_TIPS_FLAG = "KEY_BOTTOM_TIPS_FLAG";

    /**
     * 底部tips显示的开始时间
     */
    public static final String KEY_BOTTOM_TIPS_SHOW_TIME = "KEY_BOTTOM_TIPS_SHOW_TIME";

    /**
     * 底端tips的广告开始时间
     */
    public static final String KEY_BOTTOM_TIPS_AD_TIME = "KEY_BOTTOM_TIPS_AD_TIME";

    public static final String KEY_ALREADY_REMIND = "KEY_ALREADY_REMIND";

    public static final String VALUE_ALREADY_REMIND = "1";

    public static final String VALUE_INIT_REMIND = "0";

    public static final String SCAN_CFG = "SCAN_CFG";

    public static final String KEY_QIYI_COM = "KEY_QIYI_COM";

    public static final String PUSH_MSG_ID = "PUSH_MSG_ID";

    public static final String KEY_CHASE_REMIND = "KEY_CHASE_REMIND";

    public static final String CHASE_REMINDED_LIST = "CHASE_REMINDED_LIST";

    /**
     * 客户端版本号
     */
    public static final String CLEINT_VERSION_CODE = "CLEINT_VERSION";

    /**
     * 加载风云榜帮助信息的常量标识
     */
    public static final String KEY_CLIENT_OPEN_MESSAGE = "CLIENT_OPEN_MESSAGE";

    /**
     * phone 与 pad 切换参数
     * 1. phone
     * 2. pad
     * 默认为1
     */
    public static final String KEY_CLIENT_TYPE = "CLIENT_TYPE";

    public static final int VALUE_CHASE_REMIND = 0;

    /**
     * 频道分类版本时间戳
     */
    public static final String CATEGORY_TAG_UPTIME = "CATEGORY_TAG_UPTIME";
    /**
     * The key for first prompt download success
     */
    public static final String KEY_DOWNLOAD_SUCCESS_FIRST = "KEY_DOWNLOAD_SUCCESS_FIRST";
    /**
     * 显示添加追剧提示
     */
    public static final String SHOW_CHASE_PROMPT = "Show_Chase_prompt";

    /**
     * 用户选择的码率类型
     */
    public static final String USER_CURRENT_RATE_TYPE = "USER_CURRENT_RATE_TYPE";
    /**
     * 用户选择的码率类型_下载
     */
    public static final String USER_DOWNLOAD_RATE_TYPE = "USER_DOWNLOAD_RATE_TYPE";

    /**
     * 用户选择的路由_下载
     */
    public static final String USER_DOWNLOAD_ROUTER_TYPE = "USER_DOWNLOAD_ROUTER_TYPE";
    /**
     * 初始化setting
     **/
    public static final String KEY_INIT_SETTING = "KEY_INIT_SETTING";
    /**
     * 已初始化setting
     **/
    public static final String VALUE_INIT_SETTING_ALREADY = "1";

    public static final String VALUE_SUBSCRIPT_LOCATION = "VALUE_SUBSCRIPT_LOCATION";

    public static final String KEY_AD_DOWNLOAD_URL = "KEY_AD_DOWNLOAD_URL";

    /**
     * 崩溃率上传
     */
    public static final String KEY_INITAPP_ISCRASH = "KEY_INITAPP_ISCRASH";

    /**
     * 正常退出客户端时设置（isCrash＝0）。
     */
    public static final String VALUE_INITAPP_ISCRASH_CRASH_NORMALEXIT = "0";

    public static final String VALUE_IDFV_INFO = "VALUE_IDFV_INFO";

    /**
     * 播放记录更新提醒数据
     */
    public static final String VALUE_REMIND_INFO = "VALUE_REMIND_INFO";

    /**
     * 百度云推送 百度服务器根据应用api_key生成的终端唯一userID
     */
    public static final String BAIDU_PUSH_USER_ID = "baiduPushUserID";

    /**
     * 小米云推送 小米服务器根据应用app_id,app_key生成的终端唯一userID
     */
    public static final String XIAO_MI_PUSH_USE_ID = "xiaoMiPushUserID";

    /**
     * 华为云推送 华为服务器根据应用app_id,app_type, app_secret生成的终端唯一userID
     */
    public static final String HUA_WEI_PUSH_USE_ID = "huaweiPushUserID";

    /**
     * 设置下载路径
     */
    public static final String OFFLINE_DOWNLOAD_DIR = "offlineDownloadDir";

    public static final String AD_FALG = "AD_FLAG";

    /**
     * 发生错误时重启次数
     */
    public static final String IS_ERROR_RESTART_COUNT = "IS_ERROR_RESTART_COUNT";

    /**
     * 测试shared
     */
    public static final String TEST_PID = "TEST_PID";

    /**
     * 由push进入vip购买
     */
    public static final String IS_IN_VIPPAY_FROM_PUSH = "IS_IN_VIPPAY_FROM_PUSH";

    /**
     * 免责声明开关
     */
    public static final String QIYI_DISCLAIMER = "QIYI_DISCLAIMER";

    /* 我的菜单项存储sp */
    public static final String KEY_MYMAIN_MENU = "KEY_MYMAIN_MENU";
    /* 我的菜单(豆腐块)项存储sp */
    public static final String KEY_MYMAIN_MENU_GRID = "KEY_MYMAIN_MENU_GRID";
    /* 7.4新增台湾站"我的"页面sp */
    public static final String KEY_MYMAIN_MENU_TW = "KEY_MYMAIN_MENU_TW";

    /**
     * 我的页面未读会员消息数
     */
    public static final String VIP_MESSAGE_COUNT = "VIP_MESSAGE_COUNT";

    /**
     * 台湾模式我的页面未读我的消息数
     */
    public static final String TW_MINE_MESSAGE_COUNT = "TW_MINE_MESSAGE_COUNT";

    /**
     * 我的页面“我的皮肤”是否显示皮肤推荐位
     */
    public static final String MY_MAIN_NEW_SKIN_FLAG = "MY_MAIN_NEW_SKIN_FLAG";

    /**
     * 自定义的服务管理顺序的数据本地存储
     */
    public static final String SERVER_MANAGER_CUSTOM_ORDER = "SERVER_MANAGER_CUSTOM_ORDER";

    /**
     * 发现菜单项存储sp
     */
    public static final String DISCOVERY_MENU = "DISCOVERY_MENU";

    /**
     * 我的菜单更新时间
     */
    public static final String MYMAIN_MENU_UP_TIME = "MYMAIN_MENU_UP_TIME";
    /**
     * 我的菜单缓冲文件的版本信息
     */
    public static final String MYMAIN_MENU_VERSION = "MYMAIN_MENU_UP_VERSION";

    /**
     * 发现更新时间
     */
    public static final String DISCOVERY_MENU_UP_TIME = "DISCOVERY_MENU_UP_TIME";

    /**
     * 表示发现时间戳是否更新
     */
    public static final String UPDATE_DISCOVERY_TIME = "UPDATE_DISCOVERY_TIME";

    /**
     * 启动图资源
     */
    public static final String BOOT_IMAGE_SOURCE = "BOOT_IMAGE_SOURCE";

    /**
     * 启动图资源  更新时间
     */
    public static final String BOOT_IMAGE_SOURCE_UPDATE_TIME = "BOOT_IMAGE_SOURCE_UPDATE_TIME";

    /**
     * 刷动态数据逻辑 勿删 ljq
     */
    public static final String HAVE_CLICK_UGC_LOGIN = "have_click_ugc_login";


    public static final String PHONE_WELCOME_LUNCH_TIMES = "PHONE_WELCOME_LUNCH_TIMES";

    public static final String PHONE_PUSH_SWITCH = "PHONE_PUSH_SWITCH";

    public static final String PHONE_EXITDIALOG_ACT_SHOW_TIMES = "PHONE_EXITDIALOG_ACT_SHOW_TIMES";

    public static final String PHONE_EXITDIALOG_ACT_SHOW_TIMES_DATA = "PHONE_EXITDIALOG_ACT_SHOW_TIMES_DATA";


    /**
     * 一键分享开关
     */
    public static final String SHARE_A_KEY_SETTINGS = "SHARE_A_KEY_SETTINGS";

    /**
     * qiyiid
     */
    public static final String QIYI_ID = "QIYI_QIYIID";


    /**
     * cupid
     */
    public static final String CUP_ID = "CUP_ID";

    /**
     * 过滤短视频标志
     */
    public static final String SAVE_CHECK_STATE = "SAVE_CHECK_STATE";

    /**
     * 已登录用户播放记录是否已Merge
     */
    public static final String LOGIN_USER_RC_MERGED = "LOGIN_USER_RC_MERGED";

    /**
     * 已登录用户收藏是否已Merge
     */
    public static final String LOGIN_USER_COLLECTION_MERGED = "LOGIN_USER_COLLECTION_MERGED";

    /**
     * pps 获取ip信息
     */
    public static final String PPS_IP_MESSAGE = "PPS_IP_MESSAGE";//

    /**
     * 摇一摇次数(超过3次不显示)
     */
    public static final String SHAKE_COUNT = "SHAKE_COUNT";//

    /**
     * onPause收集的vv数据
     */
    public static final String KEY_PAUSE_VV_STAUS = "KEY_PAUSE_VV_STAUS";

    /**
     * 搜索结果页，上次弹出第三方浏览器弹窗时间、已弹窗次数
     */
    public static final String PHONE_SEARCH_LAST_PROMOTE_THIRD_BROWSER_TIME = "PHONE_SEARCH_LAST_PROMOTE_THIRD_BROWSER_TIME";
    public static final String PHONE_SEARCH_THIRD_BROWSER_PROMOTED_TIMES = "PHONE_SEARCH_THIRD_BROWSER_PROMOTED_TIMES";

    /**
     * 为Pad导流量，至多出现三次
     */
    public static final String PROMOTE_PAD_TIMES = "PROMOTE_PAD_TIMES";
    public static final String PROMOTE_PAD_LAST_DOWNLOAD_URL = "PROMOTE_PAD_LAST_DOWNLOAD_URL";

    /**
     * ppq add frd setting
     */
    public static final String MY_SETTING_PPQ_ADD_FRD_SETTING = "MY_SETTING_PPQ_ADD_FRD_SETTING";
    public static final String MY_SETTING_PPQ_VIDEO_SETTING = "MY_SETTING_PPQ_VIDEO_SETTING";

    /**
     * 用户上次 城市ID
     */
    public static final String PHONE_TICKETS_CITY_ID = "PHONE_TICKETS_CITY_ID";
    /**
     * 用户上次 经纬度
     */
    public static final String PHONE_TICKETS_GPS_INFO = "PHONE_TICKETS_GPS_INFO";

    /**
     * app版本（动态加载so用）
     */
    public static final String APP_VERION_FOR_DYNAMOCSO = "APP_VERION_FOR_DYNAMOCSO";

    /**
     * 角标Map in InitApp
     */
    public static final String ANGLE_ICONS2_IN_INIT_APP = "ANGLE_ICONS2_IN_INIT_APP";

    /**
     * 节日皮肤开关
     */
    public static final String KEY_HOLIDAY_SKIN_SWITCH = "KEY_HOLIDAY_SKIN_SWITCH_NEW";

    /**
     * 弹幕
     */
    public static final String BULLET_CH_DEFAULT = "bullet_ch_default";

    /**
     * 会员皮肤开关
     */
    public static final String KEY_VIP_SKIN_SWITCH = "KEY_VIP_SKIN_SWITCH_NEW";

    /**
     * 首次开启该版本（新装 or 升级）
     */
    public static final String KEY_SHOW_PPS2QY_MODE = "KEY_SHOW_PPS2QY_MODE";

    /**
     * initLogin接口传递参数ns次数
     */
    public static final String KEY_FOR_UPLOAD_NS_COUNT = "KEY_FOR_UPLOAD_NS_COUNT";

    /**
     * initLogin接口是否上传过arid
     */
    public static final String KEY_FOR_UPLOAD_ARID = "KEY_FOR_UPLOAD_ARID";

    /**
     * 该版本首次启动(V7.0)，展示启动动画
     */
    public static final String KEY_FOR_SHOW_LAUNCH_ANIMATION = "KEY_FOR_SHOW_LAUNCH_ANIMATION";

    /***
     * 支持的运营商MNC json
     */
    public static final String KEY_OPERATOR_JSON = "KEY_OPERATOR_JSON";


    /**
     * 是否显示过VR的手势或者陀螺仪引导图
     */
    public static final String KEY_VR_GESTURE_GUAID = "KEY_VR_GESTURE_GUAID";

    /**
     * 陀螺仪是否可用
     */

    public static final String KEY_VR_GYRO_ENABLE = "KEY_VR_GYRO_ENABLE";
    /**
     * 标示首页的缓存数据是否需要删除，如果缓存数据有脏数据，导致首页加载crash，则需要删除
     */
    public static final String HOME_PAGE_CACHE_SHOULD_DELETE = "home_page_cache_should_delete";

    /**
     * Error Codes 上次保存的时间戳，用来判断是否需要更新
     */
    public static final String ERROR_CODES_LAST_TIMESTAMP = "ERROR_CODES_LAST_TIMESTAMP";

    /**
     * 首页顶部导航数据
     */
    public static final String HOME_TOP_MENU = "home_top_menu";

    /**
     * 是否显示vip到期提醒Card，默认显示，只有用户点击续费后，不再显示
     */
    public static final String IS_NEED_SHOW_VIP_EXPIRATION_TIME_REMINDER = "IS_NEED_SHOW_VIP_EXPIRATION_TIME_REMINDER";

    /**
     * 记录最近一次点击“我的”的“消息”
     */
    public static final String TIMESTAMP_LAST_CLICK_TAB_ME_MESSAGE = "timestamp_last_click_tab_me_message";

    //奇摩
    /**
     * 首次使用奇摩时，提示物理键调节奇摩声音
     */
    public static final String FIRST_USE_QIMO_KEY_VOLUME_GUID = "FIRST_USE_QIMO_KEY_VOLUME_GUID";
    /**
     * 首次使用奇摩投屏
     */
    public static final String FIRST_USE_DLAN = "FIRST_USE_DLAN";
    /**
     * 奇摩首次使用提示切换码流
     */
    public static final String QIMO_CHANGERATE_GUID_KEY = "QIMO_CHANGERATE_KEY";

    /**
     * 换肤是否需要更新
     */
    public static final String SKIN_TIME = "skin_time";

    /**
     * 底部频道导航
     */
    public static final String HOME_BOTTOM_MENU = "home_bottom_menu";

    /********
     * 离线下载VIP标示
     *********/
    public static final String KEY_DOWNLOAD_VIP_TIPS = "KEY_DOWNLOAD_VIP_TIPS";     //vip 试用加速的试用时间

    public static final String KEY_AUTO_DOWNLOAD = "KEY_AUTO_DOWNLOAD";

    /**
     * 本地视频是否第一次删除
     */
    public static final String LOCAL_VIDEO_IS_FIRST_DELETED = "LOCAL_VIDEO_IS_FIRST_DELETED";

    /*********
     * 记录app启动时间
     ***********/
    public static final String KEY_APPLICATION_LAUNCH_TIME = "key_application_launch_time";

    public static final String KEY_CURRENT_DAY_DOWNLOAD_COUNT = "KEY_CURRENT_DAY_DOWNLOAD_COUNT";

    public static final String KEY_CURRENT_DAY = "KEY_CURRENT_DAY";
    /**
     * v_codec接口返回的codec_type缓存在本地的key
     */
    public static final String KEY_V_CODEC_TYPE = "KEY_V_CODEC_TYPE";

    // APP启动时首页底部显示播放/阅读记录提醒
    public static boolean mShowHistoryTipsAtStart = false;

    public static final String KEY_PLAYER_ADS_SLIENCE = "KEY_PLAYER_ADS_SLIENCE";

    /**
     * 允许推送动态消息
     */
    public static final String MY_SETTING_PUSH_FEED = "MY_SETTING_PUSH_FEED";
    public static final String NAVIBAR_EMBEDDED_PLAYER = "NAVIBAR6";

    /**
     * 小皮包显示控制逻辑
     */
    public static final String AD_PIBAO = "AD_PIBAO";

    /**
     * PPS 游戏显示控制逻辑
     */
    public static final String AD_PPSGAME = "AD_PPSGAME";

    /**
     * 允许搜索通知显示
     */
    public static final String MY_SETTING_SHOW_NOTIFICATION = "MY_SETTING_SHOW_NOTIFICATION";

    /**
     * 播放器内去榜单提醒
     * 横屏切竖屏时，一天一次
     */
    public static final String USER_PLAYER_CATRANK = "USER_PLAYER_CATRANK";

    /**
     * 地区模式
     */
    public static final String AREA_MODE_TAIWAN = "AREA_MODE_TAIWAN";

    /**
     * 下次是否提醒地区模式变化
     */
    public static final String AREA_MODE_NOTIFY_NEXT = "AREA_MODE_NOTIFY_NEXT";

    /**
     * 用户设置的播放器解码类型
     */
    public static final String USER_DECODE_TYPE = "USER_DECODE_TYPE";

    /**
     * native播放需要的js相关数据
     */
    public static final String NATIVE_VIDEO_DATA_UPDATE = "NATIVE_VIDEO_DATA_UPDATE";

    /**
     * 是否是第一次展示奇摩投屏图标
     */
    public static final String QIMO_ICON_FIRST_SHOW = "QIMO_ICON_FIRST_SHOW";

    public static final String NATIVE_VIDEO_DATA = "NATIVE_VIDEO_DATA";

    /**
     * 是否屏蔽定时器提醒
     * 1.用户进入过横屏更多
     * 2.用户点击过屏幕下方的浮层提示（浮层里的立即使用或关闭都算）
     */
    public static final String TIMER_OPERATE = "TIMER_OPERATE";

    public final static String KEY_TIPS_MOVIE_LASTTIME = "key_tips_movie_lasttime";

    /**
     * 设置是否是台湾IP
     */
    public final static String KEY_IS_TAIWAN_IP = "key_is_taiwan_ip";

    /**
     * 设置debugkey
     */
    public final static String QIYI_DEBUG_KEY = "qiyi_debug_key";

    /***
     * 记录应用安装时间
     */
    public final static String KEY_TIME_INSTALL_APP = "key_time_install_app";


    /***
     * 记录应用更新时间
     */

    public final static String KEY_TIME_UPDATE_APP = "key_time_update_app";

    /**
     * 添加第一次订阅标识
     */

    public final static String KEY_SUBSCRIBE_FIRST = "KEY_SUBSCRIBE_FIRST";


    /***
     * 奥运会弹窗都不显示
     */
    public static final String KEY_OLYMPIC_POP_NOT_SHOW_ALL = "KEY_OLYMPIC_NOT_SHOW_ALL";

    /***
     * 奥运会弹窗某一类型的不显示
     */
    public static final String KEY_OLYMPIC_POP_NOT_SHOW_SLOT_ID = "KEY_OLYMPIC_NOT_SHOW_SLOT_ID";

    public static final String KEY_COCOS_UPDATE_TIME_LAST_START = "cocos_so_update_time_last_start";
    public static final String KEY_COCOS_UPDATE_TIME_THIS_TIME = "cocos_so_update_time_this_time";
    public static final String KEY_SO_ZIP_CRC = "cocos_zip_crc";
    /***
     * 订阅页toast时间记录，保证每天只提示一次
     */
    public static final String KEY_SUBSCRIBE_LAST_TIP_TIME = "KEY_SUBSCRIBE_LAST_TIP_TIME";
    /**
     * ————————————————————————————————————————————
     * sharedpreferences key      end
     * ————————————————————————————————————————————
     */

    /***
     * 丘比特广告视频最大时长
     */
    public final static String KEY_RECIPROCAL_TIME_AD_LIMIT = "key_reciprocal_time_ad_limit";

    //-----PAD-------//
    /*
    for pad
     */
    /**
     * 收银台上次访问的URL
     */
    public final static String OLD_URL_FOR_PAY = "OLD_URL_FOR_PAY";

    public static final String NAVI_MY_MESSAGE_SHOWFLAG = "NAVI_MY_MESSAGE_SHOWFLAG";


    /**
     * 搜索内容保持一致
     */
    public static final String SEARCH_DEFAULT_WORD = "SEARCH_DEFAULT_WORD";

    /**
     * 登录用户来源平台 参考SNSTYPE LOGIN_TYPE
     */
    public static final String SNS_LOGIN_TYPE = "SNS_LOGIN_TYPE";

    /**
     * 屏幕是否为半屏
     */
    public static final String KEY_IS_HALF_SCREEN = "isHalfScreen";
    //-----PAD-- END-----//

    /**
     * 消息推送服务上次上传token的日期, 目前策略为每三天上传一次
     */
    public static final String KEY_LAST_UPLOAD_PUSH_TOKEN_TIME = "lastUploadTokenDate";

    /**
     * 意见反馈UI数据缓存
     */
    public final static String SP_FEEDBACK_DATA = "sp_feedback_data";
}
