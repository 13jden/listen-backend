package com.example.common.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zjw
 * @description RestHighLevelClient 配置（ES 7.6.2）
 */
@Configuration
@Slf4j
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username}")
    private String elasticsearchUsername;

    @Value("${spring.elasticsearch.password}")
    private String elasticsearchPassword;

    /**
     * 配置 RestHighLevelClient（ES 7.x 客户端）
     */
    @Bean
    public RestHighLevelClient restHighLevelClient() {
        // 解析地址，格式如: localhost:9200
        String hostAndPort = elasticsearchUris.replace("http://", "").replace("https://", "");
        String[] hostPort = hostAndPort.split(":");
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 9200;

        // 初始化连接ES的HttpHost信息
        HttpHost httpHost = new HttpHost(host, port, "http");

        // 设置认证信息
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));

        // 构建时设置连接信息，基于set设置认证信息
        RestClientBuilder restClientBuilder = RestClient.builder(httpHost);
        restClientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        });

        // 构建连接ES的client对象
        return new RestHighLevelClient(restClientBuilder);
    }

    @PostConstruct
    public void init() {
        log.info("Elasticsearch 配置初始化完成，地址: {}", elasticsearchUris);
    }
}
