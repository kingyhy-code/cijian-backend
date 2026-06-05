package com.cijian.search.mq;

import com.cijian.common.mq.RocketMQTopics;
import com.cijian.search.document.WorkDocument;
import com.cijian.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
// @RocketMQMessageListener — RocketMQ client 版本与 broker 5.2.0 不兼容，暂以 REST 方式同步
public class IndexSyncConsumer {

    private final SearchService searchService;

    public void onMessage(String message) {
        try {
            JSONObject json = JSONUtil.parseObj(message);
            handleMessage(json);
        } catch (Exception e) {
            log.error("Failed to parse index sync message: {}", e.getMessage());
        }
    }

    /** 供外部模块调用 */
    @SuppressWarnings("unchecked")
    public void handleMessage(Map<String, Object> message) {
        try {
            String action = (String) message.getOrDefault("action", "index");

            if ("delete".equals(action)) {
                Long workId = toLong(message.get("workId"));
                if (workId != null) {
                    searchService.delete(workId);
                    log.info("ES index deleted: workId={}", workId);
                }
                return;
            }

            WorkDocument doc = WorkDocument.builder()
                    .id(toLong(message.get("id")))
                    .title((String) message.get("title"))
                    .summary((String) message.get("summary"))
                    .content((String) message.get("content"))
                    .authorId(toLong(message.get("authorId")))
                    .authorName((String) message.get("authorName"))
                    .status(((Number) message.getOrDefault("status", 1)).intValue())
                    .isInspiration(((Number) message.getOrDefault("isInspiration", 0)).intValue())
                    .isMasterpiece(((Number) message.getOrDefault("isMasterpiece", 0)).intValue())
                    .country((String) message.get("country"))
                    .viewCount(toLong(message.get("viewCount"), 0L))
                    .likeCount(toLong(message.get("likeCount"), 0L))
                    .commentCount(toLong(message.get("commentCount"), 0L))
                    .publishedAt((String) message.get("publishedAt"))
                    .build();
            searchService.index(doc);
            log.info("ES index updated: workId={}", message.get("id"));
        } catch (Exception e) {
            log.error("Index sync failed: {}", e.getMessage());
        }
    }

    private Long toLong(Object obj) { return toLong(obj, null); }
    private Long toLong(Object obj, Long defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.valueOf(obj.toString()); } catch (Exception e) { return defaultVal; }
    }
}
