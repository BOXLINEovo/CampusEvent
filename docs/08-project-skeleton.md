# CampusEvent 项目骨架（第8批）

> 状态：骨架已生成；技术栈已确认；`D8-01`～`D8-05` 已按你的确认落地；**未实现业务逻辑**。
>
> 技术栈：Java 17 + Spring Boot 3.3.5 + Spring MVC/Thymeleaf（服务端渲染）+ Spring Data JPA + PostgreSQL 16 + Flyway + JUnit 5/Spring Boot Test。
>
> 已创建 2 个 Web 冒烟测试（使用 MockMvc 纯 Web 测试）与 dev-only 演示数据；集成测试/业务测试在 baseline-v1 添加。
>
> 设计依据：`docs/06-design-review-package.md`（设计评审通过）。

---

## 1. 技术栈确认

| 项 | 选择 |
|---|---|
| 语言/运行时 | Java 17 |
| 框架 | Spring Boot 3.3.5（Web、Validation、Data JPA、Thymeleaf） |
| 数据库 | PostgreSQL 16 |
| 迁移 | Flyway |
| 测试 | JUnit 5 + Spring Boot Test（已建 2 个 Web 测试） |
| 前端 | Thymeleaf 服务端渲染 |
| 本地依赖 | 便携版 PostgreSQL 16.4 |
| 云部署 | **使用 JRE 17 + `java -jar` + PostgreSQL 服务**（systemd/NSSM/任务计划，部署批次细化） |

---

## 2. 项目目录结构

```text
CampusEvent/
├── pom.xml
├── README.md
├── .gitignore
├── scripts/
│   ├── start-local.ps1
│   ├── stop-local.ps1
│   └── status-local.ps1
├── docs/
│   ├── 01-use-cases.md
│   ├── 02-class-diagram-v1.md
│   ├── 03-registration-policy-capacity.md
│   ├── 04-notification-decoupling.md
│   ├── 05-sequence-and-contracts.md
│   ├── 06-design-review-package.md
│   ├── 07-tech-stack-options.md
│   ├── 08-project-skeleton.md
│   └── review/design-review-checklist.md
└── src/
    ├── main/
    │   ├── java/com/campusevent/
    │   │   ├── CampusEventApplication.java
    │   │   ├── domain/
    │   │   │   ├── model/
    │   │   │   │   ├── Student.java
    │   │   │   │   ├── Activity.java
    │   │   │   │   ├── Registration.java
    │   │   │   │   ├── Message.java
    │   │   │   │   ├── RegistrationStatus.java
    │   │   │   │   ├── PolicyType.java
    │   │   │   │   ├── NotificationType.java
    │   │   │   │   └── ActorRole.java
    │   │   │   ├── event/
    │   │   │   │   ├── RegistrationEvent.java
    │   │   │   │   ├── RegistrationEventType.java
    │   │   │   │   ├── OutboxEvent.java
    │   │   │   │   └── OutboxStatus.java
    │   │   │   ├── policy/
    │   │   │   │   ├── RegistrationPolicy.java
    │   │   │   │   ├── DirectRegistrationPolicy.java
    │   │   │   │   └── ReviewRegistrationPolicy.java
    │   │   │   └── rule/
    │   │   │       ├── CapacityRule.java
    │   │   │       ├── DefaultCapacityRule.java
    │   │   │       └── CapacityDecision.java
    │   │   ├── application/
    │   │   │   ├── dto/
    │   │   │   │   ├── RegistrationResult.java
    │   │   │   │   └── ActivityStatistics.java
    │   │   │   ├── policy/
    │   │   │   │   └── RegistrationPolicyFactory.java
    │   │   │   ├── port/
    │   │   │   │   ├── ActivityRepository.java
    │   │   │   │   ├── RegistrationRepository.java
    │   │   │   │   ├── MessageRepository.java
    │   │   │   │   ├── OutboxRepository.java
    │   │   │   │   ├── RegistrationEventPublisher.java
    │   │   │   │   ├── RegistrationEventObserver.java
    │   │   │   │   └── NotificationService.java
    │   │   │   └── service/
    │   │   │       ├── ActivityService.java
    │   │   │       ├── RegistrationService.java
    │   │   │       └── StatisticsService.java
    │   │   ├── interfaces/
    │   │   │   └── web/
    │   │   │       ├── HealthController.java
    │   │   │       └── HomeController.java
    │   │   └── infrastructure/
    │   │       ├── outbox/
    │   │       │   ├── OutboxEventPublisher.java
    │   │       │   └── EventDispatcher.java
    │   │       ├── notification/
    │   │       │   └── InSiteNotificationService.java
    │   │       └── persistence/
    │   │           └── package-info.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── db/migration/V1__baseline.sql
    │       ├── db/dev/V2__demo_data.sql
    │       ├── static/css/app.css
    │       └── templates/index.html
    └── test/
        └── java/com/campusevent/
            └── interfaces/web/
                ├── HealthControllerTest.java
                └── HomeControllerTest.java
            # domain/application/infrastructure/integration 目录在 baseline-v1 添加
```

