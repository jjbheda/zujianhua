package qiyi.basemodule;

/**
 * Created by jiangjingbo on 2017/6/18.
 */

public class BasePro {

    public static int video_int = 1;
    private int open_aar = 2;
    protected boolean isForTest  = true;
    public static boolean getBaseMethod(int x ,int y){
        return x+y >5;
    }

    public String  getBaseStatusString(String base){
        String baseStr = "000";
        if(base.equals("110")){
            baseStr = "110";
        }
        return baseStr;
    }

}
