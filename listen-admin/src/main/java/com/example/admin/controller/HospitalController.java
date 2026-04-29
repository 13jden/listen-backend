package com.example.admin.controller;


import com.example.common.common.Result;
import com.example.wx.pojo.Hospital;
import com.example.wx.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "医院管理接口")
@RestController
@RequestMapping("/hospital")
public class HospitalController {
    @Autowired
    HospitalService hospitalService;

    @Operation(summary = "获取医院列表")
    @GetMapping("list")
    public Result gethospital() {
        return Result.success(hospitalService.getlist());
    }

    @Operation(summary = "添加医院")
    @PostMapping("add")
    public Result addHospital(@Parameter(description = "医院名称") @RequestParam String hospitalName) {
        Hospital hospital = new Hospital();
        hospital.setName(hospitalName);
        if (hospitalService.addHospital(hospital))
            return Result.success("添加成功");
        else
            return Result.error("添加失败");
    }

    @Operation(summary = "删除医院")
    @DeleteMapping("delete")
    public Result deleteHospital(@Parameter(description = "医院ID") @RequestParam int hospitalId) {
        if (hospitalService.deleteHospital(hospitalId))
            return Result.success("删除成功");
        else
            return Result.error("删除失败");
    }

    @Operation(summary = "更新医院信息")
    @PostMapping("update")
    public Result updateHospital(@Parameter(description = "医院信息") @RequestBody Hospital hospital) {
        System.out.println(hospital.getId());
        if (hospitalService.updateById(hospital))
            return Result.success("修改成功");
        else
            return Result.error("修改失败");
    }

}

