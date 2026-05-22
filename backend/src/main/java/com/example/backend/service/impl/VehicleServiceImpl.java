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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
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
 */
@Slf4j
@Service
public class VehicleServiceImpl implements VehicleService {

    @Resource
    private VehicleMapper vehicleMapper;

    @Resource
    private VehicleTempZoneMapper vehicleTempZoneMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
    //缓存穿透实现
    @Override
    public VehicleDTO getVehicleById(Long id) {
        String cacheKey = RedisConstants.getVehicleCacheKey(id);

        // 1. 先从缓存获取，使用缓存空对象实现，防止缓存穿透，防止高并发打穿数据库
        //先判断当前是否为空
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            if((cachedObj instanceof VehicleDTO) && ((VehicleDTO) cachedObj).getId()==null){
                log.debug("从缓存命中【空对象】，防止缓存穿透，ID: {}",id);
                throw new BusinessException("车辆不存在");
            }
            log.debug("从缓存获取车辆详情，ID: {}", id);
            return (VehicleDTO) cachedObj;
        }

        // 2. 缓存未命中，查询数据库
        log.debug("缓存未命中，从数据库查询车辆详情，ID: {}", id);

        // 查询车辆信息
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            //新增空对象,60s
            VehicleDTO nullDTO = new VehicleDTO();
            redisTemplate.opsForValue().set(
                    cacheKey,
                    nullDTO,
                    RedisConstants.CACHE_NULL_EXPIRE_TIME,
                    TimeUnit.SECONDS
            );
            log.warn("数据库车辆不存在，使用缓存空对象防止穿透，ID:{}",id);
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

        // 3. 写入缓存（30分钟±5分钟，防止缓存雪崩）
        redisTemplate.opsForValue().set(
            cacheKey,
            vehicleDTO,
            RedisConstants.getVehicleCacheExpireTime(),
            TimeUnit.SECONDS
        );
        log.debug("车辆详情已缓存，ID: {}", id);

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

        // 插入车辆信息（MyBatis-Plus会自动填充ID到vehicleDTO）
        int result = vehicleMapper.insert(vehicleDTO);
        if (result == 0) {
            throw new BusinessException("新增车辆失败");
        }

        // 插入温区配置（使用vehicleDTO的ID）
        List<VehicleTempZone> tempZones = vehicleDTO.getTempZones();
        if (tempZones != null && !tempZones.isEmpty()) {
            for (VehicleTempZone tempZone : tempZones) {
                tempZone.setId(null);
                tempZone.setVehicleId(vehicleDTO.getId());  // 使用vehicleDTO的ID
                vehicleTempZoneMapper.insert(tempZone);
            }
        }

        // 新增车辆后，清理监控缓存（事务提交后执行）
        cleanCacheAfterCommit(null);

        log.debug("新增车辆成功，车牌号: {}, ID: {}", vehicleDTO.getPlateNo(), vehicleDTO.getId());
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

        cleanCacheAfterCommit(null);
        log.debug("更新车辆数据成功，已注册事务提交后清理缓存的回调，ID: {}", vehicleDTO.getId());
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

        // 清除缓存
        String cacheKey = RedisConstants.getVehicleCacheKey(id);
        redisTemplate.delete(cacheKey);
        redisTemplate.delete(RedisConstants.VEHICLE_MONITOR_KEY);
        log.debug("删除车辆成功，已清除缓存，ID: {}", id);
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

