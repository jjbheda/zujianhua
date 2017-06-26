package org.qiyi.basecore.jobquequ;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by niejunjiang on 2017/2/23.
 */

public class DefaultJobHandler extends Handler implements IJobHandler {


    public DefaultJobHandler(boolean postToMain) {
        super(postToMain ? Looper.getMainLooper() :
                (Looper.myLooper() == null ? Looper.getMainLooper() : Looper.myLooper()));
    }

    @Override
    public void postResult(int resultCode, Object result) {
        Message message = new Message();
        message.obj = result;
        message.what = resultCode;
        sendMessage(message);
    }

    @Override
    public void postSuccess(Object result) {

    }

    @Override
    public void postFailed() {

    }

    @Override
    public void handleMessage(Message msg) {
        if (msg != null) {
            switch (msg.what) {
                case SUCCESS:
                    postSuccess(msg.obj);
                    break;
                case FAILED:
                    postFailed();
                    break;
            }
        }
    }

}
