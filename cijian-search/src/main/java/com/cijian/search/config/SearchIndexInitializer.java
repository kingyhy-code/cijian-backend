package com.cijian.search.config;

import com.cijian.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchIndexInitializer implements ApplicationRunner {

    private final SearchService searchService;

    @Value("${cijian.content.url:http://localhost:8082}")
    private String contentServiceUrl;

    public SearchIndexInitializer(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean ok = searchService.ensureIndex();
        if (!ok) {
            log.warn("ES index initialization failed — search will be unavailable");
            return;
        }
        log.info("ES index ready, syncing data from {}", contentServiceUrl);
        int count = searchService.bulkSyncFromContentService(contentServiceUrl);
        if (count >= 0) {
            log.info("ES synced {} works", count);
        } else {
            log.warn("ES data sync failed");
        }
    }
}
