package org.qiyi.basecore.concurrentInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.StringUtils;
import org.qiyi.net.convert.BaseResponseConvert;
import org.qiyi.net.toolbox.ConvertTool;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by kangle on 2015/8/3.
 * 封装Error Info
 * 作为持久化对象，注意不要修改这个类的名称
 */
public class ErrorCodeInfoReturn implements Serializable {

    public ArrayList<ErrorCodeInfo> concurrent;
    public ArrayList<ShareTip> share_tip;
    public ArrayList<PlayToast> play_toast;
    public ArrayList<String> vip_skip_toast;

    public static class ErrorCodeInfoParser extends BaseResponseConvert<ErrorCodeInfoReturn> {

        public ErrorCodeInfoReturn parse(JSONObject json) {

            ErrorCodeInfoReturn ret = null;

            try {
                JSONObject doc = json.optJSONObject("doc");
                if (doc != null) {
                    JSONArray concurrent = doc.optJSONArray("concurrent");
                    ret = new ErrorCodeInfoReturn();
                    ret.concurrent = new ArrayList<ErrorCodeInfo>();
                    for (int i = 0; concurrent != null && i < concurrent.length(); i++) {
                        JSONObject errorInfoJObj = concurrent.optJSONObject(i);
                        ErrorCodeInfo errorCodeInfo = new ErrorCodeInfo();
                        errorCodeInfo.button_name = errorInfoJObj.optString("button_name");
                        errorCodeInfo.button_name_traditional = errorInfoJObj.optString("button_name_traditional");
                        errorCodeInfo.button_name_new = errorInfoJObj.optString("button_name_new");
                        errorCodeInfo.button_name_new_traditional = errorInfoJObj.optString("button_name_new_traditional");
                        errorCodeInfo.mbd_error_code = errorInfoJObj.optString("mbd_error_code");
                        errorCodeInfo.proper_title = errorInfoJObj.optString("proper_title");
                        errorCodeInfo.proper_title_traditional = errorInfoJObj.optString("proper_title_traditional");
                        errorCodeInfo.entity_url = errorInfoJObj.optString("entity_url");
                        errorCodeInfo.url_new = errorInfoJObj.optString("url_new");
                        errorCodeInfo.platform = errorInfoJObj.optString("platform");
                        errorCodeInfo.unfreeze_time_min = errorInfoJObj.optString("unfreeze_time_min");
                        errorCodeInfo.unfreeze_time_max = errorInfoJObj.optString("unfreeze_time_max");
                        ret.concurrent.add(errorCodeInfo);
                    }

                    JSONArray share_tip = doc.optJSONArray("share_tip");
                    ret.share_tip = new ArrayList<ShareTip>();
                    for (int i = 0; share_tip != null && i < share_tip.length(); i++) {
                        JSONObject shareTipItemJObj = share_tip.optJSONObject(i);
                        ShareTip shareTip = new ShareTip();
                        shareTip.version = shareTipItemJObj.optString("version");
                        shareTip.icon = shareTipItemJObj.optString("icon");
                        shareTip.proper_title = shareTipItemJObj.optString("proper_title");
                        shareTip.proper_title_traditional = shareTipItemJObj.optString("proper_title_traditional");
                        ret.share_tip.add(shareTip);
                    }

                    //播放器离线文案解析
                    JSONArray play_toast = doc.optJSONArray("play_toast");
                    if (play_toast != null) {
                        final int length = play_toast.length();
                        if (length > 0) {
                            ret.play_toast = new ArrayList<>();
                            for (int i = 0; i < length; i++) {
                                JSONObject playToastItemJObj = play_toast.optJSONObject(i);
                                if (playToastItemJObj != null) {
                                    PlayToast playToast = new PlayToast();
                                    playToast.mbd_error_code = playToastItemJObj.optString("mbd_error_code");
                                    playToast.proper_title = playToastItemJObj.optString("proper_title");
                                    ret.play_toast.add(playToast);
                                }
                            }
                        }
                    }

                    JSONArray vip_tip_toast = doc.optJSONArray("skip_ad");
                    if (null != vip_tip_toast) {
                        final int length = vip_tip_toast.length();
                        if (length > 0) {
                            ret.vip_skip_toast = new ArrayList<String>();
                            for (int i = 0; i < length; i++) {
                                JSONObject vipToastItem = vip_tip_toast.optJSONObject(i);
                                if (vipToastItem != null) {
                                    String title = vipToastItem.optString("title");
                                    if (!StringUtils.isEmpty(title)) {
                                        ret.vip_skip_toast.add(title);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                DebugLog.log(ErrorCodeInfoReturn.class.getSimpleName(), "exception while parsing", e);
            }
            return ret;
        }

        @Override
        public ErrorCodeInfoReturn convert(byte[] bytes, String s) throws IOException {
            return parse(ConvertTool.convertToJSONObject(bytes, s));
        }

    }

    public static class ErrorCodeInfo implements Serializable {

        public String button_name;
        public String button_name_traditional;
        public String button_name_new;
        public String button_name_new_traditional;
        public String mbd_error_code;
        public String proper_title;
        public String proper_title_traditional;
        public String entity_url;
        public String url_new;
        public String platform;
        public String unfreeze_time_min;
        public String unfreeze_time_max;

        @Override
        public String toString() {
            return "ErrorCodeInfo{" +
                    "button_name='" + button_name + '\'' +
                    ", button_name_traditional='" + button_name_traditional + '\'' +
                    ", button_name_new='" + button_name_new + '\'' +
                    ", button_name_new_traditional='" + button_name_new_traditional + '\'' +
                    ", mbd_error_code='" + mbd_error_code + '\'' +
                    ", proper_title='" + proper_title + '\'' +
                    ", proper_title_traditional='" + proper_title_traditional + '\'' +
                    ", entity_url='" + entity_url + '\'' +
                    ", url_new='" + url_new + '\'' +
                    ", platform='" + platform + '\'' +
                    ", unfreeze_time_min='" + unfreeze_time_min + '\'' +
                    ", unfreeze_time_max='" + unfreeze_time_max + '\'' +
                    '}';
        }
    }

    public static class ShareTip implements Serializable {
        public String version;
        public String icon;
        public String proper_title;
        public String proper_title_traditional;

        @Override
        public String toString() {
            return "ShareTip{" +
                    "version='" + version + '\'' +
                    ", icon='" + icon + '\'' +
                    ", proper_title='" + proper_title + '\'' +
                    ", proper_title_traditional='" + proper_title_traditional + '\'' +
                    '}';
        }
    }

    /**
     * 播放器不能下载的提示文案
     */
    public static class PlayToast implements Serializable {
        public String mbd_error_code;
        public String proper_title;

        @Override
        public String toString() {
            return "PlayToast{" +
                    "mbd_error_code='" + mbd_error_code + '\'' +
                    ", proper_title='" + proper_title + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ErrorCodeInfoReturn{" +
                "concurrent=" + concurrent +
                ", share_tip=" + share_tip +
                ", play_toast=" + play_toast +
                '}';
    }
}
