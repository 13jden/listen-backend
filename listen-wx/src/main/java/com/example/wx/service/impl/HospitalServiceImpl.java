package com.example.wx.service.impl;

import com.example.wx.pojo.Hospital;
import com.example.wx.mapper.HospitalMapper;
import com.example.wx.service.HospitalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@Service
public class HospitalServiceImpl extends ServiceImpl<HospitalMapper, Hospital> implements HospitalService {
    @Autowired
    private HospitalMapper hospitalMapper;

    @Override
    public List<Hospital> getlist() {
        return hospitalMapper.getAll();
    }

    @Override
    public Boolean addHospital(Hospital hospital) {
        return hospitalMapper.insert(hospital)>0;
    }

    @Override
    public boolean deleteHospital(int hospitalId) {
        return hospitalMapper.deleteById(hospitalId)>0;
    }
}
