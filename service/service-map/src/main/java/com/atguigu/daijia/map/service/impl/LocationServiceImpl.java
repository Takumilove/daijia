package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final RedisTemplate redisTemplate;

    // 更新司机位置信息
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(),
                                updateDriverLocationForm.getLatitude().doubleValue());
        redisTemplate.opsForGeo()
                     .add(RedisConstant.DRIVER_GEO_LOCATION, point, updateDriverLocationForm.getDriverId().toString());
        return true;
    }

    // 删除司机位置信息
    @Override
    public Boolean removeDriverLocation(Long driverId) {
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        return true;
    }
}
