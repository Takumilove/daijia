package com.atguigu.daijia.common.login;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author RaoPengFei
 * @since 2024/8/12
 */
@Component
@Aspect
public class GuiguLoginAspect {
    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) &&@annotation(guiguLogin)")
    public void login(GuiguLogin guiguLogin) {

    }
}
