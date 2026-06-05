package com.cijian.content.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Seata 数据源代理 — 为 Seata AT 模式提供 DataSourceProxy，
 * 拦截 SQL 生成 undo log 实现分布式事务回滚。
 */
@Configuration
public class SeataDataSourceConfig {

    @Primary
    @Bean("dataSource")
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource hikari = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        return new DataSourceProxy(hikari);
    }
}
