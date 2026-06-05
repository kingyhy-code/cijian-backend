package com.cijian.interaction.feign;

import com.cijian.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cijian-content", path = "/work")
public interface ContentFeignClient {

    @GetMapping("/{id}")
    R<?> getWorkById(@PathVariable("id") Long id);

    @PostMapping("/{id}/likeCount")
    R<?> updateLikeCount(@PathVariable("id") Long workId, @RequestParam("delta") int delta);

    @PostMapping("/{id}/commentCount")
    R<?> updateCommentCount(@PathVariable("id") Long workId, @RequestParam("delta") int delta);

    @PostMapping("/{id}/collectCount")
    R<?> updateCollectCount(@PathVariable("id") Long workId, @RequestParam("delta") int delta);
}
