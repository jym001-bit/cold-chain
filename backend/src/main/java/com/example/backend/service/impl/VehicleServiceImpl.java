package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.VehicleDTO;
import com.example.backend.entity.Vehicle;
import com.example.backend.entity.VehicleTempZone;
import com.example.backend.mapper.VehicleMapper;
import com.example.backend.mapper.VehicleTempZoneMapper;
import com.example.backend.service.VehicleService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 车辆服务实现类
 */
@Service
public class VehicleServiceImpl implements VehicleService {

    @Resource
    private VehicleMapper vehicleMapper;

    @Resource
    private VehicleTempZoneMapper vehicleTempZoneMapper;

    @Override
    public Page<Vehicle> getVehicleList(Integer pageNum, Integer pageSize, String status, String keyword) {
        // 创建分页对象
        Page<Vehicle> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();

        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(Vehicle::getStatus, status);
        }

        // 关键词搜索（车牌号、司机姓名）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Vehicle::getPlateNo, keyword)
                    .or()
                    .like(Vehicle::getDriverName, keyword)
            );
        }

        // 按创建时间倒序
        wrapper.orderByDesc(Vehicle::getCreateTime);

        // 执行查询
        return vehicleMapper.selectPage(page, wrapper);
    }

    @Override
    public VehicleDTO getVehicleById(Long id) {
        // 查询车辆信息
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        // 查询温区配置
        LambdaQueryWrapper<VehicleTempZone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleTempZone::getVehicleId, id);
        wrapper.orderByAsc(VehicleTempZone::getZoneNo);
        List<VehicleTempZone> tempZones = vehicleTempZoneMapper.selectList(wrapper);

        // 组装DTO
        VehicleDTO vehicleDTO = new VehicleDTO();
        BeanUtils.copyProperties(vehicle, vehicleDTO);
        vehicleDTO.setTempZones(tempZones);

        return vehicleDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addVehicle(VehicleDTO vehicleDTO) {
        // 检查车牌号是否已存在
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vehicle::getPlateNo, vehicleDTO.getPlateNo());
        Long count = vehicleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("车牌号已存在");
        }

        // 设置默认状态
        if (!StringUtils.hasText(vehicleDTO.getStatus())) {
            vehicleDTO.setStatus("idle");
        }

        // 插入车辆信息
        int result = vehicleMapper.insert(vehicleDTO);
        if (result == 0) {
            throw new BusinessException("新增车辆失败");
        }

        // 插入温区配置
        List<VehicleTempZone> tempZones = vehicleDTO.getTempZones();
        if (tempZones != null && !tempZones.isEmpty()) {
            for (VehicleTempZone tempZone : tempZones) {
                tempZone.setVehicleId(vehicleDTO.getId());
                vehicleTempZoneMapper.insert(tempZone);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVehicle(VehicleDTO vehicleDTO) {
        // 检查车辆是否存在
        Vehicle existVehicle = vehicleMapper.selectById(vehicleDTO.getId());
        if (existVehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        // 检查车牌号是否重复（排除自己）
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vehicle::getPlateNo, vehicleDTO.getPlateNo());
        wrapper.ne(Vehicle::getId, vehicleDTO.getId());
        Long count = vehicleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("车牌号已存在");
        }

        // 更新车辆信息
        int result = vehicleMapper.updateById(vehicleDTO);
        if (result == 0) {
            throw new BusinessException("更新车辆失败");
        }

        // 更新温区配置（先删除旧的，再插入新的）
        List<VehicleTempZone> tempZones = vehicleDTO.getTempZones();
        if (tempZones != null && !tempZones.isEmpty()) {
            // 删除旧的温区配置
            LambdaQueryWrapper<VehicleTempZone> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(VehicleTempZone::getVehicleId, vehicleDTO.getId());
            vehicleTempZoneMapper.delete(deleteWrapper);

            // 插入新的温区配置
            for (VehicleTempZone tempZone : tempZones) {
                tempZone.setId(null); // 清空ID，让数据库自动生成
                tempZone.setVehicleId(vehicleDTO.getId());
                vehicleTempZoneMapper.insert(tempZone);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVehicle(Long id) {
        // 检查车辆是否存在
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        // 检查车辆状态（在途的车辆不能删除）
        if ("busy".equals(vehicle.getStatus())) {
            throw new BusinessException("在途的车辆不能删除");
        }

        // 删除温区配置（外键级联删除会自动删除，这里手动删除更安全）
        LambdaQueryWrapper<VehicleTempZone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleTempZone::getVehicleId, id);
        vehicleTempZoneMapper.delete(wrapper);

        // 删除车辆
        int result = vehicleMapper.deleteById(id);
        if (result == 0) {
            throw new BusinessException("删除车辆失败");
        }
    }

    @Override
    public void updateVehicleStatus(Long id, String status) {
        // 检查车辆是否存在
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        // 检查状态是否合法
        if (!status.equals("idle") && !status.equals("busy") && !status.equals("maintenance")) {
            throw new BusinessException("状态不合法");
        }

        // 更新状态
        vehicle.setStatus(status);
        int result = vehicleMapper.updateById(vehicle);
        if (result == 0) {
            throw new BusinessException("更新状态失败");
        }
    }
}
