package com.cijian.search.controller;

import com.cijian.common.page.PageResult;
import com.cijian.common.result.R;
import com.cijian.search.document.WorkDocument;
import com.cijian.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/works")
    public R<PageResult<WorkDocument>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "NEWEST") String sortBy) {
        return R.success(searchService.search(keyword, authorId, tag, country, pageNum, pageSize, sortBy));
    }

    @GetMapping("/suggest")
    public R<Map<String, Object>> suggest(@RequestParam String prefix) {
        return R.success(searchService.suggest(prefix));
    }

    @GetMapping("/health")
    public R<Boolean> health() {
        return R.success(searchService.ensureIndex());
    }

    @PostMapping("/reindex")
    public R<Integer> reindex(
            @RequestParam(defaultValue = "http://localhost:8082") String contentServiceUrl) {
        int count = searchService.bulkSyncFromContentService(contentServiceUrl);
        if (count < 0) {
            return R.error("ES unavailable");
        }
        return R.success("Indexed " + count + " works", count);
    }
}
