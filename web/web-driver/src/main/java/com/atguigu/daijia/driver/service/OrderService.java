package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;

import java.util.List;

public interface OrderService {

    // 查询订单状态
    Integer getOrderStatus(Long orderId);

    // 查询司机新订单数据
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    // 司机抢单
    Boolean robNewOrder(Long driverId, Long orderId);

    // 司机端查找当前订单
    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    OrderInfoVo getOrderInfo(Long orderId, Long driverId);

    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);
}
