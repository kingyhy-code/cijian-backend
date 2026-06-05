package com.cijian.common.util;

import cn.hutool.core.bean.BeanUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BeanCopyUtils {

    public static <T> T copy(Object source, Class<T> targetClass) {
        if (source == null) return null;
        return BeanUtil.copyProperties(source, targetClass);
    }

    public static <T> List<T> copyList(List<?> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(s -> BeanUtil.copyProperties(s, targetClass))
                .collect(Collectors.toList());
    }
}
