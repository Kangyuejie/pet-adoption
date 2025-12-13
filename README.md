# 宠物领养系统 (Pet Adoption System)

## 项目概述

宠物领养系统是一个基于 Web 的综合性宠物领养平台，旨在连接救助机构与潜在领养者，为流浪宠物寻找温暖的家。系统提供宠物浏览、智能匹配、在线申请、用户交流等功能，支持中英文双语界面。

## 技术栈

### 后端
- **FastAPI** - 现代化的高性能 Python Web 框架
- **SQLAlchemy** - Python SQL 工具包和 ORM
- **SQLite** - 轻量级关系型数据库
- **JWT (python-jose)** - JSON Web Token 认证
- **Passlib + bcrypt** - 密码加密

### 前端
- **Vue.js 3** - 渐进式 JavaScript 框架（CDN 方式引入）
- **Bootstrap 5** - CSS 框架
- **Bootstrap Icons** - 图标库

## 项目结构

```
bsdemo/
├── backend/
│   └── main.py              # 后端主程序（模型、API、业务逻辑）
├── static/
│   ├── css/                 # 样式文件目录
│   ├── js/                  # JavaScript 文件目录
│   └── images/              # 静态图片目录
├── templates/
│   └── index.html           # 前端单页应用
├── media/
│   └── pets/                # 宠物图片上传目录
├── requirements.txt         # Python 依赖
├── pet_adoption.db          # SQLite 数据库文件（运行时生成）
└── README.md                # 项目文档
```

---

## 后端模块详解 (backend/main.py)

### 1. 数据库模型 (Database Models)

#### 1.1 User（用户模型）
```python
class User(Base):
    __tablename__ = "users"
```
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| email | String(255) | 邮箱（唯一） |
| username | String(100) | 用户名（唯一） |
| hashed_password | String(255) | 加密密码 |
| role | String(20) | 角色：adopter/shelter/admin |
| phone | String(20) | 电话 |
| address | Text | 地址 |
| created_at | DateTime | 创建时间 |
| is_active | Boolean | 是否激活 |

#### 1.2 Pet（宠物模型）
```python
class Pet(Base):
    __tablename__ = "pets"
```
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| name | String(100) | 宠物名称 |
| pet_type | String(20) | 类型：dog/cat/other |
| breed | String(100) | 品种 |
| age_months | Integer | 年龄（月） |
| gender | String(20) | 性别：male/female/unknown |
| description | Text | 描述 |
| is_neutered | Boolean | 是否绝育 |
| is_vaccinated | Boolean | 是否接种疫苗 |
| health_notes | Text | 健康备注 |
| image_url | String(500) | 图片URL |
| location | String(200) | 所在位置 |
| latitude | Float | 纬度 |
| longitude | Float | 经度 |
| adoption_requirements | Text | 领养要求 |
| is_available | Boolean | 是否可领养 |
| created_at | DateTime | 创建时间 |
| owner_id | Integer | 发布者ID（外键） |

#### 1.3 AdoptionApplication（领养申请模型）
```python
class AdoptionApplication(Base):
    __tablename__ = "adoption_applications"
```
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| pet_id | Integer | 宠物ID（外键） |
| applicant_id | Integer | 申请人ID（外键） |
| status | String(20) | 状态：pending/approved/rejected/completed |
| home_type | String(50) | 住房类型 |
| has_yard | Boolean | 是否有院子 |
| other_pets | Text | 其他宠物情况 |
| experience | Text | 养宠经验 |
| reason | Text | 申请理由 |
| created_at | DateTime | 创建时间 |
| updated_at | DateTime | 更新时间 |

#### 1.4 Favorite（收藏模型）
```python
class Favorite(Base):
    __tablename__ = "favorites"
```
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| user_id | Integer | 用户ID（外键） |
| pet_id | Integer | 宠物ID（外键） |
| created_at | DateTime | 创建时间 |

