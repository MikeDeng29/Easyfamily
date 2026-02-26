package com.easyfamily.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easyfamily.query.aliyun-market")
public record AliyunMarketProperties(
        String baseUrl,
        String path,
        String appCode
) {
}
