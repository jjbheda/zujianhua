-dontwarn
-dontoptimize
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-ignorewarnings
-keepattributes *Annotation*

-useuniqueclassmembernames
-dontwarn android.net.http.SslError

# *默认不添加混淆的行号，打包工具动态配置
# *内部测试包或灰度包，打包时保留行号配置，包体积会增加500KB以上
# *正式发布的渠道包或给第三方的包，不保留行号
#-renamesourcefileattribute SourceFile
#-keepattributes SourceFile,LineNumberTable


-keep public class * extends android.app.Activity
-keep public class * extends android.support.v4.app.FragmentActivity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class * extends android.app.backup.BackupAgentHelper
#-keep public class * extends android.preference.Preference
#-keep public class com.android.vending.licensing.ILicensingService
-keep public class * extends android.os.IInterface

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class org.qiyi.android.video.MainActivity {
	public void hidePlayerUi();
	public void showPlayerUi();
}

#------------------  下方是android平台自带的排除项，这里不要动         ----------------

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

#-keepclassmembers class * extends android.app.Activity {
#   public void *(android.view.View);
#}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class org.iqiyi.video.ui.SNSBindWebview$MyJavaSriptInterface {
    <methods>;
}
-keepclassmembers class org.qiyi.android.video.ui.account.sns.PhoneSNSBind$MyJavaSriptInterface {
    <methods>;
}

-keepclassmembers class org.iqiyi.video.ui.SNSBindForSingleWebview$MyJavaSriptInterface {
    <methods>;
}

-keepclassmembers class org.iqiyi.video.advertising.AdsWebView$MyJavaSriptInterface {
    <methods>;
}

# ConstructProtobufData keep

#------------------  下方是第三方的排除项目         ----------------
#-libraryjars ../5.0_VideoPlayer/libs/android-support-v4.jar
-dontwarn android.support.v4.**
-dontwarn android.support.v7.**
-dontwarn org.qiyi.android.corejar.**
-dontwarn org.qiyi.basecore.algorithm.**
-dontwarn org.qiyi.basecore.engine.**
-dontwarn org.qiyi.basecore.imageloader.**
-dontwarn org.qiyi.basecore.utils.**
-dontwarn org.qiyi.basecore.jobquequ.JobManagerUtils
-keep class org.qiyi.basecore.jobquequ.AsyncJob
-keep class org.qiyi.basecore.jobquequ.JobManagerUtils
-keep class android.support.v4.** { *; }
-keep class android.support.v7.** { *; }
-keep class android.support.annotation.** { *; }
#-baidu lite sdk
-keep class com.baidu.bottom.** { *; }

-keep class org.qiyi.qyscanqrcode.scan.**{*;}

-dontwarn org.qiyi.android.video.view**
#-keep class android.support.v4.view.AccessibilityDelegateCompat$AccessibilityDelegateJellyBeanImpl {*;}
#-keep class android.support.v4.view.accessibility.AccessibilityNodeProviderCompat$AccessibilityNodeProviderKitKatImpl {*;}

-keep public class * extends android.support.v4.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.app.Fragment

#-libraryjars libs/AnyShare-FullSDK.jar
-dontwarn com.lenovo.**

#-libraryjars libs/qrdemo.jar
#-dontwarn com.testCamera.**

#-libraryjars ../5.0_VideoPlayer/libs/pushservice-3.2.0.jar
-dontwarn com.baidu.**
-keep class com.baidu.** { *; }

#-libraryjars MiPush_SDK_Client.jar
-keep class org.qiyi.android.commonphonepad.pushmessage.xiaomi.XiaoMiPushMessageReceive {*;}

-dontwarn com.xiaomi.mipush.**
-keep class com.xiaomi.mipush.sdk.** { *; }
-dontwarn com.xiaomi.push.**
-keep class com.xiaomi.push.service.** { *; }

#-dontwarn com.xiaomi.channel.commonutils.**
#-keep class com.xiaomi.channel.commonutils.** {*;}
#-dontwarn com.xiaomi.mipush.sdk.**
#-keep class com.xiaomi.mipush.sdk.**{*;}
#-dontwarn com.xiaomi.push.log.**
#-keep class com.xiaomi.push.log.**{*;}
#-dontwarn com.xiaomi.push.service.receivers.**
#-keep class com.xiaomi.push.service.receivers.**{*;}
#-dontwarn com.xiaomi.xmpush.thriftc.**
#-keep class com.xiaomi.xmpush.thriftc.**{*;}
#-dontwarn org.apache.thrift.**
#-keep class org.apache.thrift.**{*;}


#-libraryjars ../QYCoreJar/libs/cardboard.jar
-dontwarn com.google.vrtoolkit.cardboard.**
-keep class com.google.vrtoolkit.cardboard.** { *; }