#### 1.5 UserPreference（用户偏好模型）
```python
class UserPreference(Base):
    __tablename__ = "user_preferences"
```
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| user_id | Integer | 用户ID（外键，唯一） |
| preferred_types | String(100) | 偏好类型（逗号分隔） |
| min_age | Integer | 最小年龄 |
| max_age | Integer | 最大年龄 |
| max_distance | Integer | 最大距离 |
| prefer_neutered | Boolean | 偏好已绝育 |

#### 1.6 Message（消息模型）
```python
class Message(Base):
    __tablename__ = "messages"
```
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| sender_id | Integer | 发送者ID（外键） |
| receiver_id | Integer | 接收者ID（外键） |
| pet_id | Integer | 关联宠物ID（外键，可选） |
| content | Text | 消息内容 |
| is_read | Boolean | 是否已读 |
| created_at | DateTime | 创建时间 |

#### 1.7 PetComment（宠物评论模型）
```python
class PetComment(Base):
    __tablename__ = "pet_comments"
```
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| pet_id | Integer | 宠物ID（外键） |
| user_id | Integer | 评论者ID（外键） |
| content | Text | 评论内容 |
| created_at | DateTime | 创建时间 |

#### 1.8 BrowsingHistory（浏览历史模型）
```python
class BrowsingHistory(Base):
    __tablename__ = "browsing_history"
```
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| user_id | Integer | 用户ID（外键） |
| pet_id | Integer | 宠物ID（外键） |
| view_count | Integer | 浏览次数 |
| first_viewed_at | DateTime | 首次浏览时间 |
| last_viewed_at | DateTime | 最后浏览时间 |

---

### 2. API 接口详解

#### 2.1 认证模块 (Authentication)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/register` | 用户注册 | 公开 |
| POST | `/api/login` | 用户登录 | 公开 |
| GET | `/api/me` | 获取当前用户信息 | 需登录 |

**注册请求体：**
```json
{
    "email": "user@example.com",
    "username": "username",
    "password": "password",
    "role": "adopter"
}
```

**登录响应：**
```json
{
    "access_token": "jwt_token",
    "token_type": "bearer",
    "user": { ... }
}
```

#### 2.2 宠物模块 (Pets)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/pets` | 获取宠物列表（支持筛选） | 公开 |
| GET | `/api/pets/{pet_id}` | 获取宠物详情 | 公开 |
| POST | `/api/pets` | 创建宠物（支持图片上传） | shelter/admin |
| PUT | `/api/pets/{pet_id}` | 更新宠物信息 | 拥有者/admin |
| DELETE | `/api/pets/{pet_id}` | 删除宠物 | 拥有者/admin |
| GET | `/api/my-pets` | 获取我发布的宠物 | 需登录 |

**筛选参数：**
- `pet_type`: 宠物类型 (dog/cat/other)
- `gender`: 性别 (male/female)
- `min_age`: 最小年龄（月）
- `max_age`: 最大年龄（月）
- `is_neutered`: 是否绝育
- `search`: 搜索关键词（名字、品种、类型、描述、位置）

#### 2.3 领养申请模块 (Applications)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/applications` | 提交领养申请 | 需登录 |
| GET | `/api/applications` | 获取申请列表 | 需登录 |
| PUT | `/api/applications/{app_id}/status` | 更新申请状态 | shelter/admin |

**申请请求体：**
```json
{
    "pet_id": 1,
    "home_type": "house",
    "has_yard": true,
    "other_pets": "无",
    "experience": "养过3年猫",
    "reason": "喜欢这只宠物"
}
```

#### 2.4 收藏模块 (Favorites)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/favorites/{pet_id}` | 添加/取消收藏 | 需登录 |
| GET | `/api/favorites` | 获取收藏列表 | 需登录 |

