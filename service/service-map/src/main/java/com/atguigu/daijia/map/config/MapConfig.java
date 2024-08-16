package com.atguigu.daijia.map.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author RaoPengFei
 * @since 2024/8/15
 */
@Configuration
public class MapConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
