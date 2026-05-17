package com.example.backend.service;

import com.example.backend.entity.GoodsType;

import java.util.List;

/**
 * 货物类型服务接口
 */
public interface GoodsTypeService {

    /**
     * 获取所有货物类型
     */
    List<GoodsType> getAllGoodsTypes();
}
