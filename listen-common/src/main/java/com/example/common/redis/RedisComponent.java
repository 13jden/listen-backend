package com.example.common.redis;

import com.example.common.constants.Constants;
import com.example.common.dto.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    public String saveCheckCode(String code){
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey,code, Constants.REDIS_KEY_EXPIRES_TIME);
        return checkCodeKey;
    }

    public String getCheckCode(String checkCodeKey){
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }
    public TokenUserInfoDto getTokenInfo(String token){
        return  (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_TOKEN_WX+token);
    }

    public String getAdminToken(String token){
        return  (String)redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN  + token);
    }


    public String saveTokenInfo(TokenUserInfoDto tokenUserInfoDto){
        String token = UUID.randomUUID().toString();
        tokenUserInfoDto.setExpireAt(System.currentTimeMillis()+ Constants.REDIS_KEY_EXPIRES_ONE_DAY*7);
        tokenUserInfoDto.setToken(token);
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WX+token,tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY*7);
        return token;
    }
    public String saveAdminTokenInfo(String account){
        String token = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_ADMIN+token,account,Constants.REDIS_KEY_EXPIRES_TIME*60);
        return token;
    }


    public void cleanToken(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WX+token);
    }
    public void cleanCheckCode(String checkCodeKey){
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }
    public void cleanAdminToken(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_ADMIN+token);
    }

    public String saveTestInfo(String testId, List<UserDetailInfo> userDetailInfoList){
        redisUtils.setex(Constants.REDIS_KEY_TESTDETAIL+testId,userDetailInfoList,Constants.REDIS_KEY_EXPIRES_TIME*60);
        System.out.println("保存用户信息成功");
        return "保存成功";
    }

    public List<UserDetailInfo> getTestInfo(String testId){
        return (List<UserDetailInfo>)redisUtils.get(Constants.REDIS_KEY_TESTDETAIL+testId);
    }

    public int getAudioTimes(String id) {
        if(redisUtils.get(Constants.REDIS_KEY_AUDIO_TIMES+id)==null){
            return -1;
        }
        return (int) redisUtils.get(Constants.REDIS_KEY_AUDIO_TIMES+id);
    }

    public AudioDto getAudio(String id) {
        if(redisUtils.get(Constants.REDIS_KEY_AUDIO_SCORE+id)==null){
            return null;
        }
        return (AudioDto) redisUtils.get(Constants.REDIS_KEY_AUDIO_SCORE+id);
    }

    public void setAudio(String id, AudioDto audioDto) {
        redisUtils.setex(Constants.REDIS_KEY_AUDIO_SCORE+id,audioDto,Constants.REDIS_KEY_EXPIRES_ONE_DAY);
    }

    public void deleteAudio(String id) {
        // 检查 Redis 中是否存在该音频数据
        if (redisUtils.get(Constants.REDIS_KEY_AUDIO_SCORE + id) != null) {
            // 如果存在，则删除该键值对
            redisUtils.delete(Constants.REDIS_KEY_AUDIO_SCORE + id);
        }
    }

    public void saveDataInfo(IndexDataVO indexData) {
        redisUtils.setex(Constants.REDIS_KEY_PRE+"IndexData:",indexData,Constants.REDIS_KEY_EXPIRES_ONE_DAY);
    }
    public IndexDataVO getDataInfo() {
        return (IndexDataVO) redisUtils.get(Constants.REDIS_KEY_PRE+"IndexData:");
    }

}
