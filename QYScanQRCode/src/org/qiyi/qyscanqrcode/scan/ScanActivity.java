package org.qiyi.qyscanqrcode.scan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
//import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

//import com.iqiyi.paopao.api.PaoPaoApiConstants;
//import com.iqiyi.paopao.common.ui.app.PPApp;
//import com.iqiyi.passportsdk.Passport;
//import com.iqiyi.passportsdk.PassportApi;
//import com.iqiyi.passportsdk.http.ICallback;
//import com.iqiyi.passportsdk.http.api.MdeviceApi;
//import com.iqiyi.passportsdk.login.AuthorizationCall;
//import com.iqiyi.passportsdk.login.LoginFlow;
//import com.iqiyi.passportsdk.login.OnLoginSuccessListener;
//import com.iqiyi.passportsdk.model.PassportExBean;
//import com.iqiyi.passportsdk.model.UserInfo;
//import com.iqiyi.passportsdk.model.UserInfo.LoginResponse;
//import com.iqiyi.passportsdk.register.RequestCallback;

import org.json.JSONException;
import org.json.JSONObject;
//import org.qiyi.android.corejar.debug.DebugLog;
//import org.qiyi.android.corejar.deliver.controller.IResearchStatisticsController;
//import org.qiyi.android.corejar.model.Game;
//import org.qiyi.android.corejar.plugin.system.PluginSystemUtil;
//import org.qiyi.android.passport.PassportUtils;
//import org.qiyi.android.plugin.appstore.PPSGameLibrary;
//import org.qiyi.android.video.ui.account.util.PassportUtils;
import org.qiyi.qyscanqrcode.R;
import org.qiyi.qyscanqrcode.scan.camera.CameraManager;
//import org.qiyi.qyscanqrcode.scan.decoding.CaptureActivityHandler;
//import org.qiyi.qyscanqrcode.scan.decoding.InactivityTimer;
//import org.qiyi.qyscanqrcode.scan.zxing.BarcodeFormat;
//import org.qiyi.qyscanqrcode.scan.decoding.CaptureActivityHandler;
//import org.qiyi.qyscanqrcode.scan.decoding.InactivityTimer;
//import org.qiyi.qyscanqrcode.scan.zxing.BarcodeFormat;
//import org.qiyi.qyscanqrcode.scan.decoding.CaptureActivityHandler;
//import org.qiyi.qyscanqrcode.scan.decoding.InactivityTimer;
//import org.qiyi.qyscanqrcode.scan.zxing.BarcodeFormat;
import org.qiyi.qyscanqrcode.scan.decoding.CaptureActivityHandler;
import org.qiyi.qyscanqrcode.scan.decoding.InactivityTimer;
import org.qiyi.qyscanqrcode.scan.zxing.BarcodeFormat;
import org.qiyi.qyscanqrcode.scan.zxing.Result;
//import org.qiyi.android.video.activitys.CommonWebViewNewActivity;
//import org.qiyi.android.video.customview.webview.javascript.CommonJsBridge;
//import org.qiyi.android.video.skin.VipSkinController;
//import org.qiyi.android.video.ui.MyLoadingDialog;
//import org.qiyi.android.video.ui.account.PhoneAccountAc+per;
import org.qiyi.basecore.imageloader.ImageLoader;
import org.qiyi.basecore.multiwindow.MultiWindowManager;
import org.qiyi.basecore.utils.NetWorkTypeUtils;
import org.qiyi.basecore.utils.NetworkStatus;
import org.qiyi.basecore.utils.SharedPreferencesConstants;
import org.qiyi.basecore.utils.SharedPreferencesFactory;
import org.qiyi.basecore.utils.StringUtils;
//import org.qiyi.basecore.widget.ToastUtils;
//import org.qiyi.basecore.widget.commonwebview.WebViewConfiguration;
//import org.qiyi.context.QyContext;
//import org.qiyi.net.HttpManager;
//import org.qiyi.video.module.action.passport.IPassportAction;
//import org.qiyi.video.module.icommunication.ICommunication;
//import org.qiyi.video.module.icommunication.ModuleManager;
//import org.qiyi.video.module.paopao.exbean.PaopaoJumpPageDataBase;
//import org.qiyi.video.module.plugincenter.exbean.PluginIdConfig;

import java.io.IOException;
import java.util.Vector;

/**
 * @author LJQ, LY
 *         二维码扫描activity
 */
public class ScanActivity extends BaseActivity {

    public static final String TAG = "ScanActivity";

    public static final String START_FOR_RESULT = "START_FOR_RESULT";
    public static final String RESULT = "RESULT";
    //是否是由startActivityForResult打开的,默认不是
    private boolean isStartForResult = false;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    public static final int SOURCE_TV_LOGIN = 0;
    public int from_type = SOURCE_TV_LOGIN;

    private static long temptime = 0;
    private static final int REQUEST_TIME = 60000;
    private int mCodeType = 0;
    private String requestConfimLoginUrl;
    private Handler mainHandler;

    private static final int REQUEST_CODE_AUTHORIZATION = 101;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            isStartForResult = intent.getBooleanExtra(START_FOR_RESULT, false);
        }
        setContentView(R.layout.phone_scan_main_layout_scan);
//        CameraManager.reset();
//        CameraManager.init(this);
//        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
//        findViewById(R.id.button_back).setOnClickListener(this);
//        findViewById(R.id.scan_help).setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                startWebViewActivity("http://www.iqiyi.com/common/scancodehelp.html", getString(R.string.phone_scan_help));
//            }
//
//        });
//        hasSurface = false;
//        inactivityTimer = new InactivityTimer(this);
//        if (SharedPreferencesFactory.get(this, SharedPreferencesConstants.IS_FIRST_TIME_SCAN_HELP_LAUCH, true)) {
//            msHandler.sendEmptyMessageDelayed(123, 1000);
//        }
    }


