# 冷链智能调度系统 - 后端

## 项目结构

```
backend/
├── src/main/java/com/coldchain/
│   ├── ColdchainBackendApplication.java  # 主启动类
│   ├── controller/                        # 控制器层
│   │   └── TestController.java           # 测试控制器
│   ├── service/                          # 服务层
│   │   └── impl/                         # 服务实现
│   ├── mapper/                           # MyBatis Mapper
│   ├── entity/                           # 实体类
│   ├── dto/                              # 数据传输对象
│   ├── vo/                               # 视图对象
│   ├── common/                           # 公共类
│   │   ├── result/                       # 统一返回结果
│   │   │   └── Result.java              # 统一返回结果类
│   │   └── exception/                    # 异常处理
│   │       ├── BusinessException.java   # 业务异常
│   │       └── GlobalExceptionHandler.java # 全局异常处理器
│   ├── config/                           # 配置类
│   │   ├── MybatisPlusConfig.java       # MyBatis-Plus配置
│   │   └── WebConfig.java               # Web配置（跨域）
│   └── util/                             # 工具类
└── src/main/resources/
    ├── application.yml                   # 配置文件
    └── mapper/                           # MyBatis XML文件
```

## 技术栈

- **Spring Boot 2.6.13** - 核心框架
- **MyBatis-Plus 3.5.5** - ORM框架
- **MySQL 8.0** - 数据库
- **Redis** - 缓存
- **Lombok** - 简化代码
- **Hutool** - 工具类库
- **FastJSON2** - JSON处理
- **JWT** - Token认证

## 已完成功能

✅ 项目基础框架搭建  
✅ 统一返回结果封装（Result）  
✅ 全局异常处理  
✅ MyBatis-Plus配置（分页插件）  
✅ 跨域配置  
✅ 测试接口  

## 启动步骤

### 1. 确保MySQL已启动

```bash
# 检查MySQL是否运行
mysql -u root -p
```

### 2. 创建数据库

```sql
CREATE DATABASE coldchain DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 导入数据库表结构

参考文档：`C:\Users\l\Desktop\大创\04-数据库设计.md`

复制SQL语句到MySQL中执行，创建10张核心表。

### 4. 修改配置文件

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    username: root
    password: 你的MySQL密码  # 修改这里
```

### 5. 启动项目

在IDEA中：
- 找到 `ColdchainBackendApplication.java`
- 右键 → Run 'ColdchainBackendApplication'

或者使用Maven命令：
```bash
mvn spring-boot:run
```

### 6. 测试接口

启动成功后，访问：

- **测试接口1：** http://localhost:8083/api/test/hello
- **测试接口2：** http://localhost:8083/api/test/info

返回结果示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "ColdChain System is running!"
}
```

## 注意事项

### Kafka暂时注释
Kafka相关依赖和配置已经注释掉，后期需要时再启用：

**pom.xml：**
```xml
<!-- Kafka（暂时注释，后期再配置） -->
<!--
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
-->
```

**application.yml：**
```yaml
#  # Kafka配置
#  kafka:
#    bootstrap-servers: localhost:9092
```

### Redis配置
确保Redis已启动，或者暂时注释掉Redis相关配置。

如果Redis未启动，可以暂时注释掉pom.xml中的Redis依赖：
```xml
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
-->
```

## 下一步开发

### 第一周任务（5月16-22日）

- [x] 后端框架搭建
- [x] 统一返回结果封装
- [x] 全局异常处理
- [x] 测试接口
- [ ] 创建数据库表
- [ ] 订单CRUD接口
- [ ] 车辆CRUD接口

### 订单管理模块开发

1. **创建实体类：** `entity/Order.java`
2. **创建Mapper：** `mapper/OrderMapper.java`
3. **创建Service：** `service/OrderService.java` 和 `service/impl/OrderServiceImpl.java`
4. **创建Controller：** `controller/OrderController.java`
5. **Postman测试：** 测试增删改查接口

## 常见问题

### 1. 启动报错：找不到主类
**解决：** 检查pom.xml中的mainClass配置是否正确：
```xml
<mainClass>com.coldchain.ColdchainBackendApplication</mainClass>
```

### 2. 启动报错：连接数据库失败
**解决：** 
- 检查MySQL是否启动
- 检查数据库名称、用户名、密码是否正确
- 检查数据库是否已创建

### 3. 启动报错：Redis连接失败
**解决：** 
- 检查Redis是否启动
- 或者暂时注释掉Redis依赖

### 4. 访问接口404
**解决：** 
- 检查端口是否正确（8083）
- 检查接口路径是否正确
- 检查Controller是否添加了@RestController注解

## 开发规范

### 1. 命名规范
- **类名：** 大驼峰（PascalCase），如 `OrderController`
- **方法名：** 小驼峰（camelCase），如 `getOrderList`
- **常量：** 全大写+下划线，如 `MAX_SIZE`

### 2. 注释规范
- 类和方法必须添加注释
- 复杂逻辑必须添加注释

### 3. 返回结果规范
统一使用 `Result<T>` 封装返回结果：
```java
// 成功
return Result.success(data);

// 失败
return Result.error("错误信息");
```

### 4. 异常处理规范
业务异常使用 `BusinessException`：
```java
throw new BusinessException("订单不存在");
```

## 联系方式

如果遇到问题，可以：
1. 查看项目文档：`C:\Users\l\Desktop\大创\`
2. Google搜索错误信息
3. 问AI（Claude、ChatGPT）

---

**加油！一步一步来，每天进步一点！** 💪
