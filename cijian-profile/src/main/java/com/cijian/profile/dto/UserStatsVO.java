package com.cijian.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsVO {
    private Long workCount;
    private Long totalLikeCount;
    private Long sentencePraiseCount;
    private Long inspirationRefCount;
}
