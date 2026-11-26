# 第14批 change-v2 实现记录

> 基线：`baseline-v1`（当前主分支提交 `90b1d14`）。
>
> 实施方式：方案 A（最小扩展 + 装饰器 + 观察者）。
>
> 约束：原代码文件保持不变，仅新增 v2 文件；v1 UML 源文件和导出图保持不变。

## 1. 变更文件清单

### 1.1 新增主代码（30 个 Java 类/接口）

**领域与 DTO**

- `domain/model/ActivityGradeRuleV2.java`
- `domain/model/EmailNotificationStatusV2.java`
- `domain/model/EmailNotificationV2.java`
- `application/dto/StudentSummaryV2.java`
- `application/dto/GradeRuleRowV2.java`
- `application/dto/EmailMessageV2.java`
- `application/dto/EmailSendResultV2.java`

**端口与异常**

- `application/port/StudentGradeRepositoryV2.java`
- `application/port/ActivityGradeRuleRepositoryV2.java`
- `application/port/EmailSenderV2.java`
- `application/port/EmailNotificationRepositoryV2.java`
- `application/exception/GradeRestrictionExceptionV2.java`
- `application/exception/DuplicateEmailNotificationExceptionV2.java`

**应用服务**

- `application/service/GradeAwareRegistrationServiceV2.java`
- `application/service/GradeRuleAdminServiceV2.java`

**持久化与通知适配**

- `infrastructure/persistence/jdbc/JdbcStudentGradeRepositoryV2.java`
- `infrastructure/persistence/jpa/entity/ActivityGradeRuleJpaEntityV2.java`
- `infrastructure/persistence/jpa/entity/EmailNotificationJpaEntityV2.java`
- `infrastructure/persistence/jpa/mapper/ActivityGradeRuleMapperV2.java`
- `infrastructure/persistence/jpa/mapper/EmailNotificationMapperV2.java`
- `infrastructure/persistence/jpa/repository/ActivityGradeRuleJpaRepositoryV2.java`
- `infrastructure/persistence/jpa/repository/EmailNotificationJpaRepositoryV2.java`
- `infrastructure/persistence/jpa/adapter/JpaActivityGradeRuleRepositoryV2.java`
- `infrastructure/persistence/jpa/adapter/JpaEmailNotificationRepositoryV2.java`
- `infrastructure/notification/MockEmailSenderV2.java`
- `infrastructure/notification/EmailNotificationObserverV2.java`

**Web 入口**

- `interfaces/web/GradeRuleAdminControllerV2.java`
- `interfaces/web/GradeDemoControllerV2.java`
- `interfaces/web/EmailAdminControllerV2.java`
- `interfaces/web/form/GradeRuleFormV2.java`

### 1.2 新增模板

- `templates/admin/grade-rules-v2.html`
- `templates/admin/emails-v2.html`
- `templates/grade-demo-v2.html`

### 1.3 新增数据库迁移

- `db/migration/V3__grade_and_email-v2.sql`
- `db/dev/V4__demo_grade_data-v2.sql`（仅 dev 演示数据）

### 1.4 新增测试

- `src/test/java/com/campusevent/domain/model/ActivityGradeRuleV2Test.java`
- `src/test/java/com/campusevent/application/service/GradeAwareRegistrationServiceV2Test.java`
- `src/test/java/com/campusevent/infrastructure/notification/EmailNotificationObserverV2Test.java`

### 1.5 新增 UML v2

源文件：

- `docs/diagrams/use-case-v2.puml`
- `docs/diagrams/class-diagram-v2.puml`
- `docs/diagrams/seq-register-grade-v2.puml`
- `docs/diagrams/seq-email-notify-v2.puml`

导出：

- `docs/diagrams/export/use-case-v2.svg`
- `docs/diagrams/export/use-case-v2.png`
- `docs/diagrams/export/class-diagram-v2.svg`
- `docs/diagrams/export/class-diagram-v2.png`
- `docs/diagrams/export/seq-register-grade-v2.svg`
- `docs/diagrams/export/seq-register-grade-v2.png`
- `docs/diagrams/export/seq-email-notify-v2.svg`
- `docs/diagrams/export/seq-email-notify-v2.png`

### 1.6 新增文档

- `docs/13-change-impact-v2.md`（第13批影响分析）
- `docs/14-change-implementation-v2.md`（本文件）

### 1.7 原文件修改情况

- 原 Java 文件修改：0 个。
- 原模板修改：0 个。
- 原测试修改：0 个。
- 原数据库表结构修改：0 个（仅新增表）。
- v1 UML 源文件与导出图修改：0 个。

