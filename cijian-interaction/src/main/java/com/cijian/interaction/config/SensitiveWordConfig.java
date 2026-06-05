package com.cijian.interaction.config;

import com.cijian.common.dto.SensitiveWordDTO;
import com.cijian.common.result.R;
import com.cijian.common.util.SensitiveWordChecker;
import com.cijian.interaction.feign.OperationFeignClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SensitiveWordConfig {

    private final OperationFeignClient operationFeignClient;

    private volatile SensitiveWordChecker checker;

    @Bean
    public SensitiveWordChecker sensitiveWordChecker() {
        this.checker = new SensitiveWordChecker();
        loadWords();
        return this.checker;
    }

    /**
     * 每5分钟拉取词库并重建 DFA。
     */
    @Scheduled(fixedRate = 300_000)
    public void refreshWords() {
        loadWords();
    }

    private void loadWords() {
        try {
            R<List<SensitiveWordDTO>> result = operationFeignClient.getDict();
            if (result != null && result.getData() != null) {
                Map<String, Character> words = new HashMap<>();
                for (SensitiveWordDTO dto : result.getData()) {
                    Character replacement = dto.getReplacement() != null && !dto.getReplacement().isEmpty()
                            ? dto.getReplacement().charAt(0) : null;
                    words.put(dto.getWord(), replacement);
                }
                checker.reload(words);
                log.info("敏感词库加载成功，共 {} 个词", words.size());
            }
        } catch (Exception e) {
            log.warn("敏感词库加载失败: {}", e.getMessage());
        }
    }
}