#-libraryjars ../5.0_VideoPlayer/libs/mAppTracker.jar
-dontwarn cn.com.iresearch.mapptracker.**
-keep class cn.com.iresearch.mapptracker.** { *; }

#mVideoTracker.jar
-dontwarn cn.com.iresearch.mvideotracker.**
-keep class cn.com.iresearch.mvideotracker.** { *; }

#-libraryjars ../munion-android-qiyilib/libs/munion_sdk_lite_v6.7.jar
-dontwarn com.umeng.**
-keep class com.umeng.** { *; }
-keep public class com.qiyi.video.R$*{
  public static final int *;
  public static final int[] *;
}

-keep public class com.qiyi.video.market.R$*{
  public static final int *;
  public static final int[] *;
}


#********
-dontwarn org.qiyi.android.multidex**
-keep class org.qiyi.android.multidex.** { *; }

#**********

#插件中心
#-libraryjars ../QYVideoClient/libs/plugin_v1.0.jar
-keep class org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin {
    public <methods>;
}

-keep class org.qiyi.pluginlibrary.utils.ContextUtils {
    public <methods>;
}

-keepclassmembers class org.qiyi.pluginlibrary.PluginInstrument {
    public <methods>;
}

-keepclassmembers class org.qiyi.pluginlibrary.context.CustomContextWrapper {
    public boolean isOppoStyle();
}

-keepclassmembers class org.qiyi.pluginlibrary.component.InstrActivityProxy {
    public boolean isOppoStyle();
}
-keep class org.qiyi.android.plugin.baiduvoice.** {*; }
-keep class org.qiyi.android.plugin.nativeInvoke.** {*; }
-keep class org.qiyi.android.plugin.appstore.** {*; }
-keep class org.qiyi.android.plugin.paopao.** {*; }
-keep class org.qiyi.android.plugin.share.** {*; }
-keep class org.qiyi.android.plugin.videotransfer.** {*; }
-keep class org.qiyi.android.plugin.reader.** {*; }
-keep class org.qiyi.android.plugin.baiduwallet.** {*; }
-keep class org.qiyi.android.corejar.common.callback.** { *; }
-keep class org.qiyi.android.corejar.syncRequest.** { *; }
-keep class org.qiyi.android.plugin.common.** {*; }
-keep class org.qiyi.android.plugin.router.** {*; }
-keep class org.qiyi.android.plugin.ishow.** {*; }
-keep class org.qiyi.android.plugin.qiyipay.** {*; }
-keep class org.qiyi.android.plugin.qiyimall.** {*; }
-keep class org.qiyi.android.corejar.plugin.common.** {*; }
-keep class org.qiyi.android.corejar.plugin.router.** {*; }
-keep class org.qiyi.android.corejar.model.appstore.** {*; }
-keep class org.qiyi.android.plugin.ppq.** {*; }
-keep interface org.qiyi.android.corejar.plugin.qimo.IQimoService{*;}
-keep class org.qiyi.android.corejar.plugin.qimo.IQimoService$* {*; }
-keep interface org.qiyi.android.corejar.plugin.qimo.IQimoService$* {*; }
-keep class org.qiyi.android.corejar.plugin.qimo.**{*; }
-keep class org.qiyi.android.plugin.qimo.QimoPluginAction{*;}
-keep class okio.**{*;}
-keep class com.squareup.okhttp.**{*;}
-keep class okhttp3.**{*;}
-keep class org.qiyi.android.plugin.utils.PluginPackageUtils{*;}
-keep class org.qiyi.android.video.plugin.utils.PluginInfoUtils{*;}

-keepnames class org.qiyi.android.video.plugin.controller.bean.**

#***************************
#播放sdk
-keep class org.iqiyi.video.event.** { *; }
-keep class org.iqiyi.video.data.PlayerError{*;}
-keep class org.iqiyi.video.facede.** { *; }
-keep class com.video.qiyi.sdk.v2.** { *; }
-keep class org.iqiyi.video.mode.PlayData{*;}
-keep class org.iqiyi.video.mode.PlayData$Builder{
    <fields>;
    <methods>;
}
-keep interface org.iqiyi.video.player.PlayerStatusChangeListener{*;}
-keep class org.iqiyi.video.mode.PlayerRate{*;}

#***************************
-keep public class org.qiyi.android.plugin.utils.PluginDeliverUtils{
	<fields>;
    <methods>;
}
-keep public class org.qiyi.android.corejar.thread.impl.IfaceGetSearchAlbumTask{
	<fields>;
    <methods>;
}
-keep public class org.qiyi.android.video.controllerlayer.PlayerControllerForVip{
  	<fields>;
    <methods>;
}
-keep public class org.qiyi.android.corejar.thread.IDataTask$*{
    <fields>;
    <methods>;
}
-keep public class org.qiyi.android.corejar.utils.StringSecurity {
    <fields>;
    <methods>;
}

