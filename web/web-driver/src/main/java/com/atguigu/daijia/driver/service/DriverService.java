package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService {
    // 小程序授权登录
    String login(String code);

    // 获取司机登录信息
    DriverLoginVo getDriverLoginInfo(Long driverId);

    // 获取司机认证信息
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    // 更新司机认证信息
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    // 创建司机人脸模型
    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    // 判断司机当日是否进行过人脸识别
    Boolean isFaceRecognition(Long driverId);

    // 验证司机人脸
    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    // 开始接单服务
    Boolean startService(Long driverId);

    // 结束接单服务
    Boolean stopService(Long driverId);
}
