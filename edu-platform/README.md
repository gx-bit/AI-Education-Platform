# edu-platform — 在线教育微服务平台

基于 Spring Boot 3 + Spring Cloud 的生产级微服务在线教育平台，集成 AI 课程推荐、MCP 工具服务与完整 CI/CD 流水线。

---

## 架构总览

```mermaid
graph TD
    subgraph Client
        FE[Vue 3 前端<br/>:80]
        AI[AI Agent / Claude Desktop<br/>MCP Client]
    end

    subgraph Gateway
        GW[gateway-service<br/>Spring Cloud Gateway<br/>:8080]
    end

    subgraph Services
        US[user-service<br/>:8081]
        CS[course-service<br/>:8082]
        OS[order-service<br/>:8083]
        NS[notification-service<br/>:8084]
    end

    subgraph Infrastructure
        NC[Nacos<br/>注册中心 + 配置中心<br/>:8848]
        MQ[RabbitMQ<br/>消息队列<br/>:5672]
        MY[(MySQL<br/>:3306)]
        RD[(Redis<br/>:6379)]
        ZP[Zipkin<br/>链路追踪<br/>:9411]
    end

    subgraph MCP
        MCP[edu-platform MCP Server<br/>Node.js stdio]
    end

    FE -->|HTTP| GW
    AI -->|stdio| MCP
    MCP -->|HTTP + JWT| GW

    GW -->|路由 + JWT验证| US
    GW -->|路由| CS
    GW -->|路由| OS
    GW -->|路由| NS

    US --> MY
    US --> RD
    CS --> MY
    CS -->|WebClient AI推荐| Claude[(Claude API)]
    OS --> MY
    OS -->|发布事件| MQ
    MQ -->|消费| NS
    NS --> MY

    US --> NC
    CS --> NC
    OS --> NC
    NS --> NC
    GW --> NC

    US --> ZP
    CS --> ZP
    OS --> ZP
    NS --> ZP
```

---

## 服务说明

| 服务 | 端口 | 职责 |
|------|------|------|
| gateway-service | 8080 | API 网关，JWT 鉴权，请求路由，熔断降级 |
| user-service | 8081 | 用户注册/登录，JWT 签发，用户管理 |
| course-service | 8082 | 课程 CRUD，分类管理，Claude AI 推荐 |
| order-service | 8083 | 订单创建/支付模拟，消息发布 |
| notification-service | 8084 | 站内通知，消费 RabbitMQ 事件 |
| recommendation-service | 8085 | 行为采集、用户画像、混合召回、排序与推荐反馈闭环 |
| frontend | 80 | Vue 3 + Element Plus 管理前端 |
| mcp-server | stdio | MCP 工具服务，供 AI Agent 调用 |

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 框架 | Spring Boot 3.2.4 / Spring Cloud 2023.0.1 |
| 服务注册/配置 | Nacos 2.x |
| 网关 | Spring Cloud Gateway + Resilience4j |
| 认证 | JWT (jjwt 0.12.5) |
| ORM | MyBatis-Plus 3.5.5 |
| 缓存 | Redis (Spring Data Redis) |
| 消息队列 | RabbitMQ (Spring AMQP) |
| 分布式追踪 | Micrometer + Zipkin |
| AI 推荐 | Claude API (claude-sonnet-4-6) via WebClient |
| MCP 服务 | @modelcontextprotocol/sdk 1.x (Node.js) |
| 前端 | Vue 3 + Vite + Element Plus + Pinia |
| 容器化 | Docker + Docker Compose |
| CI/CD | Jenkins Declarative Pipeline |

---

## 快速启动

### 前置条件

- Docker & Docker Compose v2
- JDK 17+（本地开发）
- Node.js 18+（MCP Server 本地运行）
- Maven 3.8+（本地构建）

### 1. 克隆仓库

```bash
git clone <your-repo-url>
cd edu-platform
```

### 2. 配置环境变量

```bash
cp .env.example .env          # 编辑数据库密码、JWT Secret 等
```

`.env` 关键配置项：

```env
MYSQL_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_min_32_chars
NACOS_NAMESPACE=                # optional; leave empty for public namespace
ANTHROPIC_API_KEY=sk-ant-...    # optional; course-service AI recommendations
```

### 3. 一键启动所有服务

```bash
mvn -DskipTests package
docker compose up -d --build
```

启动顺序（由 `depends_on` + healthcheck 保证）：

```
MySQL / Redis / RabbitMQ / Nacos
    → user-service / course-service / order-service / notification-service
        → gateway-service
            → frontend
```

### 4. 验证服务健康

```bash
# 网关健康检查
curl http://localhost:8080/actuator/health

# 用户服务（通过网关路由）
curl http://localhost:8081/actuator/health
```

### 5. 访问入口

| 入口 | 地址 |
|------|------|
| 前端 | http://localhost |
| API 网关 | http://localhost:8080 |
| Nacos 控制台 | http://localhost:8848/nacos（nacos/nacos） |
| RabbitMQ 管理 | http://localhost:15672（edu/edu123456） |
| Zipkin 链路 | http://localhost:9411 |