> `GradeAwareRegistrationServiceV2` 使用 `@Primary` 在运行时接管 `RegistrationService` 的报名入口；这是新增类，不修改原服务源码。

## 2. Git diff 计划

本轮尚未提交。建议按以下顺序执行：

```powershell
# 1. 查看工作区；确认只有 v2 新增文件与既有非本轮改动
git -C 实验/CampusEvent status --short

# 2. 确认已跟踪源码没有差异
git -C 实验/CampusEvent diff --name-only -- src/main src/test docs/diagrams

# 3. 只添加 v2 新增文件，不添加 AI_DEV_LOG.md 和两份既有删除文件
git -C 实验/CampusEvent add src/main/java src/main/resources/db src/main/resources/templates src/test/java docs/13-change-impact-v2.md docs/14-change-implementation-v2.md docs/diagrams

# 4. 提交
git -C 实验/CampusEvent commit -m "feat: implement change-v2 grade rules and mock email"

# 5. 可选：建立变更版本标签
git -C 实验/CampusEvent tag -a change-v2 -m "CampusEvent change-v2"
```

注意：工作区当前的 `AI_DEV_LOG.md` 修改和两份文档删除来自本轮之前的操作，本轮保持原样；上面的 `git add` 范围不包含它们。

## 3. 变更需求点对应关系

### CR-01 年级限定报名

| 需求点 | 实现位置 |
|---|---|
| 学生年级 1—4 | `student_grade_v2` 表、`StudentSummaryV2`、`JdbcStudentGradeRepositoryV2` |
| 活动年级规则 | `activity_grade_rule_v2` 表、`ActivityGradeRuleV2`、`JpaActivityGradeRuleRepositoryV2` |
| 仅大二/大三可报名 | `GradeAwareRegistrationServiceV2` 在 `super.register` 前校验 |
| 其他年级明确提示 | `GradeRestrictionExceptionV2` 中文消息，旧控制器捕获并显示 |
| 公共约束保持 | 年级检查后调用原 `RegistrationService`，名额、时间、重复报名、软删除仍由原流程处理 |
| 原直接/审核行为保持 | 无规则时直接调用 `super.register`；`PolicyType` 仍为 `DIRECT/REVIEW` |
| 管理端配置 | `/admin/grade-rules-v2`、`GradeRuleAdminServiceV2`、`GradeRuleAdminControllerV2` |
| 演示/验收 | `/grade-demo-v2` 可选学生和活动，覆盖年级 1—4 |

### CR-02 模拟邮件通知

| 需求点 | 实现位置 |
|---|---|
| 站内通知继续运行 | `InSiteNotificationService` 未改动 |
| 三类事件新增邮件 | `EmailNotificationObserverV2` 处理 `REGISTERED/APPROVED/REJECTED` |
| 观察者接入 | 实现 `RegistrationEventObserver`，`EventDispatcher` 自动收集 |
| 记录收件人/主题/内容/状态/时间 | `email_notification_v2`、`EmailNotificationV2`、`JpaEmailNotificationRepositoryV2` |
| 失败记录错误 | 观察者捕获发送异常，写 `FAILED` 并记录日志 |
| 报名结果不回滚 | 邮件在 outbox 派发阶段执行；观察者捕获异常，不向 `EventDispatcher` 抛出 |
| 幂等 | 唯一键 `(source_event_id, student_id)`；发送前查询，重复事件跳过 |
| 查看记录 | `/admin/emails-v2`、`EmailAdminControllerV2` |

## 4. 接口兼容性

- `RegistrationPolicy`、`CapacityRule`、`RegistrationEvent`、`RegistrationEventObserver`、`EventDispatcher`、`NotificationService`、`MessageRepository` 的既有方法签名保持不变。
- `PolicyType` 保持 `DIRECT/REVIEW`，没有新增枚举值。
- `Activity`、`Student`、`Registration` 的字段与构造器保持不变。
- 旧控制器继续注入 `RegistrationService`；`@Primary` 使它们获得新的年级感知实现。
- 新增页面使用独立 URL，不修改旧导航和旧模板。
- 数据库采用新表，不改基线表结构。

## 5. 测试与验证

### 5.1 已执行

```powershell
$env:JAVA_HOME = "E:\OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9\jdk-21.0.13+9"
C:\Users\18362\AppData\Local\Temp\opencode\apache-maven-3.9.9\bin\mvn.cmd test
```

结果：26 个测试，0 失败，0 错误。