---

## 3. 四层划分与依赖规则

| 层 | 包 | 职责 | 允许依赖 | 禁止依赖 |
|---|---|---|---|---|
| 领域层 | `domain` | 实体、值对象、状态、策略、容量规则、领域事件 | 仅 JDK | Spring、JPA、HTTP、仓储实现 |
| 应用层 | `application` | 用例编排、端口、DTO、策略工厂 | `domain`、端口 | 基础设施实现、Web |
| 接口层 | `interfaces` | Web 控制器、请求/响应适配 | `application` | 直接写 SQL/领域规则 |
| 基础设施层 | `infrastructure` | JPA 适配、outbox、通知、配置 | `application`、`domain` | 反向被领域/应用依赖 |

依赖方向：

```text
interfaces ──► application ──► domain
                    ▲
                    │ implements
              infrastructure
```

- `domain` 不依赖任何外层；`Registration` 不依赖仓储，`CapacityRule` 不查库。
- `application` 只依赖端口（`ActivityRepository` 等）和领域类型，不知道 Spring/JPA。
- `interfaces` 只调用应用服务；不写业务规则。
- `infrastructure` 实现端口；具体 JPA/HTTP/调度细节都在这里。
- 组合根（Spring 配置）负责把实现注入应用服务；当前骨架尚未装配业务 Bean。

---

## 4. 已创建的接口/类骨架

### 4.1 领域层

| 类/接口 | 状态 | 说明 |
|---|---|---|
| `Student` | record | 学生身份/姓名 |
| `Activity` | class + TODO | 活动字段、`isDeleted/hasStarted/isOpenForRegistration/canReduceCapacityTo` 留空 |
| `Registration` | class + TODO | 状态机方法 `approve/reject/cancel/isActive` 留空 |
| `Message` | class + TODO | 消息字段、`markRead` 留空 |
| `RegistrationStatus` | enum | PENDING/APPROVED/REJECTED/CANCELLED |
| `PolicyType` | enum | DIRECT/REVIEW |
| `NotificationType` | enum | 3 类通知 |
| `ActorRole` | enum | STUDENT/ADMIN |
| `RegistrationEvent` | record | 事件载荷（含快照字段） |
| `RegistrationEventType` | enum | 5 个事件 |
| `OutboxEvent` | class | outbox 记录 |
| `OutboxStatus` | enum | PENDING/PROCESSING/SENT/FAILED |
| `RegistrationPolicy` | interface | `initialStatus(Activity)` + `requiresApproval()` |
| `DirectRegistrationPolicy` | class + TODO | 直接型策略 |
| `ReviewRegistrationPolicy` | class + TODO | 审核型策略 |
| `CapacityRule` | interface | `checkSubmission` + `checkApproval` |
| `DefaultCapacityRule` | class + TODO | 默认容量规则 |
| `CapacityDecision` | record | allowed/warning/message |

### 4.2 应用层