#### 2.5 偏好设置模块 (Preferences)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/preferences` | 获取用户偏好 | 需登录 |
| PUT | `/api/preferences` | 更新用户偏好 | 需登录 |

#### 2.6 消息模块 (Messages)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/messages` | 发送消息 | 需登录 |
| GET | `/api/messages` | 获取会话列表 | 需登录 |
| GET | `/api/messages/{partner_id}` | 获取与某用户的对话 | 需登录 |
| GET | `/api/messages/unread/count` | 获取未读消息数 | 需登录 |

#### 2.7 评论模块 (Comments)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/pets/{pet_id}/comments` | 添加评论 | 需登录 |
| GET | `/api/pets/{pet_id}/comments` | 获取宠物评论 | 公开 |
| DELETE | `/api/comments/{comment_id}` | 删除评论 | 评论者/admin |

#### 2.8 用户模块 (Users)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/users/{user_id}` | 获取用户主页 | 公开 |
| PUT | `/api/profile` | 更新个人资料 | 需登录 |

#### 2.9 统计模块 (Stats)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/stats` | 获取平台统计数据 | 公开 |

**响应示例：**
```json
{
    "total_pets": 10,
    "available_pets": 10,
    "total_applications": 0,
    "completed_adoptions": 0,
    "pets_by_type": {"dogs": 5, "cats": 4, "other": 1}
}
```

#### 2.10 浏览历史模块 (Browsing History)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/history/{pet_id}` | 记录浏览历史 | 需登录 |
| GET | `/api/history` | 获取浏览历史列表 | 需登录 |
| DELETE | `/api/history` | 清空所有浏览历史 | 需登录 |
| DELETE | `/api/history/{pet_id}` | 删除单条历史记录 | 需登录 |

**响应示例（GET /api/history）：**
```json
[
    {
        "id": 1,
        "name": "旺财",
        "pet_type": "dog",
        "breed": "金毛寻回犬",
        "view_count": 3,
        "last_viewed_at": "2024-01-15T10:30:00",
        "first_viewed_at": "2024-01-14T09:00:00"
    }
]
```

---

### 3. 核心算法

#### 3.1 智能匹配算法 (calculate_match_score)

根据用户偏好计算宠物匹配分数（0-100分）：

```python
def calculate_match_score(pet: Pet, preference: UserPreference) -> int:
    score = 0
    if preference:
        # 类型匹配 +30分
        if preference.preferred_types:
            types = preference.preferred_types.split(",")
            if pet.pet_type in types:
                score += 30
        # 年龄匹配 +25分
        if preference.min_age is not None and preference.max_age is not None:
            if preference.min_age <= pet.age_months <= preference.max_age:
                score += 25
        # 绝育偏好 +15分
        if preference.prefer_neutered is not None:
            if pet.is_neutered == preference.prefer_neutered:
                score += 15
    # 已接种疫苗 +10分
    if pet.is_vaccinated:
        score += 10
    # 有图片 +5分
    if pet.image_url:
        score += 5
    # 详细描述 +5分
    if pet.description and len(pet.description) > 50:
        score += 5
    return score
```

---

## 前端模块详解 (templates/index.html)

### 1. 页面视图 (Views)

| 视图名称 | 说明 |
|----------|------|
| home | 首页，展示统计数据和精选宠物 |
| pets | 宠物列表页，支持筛选和搜索 |
| favorites | 我的收藏页 |
| history | 浏览历史页，记录浏览过的宠物 |
| applications | 申请记录页 |
| preferences | 偏好设置页 |
| manage | 宠物管理页（shelter/admin） |
| messages | 消息中心页 |
| profile | 个人主页 |

### 2. 弹窗组件 (Modals)

| 组件名称 | 说明 |
|----------|------|
| loginModal | 登录弹窗 |
| registerModal | 注册弹窗 |
| detailModal | 宠物详情弹窗（含评论区） |
| applicationForm | 领养申请表单弹窗 |
| addPetModal | 添加宠物弹窗 |

