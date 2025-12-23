# 宠物领养系统 - Java Spring Boot 版本

一个功能完整的宠物领养平台，使用 Spring Boot 3.2 构建后端，Vue.js 构建前端，支持用户注册登录、宠物浏览、领养申请、实时消息等功能。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.1
- **语言**: Java 17
- **安全**: Spring Security + JWT
- **数据库**: H2 (开发) / MySQL (生产)
- **ORM**: Spring Data JPA + Hibernate
- **实时通信**: WebSocket
- **API文档**: OpenAPI 3 / Swagger UI
- **构建工具**: Maven

### 前端
- **框架**: Vue.js 3
- **UI**: Bootstrap 5
- **图标**: Bootstrap Icons
- **图表**: ECharts 5.4
- **HTTP**: Fetch API

## 功能特性

### 用户功能
- 用户注册/登录 (JWT认证)
- 个人资料管理
- 领养偏好设置
- 浏览历史记录
- 收藏宠物

### 宠物管理
- 宠物列表浏览与筛选
- 宠物详情查看
- 多图片上传与管理
- 智能匹配推荐
- 宠物评论系统

### 领养流程
- 在线提交领养申请
- 申请状态跟踪
- 救助机构审核管理

### 消息系统
- 用户间私信功能
- WebSocket 实时通知
- 消息已读状态

### 数据统计 (ECharts)
- 宠物类型分布饼图
- 月度领养趋势折线图
- 申请状态堆叠柱状图
- 宠物年龄分布饼图

## 项目结构

```
pet-adoption-java/
├── pom.xml                          # Maven 配置
├── src/main/java/com/petadoption/
│   ├── PetAdoptionApplication.java  # 启动类
│   ├── config/                      # 配置类
│   │   ├── CorsConfig.java          # 跨域配置
│   │   ├── SecurityConfig.java      # 安全配置
│   │   ├── WebSocketConfig.java     # WebSocket配置
│   │   └── OpenApiConfig.java       # Swagger配置
│   ├── controller/                  # 控制器层 (11个)
│   │   ├── AuthController.java      # 认证接口
│   │   ├── PetController.java       # 宠物接口
│   │   ├── ApplicationController.java # 申请接口
│   │   └── ...
│   ├── service/                     # 服务层 (12个)
│   │   ├── AuthService.java
│   │   ├── PetService.java
│   │   ├── MatchingService.java     # 智能匹配
│   │   └── ...
│   ├── repository/                  # 数据访问层 (9个)
│   ├── entity/                      # 实体类 (9个)
│   │   ├── User.java
│   │   ├── Pet.java
│   │   ├── AdoptionApplication.java
│   │   └── ...
│   ├── dto/                         # 数据传输对象
│   │   ├── request/                 # 请求DTO (6个)
│   │   └── response/                # 响应DTO (8个)
│   ├── enums/                       # 枚举类 (4个)
│   ├── exception/                   # 异常处理
│   ├── security/                    # 安全组件
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   ├── websocket/                   # WebSocket组件
│   └── util/                        # 工具类
│       └── DataInitializer.java     # 数据初始化
└── src/main/resources/
    ├── application.yml              # 配置文件
    └── templates/
        └── index.html               # 前端页面
```

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+

### 安装运行

1. **克隆项目**
```bash
git clone https://github.com/Kangyuejie/pet-adoption.git
cd pet-adoption/pet-adoption-java
```

2. **编译项目**
```bash
mvn clean compile
```

3. **运行项目**
```bash
mvn spring-boot:run
```

4. **访问应用**
- 主页: http://localhost:12346
- API文档: http://localhost:12346/swagger-ui.html
- H2控制台: http://localhost:12346/h2-console

### 测试账号

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | admin@petadopt.com | admin123 |
| 救助机构 | shelter@petadopt.com | shelter123 |

## API 接口

### 认证接口
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/register | 用户注册 |
| POST | /api/login | 用户登录 |
| GET | /api/me | 获取当前用户 |

### 宠物接口
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/pets | 获取宠物列表 |
| GET | /api/pets/{id} | 获取宠物详情 |
| POST | /api/pets | 发布宠物 |
| PUT | /api/pets/{id} | 更新宠物 |
| DELETE | /api/pets/{id} | 删除宠物 |
| POST | /api/pets/{id}/images | 上传图片 |

### 领养申请接口
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/applications | 获取申请列表 |
| POST | /api/applications | 提交申请 |
| PUT | /api/applications/{id}/status | 更新状态 |

### 其他接口
| 方法 | 路径 | 描述 |
|------|------|------|
| GET/POST/DELETE | /api/favorites | 收藏管理 |
| GET/PUT | /api/preferences | 偏好设置 |
| GET/POST | /api/messages | 消息系统 |
| GET | /api/history | 浏览历史 |
| GET | /api/stats | 统计数据 |
| GET | /api/stats/charts | 图表数据 |

## 数据库设计

### 核心实体

```
User (用户)
├── id, email, username, password
├── role (ADOPTER/SHELTER/ADMIN)
└── phone, address

Pet (宠物)
├── id, name, pet_type, breed
├── age_months, gender, description
├── is_neutered, is_vaccinated
├── location, image_url
└── owner_id, is_available

AdoptionApplication (领养申请)
├── id, pet_id, user_id
├── status (PENDING/APPROVED/REJECTED/COMPLETED)
├── message, contact_phone
└── created_at

其他: PetImage, Favorite, UserPreference,
      Message, PetComment, BrowsingHistory
```

## 配置说明

### application.yml 主要配置

```yaml
server:
  port: 12346

spring:
  datasource:
    url: jdbc:h2:file:./pet_adoption_db
  jackson:
    property-naming-strategy: SNAKE_CASE

jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时

file:
  upload-dir: ./media/pets
```

### 切换到 MySQL

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pet_adoption
    username: root
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
```

## 截图预览

### 首页
- 统计数据展示
- ECharts 可视化图表
- 宠物卡片列表

### 功能页面
- 宠物筛选与搜索
- 宠物详情与图片轮播
- 领养申请表单
- 消息中心
- 个人中心

## 开发说明

### 添加新的 API 接口

1. 在 `entity/` 创建实体类
2. 在 `repository/` 创建 Repository 接口
3. 在 `service/` 实现业务逻辑
4. 在 `controller/` 创建 REST 接口
5. 在 `dto/` 添加请求/响应对象

### 代码规范
- 使用 Lombok 简化代码
- 遵循 RESTful API 设计
- 统一异常处理
- JSON 使用 snake_case 命名

## 许可证

MIT License

## 作者

Kangyuejie
