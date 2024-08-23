package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;

public interface LocationService {

    // 更新司机经纬度位置
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm);
}
