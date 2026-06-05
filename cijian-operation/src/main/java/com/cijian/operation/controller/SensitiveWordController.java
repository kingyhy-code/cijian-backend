package com.cijian.operation.controller;

import com.cijian.common.dto.SensitiveWordDTO;
import com.cijian.common.page.PageResult;
import com.cijian.common.result.R;
import com.cijian.operation.entity.SensitiveWord;
import com.cijian.operation.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operation/sensitive-word")
@RequiredArgsConstructor
public class SensitiveWordController {

    private final SensitiveWordService service;

    @PostMapping
    public R<SensitiveWord> add(@RequestParam String word,
                                 @RequestParam(required = false) String replacement,
                                 @RequestParam(defaultValue = "1") Integer level) {
        return R.success(service.add(word, replacement, level));
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        service.delete(id);
        return R.success("已删除");
    }

    @GetMapping
    public R<PageResult<SensitiveWord>> list(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "20") int pageSize) {
        return R.success(service.list(pageNum, pageSize));
    }

    @PostMapping("/check")
    public R<Boolean> check(@RequestBody String text) {
        return R.success(service.containsSensitive(text));
    }

    @PostMapping("/filter")
    public R<String> filter(@RequestBody String text) {
        return R.success(service.filter(text));
    }

    @GetMapping("/dict")
    public R<List<SensitiveWordDTO>> dict() {
        return R.success(service.getAllWords());
    }
}
