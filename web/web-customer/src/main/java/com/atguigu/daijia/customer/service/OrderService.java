package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;

public interface OrderService {
    // 预估订单数据
    ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm);

    // 乘客下单
    Long submitOrder(SubmitOrderForm submitOrderForm);

    // 查询订单状态
    Integer getOrderStatus(Long orderId);

    // 乘客端查找当前订单
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);
}
