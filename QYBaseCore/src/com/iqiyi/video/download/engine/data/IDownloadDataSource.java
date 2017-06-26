package com.iqiyi.video.download.engine.data;

import java.util.Comparator;
import java.util.List;

/**
 * 下载数据源接口
 *
 * @param <T>
 */
public interface IDownloadDataSource<T>  {

    String getSourceName();

    /**
     * 某一项的id
     * @param item
     * @return
     */
    String getId(T item);

    /**
     * 获取数据源中的数据项。
     * @param index 数据项的索引
     */
    T get(int index);

    /**
     * 返回数据源中数据项的数量。
     */
    int size();

    /**
     * 添加数据项
     */
    void add(T item);

    /**
     * 将列表中所有的数据项都添加到数据源当中来
     * @param items 要添加的数据项列表
     */
    void addAll(List<T> items);

    /**
     * 数据源是否是空的。
     */
    boolean isEmpty();

    /**
     * 删除指定的数据项
     * @param index 数据项在数据源中的索引号
     */
    void delete(int index);

    /**
     * 将指定的数据项删除
     */
    void delete(T item);

    /**
     * 将列表中的数据项从数据源中全部删除
     * @param items 要删除的数据项列表
     */
    void deleteAll(List<T> items);

    /**
     * 返回数据项在数据源中的位置
     */
    int indexOf(T item);

    /**
     * 清空数据源中所有的数据项目
     */
    void clear();

    /**
     * 排序
     */
    void sort(Comparator<T> comparator);

    /**
     * 是否包含指定的项目
     */
    boolean contains(T item);

    /**
     * 复制数据源中所有的数据项
     */
    List<T> copyAll();

    /**
     * 得到数据源中的数据
     * @return
     */
    List<T> getAll();

    /**
     * 替换某一项（用于重复添加的情况）
     * @param index 原始列表中的位置
     * @param newItem 新的条目
     */
    void replace(int index, T newItem);

    /**
     * 根据Id获取一个元素的坐标
     * @param id
     * @return
     */
     int getIndexById(String id);

    /**
     * 根据id删除一个元素
     * @param ids
     */
    void deleteAllById(List<String> ids);


    /**
     * 根据id删除一个元素
     * @param id
     */
    void deleteById(String id);


    /**
     * 根据id获取一个元素
     * @param id
     * @return
     */
    T getById(String id);


}
