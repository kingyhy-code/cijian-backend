package com.cijian.profile;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.cijian")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cijian.profile.feign")
@MapperScan("com.cijian.profile.mapper")
public class CijianProfileApplication {

    public static void main(String[] args) {
        SpringApplication.run(CijianProfileApplication.class, args);
    }
}
