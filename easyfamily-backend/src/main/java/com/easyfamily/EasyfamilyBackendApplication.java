package com.easyfamily;

import com.easyfamily.query.config.QueryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.easyfamily.query.config.AliyunMarketProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({QueryProperties.class, AliyunMarketProperties.class})
public class EasyfamilyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyfamilyBackendApplication.class, args);
    }
}
