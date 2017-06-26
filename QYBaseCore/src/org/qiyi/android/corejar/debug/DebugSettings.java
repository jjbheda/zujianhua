package org.qiyi.android.corejar.debug;

/**
 * Debug调试过程中的相关参数设置
 * <p/>
 * 可以设置方法的个数({@link #setMethodCount(int)})以及是否显示线程信息({@link #hideThreadInfo()})等。
 *
 * @author zhongshan
 * @date 2015-09-20.
 */
public class DebugSettings {

    private int methodCount = 2;
    private boolean showThreadInfo = true;
    private int methodOffset = 0;


    public DebugSettings hideThreadInfo() {
        showThreadInfo = false;
        return this;
    }

    public DebugSettings setMethodCount(int methodCount) {
        if (methodCount < 0) {
            methodCount = 0;
        }
        this.methodCount = methodCount;
        return this;
    }

    public DebugSettings setMethodOffset(int offset) {
        this.methodOffset = offset;
        return this;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public boolean isShowThreadInfo() {
        return showThreadInfo;
    }

    public int getMethodOffset() {
        return methodOffset;
    }
}