//    private Handler msHandler = new Handler(Looper.getMainLooper()) {
//        public void handleMessage(android.os.Message msg) {
//            {
//                if (msg.what == 123) {
//                    startWebViewActivity("http://www.iqiyi.com/common/scancodehelp.html", getString(R.string.phone_scan_help));
//                    SharedPreferencesFactory.set(ScanActivity.this, SharedPreferencesConstants.IS_FIRST_TIME_SCAN_HELP_LAUCH, false);
//                }
//            }
//        }
//    };
//
//    private void startWebViewActivity(String url, String title) {
//        WebViewConfiguration configuration = new WebViewConfiguration.Builder().setDisableAutoAddParams(true)
//                .setUseOldJavaScriptOrScheme(false).setHaveMoreOperationView(false).setLoadUrl(url).setTitle(title).build();
////        Intent intent = new Intent(getApplicationContext(), CommonWebViewNewActivity.class);
////        intent.putExtra(CommonWebViewNewActivity.CONFIGURATION, configuration);
////        try {
////            startActivity(intent);
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
//    }
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
////            case R.id.button_back_scan:
//            case 0:
////                if (null != handler) {
////                    handler = null;
////                }
////
////                backToMultWindowActivity();
////
//                this.finish();
//                break;
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        DebugLog.v(TAG, "onResume");
//
//        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
//        SurfaceHolder surfaceHolder = surfaceView.getHolder();
//        if (hasSurface) {
//            initCamera(surfaceHolder);
//        } else {
//            surfaceHolder.addCallback(this);
//        }
//        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//
//        decodeFormats = null;
//        characterSet = null;
//
//        IResearchStatisticsController.onResume(this);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        DebugLog.v(TAG, "onRause");
//
//        if (handler != null) {
//            handler.quitSynchronously();
//            handler = null;
//        }
//        CameraManager.get().closeDriver();
//        if (!hasSurface) {
//            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
//            SurfaceHolder surfaceHolder = surfaceView.getHolder();
//            surfaceHolder.removeCallback(this);
//        }
//        IResearchStatisticsController.onPause(this);
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        DebugLog.v(TAG, "onStop");
//    }
//
//    @Override
//    protected void onDestroy() {
//        DebugLog.v(TAG, "onDestroy");
//        if (mainHandler != null) {
//            mainHandler.removeCallbacksAndMessages(null);
//        }
//        HttpManager.getInstance().cancelRequestByTag(requestConfimLoginUrl);
//        inactivityTimer.shutdown();
//        CameraManager.get().stopPreview();
//        super.onDestroy();
//    }
//
//    /**
//     * @param result
//     * @param barcode
//     */
//    public void handleDecode(Result result, Bitmap barcode) {
//        inactivityTimer.onActivity();
//        String resultString = result.getText();
//        onResultHandler(resultString, barcode);
//    }
//
//    private long ToastTime = 0;
//    private String tag = "Scan";
//
//    /**
//     * @param resultString
//     * @param bitmap
//     */
//    private void onResultHandler(String resultString, Bitmap bitmap) {
//        //如果是startActivityForResult则直接返回扫描的数据,不做其它处理
//        if (isStartForResult) {
//            Intent intent = new Intent();
//            if (MultiWindowManager.getInstance().isSupportMultiWindow()) {
////                intent.setAction(CommonJsBridge.SCAN_REQUEST_RESULT_ACTION);
//                intent.putExtra(RESULT, resultString);
//                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
//            } else {
//                intent.putExtra(RESULT, resultString);
//                setResult(RESULT_OK, intent);
//            }
//
//            backToMultWindowActivity();
//
//            finish();
//
//            return;
//        }
//        if (getActivity().isFinishing()) {
//            return;
//        }
//        this.findViewById(R.id.login_fail).setVisibility(View.GONE);
//
//        if (TextUtils.isEmpty(resultString)) {
//            Toast.makeText(ScanActivity.this, "Scan failed!", Toast.LENGTH_SHORT).show();
////            handler.sendEmptyMessage(R.id.decode_failed);
//            return;
//        }
//        //无网络
//        if (NetWorkTypeUtils.getAvailableNetWorkInfo(this) == null) {
//            if ((System.currentTimeMillis() - ToastTime) > 2000) {
//                ToastUtils.toastCustomView(getActivity(), -1, getString(R.string.scanqr_fail_network_down), Toast.LENGTH_SHORT);
//                ToastTime = System.currentTimeMillis();
//            }
//
////            handler.sendEmptyMessage(R.id.decode_failed);
//            return;
//        }
//
//        DebugLog.v("IfaceOPTLoginTask", "resultString=" + resultString);
//
//        //跳转到泡泡
//        if (resultString.contains("codetype")) {
//            Uri uri = Uri.parse(resultString);
//            String codetype = getQuery(uri, "codetype", "");
//            if (codetype.equals("5")) {
//                jumpToPaoPao(resultString);
//                return;
//            }
//        }
//        String codeType = "0";
//        if (resultString.contains("Code_type")) {
//            Uri uri = Uri.parse(resultString);
//            codeType = getQuery(uri, "Code_type", "");
//
//            int code_type = 1000;
//            try {
//                code_type = Integer.parseInt(codeType);
//            } catch (NumberFormatException e) {
//
//            }
//            mCodeType = code_type;
//
//            switch (code_type) {
//                case 7:           //蔚来汽车登录绑定
//                    nioLoginBind(resultString);
//                case 6:           //跳转到影院列表界面
//                    jumpToMovieTickets(resultString);
//                    break;
//                case 5:          //调起泡泡
//                    jumpToPaoPao(resultString);
//                    break;
//                case 4:             //下载应用
//                    gameDownLoad(resultString, bitmap);
//                    break;
//                case 3:            //调起视频播放
//                    playVideo(resultString, bitmap);
//                    break;
//                case 2:             //手机登陆
//                case 1:
////                    phoneLogin(resultString, bitmap);
//                    phoneLoginNew();
//                    break;
//                case 0:            //tvpc登陆
//                    TvOrPcLogin(resultString, bitmap);
//                    break;
//                default:
////                    new AlertDialog.Builder(this).setMessage(getString(R.string.qrtype_not_supported)).setPositiveButton(getString(R.string.btn_OK_scan), new DialogInterface.OnClickListener() {
////                        @Override
////                        public void onClick(DialogInterface dialog, int which) {
////                            backToMultWindowActivity();
////                            finish();
////                        }
////                    }).setCancelable(false).show();
//                    break;
//            }
//        } else {
//            UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
//            matcher.addURI("passport.iqiyi.com", "apis/qrcode/opt/request_login.action", 1);
//            matcher.addURI("passport.iqiyi.com", "apis/qrcode/token_login.action", 2);
//            Uri uri = Uri.parse(resultString);
//            if (1 == matcher.match(uri)) {
////                phoneLogin(resultString, bitmap);
//                phoneLoginNew();
//                return;
//            } else if (2 == matcher.match(uri)) {
//                TvOrPcLogin(resultString, bitmap);
//                return;
//            } else {
//                try {
//                    backToMultWindowActivity();
//
//                    Uri uriExt = Uri.parse(resultString);
//                    Intent downloadIntent = new Intent(Intent.ACTION_VIEW, uriExt);
//                    DebugLog.i(tag, "scandownUri = " + uriExt.toString());
//                    startActivity(downloadIntent);
//
//                    this.getActivity().finish();
//                } catch (Exception e) {
//                    new AlertDialog.Builder(this).setMessage(getString(R.string.qrtype_not_supported)).setPositiveButton(getString(R.string.btn_OK_scan), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            backToMultWindowActivity();
//                            finish();
//                        }
//                    }).setCancelable(false).show();
//                    DebugLog.i(tag, "scandownUri e= " + e);
//                }
//                return;
//            }
//        }
//    }
//
//    private void nioLoginBind(String resultString) {
//        Uri uri = Uri.parse(resultString);
//        String token = getQuery(uri, "token", "");
//        if (StringUtils.isEmpty(token)) {
//            return;
//        }
//
//        if (!PassportUtils.isLogin()) {
//
//            backToMultWindowActivity();
//
//            LoginFlow.get().setOnLoginSuccessListener(new NIOLoginBindWhenLogin(this, token));
//
//            Intent ie = new Intent(getActivity(), PhoneAccountActivity.class);
//            ie.putExtra(PhoneAccountActivity.KEY_ACTION_ID, IPassportAction.OpenUI.LOGIN_AND_CALLBACK);
//            ie.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            getActivity().startActivity(ie);
//
//            return;
//        }
//        httpNIOLoginBind(token);
//    }
//
//    private void httpNIOLoginBind(String token) {
////        showLoginLoadingBar(this.getString(R.string.loading_wait));
//
//        PassportApi.ott_token_bind(token, new ICallback<Void>() {
//            @Override
//            public void onSuccess(Void obj) {
//                if (null == getActivity() || getActivity().isFinishing()) {
//                    return;
//                }
////                dismissLoadingBar();
//                ConfirmDialog.show(getActivity(), getString(R.string.account_scanlogin_success_scan),
//                        new DialogInterface.OnDismissListener() {
//                            @Override
//                            public void onDismiss(DialogInterface dialog) {
//                                backToMultWindowActivity();
//                                finish();
//                            }
//                        });
//            }
//
//            @Override
//            public void onFailed(Object o) {
//                if (null == getActivity() || getActivity().isFinishing()) {
//                    return;
//                }
////                dismissLoadingBar();
//                if (o != null && o instanceof String) {
//                    ConfirmDialog.show(getActivity(), (String) o, new DialogInterface.OnDismissListener() {
//                        @Override
//                        public void onDismiss(DialogInterface dialog) {
//                            reScan();
//                        }
//                    });
//                } else {
////                    ToastUtils.defaultToast(getActivity(), R.string.tips_network_fail_and_try);
////                    reScan();
//                }
//            }
//        });
//    }
//
//    /**
//     * 手机扫描博泰车载项目二维码，跳转到电影票界面
//     *
//     * @param resultString
//     */
//    private void jumpToMovieTickets(String resultString) {
//        Uri uri = Uri.parse(resultString);
//        String movieId = getQuery(uri, "movieId", "");
//        if (StringUtils.isEmpty(movieId)) {
//            return;
//        }
//
//        JSONObject param = new JSONObject();
//        JSONObject bizParam = new JSONObject();
//        try {
//            bizParam.put("biz_sub_id", "3");
//            bizParam.put("biz_params", "");
//            bizParam.put("biz_dynamic_params", String.format("id=%s", movieId));
//            bizParam.put("biz_extend_params", "");
//            bizParam.put("biz_statistics", "from_type=qrcode&from_subtype=qrcode");
//            param.put("biz_params", bizParam);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        String paramJson = param.toString();
//        PluginSystemUtil.getPluginSystemUtil().invokePlugin(this, PluginIdConfig.TICKETS_ID, paramJson);
//        finish();
//    }
//
//    /**
//     * 手机扫描进入泡泡聊天或者圈子
//     *
//     * @param resultString
//     */
//    private void jumpToPaoPao(String resultString) {
//        Uri uri = Uri.parse(resultString);
//        String pageId = getQuery(uri, "pageid", "0");
//        String pageType = getQuery(uri, "type", "");
//        String wallId = getQuery(uri, "wallid", "");
//        String wallType = getQuery(uri, "walltype", "");
//        String paopaoId = getQuery(uri, "paopaoid", "");
//        JSONObject jsonObject = new JSONObject();
//        try {
//            JSONObject sub = new JSONObject();
//            sub.put("biz_params", "");
//            sub.put("biz_statistics", "");
//            sub.put("biz_extend_params",
//                    "|wallId|=|" + wallId + "|" + "|wallType|=|" + wallType + "|" + "|paopaoId|=|"
//                            + paopaoId + "|");
////            sub.put("biz_sub_id", PaoPaoApiConstants.BIZ_SUB_ID_OF_SCAN_TO_PAOPAO);
//            sub.put("biz_dynamic_params", "pageId=" + pageId + "&pageType=" + pageType);
//            jsonObject.put("biz_params", sub);
//            jsonObject.put("biz_plugin", "com.iqiyi.paopao");
//            jsonObject.put("biz_id", "7");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        DebugLog.v(TAG, "jumpToPaoPao json = " + jsonObject.toString());
//        PaopaoJumpPageDataBase data = new PaopaoJumpPageDataBase();
////        PPApp.getInstance().getPaopaoApi().cardTransferToPage(PaoPaoApiConstants.MODULE_ID_BASE_LINE_HOME_PAGE,
////                QyContext.sAppContext, jsonObject.toString(), data);
//        finish();
//    }
//
//    /**
//     * 手机扫描下载游戏
//     *
//     * @param resultString
//     * @param bitmap
//     */
//    private void gameDownLoad(String resultString, Bitmap bitmap) {
//        Uri uri = Uri.parse(resultString);
//        if (uri == null) {
////            PPSGameLibrary.appstoreTransfer(ScanActivity.this, "qrcode_inner", null, PPSGameLibrary.APPSTORE_GAME_CENTER);
//            return;
//        }
//
//        final String gameversion = getQuery(uri, "appvercode", "");
//        final String gameurl = resultString.substring(0, resultString.indexOf("?"));
//        final String gameflag = getQuery(uri, "packname", "");
//        final String gameid = getQuery(uri, "gameId", "");
//        final String gamename = getQuery(uri, "appname", "");
//        final String gamelogo = getQuery(uri, "logo", "");
//        final String gameMD5 = getQuery(uri, "md5", "");
//        final String gametype = getQuery(uri, "apptype", "2");
//
//        if (StringUtils.isEmpty(gameversion) || StringUtils.isEmpty(gameurl)
//                || StringUtils.isEmpty(gameflag) || StringUtils.isEmpty(gameid)
//                || StringUtils.isEmpty(gamename) || StringUtils.isEmpty(gamelogo)) {
//            backToMultWindowActivity();
//            Intent downloadIntent = new Intent(Intent.ACTION_VIEW, uri);
//            startActivity(downloadIntent);
//            this.getActivity().finish();
//            return;
//        }
//
//        NetworkStatus status = NetWorkTypeUtils.getNetworkStatus(this);
//        if (NetworkStatus.OFF == status) {
////            ToastUtils.defaultToast(this, R.string.phone_my_record_toast_no_net);
//            return;
//        } else if (NetworkStatus.WIFI != status) {
////            ToastUtils.defaultToast(this, R.string.phone_search_result_download_game_none_wifi);
//        }
//
//        LayoutInflater inflater = LayoutInflater.from(this);
////        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.install_dialog_view, null);
////
////        final Dialog dialog = new AlertDialog.Builder(ScanActivity.this).create();
////        dialog.setCancelable(false);
////        dialog.show();
////        dialog.getWindow().setContentView(layout);
//
////        TextView dialog_msg = (TextView) layout.findViewById(R.id.dialog_msg);
////        dialog_msg.setText(String.format(getResources().getString(R.string.scanner_confirm_download), gamename));
////        Button btnOK = (Button) layout.findViewById(R.id.dialog_ok);
////        btnOK.setOnClickListener(new OnClickListener() {
////
////            @Override
////            public void onClick(View v) {
////                dialog.dismiss();
////                Game game = new Game();
////                game.qipu_id = gameid;
////                game.appName = gamename;
////                game.appVersionName = gameversion;
////                game.appImgaeUrl = gamelogo;
////                game.appDownloadUrl = gameurl;
////                game.appPackageName = gameflag;
////                game.appType = gametype;
////                game.md5 = gameMD5;
//////                PPSGameLibrary.appstoreTransfer(ScanActivity.this, "qrcode_inner", game, PPSGameLibrary.APPSTORE_DOWNLOAD);
////            }
////        });
//
////        Button btnCancel = (Button) layout.findViewById(R.id.dialog_cancel);
////        btnCancel.setOnClickListener(new OnClickListener() {
////
////            @Override
////            public void onClick(View v) {
////                dialog.dismiss();
////                reScan();
////            }
////        });
//
//    }
//
//    /**
//     * 手机扫描播放视频
//     *
//     * @param resultString
//     * @param bitmap
//     */
//    private void playVideo(String resultString, Bitmap bitmap) {
//
//        Uri uri = Uri.parse(resultString);
//
//        String Fromtype = getQuery(uri, "Fromtype", "");
//        String Subtype = getQuery(uri, "Subtype", "");
//        String category_id = getQuery(uri, "category_id", "");
//        String id = getQuery(uri, "id", "");
//        String tvid = getQuery(uri, "tvid", "");
//        String progress = getQuery(uri, "progress", "0");
//
//        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(tvid)) {
//            rescan();
//            return;
//        }
//        NetworkStatus status = NetWorkTypeUtils.getNetworkStatus(this);
////        if (NetworkStatus.OFF == status) {
////            ToastUtils.defaultToast(this, R.string.phone_my_record_toast_no_net);
////            return;
////        } else if (NetworkStatus.WIFI != status) {
////            ToastUtils.defaultToast(this, R.string.phone_search_result_download_game_none_wifi);
////        }
//
//        StringBuilder params = new StringBuilder(32);
//        params.append("qiyimobile://self/res.made");
//        params.append("?").append("identifier").append("=").append("qymobile");
//        params.append("&").append("cid").append("=").append(category_id);
//        params.append("&").append("aid").append("=").append(id);
//        params.append("&").append("tvid").append("=").append(tvid);
//        params.append("&").append("to").append("=").append("0");
//        params.append("&").append("from_type").append("=").append(Fromtype);
//        params.append("&").append("from_sub_type").append("=").append(Subtype);
//        params.append("&").append("progress").append("=").append(progress);
//
//        Intent intent = new Intent();
//        intent.setData(Uri.parse(params.toString()));
//        intent.setAction("android.intent.action.qiyivideo.player");
//        intent.setPackage("com.qiyi.video");
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//
//        finish();
//
//    }
//
//    /**
//     * 手机端扫描登录TV端
//     *
//     * @param resultString
//     * @param bitmap
//     */
//    private void TvOrPcLogin(String resultString, Bitmap bitmap) {
//        //获得返回结果
//        //VERIFY验证-------BENGIN------------
//        //http://passport.iqiyi.com/apis/qrcode/token_login.action
//
//
//        // TV 二维码扫描结果：http://passport.ptqy.gitv.tv/apis/qrcode/token_login.action?agenttype=28&token=633ef2c78284da5c&Code_type=0
//        DebugLog.v(TAG, "TvOrPcLogin # resultString=" + resultString);
//
//        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
//        matcher.addURI("passport.iqiyi.com", "apis/qrcode/token_login.action", 1);
//        matcher.addURI("passport.ptqy.gitv.tv", "apis/qrcode/token_login.action", 1);
//        Uri uri = Uri.parse(resultString);
//        if (1 != matcher.match(uri)) {
//            rescan();
//            return;
//        }
//        //VERIFY验证-------END------------
//        String token = getQuery(uri, "token", "");
//        String action = getQuery(uri, "action", "");
//        if (StringUtils.isEmpty(token)) {
//            rescan();
//            return;
//        }
//        doLoginOPT(token, action);
//    }
//
//    private void phoneLoginNew() {
////        ConfirmDialog.show(this, getString(R.string.phone_my_account_scan_phonelogin)
////                , getString(R.string.cancel), null
////                , getString(R.string.phone_my_account_scan_tologin), new OnClickListener() {
////                    @Override
////                    public void onClick(View v) {
////                        Intent intent = new Intent(ScanActivity.this, PhoneAccountActivity.class);
////                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                        startActivity(intent);
////                        finish();
////                    }
////                });
//    }
//
//    /**
//     * 手机端扫描登录手机端
//     *
//     * @param resultString
//     * @param bitmap
//     * @deprecated
//     */
//    private void phoneLogin(String resultString, Bitmap bitmap) {
////        showLoginLoadingBar(getResources().getString(R.string.scanner_request_network));
//        //获得返回结果
//        //VERIFY验证-------BENGIN------------
//        //http://passport.iqiyi.com/apis/qrcode/opt/request_login.action
//        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
//        matcher.addURI("passport.iqiyi.com", "apis/qrcode/opt/request_login.action", 1);
//        Uri uri = Uri.parse(resultString);
//        if (1 != matcher.match(uri)) {
//            rescan();
//            return;
//        }
//
//        //VERIFY验证-------END------------
//        String token = getQuery(uri, "token", "");
//        if (StringUtils.isEmpty(token)) {
//            rescan();
//            return;
//        }
//
//        doRequestPhoneLogin(token);
////        dismissLoadingBar();
//    }
//
//
//    // loading界面
////    private MyLoadingDialog mLoadingBar;
//
//    /**
//     * 显示登录的loadingBar（样式独一无二）
//     *
//     * @param
//     */
////    public void showLoginLoadingBar(String message) {
////
////        if (null == mLoadingBar) {
////            mLoadingBar = new MyLoadingDialog(this, true, R.drawable.phone_toast_qrloginprogress_img);
////        }
////
////        mLoadingBar.getWindow().setGravity(Gravity.CENTER);
////        mLoadingBar.setProgressStyle(android.R.attr.progressBarStyleSmall);
////        if (!StringUtils.isEmpty(message)) {
////            mLoadingBar.setDisplayedText(message);
////        }
////        mLoadingBar.setIsLoginStyle(true);
////        mLoadingBar.setCancelable(false);
////        mLoadingBar.setCanceledOnTouchOutside(false);
////        mLoadingBar.show();
////        mLoadingBar.setOnKeyListener(new DialogInterface.OnKeyListener() {
////            @Override
////            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
////                if (keyCode == KeyEvent.KEYCODE_BACK) {
////                    dismissLoadingBar();
////                    return true;
////                }
////
////                return false;
////            }
////        });
////    }
////
////
////    /**
////     * 移除当前显示的loadingBar
////     *
////     * @see #mLoadingBar
////     */
////    public void dismissLoadingBar() {
////        if (mLoadingBar != null && mLoadingBar.isShowing()) {
////            mLoadingBar.dismiss();
////            mLoadingBar = null;
////        }
////    }
//
//    /**
//     * 请求登录
//     *
//     * @param token
//     * @deprecated
//     */
//    private void doRequestPhoneLogin(final String token) {
//        PassportApi.optRequestLogin(token, new ICallback<String>() {
//            @Override
//            public void onSuccess(String obj) {
//                if (null == getActivity()) {
//                    return;
//                }
//                if (null == obj) {
//                    rescan();
//                    return;
//                }
//
//                //TODO
//
//                temptime = System.currentTimeMillis();
//                doRealConfimLogin(token);
//            }
//
//            @Override
//            public void onFailed(Object o) {
//                rescan();
//            }
//        });
//    }
//
//    @Deprecated
//    private void doRealConfimLogin(String token) {
//        //轮询 查看是否登录
//        if (System.currentTimeMillis() - temptime <= REQUEST_TIME) {
//            doRequestConfirmLoginDirect(token);
//        } else {
////            Toast.makeText(QyContext.sAppContext, getResources().getString(R.string.scanner_network_error), Toast.LENGTH_SHORT).show();
////            dismissLoadingBar();
//            getActivity().finish();
//        }
//    }
//
//    @Deprecated
//    private void doRequestConfirmLoginDirect(final String token) {
//        if (NetWorkTypeUtils.getAvailableNetWorkInfo(this) == null) {
////			UIUtils.toastCustomView(getActivity(), -1, this.getString(R.string.scan_iqiyi_network_down), Toast.LENGTH_SHORT);
//            this.finish();
//            return;
//        }
//
//        requestConfimLoginUrl = PassportApi.optIsLoginConfirmed(token, new ICallback<String>() {
//            @Override
//            public void onSuccess(String obj) {
//                if (null == getActivity()) {
//                    return;
//                }
//
//                if ("fail".equals(obj)) {
//                    getActivity().finish();
//                    return;
//                } else if ("retry".equals(obj)) {
//                    RetryDoRealConfimLogin(token, 500);
//                } else {
//                    doLogin(obj, token);
//                }
//            }
//
//            @Override
//            public void onFailed(Object o) {
//                rescan();
//            }
//        });
//
//    }
//
//    private void setLoginBitmap(final ImageView user_img, String url) {
////        user_img.setImageResource(R.drawable.face_icon_big);
//        ImageLoader.getBitmapRawData(this, url, false, new ImageLoader.ImageListener() {
//            @Override
//            public void onSuccessResponse(Bitmap bitmap, String url, boolean isCached) {
//                if (isFinishing()) {
//                    return;
//                }
//                ImageLoader.putBitmapToCache(ScanActivity.this, url, bitmap);
//                user_img.setImageBitmap(toRoundBitmap(bitmap));
//            }
//
//            @Override
//            public void onErrorResponse(int errorCode) {
//            }
//        }, true, true);
//    }
//
//    /**
//     * 转换图片成圆形
//     *
//     * @param bitmap 传入Bitmap对象
//     * @return
//     */
//    private Bitmap toRoundBitmap(Bitmap bitmap) {
//        int width = bitmap.getWidth();
//        int height = bitmap.getHeight();
//        float roundPx;
//        float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
//        if (width <= height) {
//            roundPx = width / 2.0f;
//            top = 0;
//            bottom = width;
//            left = 0;
//            right = width;
//            height = width;
//            dst_left = 0;
//            dst_top = 0;
//            dst_right = width;
//            dst_bottom = width;
//        } else {
//            roundPx = height / 2.0f;
//            float clip = (width - height) / 2.0f;
//            left = clip;
//            right = width - clip;
//            top = 0;
//            bottom = height;
//            width = height;
//            dst_left = 0;
//            dst_top = 0;
//            dst_right = height;
//            dst_bottom = height;
//        }
//
//        Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
//        Canvas canvas = new Canvas(output);
//
//        final int color = 0xff424242;
//        final Paint paint = new Paint();
//        final Rect src = new Rect((int) left, (int) top, (int) right,
//                (int) bottom);
//        final Rect dst = new Rect((int) dst_left, (int) dst_top,
//                (int) dst_right, (int) dst_bottom);
//        final RectF rectF = new RectF(dst);
//
//        paint.setAntiAlias(true);
//
//        canvas.drawARGB(0, 0, 0, 0);
//        paint.setColor(color);
//        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
//
//        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
//        canvas.drawBitmap(bitmap, src, dst, paint);
//        return output;
//    }
//
//    /**
//     * 查询参数
//     *
//     * @param uri
//     * @param key
//     * @param defaultValue
//     * @return
//     */
//    public String getQuery(Uri uri, String key, String defaultValue) {
//        String s = uri.getQueryParameter(key);
//        try {
//            return s.trim();
//        } catch (Exception ex) {
//            return defaultValue;
//        }
//    }
//
//    private static class QrTokenLoginWhenLogin extends OnLoginSuccessListener<ScanActivity> {
//        private String token;
//        private String action;
//
//        public QrTokenLoginWhenLogin(ScanActivity scanActivity, String token, String action) {
//            super(scanActivity, 50 * 1000);
//            this.token = token;
//            this.action = action;
//        }
//
//        @Override
//        public void onLoginSuccess() {
//            ScanActivity scanActivity = getReference();
//            if (scanActivity != null) {
//                scanActivity.doLoginOPT(token, action);
//            }
//        }
//    }
//
//    private static class NIOLoginBindWhenLogin extends OnLoginSuccessListener<ScanActivity> {
//        private String token;
//
//        public NIOLoginBindWhenLogin(ScanActivity scanActivity, String token) {
//            super(scanActivity, 50 * 1000);
//            this.token = token;
//        }
//
//        @Override
//        public void onLoginSuccess() {
//            ScanActivity scanActivity = getReference();
//            if (scanActivity != null) {
//                scanActivity.httpNIOLoginBind(token);
//            }
//        }
//    }
//
//    private void doLoginOPT(String token, String action) {
//        DebugLog.v(TAG, "doLoginOPT # token=" + token);
//        if (!PassportUtils.isLogin()) {
//
//            backToMultWindowActivity();
//
//            LoginFlow.get().setOnLoginSuccessListener(new QrTokenLoginWhenLogin(this, token, action));
//
//            Intent ie = new Intent();
//            ie.setClass(this.getActivity(), PhoneAccountActivity.class);
//            ie.putExtra(PhoneAccountActivity.KEY_ACTION_ID, IPassportAction.OpenUI.LOGIN_AND_CALLBACK);
//            ie.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            getActivity().startActivity(ie);
//            return;
//        }
//
//        int logincall_action = 0;
//        if (!TextUtils.isEmpty(action)) {
//            try {
//                logincall_action = Integer.parseInt(action);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        if (logincall_action == AuthorizationCall.ACTION_QR_NEWDEVICE
//                || logincall_action == AuthorizationCall.ACTION_LOGIN) {
//            AuthorizationCall authorizationCall = new AuthorizationCall();
//            authorizationCall.action = logincall_action;
//            authorizationCall.data = token;
//            LoginFlow.get().setAuthorizationCall(authorizationCall);
//            Intent authIntent = new Intent(ScanActivity.this, AuthorizationActivity.class);
//            authIntent.putExtra(AuthorizationActivity.INTENT_LOGINCALL, authorizationCall);
//            startActivityForResult(authIntent, REQUEST_CODE_AUTHORIZATION);
//            return;
//        }
//
//        optLogin(token);
//    }
//
//    private void optLogin(final String token) {
////        showLoginLoadingBar(this.getString(R.string.phone_login));
//
//        MdeviceApi.qrTokenLogin(token, new ICallback<Void>() {
//            @Override
//            public void onSuccess(Void obj) {
//                doOPTLoginDirect(token);
//            }
//
//            @Override
//            public void onFailed(Object o) {
//                if (null == getActivity() || getActivity().isFinishing()) {
//                    return;
//                }
////                dismissLoadingBar();
//                if (o != null && o instanceof String) {
//                    ConfirmDialog.show(getActivity(), (String) o, new DialogInterface.OnDismissListener() {
//                        @Override
//                        public void onDismiss(DialogInterface dialog) {
//                            reScan();
//                        }
//                    });
//                } else {
////                    ToastUtils.defaultToast(getActivity(), R.string.tips_network_fail_and_try);
//                    reScan();
//                }
//            }
//        });
//    }
//
//    private void doOPTLoginDirect(String token) {
//        MdeviceApi.qrTokenLoginConfirm(token, new ICallback<Void>() {
//            @Override
//            public void onSuccess(Void obj) {
//                if (null == getActivity() || getActivity().isFinishing()) {
//                    return;
//                }
////                dismissLoadingBar();
////                ConfirmDialog.show(getActivity(), getString(R.string.account_scanlogin_success),
////                        new DialogInterface.OnDismissListener() {
////                            @Override
////                            public void onDismiss(DialogInterface dialog) {
////                                backToMultWindowActivity();
////                                finish();
////                            }
////                        });
////                ConfirmDialog.show(getActivity(), getString(R.string.account_scanlogin_success),
////                        getString(R.string.btn_cancel), new OnClickListener() {
////                            @Override
////                            public void onClick(View v) {
////                                PassportHelper.pingbackClick("accguard_scan_suc_cancel", "accguard_scan_suc");
////                                backToMultWindowActivity();
////                                finish();
////                            }
////                        }, getString(R.string.account_scanlogin_success_right), new OnClickListener() {
////                            @Override
////                            public void onClick(View v) {
////                                PassportHelper.pingbackClick("accguard_scan_suc_set", "accguard_scan_suc");
////                                backToMultWindowActivity();
////                                PassportHelper.toAccountActivity(getActivity(), PhoneAccountActivity.ACCOUNT_PROTECT);
////                                finish();
////                            }
////                        });
//                PassportHelper.pingbackShow("accguard_scan_suc");
//            }
//
//            @Override
//            public void onFailed(Object o) {
//                if (null == getActivity() || getActivity().isFinishing()) {
//                    return;
//                }
////                dismissLoadingBar();
//                if (o != null && o instanceof String) {
//                    ConfirmDialog.show(getActivity(), (String) o, new DialogInterface.OnDismissListener() {
//                        @Override
//                        public void onDismiss(DialogInterface dialog) {
//                            reScan();
//                        }
//                    });
//                } else {
////                    ToastUtils.defaultToast(getActivity(), R.string.tips_network_fail_and_try);
//                    rescan();
//                }
//            }
//        });
//    }
//
//    @Deprecated
//    protected void RetryDoRealConfimLogin(final String token, int delayTime) {
//        if (mainHandler == null) {
//            mainHandler = new Handler(Looper.getMainLooper());
//        }
//        mainHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                doRealConfimLogin(token);
//            }
//        }, delayTime);
//    }
//
//    private void rescan() {
//        reScan();
//    }
//
//    private void reScan() {
//        if (getActivity().isFinishing()) {
//            return;
//        }
////        dismissLoadingBar();
////		if((System.currentTimeMillis()-ToastTime) > 2000)
////		{
////			UIUtils.toastCustomView(getActivity(), -1, getString(R.string.scan_iqiyi), Toast.LENGTH_SHORT);
////			ToastTime = System.currentTimeMillis();
////		}
//
//        if (null != handler) {
////            handler.sendEmptyMessage(R.id.decode_failed);
//        }
//        return;
//    }
//
//    private Activity getActivity() {
//        return this;
//    }
//
//
//    private void initCamera(SurfaceHolder surfaceHolder) {
//        try {
//            CameraManager.get().openDriver(surfaceHolder);
//            if (handler == null) {
//                handler = new CaptureActivityHandler(this, decodeFormats,
//                        characterSet);
//            }
//        } catch (IOException ioe) {
//            DebugLog.v(TAG, ioe.toString());
//            CameraManager.get().setCameraNull();
//            finish();
//        } catch (RuntimeException e) {
//            DebugLog.v(TAG, "Unexpected error initializing camera :" + e.toString());
//            CameraManager.get().setCameraNull();
//            finish();
//        }
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width,
//                               int height) {
//
//    }
//
//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//        if (!hasSurface) {
//            initCamera(holder);
//            hasSurface = true;
//        }
//        DebugLog.v(TAG, "surfaceCreated");
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        hasSurface = false;
//        holder.removeCallback(this);
//        DebugLog.v(TAG, "surfaceDestroyed");
//    }
//
//    public ViewfinderView getViewfinderView() {
//        return viewfinderView;
//    }
//
//    public Handler getHandler() {
//        return handler;
//    }
//
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_BACK:
//                if (null != handler) {
//                    handler = null;
//                }
//
//                backToMultWindowActivity();
//
//                this.finish();
//
//                break;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//
//    private void backToMultWindowActivity() {
//        MultiWindowManager.getInstance().backToMultWindowActivity(this);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        DebugLog.v(TAG, "onActivityResult # requestCode=" + requestCode + ", resultCode=" + resultCode);
//
//        if (requestCode == REQUEST_CODE_AUTHORIZATION) {
//            if (resultCode == Activity.RESULT_OK) {
//                AuthorizationCall authorizationCall = LoginFlow.get().getAuthorizationCall();
//                if (authorizationCall != null) {
//                    if (authorizationCall.action == AuthorizationCall.ACTION_QR_NEWDEVICE
//                            || authorizationCall.action == AuthorizationCall.ACTION_LOGIN) {
//                        optLogin(authorizationCall.data);
//                    }
//                }
//            }
//            LoginFlow.get().setAuthorizationCall(null);
//        }
//
//        if (null != handler) {
////            handler.sendEmptyMessage(R.id.restart_preview);
//        }
//        if (resultCode != RESULT_OK) {
//            if (this.mCodeType == 1 || this.mCodeType == 2) {
//                this.findViewById(R.id.login_fail).setVisibility(View.VISIBLE);
//            }
//            return;
//        }
//
//        switch (requestCode) {
//            case 100:
//                setResult(RESULT_OK);
//                backToMultWindowActivity();
//                this.finish();
//                return;
//            default:
//                break;
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//
//    }
//
//    @Deprecated
//    protected void doLogin(final String authcookie, final String token) {
//        if (getActivity().isFinishing()) {
//            return;
//        }
//        ICommunication<PassportExBean> passportModule = ModuleManager.getInstance().getPassportModule();
//        PassportExBean passportExBean = PassportExBean.obtain(IPassportAction.ACTION_PASSPORT_GET_USERINFO);
//        UserInfo userInfo = passportModule.getDataFromModule(passportExBean);
//        DebugLog.i("myuserinfo", "uname:" + userInfo.getUserAccount());
//        PassportExBean passportExBean2 = PassportExBean.obtain(IPassportAction.ACTION_PASSPORT_GET_ISLOGIN);
//        boolean isLogin = passportModule.getDataFromModule(passportExBean2);
//        if (isLogin) {
//            DebugLog.i("myuserinfo", "has login in");
//            Passport.loginByAuth(authcookie, new RequestCallback() {
//
//                @Override
//                public void onFailed(String code, String failMsg) {
//                    Toast.makeText(QyContext.sAppContext, getResources().getString(R.string.scanner_login_failed), Toast.LENGTH_SHORT).show();
//                    getActivity().finish();
//                    return;
//                }
//
//                @Override
//                public void onSuccess() {
//                    LoginResponse loginResponse = PassportUtils.getUserInfo().getLoginResponse();
//
////					Toast.makeText(QyContext.sAppContext, "用户："+loginResponse.uname, Toast.LENGTH_SHORT).show();
//                    LayoutInflater inflater = LayoutInflater.from(ScanActivity.this);
//                    RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.phone_login_dialog_view_scan, null);
//
//                    final Dialog dialog = new AlertDialog.Builder(ScanActivity.this).create();
//                    dialog.setCancelable(false);
//                    dialog.show();
//                    dialog.getWindow().setContentView(layout);
//
//                    ImageView user_img = (ImageView) layout.findViewById(R.id.user_img);
//                    setLoginBitmap(user_img, loginResponse.icon);
//                    TextView user_id = (TextView) layout.findViewById(R.id.user_id);
//                    user_id.setText(loginResponse.uname);
//
//                    Button btnOK = (Button) layout.findViewById(R.id.dialog_login);
//                    btnOK.setOnClickListener(new OnClickListener() {
//
//                        @Override
//                        public void onClick(View v) {
//                            dialog.dismiss();
//                            /////////////////////////////////
////                            showLoginLoadingBar(ScanActivity.this.getString(R.string.phone_login));
//                            Passport.loginByAuth(authcookie, new RequestCallback() {
//
//                                @Override
//                                public void onSuccess() {
//
////                                    dismissLoadingBar();
//                                    //设置登录成功
//                                    setResult(RESULT_OK);
//                                    getActivity().finish();
//
//                                    ICommunication<PassportExBean> passportModule = ModuleManager.getInstance().getPassportModule();
//                                    boolean isVipValid = passportModule.getDataFromModule(PassportExBean.obtain(IPassportAction.ACTION_PASSPORT_GET_ISVIPVALID));
//                                    if (!isVipValid) {
////                                        VipSkinController.getInstance().clearSkin();
//                                    }
//                                }
//
//                                @Override
//                                public void onFailed(String code, String failMsg) {
////                                    dismissLoadingBar();
//                                    getActivity().finish();
//                                }
//
//                                @Override
//                                public void onNetworkError() {
////                                    dismissLoadingBar();
//                                    getActivity().finish();
//                                }
//
//                            });
//                            //////////////////////////////
//                        }
//                    });
//
//                    Button btnCancel = (Button) layout.findViewById(R.id.dialog_cancel);
//                    btnCancel.setOnClickListener(new OnClickListener() {
//
//                        @Override
//                        public void onClick(View v) {
//                            dialog.dismiss();
//                            reScan();
//                        }
//                    });
//                    return;
//                }
//
//                @Override
//                public void onNetworkError() {
//                    Toast.makeText(QyContext.sAppContext, getResources().getString(R.string.scanner_network_error), Toast.LENGTH_SHORT).show();
//                    getActivity().finish();
//                    return;
//                }
//
//            });
//        } else {
////            showLoginLoadingBar(ScanActivity.this.getString(R.string.phone_login));
//            Passport.loginByAuth(authcookie, new RequestCallback() {
//
//                @Override
//                public void onSuccess() {
//
////                    dismissLoadingBar();
//                    //设置登录成功
//                    setResult(RESULT_OK);
//                    getActivity().finish();
//                }
//
//                @Override
//                public void onFailed(String code, String failMsg) {
//                    Toast.makeText(QyContext.sAppContext, getResources().getString(R.string.scanner_login_failed), Toast.LENGTH_SHORT).show();
////                    dismissLoadingBar();
//                    getActivity().finish();
//                }
//
//                @Override
//                public void onNetworkError() {
//                    RetryDoRealConfimLogin(token, 10);
//                }
//
//            });
//        }
//    }
}
