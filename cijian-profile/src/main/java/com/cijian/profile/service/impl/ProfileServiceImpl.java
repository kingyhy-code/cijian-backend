package com.cijian.profile.service.impl;

import com.cijian.common.result.R;
import com.cijian.profile.dto.*;
import com.cijian.profile.feign.ContentProfileFeignClient;
import com.cijian.profile.feign.InteractionProfileFeignClient;
import com.cijian.profile.mapper.LianciLogMapper;
import com.cijian.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ContentProfileFeignClient contentClient;
    private final InteractionProfileFeignClient interactionClient;
    private final LianciLogMapper lianciLogMapper;

    @Override
    public UserStatsVO getUserStats(Long userId) {
        long workCount = fetch(() -> contentClient.countByAuthor(userId, 1));
        long totalLikeCount = fetch(() -> contentClient.totalLikeCount(userId));
        long sentencePraiseCount = fetch(() -> interactionClient.countSentencePraise(userId));
        long inspirationRefCount = fetch(() -> contentClient.totalInspirationRefCount(userId));

        return UserStatsVO.builder()
                .workCount(workCount)
                .totalLikeCount(totalLikeCount)
                .sentencePraiseCount(sentencePraiseCount)
                .inspirationRefCount(inspirationRefCount)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TagStatVO> getUserTagStats(Long userId) {
        try {
            R<List<TagStatVO>> result = contentClient.getTagStats(userId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    @Override
    public LianciReportVO getLianciReport(Long userId) {
        long totalAdopted = lianciLogMapper.countByUserId(userId);

        Map<Integer, Long> levelBreakdown = new LinkedHashMap<>();
        levelBreakdown.put(1, 0L);
        levelBreakdown.put(2, 0L);
        levelBreakdown.put(3, 0L);
        levelBreakdown.put(4, 0L);

        List<Map<String, Object>> levelCounts = lianciLogMapper.countByLevel(userId);
        for (Map<String, Object> row : levelCounts) {
            Integer level = ((Number) row.get("level")).intValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            levelBreakdown.put(level, cnt);
        }

        List<WordStatVO> topWords = lianciLogMapper.topReplacementWords(userId, 5).stream()
                .map(row -> WordStatVO.builder()
                        .word((String) row.get("word"))
                        .count(((Number) row.get("cnt")).longValue())
                        .build())
                .collect(Collectors.toList());

        return LianciReportVO.builder()
                .totalReminded(totalAdopted)
                .totalAdopted(totalAdopted)
                .levelBreakdown(levelBreakdown)
                .topReplacementWords(topWords)
                .redundancyTrend(Collections.emptyList())
                .build();
    }

    @Override
    public CollectionExportVO exportCollections(Long userId) {
        StringBuilder md = new StringBuilder();
        md.append("# 我的收藏夹\n\n");
        md.append("> 导出时间: ").append(java.time.LocalDateTime.now()).append("\n\n");
        md.append("收藏数据请通过 `/collection/list` 接口获取，本模块提供聚合导出框架。\n");

        return CollectionExportVO.builder()
                .fileName("collections_" + userId + ".md")
                .content(md.toString())
                .build();
    }

    private long fetch(java.util.function.Supplier<R<Long>> supplier) {
        try {
            R<Long> result = supplier.get();
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ignored) {}
        return 0L;
    }
}
