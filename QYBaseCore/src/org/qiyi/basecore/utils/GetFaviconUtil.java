package org.qiyi.basecore.utils;

import android.support.annotation.Nullable;

import org.qiyi.android.corejar.debug.DebugLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取网页icon工具类
 * Created by zhangqixun on 16/10/10.
 */
public class GetFaviconUtil {

    public static final String TAG = "GetFaviconUtil";
    private volatile static GetFaviconUtil instance;
    private String url;
    private ICallBack iCallBack;
    private static final Pattern IMAGE_PATTERN = Pattern.compile("<img.*?src.*?http.*?\\.(jpg|png).*?");

    private GetFaviconUtil(){

    }

    public static GetFaviconUtil getInstance(){
        if(instance == null){
            synchronized (GetFaviconUtil.class){
                if(instance == null){
                    instance = new GetFaviconUtil();
                }
            }
        }
        return instance;
    }

    public void getFaviconByUrl(String webUrl, ICallBack callBack){
        url = webUrl;
        iCallBack = callBack;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String iconUrl = null;
                try {
                    iconUrl = getIconUrlString(url);
                } catch (MalformedURLException e) {
                    ExceptionUtils.printStackTrace(e);
                }
                iCallBack.onResponse(iconUrl);
            }
        }, "GetFaviconUtil").start();
    }

    // 获取Icon地址
    private String getIconUrlString(String urlString) throws MalformedURLException {
        urlString = getFinalUrl(urlString);
        DebugLog.log(TAG, "getFinalUrl:" + urlString);
        return getIconUrlByRegex(urlString);
    }

    // 获取稳定url
    private String getFinalUrl(String urlString) {
        HttpURLConnection connection = null;
        try {
            connection = getConnection(urlString);
            connection.connect();

            // 是否跳转，若跳转则跟踪到跳转页面
            if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM
                    || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                String location = connection.getHeaderField("Location");
                if (!location.contains("http")) {
                    location = urlString + "/" + location;
                }
                return location;
            }
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        return urlString;
    }

    // 从html中获取Icon地址
    private String getIconUrlByRegex(String urlString) {
        String imgTag = getImageTag(urlString);
        if (imgTag == null) {
            return null;
        }
        int index = imgTag.indexOf("http");
        if (index > -1) {
            return imgTag.substring(index);
        }
        return null;
    }

    // 获取网页中的img tag
    private String getImageTag(String urlString) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = getConnection(urlString);
            connection.connect();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line = null;
            String img = null;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = IMAGE_PATTERN.matcher(line);
                if (matcher.find()) {
                    img = matcher.group();
                    break;
                }
            }
            return img;
        } catch (Exception e) {
            ExceptionUtils.printStackTrace(e);
            return null;
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (connection != null)
                    connection.disconnect();
            } catch (IOException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }
    }


    // 获取一个连接
    private HttpURLConnection getConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(1000);
        return connection;
    }

    public static String getIconJS() {
        return "javascript:function getImagesStyle(){" +
                "var objs = document.getElementsByTagName(\"img\");" +
                "var imgStyle = \'\';" +
                "var imgWidth = \'\';" +
                "var imgHeight = \'\';" +
                "var finalImg = \'\';" +
                "for(var i=0;i<objs.length;i++){" +
                "imgStyle = objs[i].style.cssText;" +
                "imgWidth = objs[i].offsetWidth;" +
                "imgHeight = objs[i].offsetHeight;" +
                "if(imgStyle.indexOf(\"display:none\") == -1){" +
                "if(imgWidth >= 120 && imgHeight >= 120){" +
                "finalImg = objs[i].src;" +
                "break;" +
                "}}};" +
                "return finalImg;" +
                "};";
    }

    public interface ICallBack{
        void onResponse(@Nullable String iconUrl);
    }
}


