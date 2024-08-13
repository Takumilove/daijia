package com.atguigu.daijia.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author RaoPengFei
 * @since 2024/8/10
 */
@Component
@RequiredArgsConstructor
public class WxConfigOperator {

    private final WxConfigProperties wxConfigProperties;

    @Bean
    public WxMaService wxMaService() {
        // 微信小程序id和秘钥
        WxMaDefaultConfigImpl wxMaConfig = new WxMaDefaultConfigImpl();
        wxMaConfig.setAppid(wxConfigProperties.getAppId());
        wxMaConfig.setSecret(wxConfigProperties.getSecret());

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(wxMaConfig);
        return service;
    }
}
