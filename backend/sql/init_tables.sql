-- =============================================
-- 冷链智能调度系统 - 数据库表结构
-- 数据库名：cold_chain
-- 创建时间：2026-05-16
-- =============================================

USE cold_chain;

-- =============================================
-- 1. sys_user（用户表）
-- =============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（加密）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    role VARCHAR(20) NOT NULL DEFAULT 'dispatcher' COMMENT '角色：admin-管理员，dispatcher-调度员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始数据（密码：123456，需要后期加密）
INSERT INTO sys_user (username, password, real_name, role) VALUES
('admin', '123456', '管理员', 'admin'),
('dispatcher1', '123456', '调度员1', 'dispatcher');

-- =============================================
-- 2. goods_type（货物类型表）
-- =============================================
DROP TABLE IF EXISTS goods_type;
CREATE TABLE goods_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '货物类型ID',
    type_name VARCHAR(50) NOT NULL UNIQUE COMMENT '类型名称',
    min_temp DECIMAL(5,2) NOT NULL COMMENT '最低温度（℃）',
    max_temp DECIMAL(5,2) NOT NULL COMMENT '最高温度（℃）',
    sensitivity INT NOT NULL COMMENT '温度敏感度：1-低，2-中，3-高，4-极高',
    description VARCHAR(200) COMMENT '描述',
    keywords VARCHAR(500) COMMENT '关键词（用于智能分类，逗号分隔）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_type_name (type_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物类型表';

-- 初始数据（8种货物类型）
INSERT INTO goods_type (type_name, min_temp, max_temp, sensitivity, description, keywords) VALUES
('海鲜', -18.00, -18.00, 3, '冷冻海鲜产品', '三文鱼,金枪鱼,虾,蟹,鱼,海鲜'),
('肉类', -18.00, -18.00, 3, '冷冻肉类产品', '猪肉,牛肉,羊肉,鸡肉,鸭肉,肉'),
('蔬菜', 0.00, 4.00, 2, '新鲜蔬菜', '白菜,萝卜,西红柿,黄瓜,蔬菜'),
('水果', 0.00, 4.00, 2, '新鲜水果', '苹果,香蕉,橙子,葡萄,水果'),
('乳制品', 0.00, 4.00, 3, '乳制品', '牛奶,酸奶,奶酪,乳制品'),
('冷冻食品', -18.00, -18.00, 2, '冷冻食品', '速冻饺子,冰淇淋,冷冻食品'),
('药品', 2.00, 8.00, 4, '需冷藏药品', '疫苗,胰岛素,药品'),
('常温货物', 15.00, 25.00, 1, '常温货物', '饮料,零食,常温');

