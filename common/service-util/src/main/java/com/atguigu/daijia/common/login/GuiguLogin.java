package com.atguigu.daijia.common.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author RaoPengFei
 * @since 2024/8/12
 */
// 登录校验
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GuiguLogin {

}
