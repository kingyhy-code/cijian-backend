package com.cijian.operation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cijian.common.feign")
@org.mybatis.spring.annotation.MapperScan("com.cijian.operation.mapper")
public class CijianOperationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CijianOperationApplication.class, args);
    }
}
