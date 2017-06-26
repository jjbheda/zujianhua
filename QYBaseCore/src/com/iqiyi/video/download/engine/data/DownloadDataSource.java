package com.iqiyi.video.download.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 数据源抽象类
 * <p/>
 * 基于Id唯一标识
 * <p/>
 * 利用泛型，覆写ArrayList操作
 */
public abstract class DownloadDataSource<T> implements IDownloadDataSource<T> {

    /**
     * 实际的对象列表。
     */
    protected ArrayList<T> mItemList;

    protected DownloadDataSource() {

        mItemList = new ArrayList<T>();

    }

    //////////////添加任务///////////////////////////
    @Override
    public synchronized void add(T item) {
        if (item == null)
            return;

        int index = getIndexById(getId(item));
        if (index == -1) {
            mItemList.add(item);
        } else {
            replace(index, item);
        }
    }


    @Override
    public synchronized void addAll(List<T> items) {
        if (items == null || items.size() == 0)
            return;

        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            int index = getIndexById(getId(item));
            if (index == -1) {
                mItemList.add(item);
            } else {
                replace(index, item);
            }
        }
    }

    //////////////删除任务///////////////////////////

    @Override
    public synchronized void deleteById(String id) {
        int index = getIndexById(id);
        if (index != -1)
            delete(index);
    }

    @Override
    public synchronized void delete(int index) {
        if (index < 0 || index >= mItemList.size())
            return;

        mItemList.remove(index);
    }

    @Override
    public synchronized void delete(T item) {

        mItemList.remove(item);

    }

    @Override
    public synchronized void deleteAll(List<T> items) {

        mItemList.removeAll(items);
    }

    @Override
    public synchronized void deleteAllById(List<String> ids) {
        if (ids == null || ids.size() == 0)
            return;
        List<T> items = new ArrayList<T>();
        for (int i = 0; i < ids.size(); i++) {
            T item = getById(ids.get(i));
            if (item != null) {
                items.add(item);
            }
        }
        deleteAll(items);
    }

    @Override
    public synchronized void clear() {

        mItemList.clear();

    }

    ///////////////////修改任务/////////////////////////////////

    /**
     * 替换某一项（用于重复添加的情况）
     * TIP 子类可覆盖此方法
     *
     * @param index
     * @param newItem
     */
    @Override
    public void replace(int index, T newItem) {
        mItemList.set(index, newItem);
    }


    /////////////////获取任务////////////////////////////////

    @Override
    public T get(int index) {
        return mItemList.get(index);
    }

    @Override
    public T getById(String id) {
        int index = getIndexById(id);
        if (index != -1)
            return get(index);
        else
            return null;
    }

    /**
     * 返回数据源中所有的数据项的副本
     */
    @Override
    public List<T> copyAll() {
        return new ArrayList<T>(mItemList);
    }


    @Override
    public List<T> getAll() {
        return mItemList;
    }


    @Override
    public int size() {
        return mItemList.size();
    }


    @Override
    public boolean isEmpty() {
        return mItemList.isEmpty();
    }


    @Override
    public int indexOf(T item) {
        return mItemList.indexOf(item);
    }

    @Override
    public boolean contains(T item) {
        if (item == null)
            return false;

        final String id = getId(item);
        for (int i = 0; i < size(); i++) {
            T tmp = get(i);
            if (getId(tmp).equals(id)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public int getIndexById(String id) {
        for (int i = 0; i < size(); i++) {
            T tmp = get(i);
            if (getId(tmp).equals(id)) {
                return i;
            }
        }
        return -1;
    }

    ////////////////排序////////////////////////////////
    @Override
    public void sort(Comparator<T> comparator) {
        Collections.sort(mItemList, comparator);
    }


}
