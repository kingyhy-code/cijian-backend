package com.cijian.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LianciReportVO {
    private Long totalReminded;
    private Long totalAdopted;
    private Map<Integer, Long> levelBreakdown;
    private List<WordStatVO> topReplacementWords;
    private List<RedundancyPointVO> redundancyTrend;
}
