package com.example.wx.mapper;

import com.example.wx.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM user WHERE open_id = #{openid} ")
    User findByOpenId(String openid);

    // 查询某一天的新增用户数
    @Select("SELECT COUNT(*) FROM user WHERE DATE(register_time) = DATE(#{date})")
    int getUserNum(Date date);

    // 查询昨天的用户数
    @Select("SELECT COUNT(*) FROM user WHERE DATE(register_time) = DATE_SUB(DATE(#{date}), INTERVAL 1 DAY)")
    int getUserNumYesterday(Date date);

    // 查询用户总数
    @Select("SELECT COUNT(*) FROM user")
    int getAllNum();

}
