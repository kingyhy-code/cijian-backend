package com.cijian.common.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 敏感词检测器——线程安全的 DFA 包装，支持运行时重建。
 */
public class SensitiveWordChecker {

    private final AtomicReference<SensitiveWordMatcher> matcherRef =
            new AtomicReference<>(SensitiveWordMatcher.empty());

    /**
     * 用词库重建 DFA 匹配器。key=敏感词, value=替换字符（null 表示仅检测不替换）。
     */
    public void reload(Map<String, Character> words) {
        matcherRef.set(SensitiveWordMatcher.build(words));
    }

    /**
     * 检测文本是否包含任意敏感词。
     */
    public boolean matches(String text) {
        return matcherRef.get().matches(text);
    }

    /**
     * 过滤文本，替换命中敏感词。
     */
    public String filter(String text) {
        return matcherRef.get().filter(text);
    }
}
