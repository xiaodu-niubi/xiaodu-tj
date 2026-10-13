# 小杜课堂 (XDXT)

> 基于 Spring Boot 3 + Spring Cloud 微服务架构的在线教育平台，集成 AI 智能助理功能。

## 项目简介

小杜课堂是一个完整的在线教育系统，涵盖课程管理、学习记录、考试系统、支付交易、媒资管理、AI 智能问答等核心功能。前后端分离，后端采用微服务架构，前端提供学生端（门户）和管理端两套界面。

## 技术栈

| 类别 | 技术 |
|------|------|
| 核心框架 | Spring Boot 3.3.5、Spring Cloud、Spring Cloud Alibaba |
| 服务注册/配置 | Nacos |
| 网关 | Spring Cloud Gateway |
| 认证授权 | JWT + JKS 非对称加密 |
| 数据库 | MySQL、Redis、MongoDB、Elasticsearch |
| 消息队列 | RabbitMQ |
| 对象存储 | 阿里云 OSS / 腾讯云 COS |
| 视频点播 | 腾讯云 VOD |
| AI 大模型 | 阿里云 DashScope（通义千问）/ Spring AI |
| 支付 | 支付宝、微信支付 |
| 短信 | 阿里云 SMS |
| 搜索 | Elasticsearch |
| 容器化 | Docker |
| 语言/工具 | Java 17、Maven |

## 微服务模块

| 模块 | 服务名 | 端口 | 说明 |
|------|--------|------|------|
| xd-gateway | gateway-service | 10010 | API 网关，统一入口 |
| xd-auth | auth-service | 8081 | 认证授权，JWT 签发与验证 |
| xd-user | user-service | 8082 | 用户管理，学员与员工 |
| xd-search | search-service | 8083 | 课程全文搜索（Elasticsearch） |
| xd-media | media-service | 8084 | 媒资管理，图片/视频上传与播放 |
| xd-message | message-service | 8085 | 消息中心，站内信/短信通知 |
| xd-course | course-service | 8086 | 课程管理，分类/章节/小节 |
| xd-pay | pay-service | 8087 | 支付中心，支付宝/微信支付 |
| xd-trade | trade-service | 8088 | 交易中心，订单管理 |
| xd-exam | exam-service | 8089 | 考试系统，题库/试卷/答题 |
| xd-learning | learning-service | 8090 | 学习记录，进度追踪 |
| xd-remark | remark-service | 8091 | 互动问答，点赞/评论 |
| xd-promotion | promotion-service | 8092 | 促销活动，优惠券/折扣 |
| xd-data | data-service | 8093 | 数据统计，学习报表 |
| xd-aigc | aigc-service | 8094 | AI 智能助理，课程推荐/问答 |

## 项目结构

```
xdxt/
├── xd-common/          # 公共工具类、常量、异常定义
├── xd-api/             # 各服务 Feign 接口定义
├── xd-auth/            # 认证模块（含 SDK）
│   ├── xd-auth-service/
│   ├── xd-auth-common/
│   ├── xd-auth-resource-sdk/
│   └── xd-auth-gateway-sdk/
├── xd-gateway/         # API 网关
├── xd-user/            # 用户服务
├── xd-course/          # 课程服务
├── xd-media/           # 媒资服务
├── xd-learning/        # 学习服务
├── xd-exam/            # 考试服务
├── xd-trade/           # 交易服务
├── xd-pay/             # 支付服务（含 API/Domain 分层）
├── xd-message/         # 消息服务（含 API/Domain 分层）
├── xd-promotion/       # 促销服务
├── xd-search/          # 搜索服务
├── xd-remark/          # 互动服务
├── xd-data/            # 数据统计服务
├── xd-aigc/            # AI 智能助理服务
├── 前端/
│   ├── xd-admin/       # 管理后台（Vue）
│   └── xd-protal/      # 学生门户（Vue）
├── Dockerfile          # 服务镜像构建文件
└── pom.xml             # 父工程 POM
```

## AI 智能助理功能

`xd-aigc` 模块基于 Spring AI 框架，接入阿里云 DashScope（通义千问）大模型，提供：

- **多轮对话**：带记忆的上下文对话（支持 Redis / MySQL / MongoDB 存储）
- **路由 Agent**：自动识别用户意图，分发至推荐/购买/通用对话 Agent
- **课程推荐**：根据用户需求智能推荐课程
- **购买引导**：引导用户完成课程购买流程
- **语音对话**：支持 DashScope 语音转文字

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose（可选）
- Nacos 2.x
- MySQL 8.x
- Redis 6.x
- Elasticsearch 8.x

### 环境变量配置

各服务通过环境变量注入敏感配置，启动前需设置：

```bash
# 阿里云 OSS
export OSS_ACCESSKEY_ID=your_access_key_id
export OSS_ACCESSKEY_SECRET=your_access_key_secret

# 支付宝
export ALI_PAY_APP_ID=your_app_id
export ALI_PAY_MERCHANT_PRIVATE_KEY=your_private_key
export ALI_PAY_PUBLIC_KEY=your_public_key

# 微信支付
export WX_PAY_APP_ID=your_app_id
export WX_PAY_MCH_ID=your_mch_id
export WX_PAY_MCH_SERIAL_NO=your_serial_no
export WX_PAY_PRIVATE_KEY=your_private_key
export WX_PAY_API_V3_KEY=your_api_v3_key

# 认证服务
export JKS_KEYSTORE_PASSWORD=your_keystore_password
export JKS_KEY_SECRET=your_key_secret

# 腾讯云 VOD
export TENCENT_VOD_URL_KEY=your_url_key

# 促销加密
export PROMOTION_AES_KEY=your_aes_key
export PROMOTION_AES_IV=your_aes_iv
```

### 本地开发

1. 启动基础设施（Nacos、MySQL、Redis、Elasticsearch、RabbitMQ）
2. 在各服务的 `application-local.yml` 中配置本地连接地址（**此文件不提交 git**）
3. 按依赖顺序启动服务：`xd-auth` → `xd-gateway` → 其他业务服务

### Docker 部署

```bash
# 打包
mvn clean package -DskipTests

# 构建镜像（单个服务示例）
cp xd-aigc/target/*.jar app.jar
docker build -t xd-aigc:1.0.0 .
```

## 许可证

本项目仅供学习交流使用。
