package com.example.common.elasticsearch.repository;

import com.example.common.elasticsearch.entity.TestEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestEsRepository extends ElasticsearchRepository<TestEs, String> {
}
