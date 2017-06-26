package org.qiyi.video.module.download.exbean;

import java.io.Serializable;

/**
 * Created by songguobin on 2017/1/22.
 *
 * 排序调度器，计算优先级
 *
 */
public class ScheduleBean implements Serializable{

    public int prority;//优先级

    public int groupPriority;//组优先级

    @Override
    public String toString() {
        return "ScheduleBean{" +
                "prority=" + prority +
                ", groupPriority=" + groupPriority +
                '}';
    }
}
