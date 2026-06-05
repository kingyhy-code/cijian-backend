package com.cijian.content;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.cijian")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cijian.content.feign")
@MapperScan("com.cijian.content.mapper")
public class CijianContentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CijianContentApplication.class, args);
    }
}