-keep public class com.mcto.ads.AdsClient{
	<fields>;
    <methods>;
}
-keep public class com.mcto.ads.CupidAd{
	<fields>;
    <methods>;
}
##########合并pingback###########
-keep public class com.qiyi.pingback.merge.model.PingbackModel{
	<fields>;
}

####EventBus混淆###
-keepclassmembers class ** {
     @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

-keep public class com.mcto.cupid.**{ *; }

-keep public interface com.mcto.cupid.IAdJsonDelegate{
	<fields>;
    <methods>;
}

-keep public class org.mcto.video.cupid.callback.CupidJsonDelegate{
	<fields>;
    <methods>;
}

-keep public interface com.mcto.cupid.IAdObjectAppDelegate{
	<fields>;
    <methods>;
}

-keep public class org.iqiyi.video.cupid.callback.CupidAppJsonDelegate{
	<fields>;
    <methods>;
}

#下载插件用到主工程中的类
-keep public class org.qiyi.android.corejar.engine.** {*; }
-keep public class org.qiyi.android.corejar.utils.** {*; }
-keep public class org.qiyi.basecore.utils.** {*; }

#databinding mvvm
-keep class android.databinding.** { *; }
-keep interface android.databinding.** { *; }
-dontwarn android.databinding.**

-keep public class org.qiyi.android.corejar.database.DownloadBeanOperator{
	<fields>;
	<methods>;
}

-keep public class org.qiyi.android.coreplayer.NativePlayer{
	<fields>;
	<methods>;
}

#crash reporter
-keep public class com.qiyi.crashreporter.core.NativeCrashHandler{
	public static void nativeCallback(...);
}
-keep class com.qiyi.crashreporter.bean.** { *; }

-keep public class org.qiyi.android.corejar.common.RateConstants{
	<fields>;
	<methods>;
}


#-keep public class org.qiyi.android.video.controllerlayer.utils.P2PTools{
#	<fields>;
#	<methods>;
#}


-keep public class org.qiyi.android.video.controllerlayer.ControllerManager {
    <fields>;
    <methods>;
}

-keep public class org.qiyi.android.corejar.thread.impl.IfaceGetDownloadInfo {
    <fields>;
    <methods>;
}

-keep public class org.qiyi.video.module.download.exbean.XTaskBean{
	<fields>;
	<methods>;
}

-keep public class org.qiyi.video.module.download.exbean.DownloadAPK{
	<fields>;
	<methods>;
}

-keep public class org.qiyi.android.corejar.model.DownloadBean{
	<fields>;
	<methods>;
}

-keep public class org.qiyi.video.module.download.exbean.DownloadObject{
	<fields>;
	<methods>;
}

-keep public class org.qiyi.android.video.controllerlayer.utils.CommonMethodNew{
	<fields>;
	<methods>;
}

-keep public class org.qiyi.video.initlogin.InitLogin{
	<fields>;
	<methods>;
}

-keep public class org.qiyi.video.module.download.exbean.DownloadObject$*{
	<fields>;
	<methods>;
}

-keep public class org.qiyi.android.corejar.model.Game{
	<fields>;
}

-keep public class org.qiyi.basecore.imageloader.ImageLoader{
	<fields>;
	<methods>;
}
-keep public class org.qiyi.basecore.imageloader.ImageLoader$ImageListener{
	<methods>;
}

#-keep public class com.iqiyi.video.download.** {*; }
-keep public class org.qiyi.android.corejar.thread.IParamName{*;}

-keep public class org.qiyi.android.corejar.thread.impl.BaseIfaceDataTask{
  	<fields>;
    <methods>;
}
-keep public class org.qiyi.android.corejar.deliver.controller.IResearchStatisticsController {
    <fields>;
    <methods>;
}

#分享
-dontwarn org.qiyi.android.share.**
-keepclassmembers class org.qiyi.android.share.ShareLoginForSingleWebview$MyJavaSriptInterface{
    <methods>;
}
-keepclassmembers class org.qiyi.android.share.ShareBindForSingleWebview$MyJavaSriptInterface{
    <methods>;
}

-keepclassmembers class org.qiyi.android.share.SNSBindWebview$MyJavaSriptInterface {
    <methods>;
}
-keepclassmembers class org.qiyi.android.share.SNSLoginWebview$MyJavaSriptInterface {
    <methods>;
}

-keepclassmembers class com.iqiyi.passportsdk.thirdparty.BindPhoneWebView$NewDeviceInterface{
   	<methods>;
}
-keepclassmembers class com.iqiyi.passportsdk.thirdparty.BindPhoneWebView$BindPhoneInterface{
   	<methods>;
}
-keepclassmembers class org.qiyi.basecore.widget.commonwebview.AbsCommonJsBridge{
   	<methods>;
}
-keepclassmembers class org.qiyi.basecore.widget.commonwebview.CheckSupportUploadNew{
   	<methods>;
}
-keepclassmembers class org.qiyi.basecore.widget.commonwebview.CustomWebChromeClient{
   	<methods>;
}
-keepclassmembers class org.qiyi.basecore.widget.commonwebview.CustomWebViewClient{
   	<methods>;
}
-keepclassmembers class org.qiyi.basecore.widget.commonwebview.websocket.WebSocketFactory{
   	<methods>;
}

#JS
-dontwarn org.qiyi.android.video.customview.webview.javascript.**
-keep class org.qiyi.android.video.customview.webview.javascript.**

#微信朋友圈分享
-dontwarn com.tencent.mm.**
-keep class com.tencent.mm.** { *;}
#-keep class com.tencent.mm.sdk.openapi.** implements com.tencent.mm.sdk.openapi.WXMediaMessage$IMediaObject {*;}

#易信分享
-dontwarn im.yixin.**
-keep class im.yixin.** { *;}

#-libraryjars ../munion-android-qiyilib/libs/moplus_3.0.1.jar
-dontwarn com.baidu.**
-keep class com.baidu.** { *; }

#二维码
#-keep class com.google.zxing.** { *; }

#-libraryjars ../5.0_coreplayer/libs/hardwaredecode.jar
-dontwarn org.qiyi.android.hardwaredecode.**
-keep class org.qiyi.android.hardwaredecode.** { *; }

#-libraryjars ../5.0_coreplayer/libs/libiqiyidlnasdk.jar
-dontwarn org.cybergarage.**
-keep class org.cybergarage.** { *; }
#-keep class com.iqiyi.android.dlna.sdk.** { *; }
#-keep class com.iqiyi.android.sdk.dlna.keeper.** { *; }

#-libraryjars ../5.0_controllerlayer/libs/commons-codec-1.8.jar
-dontwarn org.apache.**
-keep class org.apache.** { *; }

#-libraryjars ../5.0_controllerlayer/libs/PPS_MOB_BI_SDK_1.8.jar
#-dontwarn tv.pps.bi.**
#-keep class tv.pps.bi.** { *; }

-keep class tv.pps.bi.userbehavior.UserBehavior {
    <fields>;
    <methods>;
}
# DecodeProtobufData keep
-keep public class tv.pps.bi.protobuf.DecodeProtobufData {
    public <fields>;
    public <methods>;
}


#universal-image-loader-1.8.4-with-sources.jar
-dontwarn com.nostra13.universalimageloader.**
-keep class com.nostra13.universalimageloader.** { *; }

-dontwarn tv.pps.bi.pps.**


#libiqiyipushservice.jar
-dontwarn com.iqiyi.pushservice.**
-keep class com.iqiyi.pushservice.** { *; }
-dontwarn org.eclipse.paho.client.mqttv3.**
-keep class org.eclipse.paho.client.mqttv3.** { *; }
#-dontwarn com.iqiyi.nativeprocess.**
#-keep class com.iqiyi.nativeprocess.** { *; }
-keep class com.iqiyi.daemonservice.** {*;}

#MMAndroid_v1.2.4.3.jar
-dontwarn cn.com.mma.mobile.tracking.**
-keep class cn.com.mma.mobile.tracking.** { *; }
-dontwarn cn.mmachina.**
-keep class cn.mmachina.** { *; }

#<!--appstore中 hcdndownloader.jar-->
#hcdndownloader.jar
-dontwarn com.qiyi.hcdndownloader.**
-keep class com.qiyi.hcdndownloader.** { *; }


#-libraryjars ../5.0_QYVideoClient/libs/ppsdownloadlibrary.jar
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }
-dontwarn tv.pps.mobile.**
-keep class tv.pps.mobile.**{*;}

