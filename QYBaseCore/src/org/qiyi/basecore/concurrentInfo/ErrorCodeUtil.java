package org.qiyi.basecore.concurrentInfo;

import android.content.Context;

import org.qiyi.basecore.utils.StringUtils;

import java.util.List;

/**
 * Created by kangle on 2015/8/3.
 * Error Code Info 获取接口
 */
public class ErrorCodeUtil {

    public static void getPlayToastInfo(final Callback<List<ErrorCodeInfoReturn.PlayToast>> callback) {
        if (mIGetErrorCodeInfoReturn != null) {
            mIGetErrorCodeInfoReturn.getErrorCodeInfoReturn(new Callback<ErrorCodeInfoReturn>() {
                @Override
                public void onCallback(ErrorCodeInfoReturn data) {
                    if (callback != null) {
                        callback.onCallback(data == null ? null : data.play_toast);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onCallback(null);
            }
        }
    }

    public static void getVipSkipToastInfo(final Callback<List<String>> callback) {
        if (mIGetErrorCodeInfoReturn != null) {
            mIGetErrorCodeInfoReturn.getErrorCodeInfoReturn(new Callback<ErrorCodeInfoReturn>() {
                @Override
                public void onCallback(ErrorCodeInfoReturn data) {
                    if (callback != null) {
                        callback.onCallback(data == null ? null : data.vip_skip_toast);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onCallback(null);
            }
        }
    }

    public static void getShareTipInfo(final Callback<List<ErrorCodeInfoReturn.ShareTip>> callback) {
        if (mIGetErrorCodeInfoReturn != null) {
            mIGetErrorCodeInfoReturn.getErrorCodeInfoReturn(new Callback<ErrorCodeInfoReturn>() {
                @Override
                public void onCallback(ErrorCodeInfoReturn data) {
                    if (callback != null) {
                        callback.onCallback(data == null ? null : data.share_tip);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onCallback(null);
            }
        }
    }

    /**
     * @param timeStr  没有可不传
     * @param callback 异步回调
     */
    public static void getCorrespondingErrorCodeInfo(final String errorCode, final String timeStr,
            final Callback<ErrorCodeInfoReturn.ErrorCodeInfo> callback) {

        //临时用-2表示没有时间限制，-1表示timeStr参数不合法
        final int time;

        if (mIGetErrorCodeInfoReturn != null && errorCode != null && (time = StringUtils.isEmpty(timeStr) ? -2
                : StringUtils.getInt(timeStr, -1)) != -1) {
            mIGetErrorCodeInfoReturn.getErrorCodeInfoReturn(new Callback<ErrorCodeInfoReturn>() {
                @Override
                public void onCallback(ErrorCodeInfoReturn data) {
                    ErrorCodeInfoReturn.ErrorCodeInfo ret = null;
                    if (data != null && data.concurrent != null) {
                        for (ErrorCodeInfoReturn.ErrorCodeInfo errorCodeInfo : data.concurrent) {
                            //errorCode一致
                            if (errorCode.equals(errorCodeInfo.mbd_error_code) || (errorCodeInfo.mbd_error_code != null
                                    && errorCode.equals(errorCodeInfo.mbd_error_code.trim()))) {

                                //没有时间要求 || 满足时间要求
                                if (time == -2 || timeMatch(errorCodeInfo, time)) {
                                    ret = errorCodeInfo;
                                    break;
                                }
                            }
                        }
                    }

                    if (callback != null) {
                        callback.onCallback(ret);
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.onCallback(null);
            }
        }
    }

    private static boolean timeMatch(ErrorCodeInfoReturn.ErrorCodeInfo errorCodeInfo, int time) {
        boolean ret = false;

        int min = StringUtils.getInt(errorCodeInfo.unfreeze_time_min, Integer.MIN_VALUE);
        int max = StringUtils.getInt(errorCodeInfo.unfreeze_time_max, Integer.MAX_VALUE);

        //FIXME KANGLE 与接口确认该规则是否正确
        if (time >= min && time <= max) {
            ret = true;
        }

        return ret;
    }

    public static void setIGetErrorCodeInfoReturn(IGetErrorCodeInfoReturn mIGetErrorCodeInfoReturn) {
        ErrorCodeUtil.mIGetErrorCodeInfoReturn = mIGetErrorCodeInfoReturn;
    }

    public interface Callback<T> {
        void onCallback(T data);
    }

    public interface IGetErrorCodeInfoReturn {
        void getErrorCodeInfoReturn(Callback<ErrorCodeInfoReturn> callback);

        void getSwitchInfo(Context context, Callback<VideoSwitchInfo> callback);
    }

    private static IGetErrorCodeInfoReturn mIGetErrorCodeInfoReturn;

    public static void getSwitchInfo(final Context context, ErrorCodeUtil.Callback<VideoSwitchInfo> callback) {
        if (mIGetErrorCodeInfoReturn != null) {
            mIGetErrorCodeInfoReturn.getSwitchInfo(context, callback);
        } else if (callback != null) {
            callback.onCallback(null);
        }
    }

    public static class VideoSwitchInfo {
        public Dubi dubi;
        public FourK fourK;

        public VideoSwitchInfo(Dubi dubi, FourK fourK) {
            this.dubi = dubi;
            this.fourK = fourK;
        }

        @Override
        public String toString() {
            return "VideoSwitchInfo{" +
                    "dubi=" + dubi +
                    ", fourK=" + fourK +
                    '}';
        }
    }

    /**
     * 杜比自动开启及提示
     */
    public static class Dubi {
        public int template1_enable;
        public int template2_enable;
        public int template3_enable;
        public int template6_enable;
        public int template7_enable;
        public int template9_enable;

        public String template1;
        public String template2;
        public String template3;
        public String template6;
        public String template7;
        public String template9;

        @Override
        public String toString() {
            return "Dubi{" +
                    "template1_enable=" + template1_enable +
                    ", template2_enable=" + template2_enable +
                    ", template3_enable=" + template3_enable +
                    ", template6_enable=" + template6_enable +
                    ", template7_enable=" + template7_enable +
                    ", template9_enable=" + template9_enable +
                    ", template1='" + template1 + '\'' +
                    ", template2='" + template2 + '\'' +
                    ", template3='" + template3 + '\'' +
                    ", template6='" + template6 + '\'' +
                    ", template7='" + template7 + '\'' +
                    ", template9='" + template9 + '\'' +
                    '}';
        }
    }

    /**
     * 4K功能引导List
     */
    public static class FourK {
        public int template1_enable;
        public int template2_enable;
        public int template3_enable;

        public String template1;
        public String template2;
        public String template3;

        @Override
        public String toString() {
            return "FourK{" +
                    "template1_enable=" + template1_enable +
                    ", template2_enable=" + template2_enable +
                    ", template3_enable=" + template3_enable +
                    ", template1='" + template1 + '\'' +
                    ", template2='" + template2 + '\'' +
                    ", template3='" + template3 + '\'' +
                    '}';
        }
    }

}
