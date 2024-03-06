package com.project.seckill.service;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Service;

@Service
public class GuavaRateLimiterService {
    /*每秒控制1个许可*/
    RateLimiter rateLimiter = RateLimiter.create(1.0);

    /**
     * 获取令牌
     *
     * @return
     */
    public boolean tryAcquire() {
        return rateLimiter.tryAcquire();
    }

}
