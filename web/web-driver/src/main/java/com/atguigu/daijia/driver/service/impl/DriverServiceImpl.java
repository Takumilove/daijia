package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {
    private final DriverInfoFeignClient driverInfoFeignClient;
    private final RedisTemplate redisTemplate;
    private final LocationFeignClient locationFeignClient;
    private final NewOrderFeignClient newOrderFeignClient;

    //登录
    @Override
    public String login(String code) {
        // 远程调用
        Result<Long> longResult = driverInfoFeignClient.login(code);
        Integer codeResult = longResult.getCode();
        if (codeResult != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        Long driverId = longResult.getData();
        if (driverId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token, driverId.toString(),
                                        RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
        return token;
    }

    //司机认证信息
    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        return driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
    }

    // 获取司机认证信息
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        return driverInfoFeignClient.getDriverAuthInfo(driverId).getData();
    }

    // 更新司机认证信息
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        return driverInfoFeignClient.UpdateDriverAuthInfo(updateDriverAuthInfoForm).getData();
    }

    //创建司机人脸模型
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm).getData();
    }

    // 是否进行过人脸识别
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        return driverInfoFeignClient.isFaceRecognition(driverId).getData();
    }

    // 人脸识别
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.verifyDriverFace(driverFaceModelForm).getData();
    }

    // 开始接单服务
    @Override
    public Boolean startService(Long driverId) {
        // 1.判断完成认证
        DriverLoginVo driverLoginVo = driverInfoFeignClient.getDriverLoginInfo(driverId).getData();
        if (driverLoginVo.getAuthStatus() != 2) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }
        // 2.判断当日是否人脸识别
        boolean isFace = driverInfoFeignClient.isFaceRecognition(driverId).getData();
        if (!isFace) {
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }
        // 3.更新订单状态1开始接单
        driverInfoFeignClient.updateServiceStatus(driverId, 1);
        // 4.删除redis司机位置信息
        locationFeignClient.removeDriverLocation(driverId);
        // 5.清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }

    //停止接单服务
    @Override
    public Boolean stopService(Long driverId) {
        // 1.更新订单状态0停止接单
        driverInfoFeignClient.updateServiceStatus(driverId, 0);
        // 2.删除redis司机位置信息
        locationFeignClient.removeDriverLocation(driverId);
        // 3.清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return true;
    }
}
