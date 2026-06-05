package com.cijian.common.page;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private List<T> records;
    private int pageNum;
    private int pageSize;

    public static <T> PageResult<T> of(long total, List<T> records, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setRecords(records);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}