### 3. 国际化 (i18n)

系统支持中英文切换，翻译文本存储在 `translations` 对象中：

```javascript
const translations = {
    en: { /* 英文翻译 */ },
    zh: { /* 中文翻译 */ }
};
```

**使用方式：**
```javascript
// JavaScript
t('nav.home')  // 返回 "首页" 或 "Home"

// 模板
{{ t('nav.home') }}
```

### 4. 状态管理

使用 Vue 3 Composition API 的 `ref` 进行响应式状态管理：

```javascript
const user = ref(null);           // 当前用户
const pets = ref([]);             // 宠物列表
const favorites = ref([]);        // 收藏列表
const applications = ref([]);     // 申请列表
const conversations = ref([]);    // 会话列表
const currentChat = ref(null);    // 当前聊天
const profileUser = ref(null);    // 查看的用户资料
const petComments = ref([]);      // 宠物评论
const unreadCount = ref(0);       // 未读消息数
```

### 5. API 调用封装

```javascript
const api = async (url, options = {}) => {
    const token = localStorage.getItem('token');
    const headers = { ...options.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }
    const response = await fetch(url, { ...options, headers });
    if (!response.ok) {
        const error = await response.json();
        throw new Error(error.detail || 'Request failed');
    }
    return response.json();
};
```

---

## 安装与运行

### 1. 环境要求

- Python 3.8+
- pip 包管理器

### 2. 安装依赖

```bash
cd bsdemo
pip install -r requirements.txt
```

### 3. 启动服务

```bash
python3 backend/main.py
```

服务将在 http://localhost:12346 启动

### 4. 访问系统

打开浏览器访问：http://localhost:12346

---

## 测试账号

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | admin@petadopt.com | admin123 |
| 救助机构 | shelter@petadopt.com | shelter123 |

---

## 功能特性

### 用户功能
- [x] 用户注册/登录
- [x] 个人资料编辑
- [x] 个人主页展示
- [x] 中英文语言切换
- [x] 浏览历史记录
- [x] 历史记录清空/删除

### 宠物功能
- [x] 宠物列表浏览
- [x] 多条件筛选（类型、性别、年龄、关键词）
- [x] 宠物详情查看
- [x] 宠物收藏
- [x] 宠物评论

### 领养功能
- [x] 在线领养申请
- [x] 申请状态跟踪
- [x] 申请审核（shelter/admin）

### 匹配功能
- [x] 用户偏好设置
- [x] 智能匹配算法
- [x] 匹配度分数显示

### 交流功能
- [x] 用户间私信
- [x] 未读消息提醒
- [x] 联系宠物发布者

### 管理功能
- [x] 宠物发布（shelter/admin）
- [x] 宠物编辑/删除
- [x] 图片上传

---

## 项目亮点

1. **单页应用架构** - 前后端分离，用户体验流畅
2. **响应式设计** - 支持桌面和移动设备
3. **JWT 认证** - 安全的用户身份验证
4. **智能匹配** - 根据用户偏好推荐宠物
5. **实时交流** - 用户间即时消息功能
6. **国际化支持** - 中英文双语界面
7. **品种数据库** - 内置丰富的宠物品种选项

---

## 后续改进方向

1. **技术层面**
   - 添加 WebSocket 实现实时消息推送
   - 引入 Redis 缓存提升性能
   - 使用 PostgreSQL/MySQL 替代 SQLite
   - 添加单元测试和集成测试

2. **功能层面**
   - 地图定位功能
   - 宠物图片多图上传
   - 领养进度追踪
   - 领养成功故事分享
   - 管理员数据统计面板

3. **用户体验**
   - 添加加载动画
   - 表单验证优化
   - 移动端适配优化
   - 添加消息推送通知

---

## 许可证

MIT License

---

## 联系方式

如有问题或建议，请通过系统内消息功能联系管理员。
