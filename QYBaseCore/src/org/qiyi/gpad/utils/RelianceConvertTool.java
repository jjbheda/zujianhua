package org.qiyi.gpad.utils;


/**
 * Created by Carlyle_Pro on 16/12/15.
 * used for pad
 */
public class RelianceConvertTool {

    static IRelianceConverter _ir;
//    static HashMap<Integer,IRelianceConverter> list=new HashMap<Integer, IRelianceConverter>();

    /**
     *
     * @param ir
     */
    public static void setConverter(IRelianceConverter ir){
        _ir=ir;
    }


    public static Object getData(int id,Object...data){
        if(_ir!=null){
            return _ir.getData(id,data);
        }
        return null;
    }

    public static String getStringData(int id,Object...data){
        if(_ir!=null){
            Object object= _ir.getData(id,data);
            if(object instanceof String){
                return (String) object;
            }
        }
        return "";
    }

    public static long getLongData(int id,Object...data){
        if(_ir!=null){
            Object object= _ir.getData(id,data);
            if(object instanceof Long){
                return (long) object;
            }
        }
        return -1;
    }


    public static boolean getBoolean(int id){
        Object data=getData(id);
        if(data!=null){
            return (boolean) data;
        }
        return false;
    }

    public static String getString(int id){
        Object data=getData(id);
        if(data!=null&&data.getClass()==String.class){
            return (String) data;
        }
        return null;
    }

    public static void setData(int id,Object... data){
        if(_ir!=null){
            _ir.setData(id,data);
        }
    }

}
