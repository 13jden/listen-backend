package com.example.wx.service;

import com.example.wx.pojo.Hospital;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
public interface HospitalService extends IService<Hospital> {

    List<Hospital> getlist();


    Boolean addHospital(Hospital hospital);

    boolean deleteHospital(int hospitalId);
}
