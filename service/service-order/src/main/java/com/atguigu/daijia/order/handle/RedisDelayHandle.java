package com.atguigu.daijia.order.handle;

import com.atguigu.daijia.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author RaoPengFei
 * @since 2024/8/27
 */
@Component
@RequiredArgsConstructor
public class RedisDelayHandle {
    private final RedissonClient redissonClient;
    private final OrderInfoService orderInfoService;

    @PostConstruct
    public void listener() {
        new Thread(() -> {
            while (true) {
                // 获取到延迟队列里面的阻塞队列
                RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");
                // 从队列获取消息
                try {
                    String orderId = blockingQueue.take();

                    // 取消订单
                    if (StringUtils.hasText(orderId)) {
                        orderInfoService.orderCancel(Long.parseLong(orderId));
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
