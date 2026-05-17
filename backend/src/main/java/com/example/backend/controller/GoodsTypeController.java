package com.example.backend.controller;

import com.example.backend.common.result.Result;
import com.example.backend.entity.GoodsType;
import com.example.backend.service.GoodsTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 货物类型控制器
 */
@RestController
@RequestMapping("/api/goods-types")
public class GoodsTypeController {

    @Resource
    private GoodsTypeService goodsTypeService;

    /**
     * 获取所有货物类型
     */
    @GetMapping
    public Result<List<GoodsType>> getAllGoodsTypes() {
        List<GoodsType> list = goodsTypeService.getAllGoodsTypes();
        return Result.success(list);
    }
}