| 类/接口 | 状态 | 说明 |
|---|---|---|
| `ActivityRepository` | interface | 活动持久化端口 |
| `RegistrationRepository` | interface | `insert` + `updateStatusIfCurrent` + 查询 |
| `MessageRepository` | interface | 消息持久化/幂等/未读 |
| `OutboxRepository` | interface | outbox 保存/领取/标记 |
| `RegistrationEventPublisher` | interface | 事件发布端口（outbox） |
| `RegistrationEventObserver` | interface | 观察者端口 |
| `NotificationService` | interface | 通知处理 + 查询/已读 |
| `ActivityService` | class + TODO | 活动 CRUD/浏览/筛选 |
| `RegistrationService` | class + TODO | 报名/取消/审批编排 |
| `StatisticsService` | class + TODO | 统计 |
| `RegistrationPolicyFactory` | class + TODO | 按 policyType 选择策略 |
| `RegistrationResult` | record | registration + warning |
| `ActivityStatistics` | record | 已通过/待审核/剩余 |

### 4.3 接口层/基础设施层

| 类 | 状态 | 说明 |
|---|---|---|
| `HealthController` | 可运行 | `GET /health` |
| `HomeController` | 可运行 | `GET /` → `index.html` |
| `OutboxEventPublisher` | class + TODO | 在业务事务内写 outbox |
| `EventDispatcher` | class + TODO | 领取事件、调用观察者、SENT/FAILED |
| `InSiteNotificationService` | class + TODO | 幂等写 Message |
| `infrastructure/persistence` | package-info | JPA 适配待实现 |

> 所有 `TODO` 方法抛 `UnsupportedOperationException("TODO: baseline-v1")`；没有业务实现。

---

## 5. 数据库表草案与唯一约束

迁移文件：`src/main/resources/db/migration/V1__baseline.sql`（PostgreSQL 16）。

### 5.1 表

| 表 | 关键字段 | 说明 |
|---|---|---|
| `student` | `student_id UUID PK`, `name` | 学生；固定演示账号可后续 seed |
| `activity` | `activity_id UUID PK`, `title`, `category`, `start_at`, `location`, `capacity`, `policy_type`, `deleted_at` | 容量 > 0；policy_type 两值；软删除 |
| `registration` | `registration_id UUID PK`, `activity_id`, `student_id`, `status`, timestamps | 4 状态；状态 + 历史；拒绝理由在事件/消息中，不落报名行 |
| `message` | `message_id UUID PK`, `student_id`, `activity_id`, `source_event_id`, `type`, `title`, `content`, `is_read`, `read_at` | 站内消息；幂等键 |
| `outbox_event` | `event_id UUID PK`, `event_type`, `payload JSONB`, `status`, `retry_count`, `next_attempt_at`, `last_error` | 事务性 outbox |

### 5.2 关键唯一约束

```sql
-- 同一学生同一活动最多一条“待审核/已通过”有效报名
CREATE UNIQUE INDEX ux_registration_active
    ON registration (activity_id, student_id)
    WHERE status IN ('PENDING', 'APPROVED');

-- 通知幂等：同一事件对同一学生只写一条消息
CONSTRAINT ux_message_source_student UNIQUE (source_event_id, student_id);
```

- `outbox_event.event_id` 是主键，天然唯一；派发器按 `event_id` 幂等。
- `message` 唯一键是通知幂等的最终防线；`NotificationService` 先查 `existsBySourceEventId`，再依赖唯一约束兜底。
- PostgreSQL 使用部分唯一索引；若换 MySQL，需要生成列/独立活跃表替代。
- 迁移使用 `CREATE EXTENSION IF NOT EXISTS pgcrypto` 提供 `gen_random_uuid()`；云数据库若不允许建扩展，可改为应用生成 UUID，或让 DBA 预装 `pgcrypto`。

### 5.3 索引

- `registration (activity_id, status)`：待审核/已通过计数。
- `registration (student_id, created_at DESC)`：学生历史。
- `message (student_id, created_at DESC)`：通知列表。
- `message (student_id, is_read)`：未读数。
- `outbox_event (status, next_attempt_at)`：派发器扫描。
- `activity (deleted_at, start_at)`：学生可见列表。

