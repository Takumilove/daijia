package com.atguigu.daijia.map.service;

import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;

public interface LocationService {
    // 司机开启接单，更新司机位置信息
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    // 司机关闭接单，删除司机位置信息
    Boolean removeDriverLocation(Long driverId);
}
