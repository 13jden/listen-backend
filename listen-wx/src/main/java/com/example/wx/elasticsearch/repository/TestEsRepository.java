package com.example.wx.elasticsearch.repository;

import com.example.wx.elasticsearch.entity.TestEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestEsRepository extends ElasticsearchRepository<TestEs, String> {
}
