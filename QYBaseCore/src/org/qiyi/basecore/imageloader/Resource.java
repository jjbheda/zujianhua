package org.qiyi.basecore.imageloader;

/**
 * Created by niejunjiang on 2015/10/27.
 * 用来兼容bitmap 与gifdrawable
 * Memorycache中存放的是Resource对象，通过get 获取具体资源
 */
public class Resource<T> {
    private T resource;

    void setResource(T resource) {
        this.resource = resource;
    }

    T getResource() {
        return resource;
    }
}
