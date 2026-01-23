package com.example.common.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class StringTools {

    public static boolean isEmpty(String s){
        if(s==null || "".equals(s)||"null".equals(s)||"\u0000".equals(s)){
            return true;
        }else if("".equals(s.trim())){
            return true;
        }
        return false;
    }

    public static final String getRandomBumber(Integer count){
        return RandomStringUtils.random(count,true,true);
    }

    public static final String encodeByMd5(String originString){
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }
}
