package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TokenUserInfoDto implements Serializable {

        private String userId;

        private String name;

        private String openId;

        private String medicalId;

        private String hospital;

        private Long expireAt;

        private String token;

}
