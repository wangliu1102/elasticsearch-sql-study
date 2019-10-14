package com.wl.essql.common;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

@Configuration
public class ElasticSearchConfig {

    @Value("${elasticsearch.host}")
    private String[] ipAddress;
    @Value("${elasticsearch.maxRetryTimeout}")
    private Integer maxRetryTimeout;
    @Value("${elasticsearch.sql.host}")
    private String[] esSqlAddress;

    @Bean
    public TransportClient transportClient() {

        // client.transport.sniff: 设置为true时自动嗅探整个集群的状态，把集群中其它机器的ip地址加到客户端中
        // client.transport.ignore_cluster_name：设置为true时忽略集群名验证；
        // client.transport.ping_timeout：等待ping命令返回结果时间，默认为5秒；
        // client.transport.nodes_sampler_interval：节点之间互相ping，互连检测时间间隔
        Settings settings = Settings.builder()
                // 不允许自动刷新地址列表
                .put("client.transport.sniff", false)
                .put("client.transport.ignore_cluster_name", true)
                .build();

        // 初始化地址
        TransportAddress[] transportAddresses = new TransportAddress[esSqlAddress.length];
        for (int i = 0; i < esSqlAddress.length; i++) {
            String[] addressItems = esSqlAddress[i].split(":");
            try {
                transportAddresses[i] = new TransportAddress(InetAddress.getByName(addressItems[0]),
                        Integer.valueOf(addressItems[1]));
            } catch (Exception e) {
//                throw new EsOperationException(e);
            }
        }

        PreBuiltTransportClient preBuiltTransportClient = new PreBuiltTransportClient(settings);

        TransportClient client = preBuiltTransportClient
                .addTransportAddresses(transportAddresses);
        return client;
    }

    public String[] getEsSqlAddress() {
        return esSqlAddress;
    }

    public void setEsSqlAddress(String[] esSqlAddress) {
        this.esSqlAddress = esSqlAddress;
    }
}