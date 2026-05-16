package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.VehicleTempZone;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆温区配置Mapper接口
 */
@Mapper
public interface VehicleTempZoneMapper extends BaseMapper<VehicleTempZone> {
}
