package org.qiyi.basecore.utils;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.qiyi.basecore.db.QiyiContentProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by niejunjiang on 2017/3/15.
 */

public class ConsistencyContentObserver extends ContentObserver {
    private List<ICrossProcessDataChangeListener> mListenList;
    private String mObserverKey;

    public ConsistencyContentObserver(@NonNull String observerKey, Handler handler) {
        super(handler);
        mListenList = new ArrayList<ICrossProcessDataChangeListener>();
        mObserverKey = observerKey;
    }

    @Override
    public synchronized void onChange(boolean selfChange, Uri uri) {
        if (uri != null) {
            String authority = uri.getAuthority();
            String path = uri.getPath();
            if (!TextUtils.isEmpty(authority) && !TextUtils.isEmpty(path)) {
                if (authority.equals(QiyiContentProvider.AUTHORITY) && path.contains(mObserverKey)) {
                    Iterator<ICrossProcessDataChangeListener> iterator = mListenList.iterator();
                    while (iterator.hasNext()) {
                        ICrossProcessDataChangeListener listener = iterator.next();
                        listener.onChange();
                    }
                }
            }
        }
    }

    public synchronized void clearListener() {
        mListenList.clear();
    }

    public synchronized void addListener(ICrossProcessDataChangeListener listener) {
        if (listener != null) {
            mListenList.add(listener);
        }
    }

    public synchronized void removeListener(ICrossProcessDataChangeListener lisetner) {
        if (lisetner != null) {
            mListenList.remove(lisetner);
        }
    }

    public interface ICrossProcessDataChangeListener {
        void onChange();
    }
}
