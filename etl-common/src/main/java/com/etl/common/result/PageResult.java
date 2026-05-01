package com.etl.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总数
     */
    private long total;

    /**
     * 当前页码
     */
    private long pageNum;

    /**
     * 每页大小
     */
    private long pageSize;

    /**
     * 总页数
     */
    private long pages;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        // 防止除零异常，pageSize为0时默认为1
        long safePageSize = pageSize > 0 ? pageSize : 1;
        result.setPages((total + safePageSize - 1) / safePageSize);
        return result;
    }
}