### 5.4 软删除与并发

- 活动软删除用 `deleted_at`；查询默认过滤。存在待审核报名时拒绝软删除（应用层检查）。
- 名额批准用事务 + 行锁/条件更新；本批不实现 SQL，只保留表结构。
- `registration` 不设 `approved_count` 冗余列；当前用 `countApproved` 查询。若后续性能需要，再加列并同步维护。

---

## 6. 测试目录结构与已创建测试

```text
src/test/java/com/campusevent/
├── interfaces/web/
│   ├── HealthControllerTest.java   # 已创建
│   └── HomeControllerTest.java     # 已创建
├── domain/                         # baseline-v1
│   ├── model/                      # Registration 状态机、Activity 时间/容量
│   ├── policy/                     # Direct/Review 策略
│   └── rule/                       # CapacityRule 提交/批准
├── application/                    # baseline-v1
│   ├── service/                    # RegistrationService 编排（mock ports）
│   └── policy/                     # 工厂选择
├── infrastructure/                 # baseline-v1
│   ├── persistence/                # 仓储适配（本地 PostgreSQL）
│   ├── outbox/                     # 派发器/幂等
│   └── notification/               # Message 幂等
└── integration/                    # 端到端/云部署前冒烟
```

- 本批已创建 `HealthControllerTest`、`HomeControllerTest` 两个 Web 冒烟测试；`mvn test` 通过。
- 领域/容量规则/仓储/outbox/通知的测试后续在 baseline-v1 添加；集成测试使用本地 PostgreSQL。
- 后续建议先写领域/容量规则单测，再写本地 PostgreSQL 集成测试；不写“验收用例”以外的推测场景。

---

## 7. Git 分支与标签计划

> 本轮未执行任何 Git 操作；以下只是计划。

| 分支/标签 | 用途 | 时机 |
|---|---|---|
| `main` | 主干；骨架与后续集成 | 当前骨架先落 `main` |
| `baseline-v1`（tag） | 初版功能完成、初始测试通过后的封存点 | 初始功能 + 测试完成 |
| `change-v2`（branch + tag） | 教师 CR 的变更分支；从 `baseline-v1` 切出 | 收到 CR、完成影响分析后 |
| `feature/*`（可选） | 小步功能分支 | 需要并行/隔离时 |

建议流程：

1. 当前骨架提交到 `main`（由你执行 Git；我不代操作）。
2. 在 `main` 完成初始功能与测试，打 tag `baseline-v1`。
3. 收到 CR 后，从 `baseline-v1` 建 `change-v2`；完成变更 + 回归，打 tag `change-v2`。
4. 保留 `baseline-v1` 与 `change-v2` 的差异，用于可维护性对比。
5. 记录关键 AI 协作事件到 `AI_DEV_LOG.md`（后续批次）。

---

## 8. 验证与本机安装记录

### 8.1 验证

| 项 | 结果 |
|---|---|
| `mvn -q -DskipTests compile` | 退出码 0 |
| `mvn -q -DskipTests package` | 退出码 0；生成 `target/campus-event-0.0.1-SNAPSHOT.jar` |
| `mvn -q test` | 退出码 0；`HealthControllerTest`、`HomeControllerTest` 通过 |
| 运行时启动 | **通过**：本地 PostgreSQL 16.4 + dev profile；Flyway 应用 V1/V2；`/health` 200，`/` 200 |
| 演示数据 | 2 个学生、2 个活动（DIRECT/REVIEW）已写入 |
| 测试文件 | 2 个 Web 冒烟测试已创建；业务/集成测试后续添加 |

### 8.2 本机安装记录

