package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    // 注入远程调用接口
    private final CustomerInfoFeignClient client;
    private final RedisTemplate redisTemplate;
    private final CustomerInfoFeignClient customerInfoFeignClient;

    @Override
    public String login(String code) {
        // 1.远程调用，返回用户id
        Result<Long> loginResult = client.login(code);

        // 2.判断如果返回失败，返回错误提示
        Integer codeResult = loginResult.getCode();
        if (codeResult != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 3.获取远程调用返回的用户id
        Long customerId = loginResult.getData();
        // 4.判断返回用户id是否为空，如果为空，返回错误提示
        if (customerId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 5.生成token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        // 6.把用户id放到Redis，设置过期时间
        // key:token value:customerId
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token, customerId.toString(),
                                        RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
        // 7.返回token字符串
        return token;
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(String token) {
        // 2.根据token去redis查询
        // 3.查询出token在redis里面对应的用户id
        String customerId = (String) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        if (!StringUtils.hasText(customerId)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 4.根据用户id查询用户信息，远程调用
        Result<CustomerLoginVo> customerLoginVoResult = customerInfoFeignClient.getCustomerLoginInfo(
                Long.valueOf(customerId));
        Integer code = customerLoginVoResult.getCode();
        if (code != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        CustomerLoginVo customerLoginVo = customerLoginVoResult.getData();
        if (customerLoginVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 5.返回用户信息
        return customerLoginVo;
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        // 4.根据用户id查询用户信息，远程调用
        Result<CustomerLoginVo> customerLoginVoResult = customerInfoFeignClient.getCustomerLoginInfo(customerId);
        Integer code = customerLoginVoResult.getCode();
        if (code != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        CustomerLoginVo customerLoginVo = customerLoginVoResult.getData();
        if (customerLoginVo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 5.返回用户信息
        return customerLoginVo;
    }

    // 更新用户微信手机号
    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        // Result<Boolean> booleanResult = customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneForm);
        return true;
    }

}