- 新增 3 个测试类、10 个测试方法。
- 原有 5 个测试类、16 个测试方法保持通过。

### 5.2 启动与接口验收

已使用 dev 配置启动，Flyway 校验 4 个迁移通过，JPA 校验通过。

已验证：

- `GET /health` → 200，`{"status":"UP"}`。
- `GET /admin/grade-rules-v2` → 200。
- `GET /grade-demo-v2` → 200。
- `GET /admin/emails-v2` → 200。
- 学生 A（年级 2）报名年级限定活动 → `APPROVED`。
- 学生 B（年级 3）报名年级限定活动 → `APPROVED`。
- 学生 C（年级 1）报名年级限定活动 → 拒绝，提示包含“仅限大二、大三”和“当前年级：大一”。
- 旧 `/activities/{id}/registrations` 直接报名 → 成功，说明无规则路径未被破坏。
- 审核报名提交 → `PENDING`；管理员批准 → `APPROVED`，邮件记录 `SENT`。
- 审核拒绝 → `REJECTED`，邮件记录 `SENT` 且含拒绝原因。
- outbox 事件最终 `SENT`，邮件表与站内通知并存。

## 6. 回归建议清单（待执行）

在 `实验/CampusEvent` 目录执行：

```powershell
$env:JAVA_HOME = "E:\OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9\jdk-21.0.13+9"
C:\Users\18362\AppData\Local\Temp\opencode\apache-maven-3.9.9\bin\mvn.cmd test
```

需要重跑的已有功能项：

1. 活动创建、编辑、软删除与表单校验。
2. 活动列表、关键字筛选、详情与统计。
3. 直接报名成功、满员拒绝、重复报名拒绝、已开始/已删除拒绝。
4. 审核报名提交、满员提交警告、批准成功、批准满额失败、拒绝、取消。
5. 站内通知：报名成功、审核通过、审核拒绝；提交/取消按原规则处理；未读/已读。
6. outbox 定时派发、失败重试、重复派发幂等。
7. 并发报名/审批的名额一致性与状态更新条件。
8. 旧页面：首页、活动列表/详情、我的报名、通知、管理页、统计页。
9. 健康检查与 dev 启动。

建议命令：

```powershell
# 全部测试
mvn test

# 仅本次新增测试
mvn -Dtest=ActivityGradeRuleV2Test,GradeAwareRegistrationServiceV2Test,EmailNotificationObserverV2Test test

# 打包并启动（dev）
mvn -DskipTests package
java -jar target/campus-event-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## 7. 本轮记录

| 项目 | 记录 |
|---|---|
| AI 交互轮次 | 1 次实现指令（第14批）；AI 内部执行 9 个阶段：读取基线、领域/端口、持久化、服务/控制器、迁移/模板、测试、编译修复、启动验收、UML v2 |
| 修改总时长 | 约 20 分钟（16:24—16:44，含编译、单测、启动和接口验收） |
| 新增主代码类/接口 | 30 个 |
| 新增测试类 | 3 个（10 个测试方法） |
| 修改核心业务类 | 0 个（原文件保持不变） |
| 新增模板 | 3 个 |
| 新增迁移 | 2 个（1 个主迁移 + 1 个 dev 演示数据） |
| 新增 UML v2 | 4 张，导出 8 个文件 |

## 8. 待决策项与检查点

### 待决策项

1. Java 文件采用 `V2` 后缀（Java 公共类不能含 `-`），其他文件采用 `-v2`；确认是否接受。
2. 年级采用独立 `student_grade_v2` 表；确认是否接受，或改为 `student.grade` 列。
3. 邮件发送失败当前记录 `FAILED` 并结束 outbox 事件，不自动重试；确认是否需要重试。
4. 新页面通过直接 URL 访问，旧导航保持不变；确认验收方式。
5. UML v2 当前覆盖 4 张受影响图；确认是否需要为全部 9 张图生成 v2。
6. 当前工作区存在非本轮的 `AI_DEV_LOG.md` 修改和两份文档删除；提交时只添加 v2 文件。

### 建议检查点

- 执行 `git diff --name-only -- src/main src/test` 确认原文件无改动。
- 检查 v1 UML 源文件与导出图时间戳/内容保持不变。
- 跑完既有 5 个测试类与新增 3 个测试类。
- 在 dev 库验证年级 1/4 拒绝、2/3 通过、满员、重复报名。
- 在 `/admin/emails-v2` 验证邮件记录和失败日志。
- 确认 `outbox_event` 最终为 `SENT`，报名状态保持正确。
