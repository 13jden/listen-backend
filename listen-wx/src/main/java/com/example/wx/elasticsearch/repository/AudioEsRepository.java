package com.example.wx.elasticsearch.repository;

import com.example.wx.elasticsearch.entity.AudioEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioEsRepository extends ElasticsearchRepository<AudioEs, String> {
}
