package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final LocationFeignClient locationFeignClient;

    // 更新司机经纬度位置
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        Result<Boolean> booleanResult = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
        return booleanResult.getData();
    }
}
