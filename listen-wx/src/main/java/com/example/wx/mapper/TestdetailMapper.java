package com.example.wx.mapper;

import com.example.wx.pojo.Testdetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author dzk
 * @since 2025-02-06
 */
@Mapper
public interface TestdetailMapper extends BaseMapper<Testdetail> {

    // 插入数据方法，使用反引号包裹 `index` 和其他字段，避免与保留字冲突
    @Insert("INSERT INTO testdetail (id, test_id, audio_id, user_audio_path, `index`, score, user_content, test_time) " +
            "VALUES (#{id}, #{testId}, #{audioId}, #{userAudioPath}, #{index}, #{score}, #{userContent}, #{testTime})")
    void insertTestDetail(Testdetail testDetail);

    // 根据 testId 查询 Testdetail 列表，避免冲突，反引号包裹字段
    @Select("SELECT id, test_id, audio_id, user_audio_path, `index`, score, user_content, test_time " +
            "FROM testdetail WHERE test_id = #{testId} " +
            "ORDER BY `index` ASC")
    List<Testdetail> selectList(String testId);

    @Select("SELECT id, test_id, audio_id, user_audio_path, `index`, score, user_content, test_time " +
            "FROM testdetail WHERE id = #{testDetailId}")
    Testdetail selectById(String testDetailId);

    @Update("UPDATE testdetail SET test_id = #{testId}, audio_id = #{audioId}, user_audio_path = #{userAudioPath}, " +
            "`index` = #{index}, score = #{score}, user_content = #{userContent}, test_time = #{testTime} " +
            "WHERE id = #{id}")
    int updateById(Testdetail testDetail);

    @Select("SELECT * FROM testdetail WHERE test_id = #{testId}"+
            "ORDER BY `index` ASC")
    List<Testdetail> selectByTestId(String testId);
}
