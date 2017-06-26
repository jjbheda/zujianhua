package com.iqiyi.video.download.filedownload.schedule;

import com.iqiyi.video.download.engine.task.ITaskSchedule;

import org.qiyi.video.module.download.exbean.ScheduleBean;

/**
 * Created by songguobin on 2017/1/22.
 *
 * 文件排序调度器
 *
 *  优先级从高到低排列
 *
 */

public class FileSchedule implements ITaskSchedule<ScheduleBean>{
    @Override
    public int compare(ScheduleBean task1, ScheduleBean task2) {

        if( task1 != null && task2 != null){

            if(task1.groupPriority == task2.groupPriority){
                return task2.prority-task1.prority;
            } else{
                return task2.groupPriority - task1.groupPriority;
            }

        } else{
            return 0;
        }
    }
}
