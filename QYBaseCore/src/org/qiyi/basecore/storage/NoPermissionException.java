package org.qiyi.basecore.storage;

/**
 * Created by liuchun on 2016/10/12.
 *
 * 没有权限异常
 *
 */

public class NoPermissionException extends Exception {
    private static String PREFIX_TAG = "No System Permission: ";

    public NoPermissionException() {
        super();
    }

    public NoPermissionException(String detailMessage) {
        super(PREFIX_TAG + detailMessage);
    }

    public NoPermissionException(String detailMessage, Throwable throwable) {
        super(PREFIX_TAG + detailMessage, throwable);
    }
}
