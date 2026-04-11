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

    /** 删除音频列表缓存（须在数据库对音频的增删改成功后调用，先 DB 再删缓存） */
    public void deleteAudio(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        redisUtils.delete(Constants.REDIS_KEY_AUDIO_SCORE + id);
    }

    /** 删除管理端「测试详情列表」缓存 */
    public void deleteTestDetailCache(String testId) {
        if (testId == null || testId.isEmpty()) {
            return;
        }
        redisUtils.delete(Constants.REDIS_KEY_TESTDETAIL + testId);
    }

    /** 删除首页统计缓存 */
    public void deleteIndexDataCache() {
        redisUtils.delete(Constants.REDIS_KEY_PRE + "IndexData:");
    }

    public void saveDataInfo(IndexDataVO indexData) {
        redisUtils.setex(Constants.REDIS_KEY_PRE+"IndexData:",indexData,Constants.REDIS_KEY_EXPIRES_ONE_DAY);
    }
    public IndexDataVO getDataInfo() {
        return (IndexDataVO) redisUtils.get(Constants.REDIS_KEY_PRE+"IndexData:");
    }

    public void lpushScoreTask(TestScoreTaskMessage message) {
        redisUtils.lpush("listen:score:queue", message, null);
    }

    public TestScoreTaskMessage rpopScoreTask() {
        Object result = redisUtils.rpop("listen:score:queue");
        if (result instanceof TestScoreTaskMessage) {
            return (TestScoreTaskMessage) result;
        }
        return null;
    }

}
