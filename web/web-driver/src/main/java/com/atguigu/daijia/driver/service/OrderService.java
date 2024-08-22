package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface OrderService {

    // 查询订单状态
    Integer getOrderStatus(Long orderId);

    // 查询司机新订单数据
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    // 司机抢单
    Boolean robNewOrder(Long driverId, Long orderId);
}
