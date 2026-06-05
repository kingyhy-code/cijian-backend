package com.cijian.profile.feign;

import com.cijian.common.result.R;
import com.cijian.profile.dto.TagStatVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "cijian-content", path = "/work")
public interface ContentProfileFeignClient {

    @GetMapping("/count/by-author")
    R<Long> countByAuthor(@RequestParam("authorId") Long authorId,
                          @RequestParam(value = "status", required = false) Integer status);

    @GetMapping("/like-count/total")
    R<Long> totalLikeCount(@RequestParam("authorId") Long authorId);

    @GetMapping("/ref-count/total")
    R<Long> totalInspirationRefCount(@RequestParam("authorId") Long authorId);

    @GetMapping("/tags/stat")
    R<List<TagStatVO>> getTagStats(@RequestParam("authorId") Long authorId);
}
