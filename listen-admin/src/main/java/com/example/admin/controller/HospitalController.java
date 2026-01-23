package com.example.admin.controller;


import com.example.common.common.Result;
import com.example.wx.pojo.Hospital;
import com.example.wx.service.HospitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author dzk
 * @since 2025-01-24
 */
@RestController
@RequestMapping("/hospital")
public class HospitalController {
    @Autowired
    HospitalService hospitalService;

    @RequestMapping("list")
    public Result gethospital(){
        return Result.success(hospitalService.getlist());
    }


    @RequestMapping("add")
    public Result addHospital(@RequestParam String hospitalName){
        Hospital hospital = new Hospital();
        hospital.setName(hospitalName);
        if(hospitalService.addHospital(hospital))
            return Result.success("添加成功");
        else
            return Result.error("添加失败");
    }

    @RequestMapping("delete")
    public Result deleteHospital(@RequestParam int hospitalId){
        if(hospitalService.deleteHospital(hospitalId))
            return Result.success("删除成功");
        else
            return Result.error("删除失败");
    }

    @RequestMapping("update")
    public Result updateHospital(@RequestBody Hospital hospital){
        System.out.println(hospital.getId());
        if(hospitalService.updateById(hospital))
            return Result.success("修改成功");
        else
            return Result.error("修改失败");
    }

}

