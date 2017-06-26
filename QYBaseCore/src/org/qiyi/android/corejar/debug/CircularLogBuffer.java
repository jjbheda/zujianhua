package org.qiyi.android.corejar.debug;

import android.os.Process;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shizhengyin on 2015/12/25.
 */
public class CircularLogBuffer {
    public int logSize = 200;
    public static final int SINGLE_LOG_SIZE_LIMIT = 512;
    private int mInsertIndex = 0;
    private SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
    private List<CircularLog> mLogs;
    private boolean mFullBuffer = false;
    public boolean enabled = true;

    public CircularLogBuffer() {
        mLogs = new ArrayList<>();
    }

    public CircularLogBuffer(int bufferSize) {
        logSize = bufferSize;
        mLogs = new ArrayList<>();
    }

    public String toString() {
        if (mLogs != null && mLogs.size() > 0) {
            StringBuilder sb = new StringBuilder();
            int start = mFullBuffer ? mInsertIndex : 0;
            int size = mFullBuffer ? logSize : mLogs.size();
            for(int i=0; i<size; i++){
                sb.append(mLogs.get((start + i) % size).toString());
            }
            return sb.toString();
        }
        else {
            return "";
        }
    }

    public synchronized void log(String tag, String prior, String msg){
        if(!enabled || mLogs == null) {
            return;
        }
        long time = System.currentTimeMillis();
        int pid = Process.myPid();
        int tid = Process.myTid();
        if(mInsertIndex >= logSize){
            mInsertIndex = 0;
            mFullBuffer = true;
        }

        if(!mFullBuffer){
            mLogs.add(mInsertIndex, new CircularLog());
        }

        if (mLogs.size() <= 0) {
            return;
        }

        CircularLog log = mLogs.get(mInsertIndex);
        log.tag = tag;
        log.prior = prior;
        log.msg = msg;
        log.pid = pid;
        log.tid = tid;
        log.time = time;
        mInsertIndex++;
    }

    class CircularLog{
        String tag;
        String prior;
        String msg;
        int tid;
        int pid;
        long time;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            String logTime = formatter.format(time);
            sb.append(logTime);
            sb.append(" ");
            sb.append(pid);
            sb.append(" ");
            sb.append(tid);
            sb.append(" ");
            sb.append(prior);
            sb.append(" ");
            sb.append(tag);
            sb.append(" ");
            sb.append(msg);
            sb.append("\n");
            if(sb.length() > SINGLE_LOG_SIZE_LIMIT){
                return sb.toString().substring(0, SINGLE_LOG_SIZE_LIMIT);
            }
            return sb.toString();
        }
    }

}