        // 【修复：双写不一致】事务提交后删缓存
        cleanCacheAfterCommit(null);
        log.debug("更新车辆状态成功，已清除缓存，ID: {}, 状态: {}", id, status);
    }

    @Override
    public List<VehicleMonitorDTO> getVehicleMonitorData() {
        String cacheKey = RedisConstants.VEHICLE_MONITOR_KEY;

        // 1. 先从缓存获取
        List<VehicleMonitorDTO> cachedList = (List<VehicleMonitorDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedList != null) {
            log.debug("从缓存获取车辆监控数据，数量: {}", cachedList.size());
            return cachedList;
        }

        // 2. 缓存未命中，查询数据库并生成监控数据
        log.debug("缓存未命中，生成车辆监控数据");

        // 查询所有车辆
        List<Vehicle> vehicles = vehicleMapper.selectList(null);
        List<VehicleMonitorDTO> monitorList = new ArrayList<>();
        Random random = new Random();

        // 北京市区的一些坐标点（用于模拟车辆位置）
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

            // 基本信息
            dto.setId(vehicle.getId());
            dto.setPlateNo(vehicle.getPlateNo());
            dto.setStatus(vehicle.getStatus());
            dto.setDriverName(vehicle.getDriverName());
            dto.setDriverPhone(vehicle.getDriverPhone());

            // 模拟实时数据
            if ("busy".equals(vehicle.getStatus())) {
                // 在途车辆：模拟冷链温度和速度
                BigDecimal targetTemp = new BigDecimal(random.nextInt(30) - 20); // -20到10度
                BigDecimal fluctuation = new BigDecimal(random.nextDouble() * 0.5 - 0.25); // ±0.25度波动
                dto.setTargetTemp(targetTemp);
                dto.setCurrentTemp(targetTemp.add(fluctuation).setScale(1, BigDecimal.ROUND_HALF_UP));
                dto.setSpeed(30 + random.nextInt(30)); // 30-60 km/h
                dto.setRuntime(random.nextInt(300)); // 0-300分钟
            } else if ("idle".equals(vehicle.getStatus())) {
                // 空闲车辆：常温，速度为0
                dto.setTargetTemp(new BigDecimal("15"));
                dto.setCurrentTemp(new BigDecimal("15.0"));
                dto.setSpeed(0);
                dto.setRuntime(0);
            } else {
                // 维修车辆：常温，速度为0
                dto.setTargetTemp(new BigDecimal("20"));
                dto.setCurrentTemp(new BigDecimal("20.0"));
                dto.setSpeed(0);
                dto.setRuntime(0);
            }

            // 模拟位置（循环使用预设坐标）
            BigDecimal[] location = locations[i % locations.length];
            dto.setLongitude(location[0]);
            dto.setLatitude(location[1]);

            monitorList.add(dto);
        }

        // 3. 写入缓存（30秒过期）
        if (!monitorList.isEmpty()) {
            redisTemplate.opsForValue().set(
                cacheKey,
                monitorList,
                RedisConstants.VEHICLE_MONITOR_EXPIRE_TIME,
                TimeUnit.SECONDS
            );
            log.debug("车辆监控数据已缓存，数量: {}", monitorList.size());
        }

        return monitorList;
    }
    /**
     * 辅助工具方法：确保在 Spring 事务提交(Commit)成功后，才异步执行 Redis 缓存删除。
     * 解决”事务未提交、缓存已被删，导致并发读取旧数据回填 Redis”的经典不一致隐患。
     *
     * @param id 车辆ID，若为 null 则仅清理大屏监控全局缓存
     */
    private void cleanCacheAfterCommit(Long id) {
        // 检查当前线程是否存在活动的 Spring 事务
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 执行到这里，意味着数据库已经落盘，可以安全清除缓存了
                    if (id != null) {
                        String cacheKey = RedisConstants.getVehicleCacheKey(id);
                        redisTemplate.delete(cacheKey);
                    }
                    // 清理车辆监控缓存
                    redisTemplate.delete(RedisConstants.VEHICLE_MONITOR_KEY);
                    // 清理Dashboard缓存（车辆数量或状态变化）
                    redisTemplate.delete(RedisConstants.DASHBOARD_STATS_KEY);
                    log.debug("事务提交成功，Redis 缓存延期清理完成。车辆ID: {}", id);
                }
            });
        } else {
            // 如果当前方法没有事务上下文中运行，则直接同步清除缓存
            if (id != null) {
                redisTemplate.delete(RedisConstants.getVehicleCacheKey(id));
            }
            redisTemplate.delete(RedisConstants.VEHICLE_MONITOR_KEY);
            redisTemplate.delete(RedisConstants.DASHBOARD_STATS_KEY);
        }
    }
}
