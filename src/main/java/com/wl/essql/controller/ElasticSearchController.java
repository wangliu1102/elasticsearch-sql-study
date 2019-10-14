package com.wl.essql.controller;

import com.wl.essql.common.CommonResult;
import com.wl.essql.dto.QueryDto;
import com.wl.essql.dto.SearchResultDTO;
import com.wl.essql.service.ElasticSearchSqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ES索引数据查询 - elasticsearch-sql 方式
 * <p>
 * Created by 王柳
 * Date 2019/1/14 14:05
 * version:1.0
 */
@RequestMapping("/es/data")
@RestController
public class ElasticSearchController {

    @Autowired
    private ElasticSearchSqlService elasticSearchSqlService;

    @PostMapping(value = "/search")
    public CommonResult search(@RequestBody QueryDto queryDto) {
        SearchResultDTO resultDTO = elasticSearchSqlService.search(queryDto.getSql());
        return CommonResult.success(resultDTO.getResult());
    }

    @PostMapping(value = "/query")
    public CommonResult query(@RequestBody QueryDto queryDto) {
        SearchResultDTO resultDTO = elasticSearchSqlService.query(queryDto.getSql(), queryDto.getIndex());
        return CommonResult.success(resultDTO.getResult());
    }

    @PostMapping(value = "/explain")
    public CommonResult explain(@RequestBody QueryDto queryDto) {
        return CommonResult.success(elasticSearchSqlService.explain(queryDto.getSql()));
    }

}
