package org.qiyi.basecore.constant;

/**
 * 存放basecore中使用的SharedPreference Key
 * Author:yuanzeyao <br/>
 * Date:16/5/25 14:05 <br/>
 * email:yuanzeyao@qiyi.com <br/>
 */
public interface BaseCoreSPConstants {
    //基础库的sp文件名
    //不用跨进程,放在这个文件
    public static final String BASE_CORE_SP_FILE="base_core_file";
    //需要跨进程的放在这个文件
    public static final String BASE_CORE_SP_FILE_MULTIPRO="base_core_file_multiprocess";

    //debug 模式网络请求是否只走系统代理
    public static final String DEBUG_KEY_NET_PROXY_MODE = "debug_proxy_mode";

    //debug 是否信任*.iqiyi.com的https证书
    public static final String KEY_ISBELIVECERTIFICATE = "isBeliveCertificate";

    //使用两种方式扫描sd卡,然后merge
    public static final String KEY_SCAN_SD_DOUBLE="scan_sd_double";

    //device id 1.0 (qyId) key
    public static final String KEY_IMEI_V1 = "imei_v1";
    public static final String KEY_MACADDR_MD5_V1 = "md5_mac_v1x";
    public static final String KEY_OPENUDID_V1 = "openudid_v1";
    public static final String KEY_QYID_V1 = "qyid_v1";

    //device id 2.0 key
    public static final String KEY_IMEI = "imei";
    public static final String KEY_MACADDR = "macaddr";
    public static final String KEY_ANDROIDID = "android_id";
    public static final String KEY_MD5_QYID_V2 = "md5_qyid";


    public static final String MOVE_FLAG = "has_move_sp_flag";//迁移SP标记key


    //新建的SharedPreference Name  在这里声明
    public static final String APP_INIT_SP = "app_init_sp";       // 开启启动需要加载的SP数据文件名称  ，可以是以下任意一种业务数据，在开机阶段统一加载控制。
    public static final String CARD_SP = "qiyi_card_sp";          // 除app_init中需要使用的，剩余所有和Card 显示相关的SP 存储文件名称。
    public static final String ACCOUNT_SP = "qiyi_account_sp";    // 除app_init中需要使用的，剩余所有和账户相关的逻辑SP 存储文件名称。
    public static final String VIDEO_SP = "qiyi_video_sp";        //除app_init中需要使用的，剩余所有和视频播放相关的SP 存储文件名称。
    public static final String COMMON_SP = "qiyi_common_sp";      //除app_init中需要使用的，不属于以上业务的全部放到common中
    public static final String PLUGIN_SP = "qiyi_plugin_sp";      //除app_init中需要使用的，剩余和插件相关的SP存储文件名称

}
