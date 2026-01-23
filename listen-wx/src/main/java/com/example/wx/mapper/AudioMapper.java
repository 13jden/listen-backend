package com.example.wx.mapper;

import com.example.wx.pojo.Audio;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author dzk
 * @since 2025-02-04
 */
@Mapper
public interface AudioMapper extends BaseMapper<Audio> {
    @Select("SELECT * FROM audio ORDER BY RAND() LIMIT #{num}")
    List<Audio> getRandomAudio(int num);

    @Select("SELECT COUNT(*) FROM testdetail WHERE audio_id = #{id}")
    int getTestTimes(String id);

    @Select("SELECT ROUND(COALESCE(AVG(score), 0.0), 1) FROM testdetail WHERE audio_id = #{id} AND test_time IS NOT NULL")
    Double getTestScore(String id);
}