-- =============================================
-- 3. vehicle（车辆表）
-- =============================================
DROP TABLE IF EXISTS vehicle;
CREATE TABLE vehicle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '车辆ID',
    plate_no VARCHAR(20) NOT NULL UNIQUE COMMENT '车牌号',
    vehicle_type VARCHAR(50) NOT NULL COMMENT '车型',
    max_weight DECIMAL(10,2) NOT NULL COMMENT '最大载重（kg）',
    temp_zone_count INT NOT NULL DEFAULT 1 COMMENT '温区数量：1-单温区，2-双温区，3-三温区',
    status VARCHAR(20) NOT NULL DEFAULT 'idle' COMMENT '状态：idle-空闲，busy-在途，maintenance-维修',
    driver_name VARCHAR(50) COMMENT '司机姓名',
    driver_phone VARCHAR(20) COMMENT '司机电话',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_plate_no (plate_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆表';

-- 初始数据（3辆车）
INSERT INTO vehicle (plate_no, vehicle_type, max_weight, temp_zone_count, driver_name, driver_phone) VALUES
('京A12345', '厢式货车', 5000.00, 3, '张三', '13800138001'),
('京B67890', '厢式货车', 3000.00, 2, '李四', '13800138002'),
('京C11111', '厢式货车', 2000.00, 1, '王五', '13800138003');

-- =============================================
-- 4. vehicle_temp_zone（车辆温区配置表）
-- =============================================
DROP TABLE IF EXISTS vehicle_temp_zone;
CREATE TABLE vehicle_temp_zone (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '温区配置ID',
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    zone_no INT NOT NULL COMMENT '温区编号：1,2,3',
    min_temp DECIMAL(5,2) NOT NULL COMMENT '最低温度（℃）',
    max_temp DECIMAL(5,2) NOT NULL COMMENT '最高温度（℃）',
    volume DECIMAL(10,2) NOT NULL COMMENT '容积（立方米）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id) ON DELETE CASCADE,
    UNIQUE KEY uk_vehicle_zone (vehicle_id, zone_no),
    INDEX idx_vehicle_id (vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆温区配置表';

-- 初始数据
-- 车辆1：三温区（-18℃ / 0-4℃ / 15-25℃）
INSERT INTO vehicle_temp_zone (vehicle_id, zone_no, min_temp, max_temp, volume) VALUES
(1, 1, -18.00, -18.00, 5.0),
(1, 2, 0.00, 4.00, 5.0),
(1, 3, 15.00, 25.00, 5.0);

-- 车辆2：双温区（-18℃ / 0-4℃）
INSERT INTO vehicle_temp_zone (vehicle_id, zone_no, min_temp, max_temp, volume) VALUES
(2, 1, -18.00, -18.00, 4.0),
(2, 2, 0.00, 4.00, 4.0);

-- 车辆3：单温区（-18℃）
INSERT INTO vehicle_temp_zone (vehicle_id, zone_no, min_temp, max_temp, volume) VALUES
(3, 1, -18.00, -18.00, 6.0);

-- =============================================
-- 5. dispatch_order（订单表）
-- =============================================
DROP TABLE IF EXISTS dispatch_order;
CREATE TABLE dispatch_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    goods_type_id BIGINT NOT NULL COMMENT '货物类型ID',
    goods_name VARCHAR(100) NOT NULL COMMENT '货物名称',
    weight DECIMAL(10,2) NOT NULL COMMENT '重量（kg）',
    volume DECIMAL(10,2) COMMENT '体积（立方米）',
    start_address VARCHAR(200) NOT NULL COMMENT '起点地址',
    start_lng DECIMAL(10,6) COMMENT '起点经度',
    start_lat DECIMAL(10,6) COMMENT '起点纬度',
    end_address VARCHAR(200) NOT NULL COMMENT '终点地址',
    end_lng DECIMAL(10,6) COMMENT '终点经度',
    end_lat DECIMAL(10,6) COMMENT '终点纬度',
    earliest_time DATETIME COMMENT '最早配送时间',
    latest_time DATETIME COMMENT '最晚配送时间',
    customer_name VARCHAR(50) COMMENT '客户姓名',
    customer_phone VARCHAR(20) COMMENT '客户电话',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待调度，scheduled-已调度，in_transit-执行中，completed-已完成，cancelled-已取消',
    create_user_id BIGINT COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (goods_type_id) REFERENCES goods_type(id),
    FOREIGN KEY (create_user_id) REFERENCES sys_user(id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 初始测试数据（10个订单）
INSERT INTO dispatch_order (order_no, goods_type_id, goods_name, weight, volume, start_address, end_address, customer_name, customer_phone, create_user_id) VALUES
('D20260516001', 1, '三文鱼', 100.00, 0.5, '北京市朝阳区仓库A', '北京市海淀区超市B', '张经理', '13900139001', 1),
('D20260516002', 3, '白菜', 200.00, 1.0, '北京市朝阳区仓库A', '北京市丰台区餐厅C', '李经理', '13900139002', 1),
('D20260516003', 2, '牛肉', 150.00, 0.8, '北京市朝阳区仓库A', '北京市西城区超市D', '王经理', '13900139003', 1),
('D20260516004', 4, '苹果', 120.00, 0.6, '北京市朝阳区仓库A', '北京市东城区水果店E', '赵经理', '13900139004', 1),
('D20260516005', 5, '牛奶', 80.00, 0.4, '北京市朝阳区仓库A', '北京市朝阳区便利店F', '刘经理', '13900139005', 1),
('D20260516006', 6, '速冻饺子', 180.00, 0.9, '北京市朝阳区仓库A', '北京市海淀区超市G', '陈经理', '13900139006', 1),
('D20260516007', 1, '金枪鱼', 90.00, 0.5, '北京市朝阳区仓库A', '北京市丰台区餐厅H', '杨经理', '13900139007', 1),
('D20260516008', 3, '西红柿', 110.00, 0.6, '北京市朝阳区仓库A', '北京市西城区餐厅I', '周经理', '13900139008', 1),
('D20260516009', 2, '猪肉', 160.00, 0.8, '北京市朝阳区仓库A', '北京市东城区超市J', '吴经理', '13900139009', 1),
('D20260516010', 4, '香蕉', 100.00, 0.5, '北京市朝阳区仓库A', '北京市朝阳区水果店K', '郑经理', '13900139010', 1);

-- =============================================
-- 6. dispatch_plan（调度方案表）
-- =============================================
DROP TABLE IF EXISTS dispatch_plan;
CREATE TABLE dispatch_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '方案ID',
    plan_name VARCHAR(100) NOT NULL COMMENT '方案名称',
    plan_date DATE NOT NULL COMMENT '调度日期',
    algorithm_type VARCHAR(50) NOT NULL COMMENT '算法类型：greedy-贪心，optimized-优化算法',
    total_cost DECIMAL(10,2) COMMENT '总成本（元）',
    fuel_cost DECIMAL(10,2) COMMENT '油费（元）',
    labor_cost DECIMAL(10,2) COMMENT '人工费（元）',
    damage_cost DECIMAL(10,2) COMMENT '货损费（元）',
    temp_risk DECIMAL(10,4) COMMENT '温度风险值',
    vehicle_count INT COMMENT '使用车辆数',
    order_count INT COMMENT '订单数',
    total_distance DECIMAL(10,2) COMMENT '总里程（km）',
    avg_utilization DECIMAL(5,2) COMMENT '平均车辆利用率（%）',
    compute_time INT COMMENT '计算耗时（ms）',
    status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿，confirmed-已确认，executing-执行中，completed-已完成',
    create_user_id BIGINT COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (create_user_id) REFERENCES sys_user(id),
    INDEX idx_plan_date (plan_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度方案表';

-- =============================================
-- 7. dispatch_detail（调度明细表）
-- =============================================
DROP TABLE IF EXISTS dispatch_detail;
CREATE TABLE dispatch_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    plan_id BIGINT NOT NULL COMMENT '方案ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    temp_zone_no INT NOT NULL COMMENT '温区编号',
    sequence INT NOT NULL COMMENT '配送顺序',
    estimated_arrival DATETIME COMMENT '预计到达时间',
    estimated_distance DECIMAL(10,2) COMMENT '预计距离（km）',
    estimated_cost DECIMAL(10,2) COMMENT '预计成本（元）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (plan_id) REFERENCES dispatch_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES dispatch_order(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    INDEX idx_plan_id (plan_id),
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_sequence (sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度明细表';

-- =============================================
-- 8. temperature_record（温度记录表，按年分区）
-- =============================================
DROP TABLE IF EXISTS temperature_record;
CREATE TABLE temperature_record (
                                    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
                                    temp_zone_no INT NOT NULL COMMENT '温区编号',
                                    temperature DECIMAL(5,2) NOT NULL COMMENT '温度（℃）',
                                    location VARCHAR(200) COMMENT '位置',
                                    lng DECIMAL(10,6) COMMENT '经度',
                                    lat DECIMAL(10,6) COMMENT '纬度',
                                    record_time DATETIME NOT NULL COMMENT '记录时间',
                                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (id, record_time),  -- 主键必须包含分区字段
                                    INDEX idx_vehicle_time (vehicle_id, record_time),
                                    INDEX idx_record_time (record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='温度记录表'
    PARTITION BY RANGE (YEAR(record_time)) (
        PARTITION p2026 VALUES LESS THAN (2027),
        PARTITION p2027 VALUES LESS THAN (2028),
        PARTITION p2028 VALUES LESS THAN (2029),
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );


-- =============================================
-- 9. alarm_record（报警记录表）
-- =============================================
DROP TABLE IF EXISTS alarm_record;
CREATE TABLE alarm_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报警ID',
    vehicle_id BIGINT NOT NULL COMMENT '车辆ID',
    temp_zone_no INT COMMENT '温区编号',
    alarm_type VARCHAR(50) NOT NULL COMMENT '报警类型：temp_exceed-温度超限，temp_fluctuate-温度波动，offline-设备离线，route_deviate-偏离路线',
    alarm_level VARCHAR(20) NOT NULL COMMENT '报警级别：warning-警告，error-错误，critical-严重',
    alarm_content VARCHAR(500) NOT NULL COMMENT '报警内容',
    temperature DECIMAL(5,2) COMMENT '当前温度（℃）',
    location VARCHAR(200) COMMENT '位置',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待处理，processing-处理中，resolved-已解决',
    handler_id BIGINT COMMENT '处理人ID',
    handle_result VARCHAR(500) COMMENT '处理结果',
    alarm_time DATETIME NOT NULL COMMENT '报警时间',
    handle_time DATETIME COMMENT '处理时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
    FOREIGN KEY (handler_id) REFERENCES sys_user(id),
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_alarm_time (alarm_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警记录表';

-- =============================================
-- 10. goods_temp_zone（货物-温区关系表）
-- =============================================
DROP TABLE IF EXISTS goods_temp_zone;
CREATE TABLE goods_temp_zone (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    goods_type_id BIGINT NOT NULL COMMENT '货物类型ID',
    compatible_type_id BIGINT NOT NULL COMMENT '可混装的货物类型ID',
    compatibility INT NOT NULL COMMENT '兼容性：1-可混装，0-不可混装，-1-禁止混装',
    reason VARCHAR(200) COMMENT '原因说明',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (goods_type_id) REFERENCES goods_type(id),
    FOREIGN KEY (compatible_type_id) REFERENCES goods_type(id),
    UNIQUE KEY uk_goods_compatible (goods_type_id, compatible_type_id),
    INDEX idx_goods_type_id (goods_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物-温区关系表';

-- 初始数据（混装规则）
INSERT INTO goods_temp_zone (goods_type_id, compatible_type_id, compatibility, reason) VALUES
-- 海鲜可以和肉类、冷冻食品混装（温度相同）
(1, 2, 1, '温度相同，都是-18℃'),
(1, 6, 1, '温度相同，都是-18℃'),
-- 海鲜不可以和蔬菜、水果混装（温度差大）
(1, 3, 0, '温度差大，-18℃ vs 0-4℃'),
(1, 4, 0, '温度差大，-18℃ vs 0-4℃'),
-- 药品禁止和食品混装（法规要求）
(7, 1, -1, '法规禁止药品与食品混装'),
(7, 2, -1, '法规禁止药品与食品混装'),
(7, 3, -1, '法规禁止药品与食品混装'),
(7, 4, -1, '法规禁止药品与食品混装'),
(7, 5, -1, '法规禁止药品与食品混装'),
(7, 6, -1, '法规禁止药品与食品混装'),
-- 蔬菜可以和水果、乳制品混装（温度相同）
(3, 4, 1, '温度相同，都是0-4℃'),
(3, 5, 1, '温度相同，都是0-4℃');

-- =============================================
-- 验证表创建
-- =============================================
SHOW TABLES;

-- 查看表数据
SELECT '用户表' AS table_name, COUNT(*) AS count FROM sys_user
UNION ALL
SELECT '货物类型表', COUNT(*) FROM goods_type
UNION ALL
SELECT '车辆表', COUNT(*) FROM vehicle
UNION ALL
SELECT '车辆温区配置表', COUNT(*) FROM vehicle_temp_zone
UNION ALL
SELECT '订单表', COUNT(*) FROM dispatch_order
UNION ALL
SELECT '调度方案表', COUNT(*) FROM dispatch_plan
UNION ALL
SELECT '调度明细表', COUNT(*) FROM dispatch_detail
UNION ALL
SELECT '温度记录表', COUNT(*) FROM temperature_record
UNION ALL
SELECT '报警记录表', COUNT(*) FROM alarm_record
UNION ALL
SELECT '货物-温区关系表', COUNT(*) FROM goods_temp_zone;
