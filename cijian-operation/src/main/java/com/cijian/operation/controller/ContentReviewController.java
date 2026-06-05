package com.cijian.operation.controller;

import com.cijian.common.page.PageResult;
import com.cijian.common.result.R;
import com.cijian.operation.entity.Work;
import com.cijian.operation.service.ContentReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/operation/review")
@RequiredArgsConstructor
public class ContentReviewController {

    private final ContentReviewService service;

    @GetMapping("/pending")
    public R<PageResult<Work>> listPending(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(service.listPending(pageNum, pageSize));
    }

    @GetMapping
    public R<PageResult<Work>> listAll(@RequestParam(required = false) Integer status,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(service.listAll(status, pageNum, pageSize));
    }

    @PutMapping("/{id}/approve")
    public R<?> approve(@PathVariable Long id) {
        service.approve(id);
        return R.success("审核通过");
    }

    @PutMapping("/{id}/reject")
    public R<?> reject(@PathVariable Long id) {
        service.reject(id);
        return R.success("已驳回");
    }

    @GetMapping("/stats")
    public R<Map<String, Long>> stats() {
        return R.success(service.stats());
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        service.deleteWork(id);
        return R.success("已删除");
    }

    @DeleteMapping("/{id}/force")
    public R<?> forceDelete(@PathVariable Long id) {
        service.forceDeleteWork(id);
        return R.success("已彻底删除");
    }
}
