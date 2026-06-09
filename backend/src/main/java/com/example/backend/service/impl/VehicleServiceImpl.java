package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.constant.RedisConstants;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.VehicleDTO;
import com.example.backend.dto.VehicleMonitorDTO;
import com.example.backend.entity.Vehicle;
import com.example.backend.entity.VehicleTempZone;
import com.example.backend.mapper.VehicleMapper;
import com.example.backend.mapper.VehicleTempZoneMapper;
import com.example.backend.service.VehicleService;
import com.example.backend.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 车辆服务实现类
 *
 * <p>缓存策略：
 * <ul>
 *   <li>车辆详情：Redis 缓存（30min±5min 随机），穿不透防护 + 空值缓存 60s</li>
 *   <li>监控列表：Redis 缓存（30s），互斥锁防击穿</li>
 *   <li>写操作：事务提交后批量清理相关缓存，防止双写不一致</li>
 * </ul>
 */
@Slf4j
@Service
public class VehicleServiceImpl implements VehicleService {

    @Resource
    private VehicleMapper vehicleMapper;

    @Resource
    private VehicleTempZoneMapper vehicleTempZoneMapper;

    @Resource
    private CacheUtil cacheUtil;

    @Override
    public Page<Vehicle> getVehicleList(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<Vehicle> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(Vehicle::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Vehicle::getPlateNo, keyword)
                    .or()
                    .like(Vehicle::getDriverName, keyword));
        }
        wrapper.orderByDesc(Vehicle::getCreateTime);
        return vehicleMapper.selectPage(page, wrapper);
    }

    @Override
    public VehicleDTO getVehicleById(Long id) {
        VehicleDTO result = cacheUtil.getWithPassThrough(
                RedisConstants.getVehicleCacheKey(id),
                RedisConstants.getVehicleCacheExpireTime(),
                () -> {
                    Vehicle vehicle = vehicleMapper.selectById(id);
                    if (vehicle == null) return null;

                    // 查询温区配置
                    LambdaQueryWrapper<VehicleTempZone> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(VehicleTempZone::getVehicleId, id);
                    wrapper.orderByAsc(VehicleTempZone::getZoneNo);
                    List<VehicleTempZone> tempZones = vehicleTempZoneMapper.selectList(wrapper);

                    VehicleDTO dto = new VehicleDTO();
                    BeanUtils.copyProperties(vehicle, dto);
                    dto.setTempZones(tempZones);
                    return dto;
                }
        );

        if (result == null) {
            throw new BusinessException("车辆不存在");
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addVehicle(VehicleDTO vehicleDTO) {
        // 检查车牌号是否已存在
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vehicle::getPlateNo, vehicleDTO.getPlateNo());
        if (vehicleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("车牌号已存在");
        }

        if (!StringUtils.hasText(vehicleDTO.getStatus())) {
            vehicleDTO.setStatus("idle");
        }

        int result = vehicleMapper.insert(vehicleDTO);
        if (result == 0) {
            throw new BusinessException("新增车辆失败");
        }

        // 插入温区配置
        List<VehicleTempZone> tempZones = vehicleDTO.getTempZones();
        if (tempZones != null && !tempZones.isEmpty()) {
            for (VehicleTempZone tempZone : tempZones) {
                tempZone.setId(null);
                tempZone.setVehicleId(vehicleDTO.getId());
                vehicleTempZoneMapper.insert(tempZone);
            }
        }

        cleanCacheAfterCommit(null);
        log.debug("新增车辆成功，车牌号: {}, ID: {}", vehicleDTO.getPlateNo(), vehicleDTO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVehicle(VehicleDTO vehicleDTO) {
        Vehicle existVehicle = vehicleMapper.selectById(vehicleDTO.getId());
        if (existVehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        // 检查车牌号是否重复（排除自己）
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vehicle::getPlateNo, vehicleDTO.getPlateNo());
        wrapper.ne(Vehicle::getId, vehicleDTO.getId());
        if (vehicleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("车牌号已存在");
        }

        int result = vehicleMapper.updateById(vehicleDTO);
        if (result == 0) {
            throw new BusinessException("更新车辆失败");
        }

        // 更新温区配置（先删后插）
        List<VehicleTempZone> tempZones = vehicleDTO.getTempZones();
        if (tempZones != null && !tempZones.isEmpty()) {
            LambdaQueryWrapper<VehicleTempZone> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(VehicleTempZone::getVehicleId, vehicleDTO.getId());
            vehicleTempZoneMapper.delete(deleteWrapper);

            for (VehicleTempZone tempZone : tempZones) {
                tempZone.setId(null);
                tempZone.setVehicleId(vehicleDTO.getId());
                vehicleTempZoneMapper.insert(tempZone);
            }
        }

        cleanCacheAfterCommit(null);
        log.debug("更新车辆成功，ID: {}", vehicleDTO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        if ("busy".equals(vehicle.getStatus())) {
            throw new BusinessException("在途的车辆不能删除");
        }

        // 删除温区配置
        LambdaQueryWrapper<VehicleTempZone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleTempZone::getVehicleId, id);
        vehicleTempZoneMapper.delete(wrapper);

        int result = vehicleMapper.deleteById(id);
        if (result == 0) {
            throw new BusinessException("删除车辆失败");
        }

        // 直接清理缓存（删除操作不需要等事务提交）
        cacheUtil.delete(
                RedisConstants.getVehicleCacheKey(id),
                RedisConstants.VEHICLE_MONITOR_KEY,
                RedisConstants.DASHBOARD_STATS_KEY
        );
        log.debug("删除车辆成功，已清除缓存，ID: {}", id);
    }

    @Override
    public void updateVehicleStatus(Long id, String status) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        if (!status.equals("idle") && !status.equals("busy") && !status.equals("maintenance")) {
            throw new BusinessException("状态不合法");
        }

        vehicle.setStatus(status);
        int result = vehicleMapper.updateById(vehicle);
        if (result == 0) {
            throw new BusinessException("更新状态失败");
        }

        cleanCacheAfterCommit(null);
        log.debug("更新车辆状态成功，ID: {}, 状态: {}", id, status);
    }

    @Override
    public List<VehicleMonitorDTO> getVehicleMonitorData() {
        return cacheUtil.getWithMutex(
                RedisConstants.VEHICLE_MONITOR_KEY,
                RedisConstants.VEHICLE_MONITOR_EXPIRE_TIME,
                () -> buildMonitorData()
        );
    }

    /**
     * 构建车辆监控数据（数据库查询 + 模拟实时数据组装）
     */
    private List<VehicleMonitorDTO> buildMonitorData() {
        List<Vehicle> vehicles = vehicleMapper.selectList(null);
        List<VehicleMonitorDTO> monitorList = new ArrayList<>();
        Random random = new Random();

        BigDecimal[][] locations = {
                {new BigDecimal("116.397428"), new BigDecimal("39.90923")},   // 天安门
                {new BigDecimal("116.407526"), new BigDecimal("39.904030")},  // 王府井
                {new BigDecimal("116.391365"), new BigDecimal("39.906901")},  // 故宫
                {new BigDecimal("116.368904"), new BigDecimal("39.913423")},  // 西单
                {new BigDecimal("116.434446"), new BigDecimal("39.921489")},  // 三里屯
                {new BigDecimal("116.481488"), new BigDecimal("39.989545")},  // 望京
                {new BigDecimal("116.296203"), new BigDecimal("39.906217")},  // 五棵松
                {new BigDecimal("116.355560"), new BigDecimal("39.874557")}   // 丰台
        };

        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            VehicleMonitorDTO dto = new VehicleMonitorDTO();

            dto.setId(vehicle.getId());
            dto.setPlateNo(vehicle.getPlateNo());
            dto.setStatus(vehicle.getStatus());
            dto.setDriverName(vehicle.getDriverName());
            dto.setDriverPhone(vehicle.getDriverPhone());

            if ("busy".equals(vehicle.getStatus())) {
                BigDecimal targetTemp = new BigDecimal(random.nextInt(30) - 20);
                BigDecimal fluctuation = new BigDecimal(random.nextDouble() * 0.5 - 0.25);
                dto.setTargetTemp(targetTemp);
                dto.setCurrentTemp(targetTemp.add(fluctuation).setScale(1, BigDecimal.ROUND_HALF_UP));
                dto.setSpeed(30 + random.nextInt(30));
                dto.setRuntime(random.nextInt(300));
            } else if ("idle".equals(vehicle.getStatus())) {
                dto.setTargetTemp(new BigDecimal("15"));
                dto.setCurrentTemp(new BigDecimal("15.0"));
                dto.setSpeed(0);
                dto.setRuntime(0);
            } else {
                dto.setTargetTemp(new BigDecimal("20"));
                dto.setCurrentTemp(new BigDecimal("20.0"));
                dto.setSpeed(0);
                dto.setRuntime(0);
            }

            BigDecimal[] location = locations[i % locations.length];
            dto.setLongitude(location[0]);
            dto.setLatitude(location[1]);

            monitorList.add(dto);
        }

        return monitorList;
    }

    /**
     * 事务提交后清理缓存。
     *
     * <p>保证数据库已落盘才删缓存，防止并发线程读到旧数据回填。
     * 若不在事务上下文中则直接同步删除。
     *
     * @param id 车辆ID，为 null 时仅清理全局缓存
     */
    private void cleanCacheAfterCommit(Long id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidateCaches(id);
                }
            });
        } else {
            invalidateCaches(id);
        }
    }

    private void invalidateCaches(Long id) {
        if (id != null) {
            cacheUtil.delete(RedisConstants.getVehicleCacheKey(id));
        }
        cacheUtil.delete(RedisConstants.VEHICLE_MONITOR_KEY, RedisConstants.DASHBOARD_STATS_KEY);
        log.debug("缓存清理完成，车辆ID: {}", id);
    }
}
