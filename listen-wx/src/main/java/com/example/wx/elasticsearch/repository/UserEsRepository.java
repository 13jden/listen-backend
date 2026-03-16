package com.example.wx.elasticsearch.repository;

import com.example.wx.elasticsearch.entity.UserEs;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEsRepository extends ElasticsearchRepository<UserEs, String> {
}
