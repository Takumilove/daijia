package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;

public interface LocationService {

    // 更新司机经纬度位置
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);
}
