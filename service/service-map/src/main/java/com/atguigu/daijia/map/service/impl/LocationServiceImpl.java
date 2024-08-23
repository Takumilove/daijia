package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final RedisTemplate redisTemplate;
    private final DriverInfoFeignClient driverInfoFeignClient;

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

    // 搜索附近满足条件的司机
    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        // 搜索经纬度位置5公里以内的司机
        // 1.操作redis里面geo
        // 定义point
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(),
                                searchNearByDriverForm.getLatitude().doubleValue());
        // 定义距离，5公里
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS, RedisGeoCommands.DistanceUnit.KILOMETERS);
        // 创建Circle对象,point,distance
        Circle circle = new Circle(point, distance);
        // 定义GEO参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                                                                          .includeDistance()
                                                                                          .includeCoordinates()
                                                                                          .sortAscending();
        GeoResults<RedisGeoCommands.GeoLocation<String>> result = redisTemplate.opsForGeo()
                                                                               .radius(RedisConstant.DRIVER_GEO_LOCATION,
                                                                                       circle, args);
        // 2.查询redis最终返回list集合
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = result.getContent();
        // 3.对查询list集合进行处理
        // 遍历list集合，得到每个司机信息
        // 根据每个司机个性化设置信息判断
        List<NearByDriverVo> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(content)) {
            Iterator<GeoResult<RedisGeoCommands.GeoLocation<String>>> iterator = content.iterator();
            while (iterator.hasNext()) {
                GeoResult<RedisGeoCommands.GeoLocation<String>> item = iterator.next();
                // 获取司机id
                Long driverId = Long.valueOf(item.getContent().getName());
                // 远程调用，获取司机id个性化设置信息
                Result<DriverSet> driverSetResult = driverInfoFeignClient.getDriverSet(driverId);
                DriverSet driverSet = driverSetResult.getData();
                // 判断司机是否满足条件
                // 订单里程
                BigDecimal orderDistance = driverSet.getOrderDistance();
                if (orderDistance.doubleValue() != 0 && orderDistance.subtract(
                        searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
                    continue;
                }
                // 接单距离
                BigDecimal acceptDistance = driverSet.getAcceptDistance();
                // 计算司机与用户的距离
                BigDecimal currentDistance = new BigDecimal(item.getDistance().getValue()).setScale(2,
                                                                                                    RoundingMode.HALF_UP);
                if (acceptDistance.doubleValue() != 0 && acceptDistance.subtract(currentDistance).doubleValue() < 0) {
                    continue;
                }
                // 封装复合条件数据
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                list.add(nearByDriverVo);
            }
        }
        return list;
    }

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());
        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
        redisTemplate.opsForValue().set(key, orderLocationVo);
        return true;
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        String key = RedisConstant.UPDATE_ORDER_LOCATION + orderId;
        OrderLocationVo orderLocationVo = (OrderLocationVo) redisTemplate.opsForValue().get(key);
        return orderLocationVo;
    }
}
