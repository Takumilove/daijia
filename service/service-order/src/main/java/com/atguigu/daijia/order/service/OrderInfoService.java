package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {

    // 乘客下单
    Long saveOrderInfo(OrderInfoForm orderInfoForm);

    // 根据订单id获取订单状态
    Integer getOrderStatus(Long orderId);
}
