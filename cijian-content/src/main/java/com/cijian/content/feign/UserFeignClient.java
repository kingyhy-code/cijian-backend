package com.cijian.content.feign;

import com.cijian.common.result.R;
import com.cijian.content.dto.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/user")
public interface UserFeignClient {

    @GetMapping("/{id}")
    R<UserVO> getUserById(@PathVariable("id") Long id);
}
