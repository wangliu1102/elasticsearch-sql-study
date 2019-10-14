package com.wl.essql.service;

import com.wl.essql.dto.SearchResultDTO;

public interface ElasticSearchSqlService {

    SearchResultDTO search(String sql);

    String explain(String sql);

    SearchResultDTO query(String sql, String index);
}
