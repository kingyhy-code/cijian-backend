package com.cijian.common.util;

import java.util.HashMap;
import java.util.Map;

/**
 * DFA（确定性有穷自动机）敏感词匹配器。
 * 线程安全——构建后只读。
 */
public final class SensitiveWordMatcher {

    private static final SensitiveWordMatcher EMPTY = new SensitiveWordMatcher(new HashMap<>());

    private final Map<Character, Node> root;

    private SensitiveWordMatcher(Map<Character, Node> root) {
        this.root = root;
    }

    /**
     * 用词库构建匹配器。Map 的 key 是敏感词，value 是替换字符（可为 null 表示仅检测）。
     */
    public static SensitiveWordMatcher build(Map<String, Character> words) {
        Map<Character, Node> root = new HashMap<>();
        for (Map.Entry<String, Character> entry : words.entrySet()) {
            String word = entry.getKey();
            Character replacement = entry.getValue();
            if (word == null || word.isEmpty()) continue;
            insert(root, word, replacement);
        }
        return words.isEmpty() ? EMPTY : new SensitiveWordMatcher(root);
    }

    /**
     * 空匹配器，matches 始终返回 false。
     */
    public static SensitiveWordMatcher empty() {
        return EMPTY;
    }

    private static void insert(Map<Character, Node> root, String word, Character replacement) {
        Map<Character, Node> current = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Node node = current.get(c);
            if (node == null) {
                node = new Node();
                current.put(c, node);
            }
            current = node.children;
        }
        current.put('\0', new Node(replacement));
    }

    /**
     * 检测文本是否包含任意敏感词。
     */
    public boolean matches(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            int matchedLen = matchAt(text, i);
            if (matchedLen > 0) return true;
        }
        return false;
    }

    /**
     * 过滤文本，将命中敏感词替换为对应的替换字符。
     */
    public String filter(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int matchedLen = matchAt(text, i);
            if (matchedLen > 0) {
                Node leaf = findLeaf(text, i, matchedLen);
                String replacement = leaf != null && leaf.replacement != null
                        ? String.valueOf(leaf.replacement)
                        : "***";
                sb.append(replacement);
                i += matchedLen;
            } else {
                sb.append(text.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * 从 pos 位置开始，返回最长匹配长度。无匹配返回 0。
     */
    private int matchAt(String text, int pos) {
        Map<Character, Node> current = root;
        int matchLen = 0;
        for (int i = pos; i < text.length(); i++) {
            Node node = current.get(text.charAt(i));
            if (node == null) break;
            current = node.children;
            matchLen = i - pos + 1;
        }
        Node end = current.get('\0');
        return end != null ? matchLen : 0;
    }

    /**
     * 从 pos 开始、长度为 len 的词末尾叶子节点。
     */
    private Node findLeaf(String text, int pos, int len) {
        Map<Character, Node> current = root;
        for (int i = 0; i < len; i++) {
            Node node = current.get(text.charAt(pos + i));
            if (node == null) return null;
            current = node.children;
        }
        return current.get('\0');
    }

    private static class Node {
        final Map<Character, Node> children = new HashMap<>();
        final Character replacement;

        Node() {
            this.replacement = null;
        }

        Node(Character replacement) {
            this.replacement = replacement;
        }
    }
}
