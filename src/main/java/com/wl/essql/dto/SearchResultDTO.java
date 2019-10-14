package com.wl.essql.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SearchResultDTO {

    /**
     * 查询结果总数
     */
    private long total;

    /**
     * 查询耗时
     */
    private long time;

    /**
     * 返回结果大小
     */
    private long resultSize;

    /**
     * 数据
     */
    private List<IndexRowData> result;

    /**
     * 结果列
     */
    private Set<String> resultColumns;
}
