package com.example.wx.mapper;

import com.example.wx.pojo.Usertest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
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
public interface UsertestMapper extends BaseMapper<Usertest> {

    @Select("SELECT * FROM usertest WHERE user_id = #{userId} AND num < #{num} LIMIT 1")
    Usertest selectByUserId(String userId, int num);

    @Select("SELECT * FROM usertest WHERE user_id = #{userId} ORDER BY test_time DESC")
    List<Usertest> getTestByUserId(String userId);

    @Select("SELECT DATE_FORMAT(end_time, '%Y年%m月%d日') AS end_date " +
            "FROM usertest " +
            "WHERE user_id = #{userId} AND end_time IS NOT NULL " +
            "ORDER BY end_time DESC LIMIT 1")
    String getLatestTestDate(String userId);

    @Select("SELECT * FROM usertest WHERE user_id = #{userId} AND end_time IS NOT NULL ORDER BY end_time DESC LIMIT 1")
    Usertest getLatestTestByUserId(String userId);

    /**
     * 根据 userId 查询 usertest 表中的记录条数
     *
     * @param userId 用户ID
     * @return 记录条数
     */
    @Select("SELECT COUNT(*) FROM usertest WHERE user_id = #{userId}")
    int getTimesByUserId(String userId);

    /**
     * 根据 userId 查询 usertest 表中所有 avg_score 的平均值
     *
     * @param userId 用户ID
     * @return avg_score 的平均值
     */
    @Select("SELECT COALESCE(AVG(avg_score), 0.0) FROM usertest WHERE user_id = #{userId} AND end_time IS NOT NULL")
    Double getScoreByUserId(String userId);

    // 查询某一天的测试次数
    @Select("SELECT COUNT(*) FROM usertest WHERE DATE(test_time) = DATE(#{date})")
    int getTimesByTime(Date date);

    // 查询昨天的测试次数
    @Select("SELECT COUNT(*) FROM usertest WHERE DATE(test_time) = DATE_SUB(DATE(#{date}), INTERVAL 1 DAY)")
    int getTimesYesterday(Date date);

    // 查询总测试次数
    @Select("SELECT COUNT(*) FROM usertest")
    int getAllTimes();



}
