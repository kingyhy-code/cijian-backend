package com.cijian.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.cijian")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cijian.common.feign")
@MapperScan("com.cijian.user.mapper")
public class CijianUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(CijianUserApplication.class, args);
    }
}
