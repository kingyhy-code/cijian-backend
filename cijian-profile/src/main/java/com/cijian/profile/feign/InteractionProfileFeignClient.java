package com.cijian.profile.feign;

import com.cijian.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cijian-interaction")
public interface InteractionProfileFeignClient {

    @GetMapping("/like/sentence-praise")
    R<Long> countSentencePraise(@RequestParam("authorId") Long authorId);
}
