package com.example.common.elasticsearch.repository;

import com.example.common.elasticsearch.entity.UserEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEsRepository extends ElasticsearchRepository<UserEs, String> {
}
