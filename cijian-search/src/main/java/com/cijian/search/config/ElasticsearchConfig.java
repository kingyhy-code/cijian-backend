package com.cijian.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String uris;

    @Bean
    @ConditionalOnProperty(value = "spring.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
    public ElasticsearchClient elasticsearchClient() {
        try {
            String host = uris.replace("http://", "").replace("https://", "");
            String[] parts = host.split(":");
            String h = parts[0];
            int p = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
            RestClient restClient = RestClient.builder(new HttpHost(h, p)).build();
            RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            return new ElasticsearchClient(transport);
        } catch (Exception e) {
            log.warn("Elasticsearch unavailable at {}: {}", uris, e.getMessage());
            return null;
        }
    }
}
