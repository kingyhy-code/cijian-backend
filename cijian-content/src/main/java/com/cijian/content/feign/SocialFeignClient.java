package com.cijian.content.feign;

import com.cijian.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "cijian-interaction", path = "/follow")
public interface SocialFeignClient {

    @GetMapping("/following/{userId}/ids")
    R<List<Long>> getFollowingUserIds(@PathVariable("userId") Long userId);
}