---

## API 快速参考

### 认证

```bash
# 注册
POST http://localhost:8080/api/user/register
{
  "username": "test",
  "password": "Test@1234",
  "email": "test@example.com"
}

# 登录（返回 JWT）
POST http://localhost:8080/api/user/login
{
  "username": "test",
  "password": "Test@1234"
}
```

### 课程

```bash
# 搜索课程
GET http://localhost:8080/api/course/list?keyword=Java&page=1&size=10

# AI 推荐
GET http://localhost:8080/api/course/recommend?interest=Java
Authorization: Bearer <JWT>
```

### 订单

```bash
# 创建订单
POST http://localhost:8080/api/order/create
Authorization: Bearer <JWT>
{
  "courseId": 1
}
```

---

## MCP Server（AI Agent 集成）

MCP Server 将平台核心接口封装为 MCP 工具，供 Claude Desktop 或其他 AI Agent 调用。

### 启动 MCP Server

```bash
cd mcp-server
npm install
cp .env.example .env    # 填写 GATEWAY_URL 和 PLATFORM_TOKEN
npm start
```

### 集成到 Claude Desktop

将 `mcp-config.example.json` 内容合并到 Claude Desktop 配置文件：

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "edu-platform": {
      "command": "node",
      "args": ["/absolute/path/to/mcp-server/src/index.js"],
      "env": {
        "GATEWAY_URL": "http://localhost:8080",
        "PLATFORM_TOKEN": "<管理员JWT>"
      }
    }
  }
}
```

### 可用 MCP 工具

| 工具 | 描述 |
|------|------|
| `search_courses` | 搜索课程（关键词/分类/难度/分页） |
| `get_course_detail` | 获取课程完整详情 |
| `get_course_categories` | 获取所有课程分类 |
| `get_ai_recommendations` | 获取 AI 推荐课程列表 |
| `get_order_stats` | 获取平台订单统计 |
| `list_orders` | 查询订单列表（管理员） |
| `list_users` | 查询用户列表（管理员） |
| `get_user_detail` | 获取用户详情 |
| `get_user_notifications` | 查询用户通知 |

---

## 本地开发

### 编译所有模块

```bash
# 必须从根目录执行，确保 common 模块先安装到本地仓库
mvn clean install -DskipTests -T 4
```

### 单独启动某个服务

```bash
# 确保基础设施（MySQL/Redis/Nacos/RabbitMQ）已通过 docker compose 启动
docker compose up -d mysql redis nacos rabbitmq

# 启动 user-service
cd user-service
mvn spring-boot:run
```

### 前端开发模式

```bash
cd frontend
npm install
npm run dev    # http://localhost:5173（代理到 :8080）
```

---

## CI/CD（Jenkins）

`Jenkinsfile` 定义了完整的 7 阶段流水线：

| 阶段 | 触发条件 | 说明 |
|------|----------|------|
| Checkout | 所有分支 | 拉取代码 |
| Build & Test | 所有分支 | `mvn clean install`，发布测试报告 |
| Code Quality | main | SonarQube 静态分析 |
| Docker Build | 所有分支 | 并行构建 6 个镜像并推送到仓库 |
| Deploy Staging | develop / main | SSH 部署到 Staging 环境 |
| Smoke Test | develop / main | 健康检查验证 |
| Deploy Production | main | **手动确认**后部署到生产 |

**Jenkins 需配置的 Credentials：**

| ID | 类型 | 用途 |
|----|------|------|
| `docker-registry-credentials` | Username/Password | 阿里云镜像仓库登录 |
| `deploy-server-ssh` | SSH Private Key | 部署服务器 SSH 密钥 |

---

## 项目结构

```
edu-platform/
├── common/
│   ├── common-core/          # 通用工具、响应封装、异常处理
│   └── common-security/      # JWT 工具类、安全注解
├── gateway-service/          # Spring Cloud Gateway
├── user-service/             # 用户认证与管理
├── course-service/           # 课程与 AI 推荐
├── order-service/            # 订单与支付
├── notification-service/     # 站内通知
├── frontend/                 # Vue 3 前端
├── mcp-server/               # MCP 工具服务 (Node.js)
│   ├── src/index.js
│   ├── package.json
│   └── .env.example
├── docker-compose.yml
├── Jenkinsfile
├── .gitignore
└── README.md
```

---

## 支付宝支付配置

付费订单通过支付宝电脑网站支付完成。订单只有在支付宝异步通知通过 RSA2
验签，并校验应用、商户、订单号和金额后才会更新为已支付。首次接入请使用
`.env.example` 中的沙箱配置；`ALIPAY_NOTIFY_URL` 必须是公网可访问的 HTTPS 地址。

已有数据库需要先执行 `sql/migrate-alipay-payment.sql`。

## License

MIT
