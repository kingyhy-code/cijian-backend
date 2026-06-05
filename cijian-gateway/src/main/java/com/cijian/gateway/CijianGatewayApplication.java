package com.cijian.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.cijian")
@EnableDiscoveryClient
public class CijianGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CijianGatewayApplication.class, args);
    }
}
