package com.cijian.operation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.page.PageResult;
import com.cijian.common.dto.SensitiveWordDTO;
import com.cijian.common.util.SensitiveWordMatcher;
import com.cijian.operation.entity.SensitiveWord;
import com.cijian.operation.mapper.SensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final SensitiveWordMapper mapper;

    private volatile SensitiveWordMatcher matcher = SensitiveWordMatcher.empty();

    @PostConstruct
    void init() {
        rebuildMatcher();
    }

    private void rebuildMatcher() {
        List<SensitiveWord> words = mapper.selectList(null);
        Map<String, Character> map = new HashMap<>();
        for (SensitiveWord sw : words) {
            Character replacement = null;
            if (sw.getReplacement() != null && !sw.getReplacement().isEmpty()) {
                replacement = sw.getReplacement().charAt(0);
            }
            map.put(sw.getWord(), replacement);
        }
        this.matcher = SensitiveWordMatcher.build(map);
    }

    public SensitiveWord add(String word, String replacement, Integer level) {
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(word);
        sw.setReplacement(replacement);
        sw.setLevel(level != null ? level : 1);
        mapper.insert(sw);
        rebuildMatcher();
        return sw;
    }

    public void delete(Long id) {
        mapper.deleteById(id);
        rebuildMatcher();
    }

    public PageResult<SensitiveWord> list(int pageNum, int pageSize) {
        Page<SensitiveWord> page = mapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SensitiveWord>().orderByDesc(SensitiveWord::getCreatedAt));
        return PageResult.of(page.getTotal(), page.getRecords(), pageNum, pageSize);
    }

    public String filter(String text) {
        return matcher.filter(text);
    }

    public boolean containsSensitive(String text) {
        return matcher.matches(text);
    }

    /**
     * 导出全部词库，供其他模块拉取。
     */
    public List<SensitiveWordDTO> getAllWords() {
        return mapper.selectList(null).stream()
                .map(sw -> new SensitiveWordDTO(sw.getWord(), sw.getReplacement()))
                .collect(Collectors.toList());
    }
}
