package com.iqiyi.video.download.filedownload.ipc;

import android.text.TextUtils;

import com.iqiyi.video.download.filedownload.FileDownloadCallback;
import com.iqiyi.video.download.filedownload.FileDownloadExBean;

import org.qiyi.android.corejar.debug.DebugLog;
import org.qiyi.basecore.utils.ExceptionUtils;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Created by songguobin on 2017/3/17.
 */

public class LocalMessageProcesser {

    private  static final String TAG = "LocalMessageProcesser";

    private static LocalMessageProcesser callbackProcesser;

    private HashMap<String,CopyOnWriteArrayList<FileDownloadCallback>> callbackHashMap = new HashMap<String,CopyOnWriteArrayList<FileDownloadCallback>>();

    public synchronized static LocalMessageProcesser getInstance(){

        if(callbackProcesser == null ){
            callbackProcesser = new LocalMessageProcesser();
        }
        return callbackProcesser;
    }

    public LocalMessageProcesser() {


    }

    public FileDownloadExBean processCallback(FileDownloadExBean msg){

        try{

            return MessageCenter.processLocalMessage(msg);

        }catch (Exception e){

            ExceptionUtils.printStackTrace(e);

        }

        return null;
    }

    /**
     * 注册callback
     * @param key
     * @param callback
     */
    public void registerCallback(String key,FileDownloadCallback callback) {

        if(TextUtils.isEmpty(key)||callback == null){
            DebugLog.e(TAG,"registerCallback key == null || callback == null");
            return;
        }

        if(!callbackHashMap.containsKey(key)){
           //key不存在
            callbackHashMap.put(key,new CopyOnWriteArrayList<FileDownloadCallback>());
            callbackHashMap.get(key).add(callback);
        } else{
            //key存在，则直接添加到列表
            if (callbackHashMap.get(key) != null && !callbackHashMap.get(key).contains(callback)) {
                callbackHashMap.get(key).add(callback);
            } else {
                DebugLog.log(TAG, "callback" + callback.toString() + " has duplicated");
            }
        }

        DebugLog.log(TAG,"registerCallback FileDownloadCallback = " + key + "--callback = " + callback.toString());

    }

    /**
     * 注销callback
     * @param key
     */
    public void unregisterCallback(String key,FileDownloadCallback callback){

        if(TextUtils.isEmpty(key)||callback == null){
            DebugLog.e(TAG,"unregisterCallback key == null || callback == null");
            return;
        }

        if(!callbackHashMap.containsKey(key)){
            DebugLog.log(TAG,key + " key not exist,unregister callback fail");
            return;
        } else {
            if( callbackHashMap.get(key)!=null) {
                callbackHashMap.get(key).remove(callback);
            }
            DebugLog.log(TAG,"unregister FileDownloadCallback = " + key + "--callback = " + callback.toString());
        }
    }


    /**
     * 下载完成时，清除key相关的所有callback
     * @param key
     */
    public void unregisterAllCallback(String key){

        if(TextUtils.isEmpty(key)){
            DebugLog.e(TAG,"unregisterAllCallback key == null");
            return;
        }

        DebugLog.log(TAG,"clear FileDownloadCallback = " + key);

        if(!callbackHashMap.containsKey(key)){
            DebugLog.log(TAG,key + " key not exist,unregister callback fail");
            return;
        } else {
            callbackHashMap.remove(key);
        }
    }



    /**
     * 获取特定key的所有callback
     * @param key
     * @return
     */
    public CopyOnWriteArrayList<FileDownloadCallback> getDownloadFileCallbacks(String key) {

        return callbackHashMap.get(key);

    }

    /**
     * 清空文件下载监听器
     */
    public void clearDownloadFileListener(){

        if(callbackHashMap != null) {

            callbackHashMap.clear();

        }

    }


}
