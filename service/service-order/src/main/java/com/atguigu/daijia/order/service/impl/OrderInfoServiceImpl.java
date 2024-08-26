package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.*;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderProfitsharingMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
    private final OrderInfoMapper orderInfoMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final RedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final OrderMonitorService orderMonitorService;
    private final OrderBillMapper orderBillMapper;
    private final OrderProfitsharingMapper orderProfitsharingMapper;

    // 乘客下单
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoForm, orderInfo);
        // 订单号
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        orderInfo.setOrderNo(orderNo);
        // 订单状态
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        orderInfoMapper.insert(orderInfo);
        // 记录日志
        this.log(orderInfo.getId(), orderInfo.getStatus());
        // 向Redis添加标示
        // 接单标示，不存在说明不在等待接单状态
        String redisKey = RedisConstant.ORDER_ACCEPT_MARK + orderInfo.getId();
        redisTemplate.opsForValue().set(redisKey, "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);
        return orderInfo.getId();
    }

    // 根据订单id获取订单状态
    @Override
    public Integer getOrderStatus(Long orderId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.select(OrderInfo::getStatus);
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        // 订单不存在
        if (orderInfo == null) {
            return OrderStatus.NULL_ORDER.getStatus();
        }
        return orderInfo.getStatus();
    }

    // Redisson分布式锁
    // 司机抢单
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        // 判断订单是否存在，通过Redis，减少数据库压力
        String redisKey = RedisConstant.ORDER_ACCEPT_MARK + orderId;
        if (!redisTemplate.hasKey(redisKey)) {
            // 抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        // 创建锁
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);
        try {
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,
                                        RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME,
                                        TimeUnit.SECONDS);            // 司机抢单
            if (flag) {
                if (!redisTemplate.hasKey(redisKey)) {
                    // 抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                // 修改订单表状态值为2：已经接单
                LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderInfo::getId, orderId);
                OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
                orderInfo.setDriverId(driverId);
                orderInfo.setAcceptTime(new Date());
                int rows = orderInfoMapper.updateById(orderInfo);
                if (rows != 1) {
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                // 删除Redis中的标示
                redisTemplate.delete(redisKey);
            }
        } catch (Exception e) {
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        return true;
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getCustomerId, customerId);
        Integer[] statusArray = {OrderStatus.ACCEPTED.getStatus(), OrderStatus.DRIVER_ARRIVED.getStatus(),
                                 OrderStatus.UPDATE_CART_INFO.getStatus(), OrderStatus.START_SERVICE.getStatus(),
                                 OrderStatus.END_SERVICE.getStatus(), OrderStatus.UNPAID.getStatus()};
        wrapper.in(OrderInfo::getStatus, statusArray);

        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last("limit 1");

        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getDriverId, driverId);
        Integer[] statusArray = {OrderStatus.ACCEPTED.getStatus(), OrderStatus.DRIVER_ARRIVED.getStatus(),
                                 OrderStatus.UPDATE_CART_INFO.getStatus(), OrderStatus.START_SERVICE.getStatus(),
                                 OrderStatus.END_SERVICE.getStatus()};
        wrapper.in(OrderInfo::getStatus, statusArray);
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last("limit 1");
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        // 封装到vo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        // 更新订单状态和到达时间，条件：orderId+driverId
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.eq(OrderInfo::getDriverId, driverId);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        orderInfo.setArriveTime(new Date());
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows == 1) {
            this.log(orderId, OrderStatus.DRIVER_ARRIVED.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, updateOrderCartForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm, orderInfo);
        orderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows == 1) {
            this.log(updateOrderCartForm.getOrderId(), OrderStatus.UPDATE_CART_INFO.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Override
    public Boolean startDriver(StartDriveForm startDriveForm) {
        // 根据订单id+司机id 更新订单状态和开始代驾时间
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        orderInfo.setStartServiceTime(new Date());
        // 只能更新自己的订单
        int row = orderInfoMapper.update(orderInfo, wrapper);
        if (row == 1) {
            // 记录日志
            this.log(startDriveForm.getOrderId(), OrderStatus.START_SERVICE.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        // 初始化订单监控统计数据
        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorService.saveOrderMonitor(orderMonitor);
        return true;
    }

    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OrderInfo::getStartServiceTime, startTime);
        wrapper.lt(OrderInfo::getStartServiceTime, endTime);
        return orderInfoMapper.selectCount(wrapper);
    }

    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, updateOrderBillForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, updateOrderBillForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        orderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        orderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        orderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        orderInfo.setEndServiceTime(new Date());
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows == 1) {
            // 添加账单数据
            OrderBill orderBill = new OrderBill();
            BeanUtils.copyProperties(updateOrderBillForm, orderBill);
            orderBill.setPayAmount(updateOrderBillForm.getTotalAmount());
            orderBillMapper.insert(orderBill);
            // 添加分账信息
            OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
            BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
            orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
            orderProfitsharing.setRuleId(updateOrderBillForm.getProfitsharingRuleId());
            orderProfitsharing.setStatus(1);
            orderProfitsharingMapper.insert(orderProfitsharing);
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectCustomerOrderPage(pageParam, customerId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectDriverOrderPage(pageParam, driverId);
        return new PageVo<>(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        LambdaQueryWrapper<OrderBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderBill::getOrderId, orderId);
        OrderBill orderBill = orderBillMapper.selectOne(wrapper);
        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill, orderBillVo);
        return orderBillVo;
    }

    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        LambdaQueryWrapper<OrderProfitsharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderProfitsharing::getOrderId, orderId);
        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(wrapper);

        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
        BeanUtils.copyProperties(orderProfitsharing, orderProfitsharingVo);
        return orderProfitsharingVo;
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        // 更新订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        // 更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
        // 只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if (row == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo, customerId);
        if (orderPayVo != null) {
            String content = orderPayVo.getStartLocation() + " 到 " + orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }

    @Override
    public Boolean updateOrderPayStatus(String orderNo) {
        // 1.根据订单编号查询，判断订单状态
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        if (orderInfo == null || orderInfo.getStatus() == OrderStatus.PAID.getStatus()) {
            return true;
        }
        // 2.更新状态
        LambdaQueryWrapper<OrderInfo> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.PAID.getStatus());
        updateOrderInfo.setPayTime(new Date());
        int rows = orderInfoMapper.update(updateOrderInfo, updateWrapper);
        if (rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        // 根据订单编号查询订单表
        OrderInfo orderInfo = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo)
                                                   .select(OrderInfo::getId, OrderInfo::getDriverId));

        // 根据订单id查询系统奖励表
        OrderBill orderBill = orderBillMapper.selectOne(
                new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId, orderInfo.getId())
                                                   .select(OrderBill::getRewardFee));

        // 封装到vo里面
        OrderRewardVo orderRewardVo = new OrderRewardVo();
        orderRewardVo.setOrderId(orderInfo.getId());
        orderRewardVo.setDriverId(orderInfo.getDriverId());
        orderRewardVo.setRewardFee(orderBill.getRewardFee());
        return orderRewardVo;
    }

    // 司机抢单：乐观锁方案解决并发问题
    public Boolean robNewOrder1(Long driverId, Long orderId) {
        // 判断订单是否存在，通过Redis，减少数据库压力
        String redisKey = RedisConstant.ORDER_ACCEPT_MARK + orderId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
            // 抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        // 司机抢单
        // 修改订单表状态值为2：已经接单
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.eq(OrderInfo::getStatus, OrderStatus.WAITING_ACCEPT.getStatus());
        // 修改值
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
        orderInfo.setDriverId(driverId);
        orderInfo.setAcceptTime(new Date());
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        // 删除Redis中的标示
        redisTemplate.delete(redisKey);
        return true;
    }

    public void log(Long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }
}
