package com.cijian.interaction.feign;

import com.cijian.common.dto.SensitiveWordDTO;
import com.cijian.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "cijian-operation", path = "/api/operation/sensitive-word")
public interface OperationFeignClient {

    @GetMapping("/dict")
    R<List<SensitiveWordDTO>> getDict();
}
