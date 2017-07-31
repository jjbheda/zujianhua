package com.huanju.chajianhuatest.bundleUtils;

import android.content.Context;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;

public class ClassPathHookUtil {

    public static void inject(Context context, DexClassLoader loader){
        if (context == null)
            return;
        ClassLoader pathLoader = context.getClassLoader();
        try {
            Object hostPathList = getPathList(pathLoader);
            Object bundlePathList = getPathList(loader);
            Object dexElements = combineArray(
                    getDexElements(hostPathList),
                    getDexElements(bundlePathList));
            setField(hostPathList, hostPathList.getClass(), "dexElements", dexElements);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object getPathList(Object baseDexClassLoader)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    private static Object getField(Object obj, Class<?> cl, String field)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }

    private static Object getDexElements(Object paramObject)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return getField(paramObject, paramObject.getClass(), "dexElements");
    }

    private static void setField(Object obj, Class<?> cl, String field,
                                 Object value) throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {

        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        localField.set(obj, value);
    }

    private static Object combineArray(Object suzhu, Object chajian) {
        Class<?> localClass = suzhu.getClass().getComponentType();
        int i = Array.getLength(suzhu);
        int j = i + Array.getLength(chajian);
        Object result = Array.newInstance(localClass, j);
        for (int k = 0; k < j; ++k) {
            if (k < i) {
                Array.set(result, k, Array.get(suzhu, k));
            } else {
                Array.set(result, k, Array.get(chajian, k - i));
            }
        }
        return result;
    }
}