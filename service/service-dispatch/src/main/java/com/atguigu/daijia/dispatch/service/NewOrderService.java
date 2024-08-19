package com.atguigu.daijia.dispatch.service;

import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;

public interface NewOrderService {
    // 创建并启动任务调度方法
    Long addAndStartTask(NewOrderTaskVo newOrderTaskVo);

    // 执行任务:搜索附近代驾司机
    void executeTask(long jobId);
}
