package com.example.common.elasticsearch.repository;

import com.example.common.elasticsearch.entity.AudioEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioEsRepository extends ElasticsearchRepository<AudioEs, String> {
}
