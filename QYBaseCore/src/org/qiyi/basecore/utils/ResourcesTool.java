package org.qiyi.basecore.utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

public class ResourcesTool {
    static final String DRAWABLE = "drawable";
    static final String STRING = "string";
    static final String STYLE = "style";
    static final String LAYOUT = "layout";
    static final String ID = "id";
    static final String COLOR = "color";
    static final String RAW = "raw";
    static final String ANIM = "anim";
    static final String ATTR = "attr";
    static final String DIMEN ="dimen";


    static String mPackageName;
    static Resources mResources;
    private static Object sInitLock = new Object();

    public static void init(Context appContext) {
        synchronized (sInitLock) {
            if (mResources == null && TextUtils.isEmpty(mPackageName)) {
                mPackageName = appContext.getPackageName();
                mResources = appContext.getResources();
            }
        }
    }

    /**
     * 获取主包资源id
     * 
     * @param sourceName
     * @param sourceType
     * @return
     */
    private static int getResourceId(String sourceName, String sourceType) {
        assetContext();
        if (mResources == null || TextUtils.isEmpty(sourceName)) {
            return -1;
        } else {
            return mResources.getIdentifier(sourceName, sourceType, mPackageName);
        }

    }

    public static int getResourceIdForString(String sourceName) {
        if (TextUtils.isEmpty(sourceName)) {
            sourceName = "emptey_string_res";
        }
        return getResourceId(sourceName, STRING);
    }

    public static int getResourceIdForID(String sourceName) {
        return getResourceId(sourceName, ID);
    }

    public static int getResourceIdForLayout(String sourceName) {
        return getResourceId(sourceName, LAYOUT);
    }

    public static int getResourceIdForDrawable(String sourceName) {
        if (TextUtils.isEmpty(sourceName)) {
            sourceName = "default_empty_drawable_transparent";// 默认一个透明图片资源
        }

        return getResourceId(sourceName, DRAWABLE);
    }

    public static int getResourceIdForStyle(String sourceName) {
        return getResourceId(sourceName, STYLE);
    }

    public static int getResourceIdForColor(String sourceName) {
        return getResourceId(sourceName, COLOR);
    }

    public static int getResourceIdForRaw(String sourceName) {
        return getResourceId(sourceName, RAW);
    }

    public static int getResourceForAnim(String sourceName) {
        return getResourceId(sourceName, ANIM);
    }

    public static int getResourceForAttr(String sourceName) {
        return getResourceId(sourceName, ATTR);
    }

    public static int getResourceForDimen(String sourceName) {
        return getResourceId(sourceName,DIMEN);
    }


    //[todo  to bereplaced]
    public static int getResourceIdForDimen(String sourceName) {
        return getResourceId(sourceName,DIMEN);
    }


    public static float getDimention(String dim,int dft){
        int id=getResourceIdForDimen(dim);
        if(id>0) {
            return mResources.getDimension(id);
        }else{
            return dft;
        }
    }

    public static int getResourceIdForDrawable(Context context,String sourceName) {
        if (context==null||TextUtils.isEmpty(sourceName)) {
            return -1;
        }
        if(mPackageName==null){
            mPackageName=context.getPackageName();
        }

        Resources mResources=context.getResources();
        return mResources.getIdentifier(sourceName, DRAWABLE, mPackageName);

    }

    public static int getResourceIdForLayout(Context context,String sourceName){
        if (context==null||TextUtils.isEmpty(sourceName)) {
            return -1;
        }
        Resources mResources=context.getResources();
        String currentPackageName;
        if(mPackageName == null){
            currentPackageName = context.getPackageName();
        }else{
            currentPackageName = mPackageName;
        }
        return mResources.getIdentifier(sourceName, LAYOUT, currentPackageName);

    }

    private static void assetContext(){
        if(ApplicationContext.app!=null) {
            if (mResources == null) {
                mResources=ApplicationContext.app.getResources();
            }

            if (mPackageName == null) {
                mPackageName = ApplicationContext.app.getPackageName();
            }

        }

    }

    public static void assetContext(Context context){
        if(context!=null) {
                mResources = context.getResources();
                mPackageName = context.getPackageName();

        }

    }


    //[used to debug for player ac null ptr]
    public static String getmPackageName(){
        return mPackageName;
    }
}
