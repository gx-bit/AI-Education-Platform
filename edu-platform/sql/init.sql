-- ==========================================
-- 在线教育平台 数据库初始化脚本
-- ==========================================

-- 用户库
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS edu_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 课程库
CREATE DATABASE IF NOT EXISTS edu_course DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 订单库
CREATE DATABASE IF NOT EXISTS edu_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 通知库
CREATE DATABASE IF NOT EXISTS edu_notification DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==================== edu_user ====================
USE edu_user;

CREATE TABLE IF NOT EXISTS t_user (
    id            BIGINT       NOT NULL COMMENT '用户ID',
    username      VARCHAR(50)  NOT NULL COMMENT '用户名',
    password      VARCHAR(100) NOT NULL COMMENT '加密密码',
    email         VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone         VARCHAR(20)           COMMENT '手机号',
    avatar        VARCHAR(500)          COMMENT '头像URL',
    real_name     VARCHAR(50)           COMMENT '真实姓名',
    role          VARCHAR(20)  NOT NULL DEFAULT 'student' COMMENT '角色',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:0禁用1正常',
    bio           VARCHAR(200)          COMMENT '个人简介',
    created_at    DATETIME     NOT NULL COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始管理员账号（密码: admin123456，BCrypt加密）
INSERT INTO t_user (id, username, password, email, role, status, created_at, updated_at, deleted)
VALUES (1, 'admin', '$2a$10$s6QIR9jgM46OU/6eMhogKOh6PehCQa0imjfSVuVj/70dGumppywZe',
        'admin@edu.com', 'admin', 1, NOW(), NOW(), 0);

-- 测试学生账号（密码: student123）
INSERT INTO t_user (id, username, password, email, role, status, created_at, updated_at, deleted)
VALUES (2, 'student01', '$2a$10$fkFEYr23F3X7pGxvUNH6ueLOP1Peph4wTPL3D5Np5xPuy2tOBcj/m',
        'student01@edu.com', 'student', 1, NOW(), NOW(), 0);

-- 测试教师账号（密码: teacher123）
INSERT INTO t_user (id, username, password, email, role, status, created_at, updated_at, deleted)
VALUES (3, 'teacher01', '$2a$10$FyqhIrxKFgut89fhRJc3PuDfhdcQr6RPpcVKdbw1ue2266KGIgBgC',
        'teacher01@edu.com', 'teacher', 1, NOW(), NOW(), 0);


-- ==================== edu_course ====================
USE edu_course;

CREATE TABLE IF NOT EXISTS t_category (
    id         BIGINT      NOT NULL COMMENT '分类ID',
    name       VARCHAR(50) NOT NULL COMMENT '分类名称',
    icon       VARCHAR(100)         COMMENT '图标',
    parent_id  BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID',
    sort       INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '状态',
    created_at DATETIME    NOT NULL COMMENT '创建时间',
    deleted    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类';

CREATE TABLE IF NOT EXISTS t_course (
    id            BIGINT         NOT NULL COMMENT '课程ID',
    title         VARCHAR(200)   NOT NULL COMMENT '标题',
    description   TEXT                    COMMENT '描述',
    cover_image   VARCHAR(500)            COMMENT '封面图',
    teacher_id    BIGINT                  COMMENT '讲师ID',
    teacher_name  VARCHAR(50)             COMMENT '讲师名',
    category_id   BIGINT                  COMMENT '分类ID',
    price         DECIMAL(10,2)  NOT NULL DEFAULT 0 COMMENT '价格',
    duration      INT                     COMMENT '时长(分钟)',
    level         VARCHAR(20)    NOT NULL DEFAULT 'beginner' COMMENT '难度',
    status        TINYINT        NOT NULL DEFAULT 0 COMMENT '0草稿1发布2下架',
    student_count INT            NOT NULL DEFAULT 0 COMMENT '学生数',
    rating        DECIMAL(3,1)   NOT NULL DEFAULT 5.0 COMMENT '评分',
    tags          VARCHAR(200)            COMMENT '标签',
    created_at    DATETIME       NOT NULL,
    updated_at    DATETIME       NOT NULL,
    deleted       TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category (category_id),
    KEY idx_status (status),
    FULLTEXT KEY ft_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 初始分类数据
INSERT INTO t_category (id,name,icon,parent_id,sort,status,created_at,deleted) VALUES
(1,'编程开发','💻',0,1,1,NOW(),0),
(2,'数据科学','📊',0,2,1,NOW(),0),
(3,'人工智能','🤖',0,3,1,NOW(),0),
(4,'产品设计','🎨',0,4,1,NOW(),0),
(5,'商业运营','📈',0,5,1,NOW(),0),
(6,'语言学习','🌍',0,6,1,NOW(),0);

-- 初始课程数据（已发布）
INSERT INTO t_course (id,title,description,cover_image,teacher_id,teacher_name,category_id,price,duration,level,status,student_count,rating,tags,created_at,updated_at,deleted) VALUES
(1,'Spring Boot 3.x 微服务实战','从零开始构建生产级微服务系统，涵盖Spring Cloud全家桶','https://picsum.photos/seed/spring/400/225',3,'teacher01',1,199.00,1200,'intermediate',1,856,4.9,'Spring Boot,微服务,Java',NOW(),NOW(),0),
(2,'Vue 3 + TypeScript 全栈开发','现代前端工程化实践，从基础到项目实战','https://picsum.photos/seed/vue/400/225',3,'teacher01',1,149.00,800,'beginner',1,1234,4.8,'Vue3,TypeScript,前端',NOW(),NOW(),0),
(3,'Python 数据分析与可视化','Pandas、Matplotlib、Seaborn 数据处理全流程','https://picsum.photos/seed/python/400/225',3,'teacher01',2,99.00,600,'beginner',1,2341,4.7,'Python,数据分析,可视化',NOW(),NOW(),0),
(4,'深度学习入门与实践','神经网络原理、PyTorch框架、CV/NLP项目实战','https://picsum.photos/seed/dl/400/225',3,'teacher01',3,299.00,1500,'advanced',1,567,4.9,'深度学习,PyTorch,AI',NOW(),NOW(),0),
(5,'Docker & Kubernetes 云原生','容器化部署、K8s集群管理、CI/CD流水线','https://picsum.photos/seed/docker/400/225',3,'teacher01',1,249.00,1000,'intermediate',1,423,4.8,'Docker,K8s,云原生',NOW(),NOW(),0),
(6,'产品经理从入门到精通','需求分析、原型设计、数据驱动决策全流程','https://picsum.photos/seed/pm/400/225',3,'teacher01',4,179.00,700,'beginner',1,789,4.6,'产品经理,需求分析',NOW(),NOW(),0),
(7,'MySQL 性能优化实战','索引优化、查询调优、分库分表实践','https://picsum.photos/seed/mysql/400/225',3,'teacher01',1,199.00,900,'intermediate',1,634,4.8,'MySQL,性能优化,数据库',NOW(),NOW(),0),
(8,'大模型应用开发','LangChain + RAG + Agent 构建AI应用','https://picsum.photos/seed/llm/400/225',3,'teacher01',3,399.00,1200,'advanced',1,1089,5.0,'LLM,AI,大模型',NOW(),NOW(),0);


-- Recommendation feedback loop: real user signals and measurable exposures.
USE edu_course;

CREATE TABLE IF NOT EXISTS t_user_behavior (
    id             BIGINT         NOT NULL,
    user_id        BIGINT                  COMMENT 'Null for anonymous visitors',
    session_id     VARCHAR(64)             COMMENT 'Anonymous browser session',
    course_id      BIGINT         NOT NULL,
    behavior_type  VARCHAR(32)    NOT NULL,
    behavior_value DECIMAL(10,2),
    context_json   JSON,
    created_at     DATETIME       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_behavior_user_time (user_id, created_at),
    KEY idx_behavior_session_time (session_id, created_at),
    KEY idx_behavior_course_type (course_id, behavior_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recommendation user behavior';

CREATE TABLE IF NOT EXISTS t_recommendation_log (
    id             BIGINT        NOT NULL,
    request_id     VARCHAR(64)   NOT NULL,
    user_id        BIGINT,
    session_id     VARCHAR(64),
    course_id      BIGINT        NOT NULL,
    rank_position  INT           NOT NULL,
    score          DECIMAL(8,4)  NOT NULL,
    reason         VARCHAR(500),
    clicked        TINYINT       NOT NULL DEFAULT 0,
    purchased      TINYINT       NOT NULL DEFAULT 0,
    created_at     DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_rec_request (request_id),
    KEY idx_rec_user_time (user_id, created_at),
    KEY idx_rec_course_time (course_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Recommendation exposure and conversion log';

-- ==================== edu_order ====================
USE edu_order;

CREATE TABLE IF NOT EXISTS t_order (
    id           BIGINT         NOT NULL COMMENT '订单ID',
    order_no     VARCHAR(50)    NOT NULL COMMENT '订单号',
    user_id      BIGINT         NOT NULL COMMENT '用户ID',
    username     VARCHAR(50)             COMMENT '用户名',
    course_id    BIGINT         NOT NULL COMMENT '课程ID',
    course_title VARCHAR(200)            COMMENT '课程标题快照',
    course_cover VARCHAR(500)            COMMENT '课程封面快照',
    amount       DECIMAL(10,2)  NOT NULL COMMENT '支付金额',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '0待支付1已支付2取消3退款',
    paid_at      DATETIME                COMMENT '支付时间',
    pay_method   VARCHAR(20)             COMMENT '支付方式',
    provider_trade_no VARCHAR(64)        COMMENT '第三方支付交易号',
    created_at   DATETIME       NOT NULL,
    updated_at   DATETIME       NOT NULL,
    deleted      TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_course_id (course_id),
    KEY idx_provider_trade_no (provider_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';


-- ==================== edu_notification ====================
USE edu_notification;

CREATE TABLE IF NOT EXISTS t_notification (
    id         BIGINT       NOT NULL COMMENT '通知ID',
    user_id    BIGINT       NOT NULL COMMENT '用户ID',
    title      VARCHAR(100) NOT NULL COMMENT '标题',
    content    TEXT         NOT NULL COMMENT '内容',
    type       VARCHAR(20)           COMMENT '类型',
    is_read    TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读1已读',
    related_id BIGINT                COMMENT '关联业务ID',
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';
