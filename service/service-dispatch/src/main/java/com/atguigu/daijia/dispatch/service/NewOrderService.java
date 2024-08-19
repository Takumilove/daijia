package com.atguigu.daijia.dispatch.service;

import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface NewOrderService {
    // 创建并启动任务调度方法
    Long addAndStartTask(NewOrderTaskVo newOrderTaskVo);

    // 执行任务:搜索附近代驾司机
    void executeTask(long jobId);

    // 查询司机新订单数据
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    // 清空新订单队列数据
    Boolean clearNewOrderQueueData(Long driverId);
}
