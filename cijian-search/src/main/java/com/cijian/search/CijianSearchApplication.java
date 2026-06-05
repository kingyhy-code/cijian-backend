package com.cijian.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
})
@EnableDiscoveryClient
public class CijianSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CijianSearchApplication.class, args);
    }
}
