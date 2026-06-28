# 校园多服务平台

校园版"闲鱼 + 美团跑腿 + 校园圈社交"一站式小程序，毕业设计项目。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | 微信小程序原生（35+ 页面） |
| 后端 | Spring Boot 2.7.18 + MyBatis-Plus 3.5.3.1 + Maven |
| 数据库 | MySQL 8.0 + Redis |
| 集成 | 阿里云 OSS / DeepSeek AI / 通义万相 / WebSocket / JWT / Spring Cache AOP |
| 部署 | 阿里云 ECS Ubuntu + JDK 21 |

## 功能模块

1. 登录认证 — wx.login → code 换 openid → JWT 签发
2. 闲置交易 — 发布/搜索/分类/详情/编辑/下架/售出 → 评价
3. 校园跑腿 — 发布(地图选点)/广场/接单/完成/取消/编辑/放弃
4. 组局 — 创建/申请审核/签到码签到/踢人/满员评价
5. 校园圈 — 闲聊 + 公告，点赞/评论/楼中楼/置顶
6. 社团 — 入驻申请 → 审核 → 成员管理
7. 即时通讯 — WebSocket 长连接 + HTTP 降级，文字 + 图片
8. 评价系统 — 1-5 星 + 匿名，四类评价 + 三榜排行
9. AI 文案 — DeepSeek API 自动生成闲置/跑腿/组局文案
10. AI 海报 — 通义万相生成背景 → Canvas 叠加文字 → OSS
11. 举报系统 — 7 类举报，管理员处理 + 系统通知
12. 管理后台 — 仪表盘/用户/物品/任务/组局/帖子/举报/社团审核/操作日志
13. 消息红点 — 统一 badge 接口，TabBar 角标
14. 敏感词 — 18 词内存 HashSet，发布时检测
15. Redis 缓存 — 5 个模块（校园圈/闲置/跑腿/排行榜/用户信息）@Cacheable
16. 实时通知 — WebSocket 实时推送，任务/组局/社团/举报事件
17. 信誉分 — 加权公式：评价×0.4+完成率×0.3+无举报×0.2+认证×0.1

## 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+
- 微信开发者工具

## 快速开始

### 1. 数据库

```sql
CREATE DATABASE campus_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

用 `backended/sql/complete_schema.sql` 建表（如已有表则只执行文件末尾的 notification 表和 ALTER 语句）。

### 2. 后端配置

修改 `backended/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_service?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: 你的数据库用户名
    password: 你的数据库密码
  redis:
    host: localhost
    port: 6379

# 阿里云 OSS（图片上传）
aliyun:
  oss:
    endpoint: oss-cn-xxx.aliyuncs.com
    access-key-id: 你的AK
    access-key-secret: 你的SK
    bucket-name: 你的bucket

# DeepSeek AI（文案生成）
deepseek:
  api-key: 你的API Key

# DashScope（AI海报）
dashscope:
  api-key: 你的API Key

# 微信小程序
wechat:
  app-id: 你的AppID
  app-secret: 你的AppSecret
  dev-mode: true  # 开发模式：true=code hash模拟，false=真实API
```

### 3. 启动后端

```bash
cd backended
mvn spring-boot:run
```

启动后访问 `http://localhost:8080`。

### 4. 前端

1. 打开微信开发者工具
2. 导入项目，选择 `frontended/` 目录
3. 填入 AppID（或选择"测试号"）
4. 修改 `frontended/utils/api.js` 中的 `BASE_URL` 为你的后端地址

## 项目结构

```
├── backended/                  # Spring Boot 后端
│   ├── sql/complete_schema.sql # 完整建库 SQL
│   └── src/main/java/com/campus/service/
│       ├── entity/             # 16 个实体类
│       ├── mapper/             # MyBatis Mapper 接口
│       ├── service/            # 16 个 Service
│       ├── controller/         # 13 个 Controller
│       ├── config/             # JWT 拦截器、WebSocket、跨域
│       ├── websocket/          # WebSocket Handler
│       ├── aspect/             # AOP 操作日志切面
│       ├── annotation/         # @OpLog 自定义注解
│       ├── dto/                # 统一返回格式 Result
│       └── util/               # JWT 工具类
│
├── frontended/                 # 微信小程序前端
│   ├── app.js / app.json / app.wxss
│   ├── utils/api.js            # API 请求封装
│   └── pages/
│       ├── index/              # 首页（校园圈）
│       ├── goods/              # 闲置交易（列表/详情/发布）
│       ├── task/               # 跑腿任务（广场/详情/发布）
│       ├── team/               # 组局（广场/详情/发布/我的）
│       ├── chat/               # 即时通讯
│       ├── message/            # 消息列表 + 通知入口
│       ├── notification/       # 系统通知
│       ├── user/               # 个人中心/编辑/认证/我的闲置/我的跑腿/社团
│       ├── campus-circle/      # 校园圈（列表/详情/发布）
│       ├── club/               # 社团（广场/详情）
│       ├── evaluation/         # 评价（评价页/评价列表）
│       ├── ranking/            # 排行榜
│       ├── report/             # 举报（举报/我的举报）
│       ├── poster/             # AI 海报生成
│       ├── admin/              # 管理后台
│       └── login/              # 登录页
│
├── 面试问答汇总.md              # 24 章面试题 + 项目结合
└── MySQL数据库建表.md           # 16 张表结构说明 + ER 关系
```

## 数据库表

user, goods, task, message, evaluation, team, team_join, announcement, comment, comment_like, post_like, club_application, club_member, report, operation_log, notification, sensitive_word

## 答辩要点

1. **技术亮点**：WebSocket 实时推送、Redis 5 模块缓存、AOP 操作日志、AI 文案+海报双模型
2. **安全措施**：JWT 认证、敏感词过滤、SQL 防注入（LambdaQueryWrapper）、操作审计
3. **业务完整**：交易 → 评价 → 信誉分闭环，发布 → 审核 → 通知闭环

## 待完善

- AI 海报 Canvas 保存待修复
- 敏感词过滤未接入校园圈
- 建议后续加 TTL 过期策略防缓存雪崩
