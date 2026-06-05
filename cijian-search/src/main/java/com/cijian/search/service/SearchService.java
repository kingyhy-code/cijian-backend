package com.cijian.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.cijian.common.page.PageResult;
import com.cijian.search.document.WorkDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {

    private static final String INDEX = "cijian_works";

    private final ElasticsearchClient client;

    public SearchService(@Autowired(required = false) ElasticsearchClient client) {
        this.client = client;
    }

    private boolean available() { return client != null; }

    public void index(WorkDocument doc) {
        if (!available()) return;
        try {
            client.index(IndexRequest.of(i -> i.index(INDEX).id(String.valueOf(doc.getId())).document(doc)));
        } catch (Exception e) {
            log.warn("ES index failed: id={}, msg={}", doc.getId(), e.getMessage());
        }
    }

    public void delete(Long workId) {
        if (!available()) return;
        try {
            client.delete(DeleteRequest.of(d -> d.index(INDEX).id(String.valueOf(workId))));
        } catch (Exception e) {
            log.warn("ES delete failed: id={}, msg={}", workId, e.getMessage());
        }
    }

    public PageResult<WorkDocument> search(String keyword, Long authorId, String tag,
                                            String country, int pageNum, int pageSize, String sortBy) {
        if (!available()) return PageResult.of(0, Collections.emptyList(), pageNum, pageSize);
        try {
            var boolBuilder = new BoolQuery.Builder();

            // Published works only
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("status").value(1))));

            // Keyword search in title and content
            if (keyword != null && !keyword.isBlank()) {
                boolBuilder.must(Query.of(q -> q.multiMatch(MultiMatchQuery.of(m -> m
                        .query(keyword)
                        .fields("title^3", "summary^2", "content", "authorName")))));
            }

            // Filter by author
            if (authorId != null) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t.field("authorId").value(authorId))));
            }

            // Filter by country
            if (country != null && !country.isBlank()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t.field("country").value(country))));
            }

            // Filter by tag — tag data is not in ES, so we skip or use a workaround
            // (Tag info could be added to the index if needed)

            int from = (pageNum - 1) * pageSize;

            var searchReq = SearchRequest.of(s -> s
                    .index(INDEX)
                    .query(boolBuilder.build()._toQuery())
                    .from(from)
                    .size(pageSize)
                    .sort(so -> so.field(f -> f
                            .field("hot".equals(sortBy) ? "likeCount" : "publishedAt")
                            .order(SortOrder.Desc))));

            SearchResponse<WorkDocument> response = client.search(searchReq, WorkDocument.class);
            List<WorkDocument> records = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return PageResult.of(total, records, pageNum, pageSize);
        } catch (Exception e) {
            log.error("ES search failed: {}", e.getMessage());
            return PageResult.of(0, Collections.emptyList(), pageNum, pageSize);
        }
    }

    public Map<String, Object> suggest(String prefix) {
        if (!available()) return Collections.emptyMap();
        try {
            var response = client.search(SearchRequest.of(s -> s
                    .index(INDEX)
                    .suggest(su -> su.text(prefix)
                            .suggesters("title_suggest", ss -> ss
                                    .completion(c -> c.field("title.suggest"))))), WorkDocument.class);
            return Map.of("suggestions", response.suggest());
        } catch (Exception e) {
            log.warn("ES suggest failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public boolean ensureIndex() {
        if (!available()) return false;
        try {
            boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(INDEX))).value();
            if (!exists) {
                client.indices().create(c -> c.index(INDEX).mappings(m -> m
                        .properties("id", p -> p.long_(l -> l))
                        .properties("title", p -> p.text(t -> t.analyzer("standard")))
                        .properties("summary", p -> p.text(t -> t.analyzer("standard")))
                        .properties("content", p -> p.text(t -> t.analyzer("standard")))
                        .properties("authorId", p -> p.long_(l -> l))
                        .properties("authorName", p -> p.keyword(k -> k))
                        .properties("status", p -> p.integer(i -> i))
                        .properties("isInspiration", p -> p.integer(i -> i))
                        .properties("isMasterpiece", p -> p.integer(i -> i))
                        .properties("country", p -> p.keyword(k -> k))
                        .properties("viewCount", p -> p.long_(l -> l))
                        .properties("likeCount", p -> p.long_(l -> l))
                        .properties("commentCount", p -> p.long_(l -> l))
                        .properties("publishedAt", p -> p.keyword(k -> k))
                ));
                log.info("ES index created: {}", INDEX);
            }
            return true;
        } catch (Exception e) {
            log.error("ES index init failed: {}", e.getMessage());
            return false;
        }
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public int bulkSyncFromContentService(String contentServiceUrl) {
        if (!available()) return -1;
        int total = 0;
        int pageNum = 1;
        int pageSize = 50;

        while (true) {
            try {
                String url = contentServiceUrl + "/work/list?pageNum=" + pageNum + "&pageSize=" + pageSize;
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response == null || response.get("data") == null) break;

                Map<String, Object> data = (Map<String, Object>) response.get("data");
                List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
                if (records == null || records.isEmpty()) break;

                for (Map<String, Object> record : records) {
                    Map<String, Object> author = (Map<String, Object>) record.get("author");
                    WorkDocument doc = WorkDocument.builder()
                            .id(toLong(record.get("id")))
                            .title((String) record.get("title"))
                            .summary((String) record.get("summary"))
                            .content((String) record.get("content"))
                            .authorId(author != null ? toLong(author.get("id")) : null)
                            .authorName(author != null ? (String) author.get("nickname") : null)
                            .status(1)
                            .isInspiration(((Number) record.getOrDefault("isInspiration", 0)).intValue())
                            .isMasterpiece(((Number) record.getOrDefault("isMasterpiece", 0)).intValue())
                            .country((String) record.get("country"))
                            .viewCount(toLong(record.get("viewCount"), 0L))
                            .likeCount(toLong(record.get("likeCount"), 0L))
                            .commentCount(toLong(record.get("commentCount"), 0L))
                            .publishedAt(record.get("publishedAt") != null ? record.get("publishedAt").toString() : null)
                            .build();
                    index(doc);
                    total++;
                }

                long totalRecords = ((Number) data.get("total")).longValue();
                if (pageNum * pageSize >= totalRecords) break;
                pageNum++;
            } catch (Exception e) {
                log.error("Bulk sync failed at page {}: {}", pageNum, e.getMessage());
                break;
            }
        }
        log.info("Bulk sync complete: {} works indexed", total);
        return total;
    }

    private Long toLong(Object obj) { return toLong(obj, null); }
    private Long toLong(Object obj, Long defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.valueOf(obj.toString()); } catch (Exception e) { return defaultVal; }
    }
}
