package com.lemon.erp.modules.ecom.connector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 渠道接口 HTTP 自动重试的配置（429 / 503）：
 * lemon.ecom.connector.http-retry.enabled（默认 true）、max-retries（默认 3）、max-wait-seconds（一次调用累计等待上限，默认 30）
 */
@Component
class ConnectorHttpSettings {

    ConnectorHttpSettings(@Value("${lemon.ecom.connector.http-retry.enabled:true}") boolean enabled,
                          @Value("${lemon.ecom.connector.http-retry.max-retries:3}") int maxRetries,
                          @Value("${lemon.ecom.connector.http-retry.max-wait-seconds:30}") long maxWaitSeconds) {
        HttpJson.retryPolicy(enabled
                ? new HttpJson.RetryPolicy(true, Math.max(0, maxRetries), Duration.ofSeconds(Math.max(0, maxWaitSeconds)), Duration.ofSeconds(1))
                : HttpJson.RetryPolicy.DISABLED);
    }
}