#<!--QYVideoCient项目中不混淆的类-->
-keep public class android.net.http.SslError
-dontwarn android.webkit.**
-keep public class android.webkit.WebViewClient

#<!--QYContext工程混淆规则-->
-keep public class org.qiyi.context.QyContext{ public *; }
-keep public class org.qiyi.context.provider.QyContextProvider{ public *;}
-keep public class org.qiyi.context.mode.AreaMode{ public *;}
-keep public class org.qiyi.context.constants.AppConstants{ public *;}
-keep public class org.qiyi.context.utils.ApkInfoUtil{ public *; }

#<!--qycorejar中不混淆的类-->
#-libraryjars ../5.0_QYCoreJar/bin/5.0_qycoreJar.jar
-dontwarn hessian.**
-keep class hessian.** { *; }
#<!--model里面不混淆的类-->
-keep class org.qiyi.android.corejar.model.AD{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.ActiviteUserInfo{
   <fields>;
 }
-keep class org.qiyi.android.corejar.model.BuyData{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.BuyInfo{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.Card{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.CategoryExt{
	<fields>;
}

-keep class org.qiyi.android.corejar.model.DynamicInfo{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.DynamicInfo.albumInfo{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.DynamicInfo.videoInfo{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.LibrarysObject{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.NewAd{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.PlayerTabInfo{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.SearchResult.Weight{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.Star{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.UgcVideoInfo{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.Vote{
	<fields>;
}
-keep class org.qiyi.android.corejar.model.Vote.voteUserJoin{
	<fields>;
}
-keep class org.qiyi.android.message.pingback.PingBackForSpec{
	<fields>;
}
-dontwarn com.caucho.**
-keep class com.caucho.** { *; }

-keep public class com.iqiyi.video.upload.ppq.model.PPQUserInfo {
    public <fields>;
    public <methods>;
}

#iqiyi-sso-sdk.jar
-dontwarn com.iqiyi.sso.sdk.**
-keep class com.iqiyi.sso.sdk.** { *; }


#<!--coreplayer中不混淆的类-->
#-libraryjars ../5.0_coreplayer/bin/5.0_coreplayer.jar
-dontwarn org.qiyi.android.coreplayer.**
-keep class org.qiyi.android.coreplayer.utils.HelpFunction { *; }
-keep class org.qiyi.android.coreplayer.TransCodeUtils { *; }
-dontwarn com.media.ffmpeg.**
-keep public class com.media.ffmpeg.FFMpegPlayer { *; }


-keep class com.comscore.** { *; }
-dontwarn com.comscore.**


-dontwarn tv.pps.jnimodule.localserver.*
-keep class tv.pps.jnimodule.localserver.** {*;}

-dontwarn tv.pps.mobile.emsbitplayer.*
-keep class tv.pps.mobile.emsbitplayer.** {*;}


-dontwarn tv.pps.module.player.video.hardware.*
-keep class tv.pps.module.player.video.hardware.** {*;}


-dontwarn com.qiyi.media.*
-keep class com.qiyi.media.** {*;}

# DecodeProtobufData keep
 -keepclassmembers class * implements java.io.Serializable {
              static final long serialVersionUID;
              static final java.io.ObjectStreamField serialPersistentFields;
              private void writeObject(java.io.ObjectOutputStream);
              private void readObject(java.io.ObjectInputStream);
              java.lang.Object writeReplace();
              java.lang.Object readResolve();
            }

 ##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

-keep class com.google.gson.** {*;}

##---------------End: proguard configuration for Gson  ----------

-keep class com.mcto.player.nativemediaplayer.** { *; }

-keep class com.mcto.player.mctoplayer.** { *; }

-keep class com.mcto.player.playerutils.** { *; }

-keep class com.mcto.player.livecontroller.** { *; }

-keep interface com.mcto.player.livecontroller.** { *; }

-keep interface com.iqiyi.video.download.service.** { *; }



-keep class org.qiyi.android.video.ui.account.** { *; }

-keep class org.qiyi.android.corejar.deliver.** { *; }

-keep class org.qiyi.video.module.deliver.** { *; }

-keep class com.qiyi.video.wxapi.** { *; }

-keep class com.android.iqiyi.sdk.** { *; }

-dontwarn com.android.iqiyi.sdk.**


-keep class org.iqiyi.video.view.CommonWebView$* {
    <fields>;
    <methods>;
}

-keep class org.iqiyi.video.vote.bean.** { *; }


-dontwarn android.media.**
-dontwarn com.alimm.ad.mobile.open.**

-dontwarn org.qiyi.android.corejar.BuildConfig
-dontwarn org.qiyi.android.videoplayer.BuildConfig
-dontwarn tv.pps.appstore.BuildConfig
-dontwarn tv.pps.jnimodule.BuildConfig
-dontwarn org.qiyi.basecore.BuildConfig

-keep class com.qiyi.video.support.lib.pulltorefresh.** { *; }

-keep class com.qiyi.android.demomonitor.Daemon { public static void main(java.lang.String[]); }

#keep for tickets plugin
#-keep public class org.qiyi.android.corejar.deliver.reddot.DiscoveryReddotBaiduStat{
#  	<fields>;
#    <methods>;
#}

-keep public class org.qiyi.android.tickets.invoke.TKPageJumpUtils{
  	<fields>;
    <methods>;
}

-keep public class org.qiyi.android.gps.GpsLocByBaiduSDK{
  	<fields>;
    <methods>;
}

-keep interface org.qiyi.android.gps.GpsLocByBaiduSDK$IGPSWebView{
    <methods>;
}


# networklib库
-keep public class org.qiyi.net.Request{
    <fields>;
    <methods>;
}

-keep public class org.qiyi.net.Request$*{
    <fields>;
    <methods>;
}

-keep public class org.qiyi.net.HttpManager{
    <methods>;
}

-keep public class org.qiyi.net.HttpManager$*{
    <fields>;
    <methods>;
}

-keep public class org.qiyi.net.callback.IHttpCallback{
    <methods>;
}

-keep public class org.qiyi.net.callback.BaseHttpCallBack{
    <methods>;
}

-keep public class org.qiyi.net.convert.IResponseConvert{
    <methods>;
}

-keep class org.qiyi.net.exception.** {*;}
#networklib end

-keep class org.qiyi.android.video.controllerlayer.plugininterface.** {*;}


# 小米sso oauth-xiaomiopenauth.jar
-keep class miui.net.** { *; }

# 小米sso相关代码
-keep class org.qiyi.android.video.ui.account.util.XiaoMiSSOUtil { *; }
-keep class org.qiyi.android.video.ui.account.util.XiaoMiSSOUtil$* { *; }

-keep public class org.qiyi.android.corejar.model.PluginDataExt{
	<fields>;
    <methods>;
}

# -----------------------------------------------------------------------
# ---------------------------- 支付相关 ----------------------------------

-keep public class org.qiyi.android.video.pay.IQYPayManager { public *; }
-keep public class org.qiyi.android.video.pay.common.models.CashierPayResult { public *;}


# ----------------------------- 支付相关 ---------------------------------
# -----------------------------------------------------------------------

# NetDoctor jar不参与混淆避免本地方法调用找不到
-keep public class com.netdoc.** {*;}

# basecore filedownload
-keep public class org.qiyi.basecore.filedownload.FileDownloadConstant{
	<fields>;
    <methods>;
}
-keep public class org.qiyi.basecore.filedownload.FileDownloadInterface{
	<fields>;
    <methods>;
}
-keep public class org.qiyi.basecore.filedownload.FileDownloadCallbackImp{
	<fields>;
    <methods>;
}
-keep public class org.qiyi.basecore.filedownload.FileDownloadNotificationConfiguration{
	<fields>;
    <methods>;
}
-keep public class org.qiyi.basecore.filedownload.FileDownloadStatus{
	<fields>;
    <methods>;
}
-keep public class org.qiyi.basecore.filedownload.FileDownloadStatus$DownloadConfiguration{
	<fields>;
    <methods>;
}

-keep class * implements org.qiyi.basecore.filedownload.FileDownloadStatus$DownloadConfiguration {
  <methods>;
  <fields>;
}

# BaseCore Card库不参与混淆
-keep class org.qiyi.basecore.card.**{*;}

# QYCommonCardView 不参与混淆
-keep class com.qiyi.card.common.**{*;}

# QYCardView 不参与混淆
-keep class com.qiyi.card.**{*;}

# for fresco
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip

# Do not strip any method/class that is annotated with @DoNotStrip
-keep @com.facebook.common.internal.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.common.internal.DoNotStrip *;
}

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

-keep public class com.qiyi.video.fragment.SubscribeFragment{
	<fields>;
    <methods>;
}

-keep public class com.qiyi.Protect {
    public <fields>;
    public <methods>;
}

-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn com.facebook.drawee.view.DraweeView

-keep class com.baidu.sapi2.** {*;}
#-keep class com.sina.sso.** {*;}
-keepattributes JavascriptInterface
-keepattributes *Annotation*

-dontwarn com.facebook.**
-keep class com.facebook.** {*;}

# .......................baiduwallet start...............
# baiduwallet start
-keep interface com.baidu.wallet.core.NoProguard
-keep public class * implements com.baidu.wallet.core.NoProguard {
    public protected *;
}
-keep class com.baidu.wallet.** { *; }
-keep class com.baidu.android.pay.** { *; }
-keep class com.baidu.android.lbspay.** { *; }
-keep class com.baidu.apollon.**{*;}
-keep class com.baidu.seclab.sps.** { *; }
-keep class com.baidu.BankCardProcessing
-keep class com.baidu.BCResult
-dontwarn com.baidu.searchbox.plugin.api.**
# baiduwallet end
# ......................baiduwallet end..................

#.......................paopao CommonWebview...........................
-keep class org.qiyi.basecore.algorithm.AESAlgorithm{
    <fields>;
    <methods>;
}
-keep class org.qiyi.basecore.utils.SharedPreferencesFactory{
    <fields>;
    <methods>;
}
-keep class org.qiyi.android.corejar.thread.IfaceResultCode{
    <fields>;
    <methods>;
}
-keep class com.iqiyi.passportsdk.model.UserInfo{
    <fields>;
    <methods>;
}
-keep class com.iqiyi.passportsdk.model.UserInfo$LoginResponse{
    <fields>;
    <methods>;
}
-keep class org.qiyi.video.module.event.passport.UserTracker{
    <methods>;
}
-keep class com.iqiyi.passportsdk.model.UgcInfo{
    <fields>;
    <methods>;
}
-keep class com.iqiyi.passportsdk.model.SNSBindInfo{
    <fields>;
    <methods>;
}
-dontwarn com.iqiyi.passportsdk.**

#.......................paopao CommonWebview.end.........................

#.......................alipay sdk 15.0.8 begin.........................
-dontwarn com.alipay.android.phone.mrpc.core.i
#.......................alipay sdk 15.0.8 end.........................


# pingback for 插件
-keep public class org.qiyi.android.corejar.pingback.PingbackManager{
	<fields>;
    <methods>;
}

-keep public class org.qiyi.android.corejar.pingback.Pingback{
	<fields>;
    <methods>;
}

#livechat sdk start
-keep public class com.iqiyi.sdk.android.livechat.api.** {
    public <fields>;
    public <methods>;
}

-keep public class com.iqiyi.sdk.android.livechat.net.** {
    public <fields>;
    public <methods>;
}

-keep public class com.iqiyi.sdk.android.livechat.** {
    public <fields>;
    public <methods>;
}

-keep class com.iqiyi.nativeprocess.NativeProcess {
    int mParentPid;
    android.content.Context mContext;
    abstract void runOnSubprocess();
    final int getParentPid();
    android.content.Context getContext();
    native <methods>;
}

-keep class com.iqiyi.nativeprocess.WatchDog {
    void runOnSubprocess();
}

#守护进程
-keep class org.qiyi.android.daemon.NativeDaemonAPI{*;}


-keep public class * extends android.app.Application

-keep public class * extends android.app.Service

-keep public class * extends android.content.BroadcastReceiver
#livechat sdk start

#proguard configuratoin on paopao begin
#-keep public class com.iqiyi.paopao.R$*{
#  public static final int *;
#  public static final int[] *;
#}
#-keep class com.iqiyi.paopao.common.plugin.** {*;}
#-keep class com.iqiyi.paopao.common.ui.activity.GroupDetailsActivity {*;}
-keep public class org.apache.** {*;}
-keep public class org.jivesoftware.** {*;}
-keep public class org.xbill.DNS.** {*;}
-keep public class de.measite.smack.** {*;}
-keep public class com.iqiyi.hcim.** {*;}
-keep public class com.kenai.jbosh.** {*;}
-keep public class com.novell.sasl.client.** {*;}
-keep public class org.apache.cordova.** {*;}
-keep class org.apache.cordova.CoreAndroid {*;}
#-keep public class org.iqiyi.video.paopao.** {*;}
#高斯模糊报错
-keep public class com.enrique.stackblur.** {*;}

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

#-keep class com.iqiyi.plug.papaqi.**{*;}
-keep class com.iqiyi.plug.ppq.common.**{*;}

-dontwarn org.apache.cordova.engine.**
-dontwarn org.qiyi.android.**
-dontwarn com.iqiyi.plug.papaqi.ui.view.internal.**
-dontwarn com.android.iqiyi.sdk.**
-dontwarn com.iqiyi.plug.ppq.common.**
-dontwarn okio.**
-dontwarn com.squareup.okhttp.**

#video edit sdk 中的代码不混淆
#-dontwarn com.iqiyi.plug.papaqi.sdk.videoedit.**

#不混淆底层的库
-dontwarn com.iqiyi.video.ppq.**
-keep class com.iqiyi.video.ppq.camcorder.**{*;}
-keep class com.iqiyi.video.ppq.gles.**{*;}
-keep class com.iqiyi.video.mediaplayer.**{*;}
-keep class okio.**{*;}
-keep class com.squareup.okhttp.**{*;}
-dontwarn org.cocos2dx.lib.ppq.encoder.**
-keep class org.cocos2dx.lib.ppq.encoder.**{*;}
-keep class com.iqiyi.sdk.imageload.**{*;}

-keep public class android.opengl.** {*;}



-keep class com.android.share.opengles.** {*;}

-keep class com.baidu.** {*;}

-keepclassmembers class ** {
    public void onEvent*(***);
}

#gif sdk 中的代码不混淆
-keep class org.qiyi.android.gif.**{*;}

#Volley 不混淆
-keep class com.android.volley.**{*;}

# Keep our interfaces so they can be used by other ProGuard rules.
# See http://sourceforge.net/p/proguard/bugs/466/
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip

# Do not strip any method/class that is annotated with @DoNotStrip
-keep @com.facebook.common.internal.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.common.internal.DoNotStrip *;
}

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn com.android.volley.toolbox.**
#proguard configuratoin on paopao end

#WebView插件使用的基线站外视频native播放
-dontwarn org.iqiyi.video.outside.**
-keep class org.iqiyi.video.outside.**{*;}

#Card 3.0相关的基类不参与混淆
-keep class org.qiyi.basecard.v3.**{*;}
#Card 通信相关模块
-keep class org.qiyi.basecard.common.channel.**{*;}

#cocos2dx相关不混淆，插件会使用
-keep class org.cocos2dx.lib.** {*;}

-keep class com.chukong.cocosplay.client.** {*;}

-keep class com.enhance.gameservice.** {*;}

# qyapm
-dontwarn com.qiyi.qyapm.agent.**
-dontwarn com.qiyi.video.QYApmAdapter
-keep class com.qiyi.qyapm.agent.** { *; }
-keep class com.qiyi.video.QYApmAdapter { *; }

#Hotfix
-keep class com.qiyi.qyhotfix.QYHotFix {
    public <fields>;
    public <methods>;
}
-keep class com.qiyi.video.hotfix.QYTinkerLoader {
    public <fields>;
    public <methods>;
}
-keep class com.qiyi.qyhotfix.QYTinkerManager {
    *;
}
#VideoApplication代理类，需反射调用
-keep class com.qiyi.video.VideoApplicationDelegate {
    public <fields>;
    public <methods>;
}


# alipay sdk begin
-keep class com.alipay.android.app.IAlixPay{*;}
-keep class com.alipay.android.app.IAlixPay$Stub{*;}
-keep class com.alipay.android.app.IRemoteServiceCallback{*;}
-keep class com.alipay.android.app.IRemoteServiceCallback$Stub{*;}
-keep class com.alipay.sdk.app.PayTask{ public *;}
-keep class com.alipay.sdk.app.AuthTask{ public *;}
-dontwarn android.net.SSLCertificateSocketFactory
# alipay sdk end

# Gson 数据解析
-keep class org.qiyi.video.mymain.model.bean.** { *; }
-keep class com.iqiyi.paopao.starwall.entity.obfuscationfree.** { *; }

#DanmaKuFlame B站弹幕
-keep class master.flame.danmaku.** { *; }
-keep class tv.cjump.jni.** {*;}

#react native, https://github.com/square/okio/issues/60
-keep class com.facebook.react.** { *; }
-keep class com.qiyi.qyreact.** { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okhttp3.internal.huc.**
-keep class com.qiyi.video.proxyapplication.QYReactApplication { *; }
-keep class org.qiyi.video.react.** { *; }

#高级编辑
-keep class com.iqiyi.gpufilter.** {*; }

#-libraryjars HwPush_SDK_V2705.jar
-keep class com.huawei.android.pushagent.**{*;}
-keep class com.huawei.android.pushselfshow.**{*;}
-keep class com.huawei.android.microkernel.**{*;}
-keep class com.baidu.mapapi.**{*;}

#eventbus3.0
-keep class org.greenrobot.eventbus.**{*;}

#module manager
-keep class org.qiyi.video.module.icommunication.**{
    public <methods>;
    public <fields>;
}


-keep class org.qiyi.video.module.action.**{
    *;
}

-keep class * extends org.qiyi.video.module.icommunication.ModuleBean{
        public <fields>;
        public <methods>;
}

#qyrouter
-keep class org.qiyi.video.router.router.ActivityRouter{
    public <methods>;
    public <fields>;
}

-keep class org.qiyi.video.router.intent.QYIntent{
    public <methods>;
    public <fields>;
}
-keep class org.qiyi.android.upload.video.model.UploadItem{*;}

# appsflyer and google play
-dontwarn com.appsflyer.FirebaseInstanceIdListener
-dontwarn com.appsflyer.UninstallUtils
-dontwarn com.google.android.gms.internal.*
-keep class com.google.android.gms.** {*;}
-keep interface com.google.android.gms.** {*;}
-keep enum com.google.android.gms.** {*;}

#为了编译后剔除R相关文件，R相关的资源文件不混淆
-keepclassmembers class **.R$* {
     public static <fields>;
}
-keep class **.R {*;}
-keep class **.R$* {*;}
-keep class **.R$*
-keep class **.R

# Understand the @Keep support annotation.
-keep class android.support.annotation.Keep
-keep @android.support.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @android.support.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @android.support.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @android.support.annotation.Keep <init>(...);
}


-keep class  qiyi.basemodule.BasePro{
    <fields>;
    <methods>;
}
