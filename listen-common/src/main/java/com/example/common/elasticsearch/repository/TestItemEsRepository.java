package com.example.common.elasticsearch.repository;

import com.example.common.elasticsearch.entity.TestItemEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestItemEsRepository extends ElasticsearchRepository<TestItemEs, String> {
}