| 项 | 值 |
|---|---|
| PostgreSQL | 16.4 便携版（非 Windows 服务） |
| 程序目录 | `%LOCALAPPDATA%\CampusEvent\pgsql` |
| 数据目录 | `%LOCALAPPDATA%\CampusEvent\pgdata` |
| 数据库/用户/密码 | `campusevent` / `campusevent` / `campusevent` |
| 端口 | 5432 |
| 脚本 | `scripts/start-local.ps1`、`stop-local.ps1`、`status-local.ps1` |
| 部署方式 | **使用 JRE 17 + `java -jar` + PostgreSQL 服务**（systemd/NSSM/任务计划） |

> 便携版 PostgreSQL 重启电脑后不会自动启动；请运行 `scripts/start-local.ps1`。业务方法仍为 TODO，但骨架已可启动并连通数据库。

---

## 9. 已确认决策与检查点

### 9.1 已确认

| 编号 | 事项 | 结论 | 落地 |
|---|---|---|---|
| **D8-01** | 持久化映射方式 | **A 领域纯类 + infrastructure JPA 实体/映射** | baseline-v1 实现；当前只有端口/骨架 |
| **D8-02** | 测试文件 | **允许创建** | 已创建 2 个 Web 冒烟测试；`mvn test` 通过 |
| **D8-03** | 演示数据 | **dev-only Flyway seed** | `db/dev/V2__demo_data.sql` + `application-dev.yml` |
| **D8-04** | 页面范围 | **先最小页面** | baseline-v1 实现列表/详情/报名/审批/通知页面 |
| **D8-05** | 部署方式 | **使用 JRE 17 + `java -jar` + PostgreSQL 服务** | 部署批次细化 systemd/NSSM/任务计划 |

### 9.2 检查点

- [x] 目录结构与分层符合预期。
- [x] 领域/应用/接口/基础设施的依赖方向没有反向依赖。
- [x] 接口签名与 `05-sequence-and-contracts.md` 一致。
- [x] 数据库唯一约束（有效报名、通知幂等）符合设计。
- [x] `D8-01=A` 已确认；持久化适配在 baseline-v1 实现。
- [x] `D8-02` 允许测试；2 个 Web 冒烟测试已创建并通过。
- [x] `D8-03` dev-only 演示数据已创建。
- [ ] 进入 baseline-v1 实现业务；本轮未写完整业务、未执行 Git 操作。

---

## 10. 文件变更

| 文件 | 操作 | 说明 |
|---|---|---|
| `pom.xml` | 新增 | Maven/Spring Boot 依赖 |
| `compose.yaml` | 删除 | 使用 systemd + jar 部署，移除本地 compose 文件 |
| `README.md` | 新增 | 骨架运行说明 |
| `.gitignore` | 新增 | 构建/IDE/环境文件忽略 |
| `src/main/java/...` | 新增 | 四层骨架、接口、TODO 服务 |
| `src/main/resources/application.yml` | 新增 | 数据源/Flyway/JPA/Thymeleaf |
| `src/main/resources/application-dev.yml` | 新增 | dev profile 加载演示数据 |
| `src/main/resources/db/migration/V1__baseline.sql` | 新增 | 表/索引/唯一约束 |
| `src/main/resources/db/dev/V2__demo_data.sql` | 新增 | dev-only 演示学生/活动 |
| `src/main/resources/static/css/app.css` | 新增 | 共享样式（导航/表格/表单/提示/响应式） |
| `src/main/resources/templates/*.html` | 修订 | 移除内联 style，改用共享 CSS |
| `src/main/resources/templates/index.html` | 新增 | 首页占位 |
| `src/test/java/.../HealthControllerTest.java` | 新增 | `/health` 冒烟测试 |
| `src/test/java/.../HomeControllerTest.java` | 新增 | 首页视图测试 |
| `scripts/start-local.ps1` | 新增 | 启动便携版 PostgreSQL + 应用 |
| `scripts/stop-local.ps1` | 新增 | 停止应用 + PostgreSQL |
| `scripts/status-local.ps1` | 新增 | 查看 5432/8080 状态 |
| `docs/08-project-skeleton.md` | 新增 | 本文 |

> 本轮未执行 Git 操作；未实现完整业务；已创建 2 个 Web 冒烟测试。
