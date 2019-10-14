package com.wl.essql.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledResultSet;
import com.alibaba.druid.pool.ElasticSearchDruidDataSourceFactory;
import com.alibaba.druid.pool.ElasticSearchResultSetMetaDataBase;
import com.alibaba.druid.util.jdbc.ResultSetMetaDataBase;
import com.alibaba.fastjson.JSONObject;
import com.wl.essql.common.ElasticSearchConfig;
import com.wl.essql.dto.IndexRowData;
import com.wl.essql.dto.SearchResultDTO;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.plugin.nlpcn.QueryActionElasticExecutor;
import org.elasticsearch.search.SearchHits;
import org.nlpcn.es4sql.SearchDao;
import org.nlpcn.es4sql.jdbc.ObjectResult;
import org.nlpcn.es4sql.jdbc.ObjectResultsExtractor;
import org.nlpcn.es4sql.query.QueryAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * ES索引数据查询 - elasticsearch-sql 方式
 * <p>
 * Created by 王柳
 * Date 2019/1/14 14:05
 * version:1.0
 */
@Service
@Slf4j
public class ElasticSearchSqlServiceImpl implements ElasticSearchSqlService {

    @Autowired
    private ElasticSearchConfig esConfiguration;

    @Autowired
    private TransportClient transportClient;

    /**
     * @Description API方式查询
     * @auther 王柳
     * @date 2019/1/14 14:08
     * @params [sql]
     */
    @Override
    public SearchResultDTO search(String sql) {
        SearchResultDTO resultDTO = new SearchResultDTO();
        try {
            long before = System.currentTimeMillis();
            log.info("before:"+ before);
            SearchDao searchDao = new SearchDao(transportClient);
            QueryAction queryAction = searchDao.explain(sql);
            Object execution = QueryActionElasticExecutor.executeAnyAction(searchDao.getClient(), queryAction);
            ObjectResult result = getObjectResult(execution, true, true, true, true);
            resultDTO.setResultColumns(Sets.newHashSet(result.getHeaders()));
            List<IndexRowData> indexRowDatas = new ArrayList<>();
            for (List<Object> line : result.getLines()) {
                IndexRowData indexRowData = new IndexRowData();
                for (int i = 0; i < result.getHeaders().size(); i++) {
                    indexRowData.build(result.getHeaders().get(i), line.get(i));
                }
                indexRowDatas.add(indexRowData);
            }
            resultDTO.setResultSize(indexRowDatas.size());
            if (execution instanceof SearchHits) {
                resultDTO.setTotal(((SearchHits) execution).getTotalHits());
            } else {
                resultDTO.setTotal(indexRowDatas.size());
            }
            resultDTO.setResult(indexRowDatas);
            long end = System.currentTimeMillis();
            log.info("end"+ end);
            log.info("end - before"+ (end - before));
            resultDTO.setTime((end - before) / 1000);
            log.info("查询数据结果集: {}", JSONObject.toJSONString(resultDTO));
        } catch (Exception e) {
            log.error("根据ES-SQL查询数据异常: {}", e.getMessage());
            throw new ElasticsearchException(e.getMessage());
        }
        return resultDTO;
    }

    private ObjectResult getObjectResult(Object execution,
                                         boolean flat,
                                         boolean includeScore,
                                         boolean includeType,
                                         boolean includeId) throws Exception {

        return (new ObjectResultsExtractor(includeScore, includeType, includeId)).extractResults(execution, flat);
    }

    /**
     * @Description 查询替换为别名进行
     * @auther 王柳
     * @date 2019/1/14 14:08
     * @params [sql]
     */
    private String replaceTableName(String sql) throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(sql);
        Select select = (Select) statement;

        StringBuilder buffer = new StringBuilder();
        ExpressionDeParser expressionDeParser = new ExpressionDeParser();

        TableNameParser tableNameParser = new TableNameParser(expressionDeParser, buffer);

        expressionDeParser.setSelectVisitor(tableNameParser);
        expressionDeParser.setBuffer(buffer);
        select.getSelectBody().accept(tableNameParser);
        return select.toString();
    }

    public class TableNameParser extends SelectDeParser {

        public TableNameParser(ExpressionVisitor expressionVisitor, StringBuilder buffer) {
            super(expressionVisitor, buffer);
        }

        @Override
        public void visit(Table tableName) {
            tableName.setName(tableName.getName());
        }
    }

    /**
     * @Description 解释SQL
     * @auther 王柳
     * @date 2019/1/14 14:08
     * @params [sql]
     */
    @Override
    public String explain(String sql) {
        try {
            SearchDao searchDao = new SearchDao(transportClient);
            QueryAction queryAction = searchDao.explain(sql);
            log.info(queryAction.explain().explain());
            return queryAction.explain().explain();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @Description JDBC 方式查询
     * @auther 王柳
     * @date 2019/1/14 14:08
     * @params [sql, index]
     */
    @Override
    public SearchResultDTO query(String sql, String index) {
        SearchResultDTO searchResultDTO = new SearchResultDTO();
        PreparedStatement ps = null;
        Connection connection = null;
        DruidDataSource dds = null;
        try {
            long before = System.currentTimeMillis();
            Properties properties = new Properties();
            properties.put("url", buildJdbcUrl() + index);
            dds = (DruidDataSource) ElasticSearchDruidDataSourceFactory.createDataSource(properties);
            connection = dds.getConnection();
            ps = connection.prepareStatement(sql);
            DruidPooledResultSet resultSet = (DruidPooledResultSet) ps.executeQuery();
            ElasticSearchResultSetMetaDataBase metaData = (ElasticSearchResultSetMetaDataBase) resultSet.getMetaData();
            List<ResultSetMetaDataBase.ColumnMetaData> columns = metaData.getColumns();
            List<IndexRowData> indexRowDatas = new ArrayList<>();
            while (resultSet.next()) {
                IndexRowData indexRowData = new IndexRowData();
                for (int i = 0, size = columns.size(); i < size; i++) {
                    String columnLabel = columns.get(i).getColumnLabel();
                    Object value = resultSet.getObject(columnLabel);
                    indexRowData.put(columnLabel, value);
                }
                indexRowDatas.add(indexRowData);
            }
            searchResultDTO.setResult(indexRowDatas);
            searchResultDTO.setResultSize(indexRowDatas.size());
            searchResultDTO.setTotal(indexRowDatas.size());
            searchResultDTO.setResult(indexRowDatas);
            searchResultDTO.setTime((System.currentTimeMillis() - before) / 1000);
            log.info("根据ES-SQL-JDBC查询数据结果: {}", JSONObject.toJSONString(searchResultDTO));
        } catch (SQLException e) {
            log.error("根据ES-SQL-JDBC查询数据异常: {}", JSONObject.toJSONString(e.getMessage()));
        } catch (Exception e) {
            log.error("根据ES-SQL-JDBC查询数据异常: {}", JSONObject.toJSONString(e.getMessage()));
        } finally {
            try {
                ps.close();
                connection.close();
                dds.close();
            } catch (SQLException e) {
                log.error("根据ES-SQL-JDBC查询数据, 关闭数据源异常: {}", e.getMessage());
            }
        }
        return searchResultDTO;
    }

    private String buildJdbcUrl() {
        StringBuffer url = new StringBuffer("jdbc:elasticsearch://");
        String[] esSqlAddress = esConfiguration.getEsSqlAddress();
        if (esSqlAddress.length == 0) {
            throw new ElasticsearchException("there is no value of [elasticSearch.sql.host], please set it.");
        }
        url.append(String.join(",", esSqlAddress)).append("/");
        return url.toString();
    }


}
