package com.cijian.interaction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.cijian")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cijian.interaction.feign")
@MapperScan("com.cijian.interaction.mapper")
public class CijianInteractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CijianInteractionApplication.class, args);
    }
}
