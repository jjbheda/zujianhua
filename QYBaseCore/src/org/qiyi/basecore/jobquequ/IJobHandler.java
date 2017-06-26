package org.qiyi.basecore.jobquequ;

/**
 * Created by niejunjiang on 2017/2/23.
 */

public interface IJobHandler {

    public static final int SUCCESS = 1;

    public static final int FAILED = SUCCESS + 1;

    void postResult(int resultCode, Object result);

    void postSuccess(Object result);

    void postFailed();
}
