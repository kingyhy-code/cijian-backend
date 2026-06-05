package com.cijian.operation.controller;

import com.cijian.common.result.R;
import com.cijian.operation.mapper.UserMapper;
import com.cijian.operation.mapper.WorkMapper;
import com.cijian.operation.service.ContentReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/operation/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final ContentReviewService reviewService;

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        long userCount = userMapper.selectCount(null);
        long workCount = workMapper.selectCount(null);
        Map<String, Long> reviewStats = reviewService.stats();
        return R.success(Map.of(
                "userCount", userCount,
                "workCount", workCount,
                "pendingReview", reviewStats.get("pending"),
                "publishedWorks", reviewStats.get("published"),
                "rejectedWorks", reviewStats.get("rejected")
        ));
    }
}
