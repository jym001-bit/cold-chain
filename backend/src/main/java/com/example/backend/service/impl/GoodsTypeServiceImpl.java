package com.example.backend.service.impl;

import com.example.backend.entity.GoodsType;
import com.example.backend.mapper.GoodsTypeMapper;
import com.example.backend.service.GoodsTypeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 货物类型服务实现
 */
@Service
public class GoodsTypeServiceImpl implements GoodsTypeService {

    @Resource
    private GoodsTypeMapper goodsTypeMapper;

    @Override
    public List<GoodsType> getAllGoodsTypes() {
        return goodsTypeMapper.selectList(null);
    }
}
