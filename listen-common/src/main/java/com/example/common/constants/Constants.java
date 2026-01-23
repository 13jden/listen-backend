package com.example.common.constants;

public class Constants {

    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{6,18}$";

    public static final Integer REDIS_KEY_EXPIRES_TIME = 60000;
    public static final Integer LENGTH_15 = 15;

    public static String REDIS_KEY_PRE = "Listen:";

    public static String REDIS_KEY_TESTDETAIL = REDIS_KEY_PRE + "testDetail:";

    public static String REDIS_KEY_CHECK_CODE =REDIS_KEY_PRE + "checkcode:";

    public static final Integer LENGTH_10 = 10;

    public static final Integer REDIS_KEY_EXPIRES_ONE_DAY = 60000*60*24;

    public static final String REDIS_KEY_TOKEN_WX = REDIS_KEY_PRE + "token:wx:";

    public static final String TOKEN_WX =  "token";

    public static final String TOKEN_ADMIN =  "adminToken";

    public static final String REDIS_KEY_AUDIO_TIMES =REDIS_KEY_PRE + "audio:times:";

    public static final String REDIS_KEY_AUDIO_SCORE =REDIS_KEY_PRE + "audio:score:";

    public static final String REDIS_KEY_TOKEN_ADMIN = REDIS_KEY_PRE + "token:admin";

}


