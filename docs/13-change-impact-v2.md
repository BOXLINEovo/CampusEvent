# 第13批 变更影响分析（CR-01 / CR-02）

> 状态：影响分析完成，待方案确认；本轮仅新增文档，v2 代码保持未创建。
>
> 基线：`baseline-v1`（提交 `90b1d14`）。
>
> 输入：`实验二教师专用材料.docx` 中的 CR-01、CR-02。
>
> 约束：原代码文件保持不变，仅新增文件；新增文件以 `-v2` 区分；v1 UML 源文件和导出图保持不变，另增 v2。

## 1. 变更需求拆解与验收条件

### 1.1 CR-01 年级限定报名

**业务规则拆解**

1. 学生增加年级属性，取值为 1—4。
2. 活动支持“年级限定直接报名”规则；规则只限定可报名年级，初始状态仍为“直接通过”。
3. 仅年级在允许范围内的学生可以报名；其他年级得到明确提示。
4. 年级规则与原有公共约束叠加：名额、活动开始时间、重复有效报名、活动软删除。
5. 原有“直接报名”和“审核报名”行为保持不变。
6. 管理端可以配置某活动的年级范围；配置对象为直接报名活动。

**验收条件**

| 编号 | 条件 | 期望 |
|---|---|---|
| CR1-A1 | 大二或大三学生报名年级限定直接活动，活动未开始、名额充足、无重复报名 | 报名成功，状态为 `APPROVED`，产生原有报名成功通知 |
| CR1-A2 | 大一或大四学生报名年级限定直接活动 | 拒绝，页面显示明确原因；不产生报名记录和报名事件 |
| CR1-A3 | 年级符合但名额已满 | 按原直接报名规则拒绝，提示名额已满 |
| CR1-A4 | 年级符合但活动已开始或已删除 | 按原公共约束拒绝，提示活动不可报名 |
| CR1-A5 | 年级符合但已有待审核/已通过报名 | 按原重复报名约束拒绝 |
| CR1-A6 | 年级限定活动改为审核报名，或对审核活动配置年级规则 | 管理端拒绝该配置，保持“年级限定直接报名”语义 |
| CR1-A7 | 未配置年级规则的活动 | 原有直接报名、审核报名流程保持原行为 |
| CR1-A8 | 学生年级缺失或超出 1—4 | 拒绝并提示年级信息异常 |
| CR1-A9 | 管理端新增/修改年级范围 | 范围在 1—4 内且 `minGrade <= maxGrade`，否则拒绝 |

### 1.2 CR-02 模拟邮件通知

**业务规则拆解**

1. 原有站内通知继续运行。
2. 对“直接报名通过”“审核通过”“审核拒绝”三类事件，新增模拟邮件记录。
3. 邮件记录包含收件人、主题、内容、状态、时间；失败时记录错误信息。
4. 邮件发送失败记录错误，已提交的报名状态保持不变。
5. 同一业务事件只产生一份相同邮件记录。
6. 邮件发送通过观察者/事件处理器接入，`RegistrationService` 保持原流程。

**验收条件**

| 编号 | 条件 | 期望 |
|---|---|---|
| CR2-A1 | 直接报名通过 | 站内消息和模拟邮件均有记录，内容对应活动与结果 |
| CR2-A2 | 审核批准 | 站内消息和模拟邮件均有通过记录 |
| CR2-A3 | 审核拒绝 | 站内消息和模拟邮件均有拒绝记录，邮件含拒绝原因 |
| CR2-A4 | 模拟邮件发送失败 | 邮件记录状态为失败并保存错误信息；报名状态保持已提交结果；站内通知仍存在 |
| CR2-A5 | 同一业务事件重复投递或重复提交同一动作 | 邮件记录保持一份；报名状态机按原规则处理 |
| CR2-A6 | 审核提交（`SUBMITTED`）或取消（`CANCELLED`） | 按原站内通知规则处理；邮件订阅者跳过这两类事件 |
| CR2-A7 | 查看模拟邮件记录 | 可查询收件人、主题、内容、状态、时间、错误信息 |

## 2. 涉及对象与影响面

### 2.1 现有类与接口（源码保持不变）

| 对象 | 现有职责 | 本次关系 |
|---|---|---|
| `RegistrationService` | 报名、审批、拒绝、取消的用例编排 | 通过新增 `@Primary` 子类在 `register` 前增加年级检查；原文件不改 |
| `RegistrationPolicy` | 只决定初始状态与是否需要审批 | 保持稳定；年级是资格规则，初始状态仍由 `DIRECT` 决定 |
| `DirectRegistrationPolicy` / `ReviewRegistrationPolicy` | 直接通过 / 待审核 | 保持不变 |
| `RegistrationPolicyFactory` | 按 `PolicyType` 返回策略 | 保持不变；`PolicyType` 继续只有 `DIRECT`、`REVIEW` |
| `CapacityRule` / `DefaultCapacityRule` | 提交与审批名额规则 | 保持不变；年级检查之后仍由原服务执行 |
| `Registration` / `RegistrationStatus` | 报名状态机 | 保持不变；年级规则不新增状态 |
| `RegistrationEvent` / `RegistrationEventType` | 领域事件 | 保持不变；邮件复用 `REGISTERED`、`APPROVED`、`REJECTED` |
| `RegistrationEventObserver` / `EventDispatcher` | 观察者扩展点与 outbox 派发 | 保持不变；新增邮件观察者自动加入观察者列表 |
| `InSiteNotificationService` | 站内通知 | 保持不变；与邮件观察者并行运行 |
| `MessageRepository` / `message` 表 | 站内消息与幂等约束 | 保持不变 |
| `Activity` / `ActivityJpaEntity` / `ActivityMapper` | 活动模型与持久化 | 保持不变；年级规则放在新增扩展表 |
| `Student` | 学生记录（当前只有 id、name） | 保持不变；年级放在新增扩展表 |
| `RegistrationController` / `ActivityController` | 旧页面入口 | 保持不变；新增 `@Primary` 服务对旧入口生效 |
| `ActivityForm` / 旧模板 | 活动管理页 | 保持不变；年级规则使用新增管理页 |

### 2.2 计划新增的类与接口

**CR-01**

| 新增对象 | 职责 |
|---|---|
| `StudentGradeV2` | 学生年级值对象，校验 1—4 |
| `StudentSummaryV2` | 演示页使用的学生摘要（id、姓名、年级） |
| `ActivityGradeRuleV2` | 活动年级范围值对象，提供 `allows(grade)` |
| `StudentGradeRepositoryV2` | 年级查询、学生摘要查询、年级维护端口 |
| `ActivityGradeRuleRepositoryV2` | 活动年级规则查询、保存、删除端口 |
| `JdbcStudentGradeRepositoryV2` | 基于 `JdbcTemplate` 的年级扩展表适配器 |
| `JpaActivityGradeRuleRepositoryV2` | 活动年级规则 JPA 适配器 |
| `ActivityGradeRuleJpaEntityV2` | 年级规则扩展表实体 |
| `ActivityGradeRuleMapperV2` | 领域对象与实体映射 |
| `ActivityGradeRuleJpaRepositoryV2` | 年级规则 Spring Data 仓库 |
| `GradeRestrictionExceptionV2` | 年级不符/年级缺失的明确业务异常 |
| `GradeAwareRegistrationServiceV2` | `@Primary` 扩展 `RegistrationService`，在 `register` 前检查年级，再委托原流程 |
| `GradeRuleAdminServiceV2` | 校验活动为直接报名、校验范围、保存规则 |
| `GradeRuleFormV2` | 管理端年级规则表单 |
| `GradeRuleAdminControllerV2` | `/admin/grade-rules-v2` 管理入口 |
| `GradeDemoControllerV2` | `/grade-demo-v2` 演示与验收入口，可选择学生和活动 |

**CR-02**

| 新增对象 | 职责 |
|---|---|
| `EmailMessageV2` | 待发送邮件值对象 |
| `EmailSendResultV2` | 模拟发送结果（成功/失败、错误） |
| `EmailSenderV2` | 邮件发送端口 |
| `MockEmailSenderV2` | 模拟发送器，记录收件人、主题、内容、状态、时间 |
| `EmailNotificationV2` | 邮件通知记录领域对象 |
| `EmailNotificationStatusV2` | `PENDING`、`SENT`、`FAILED` |
| `EmailNotificationRepositoryV2` | 邮件记录查询、幂等保存端口 |
| `JpaEmailNotificationRepositoryV2` | 邮件记录 JPA 适配器 |
| `EmailNotificationJpaEntityV2` | 邮件记录实体 |
| `EmailNotificationMapperV2` | 领域对象与实体映射 |
| `EmailNotificationJpaRepositoryV2` | 邮件记录 Spring Data 仓库 |
| `EmailNotificationObserverV2` | 观察者，处理三类事件；捕获发送失败并记录，保持报名事务结果 |
| `EmailAdminControllerV2` | `/admin/emails-v2` 邮件记录查看入口 |

### 2.3 数据库表

新增迁移文件：`src/main/resources/db/migration/V3__grade_and_email-v2.sql`（名称待 Flyway 验证）。

| 表 | 关键字段 | 说明 |
|---|---|---|
| `student_grade_v2` | `student_id` PK/FK、`grade` SMALLINT、`created_at`、`updated_at` | 学生年级扩展表；`grade` 约束 1—4；与 `student` 一对一 |
| `activity_grade_rule_v2` | `activity_id` PK/FK、`min_grade`、`max_grade`、`created_at`、`updated_at` | 活动年级范围；约束 1—4 且 `min <= max` |
| `email_notification_v2` | `email_id` PK、`source_event_id`、`student_id`、`recipient`、`subject`、`content`、`status`、`error`、`sent_at`、`created_at` | 模拟邮件记录；唯一约束 `(source_event_id, student_id)` |

设计选择：采用独立扩展表保存年级，基线 `student` 表保持原结构；如果课程检查要求物理列 `student.grade`，可改为在同一迁移中增加列，见待决策项。

### 2.4 事件与通知

- 事件类型保持 5 种，不新增枚举值。
- 邮件观察者只处理：
  - `REGISTERED`：直接报名通过；
  - `APPROVED`：审核批准；
  - `REJECTED`：审核拒绝。
- `SUBMITTED` 与 `CANCELLED` 由邮件观察者跳过，站内通知保持原规则。
- 幂等键沿用 `eventId + studentId`；邮件表唯一约束保证同一业务事件最多一份记录。
- 邮件故障在观察者内捕获并写入 `FAILED`，不向 `EventDispatcher` 抛出，从而保持 outbox 事务和报名结果稳定；错误同时写入日志。

### 2.5 测试关注点

- 年级规则：2/3 通过，1/4 拒绝；边界 1、4；`min > max`；年级缺失。
- 活动规则：直接活动可配置；审核活动配置被拒绝；活动不存在/已删除。
- 服务扩展：`@Primary` 子类覆盖旧入口；无规则活动完全沿用原流程；名额、开始时间、重复报名仍由原服务判定。
- 邮件观察者：三类事件生成记录；`SUBMITTED`/`CANCELLED` 跳过；发送失败记录；重复事件幂等；站内消息同时存在。
- 持久化：三个新表的约束；邮件唯一约束；迁移后演示数据。
- Web：管理页校验、演示页错误提示、邮件记录页展示。

## 3. 对现有设计的影响

### 3.1 保持稳定的抽象

- `RegistrationPolicy`：职责仍是“初始状态 + 是否需要审批”，年级资格不塞入该接口。
- `CapacityRule`：名额规则独立，年级检查之后继续复用。
- `Registration` 状态机：年级限定不改变 `PENDING/APPROVED/REJECTED/CANCELLED`。
- `PolicyType`：保持 `DIRECT/REVIEW` 两个值；年级限定直接报名复用 `DIRECT`，通过扩展表附加资格规则。
- `RegistrationEvent` / `OutboxEvent` / `EventDispatcher`：事件契约和 outbox 机制保持稳定。
- `RegistrationEventObserver`：邮件作为新观察者接入，属于既有扩展点。
- `InSiteNotificationService`、`MessageRepository`：原站内通知链路保持不变。

### 3.2 需要扩展的位置

- 报名入口：通过 `GradeAwareRegistrationServiceV2` 在旧入口前增加年级检查。
- 活动数据：通过 `activity_grade_rule_v2` 扩展，`Activity` 源文件保持原样。
- 学生数据：通过 `student_grade_v2` 扩展，`Student` 源文件保持原样。
- 管理端：新增年级规则与邮件记录页面，旧模板保持原样。
- 演示/验收：新增可选学生与活动的页面，解决旧页面固定学生 A 的限制。

### 3.3 OCP / DIP 判断

**满足 OCP 的部分**

- 邮件通知通过新增 `RegistrationEventObserver` 接入，`EventDispatcher` 和站内通知保持原样，是标准开闭原则扩展。
- 年级资格作为新规则对象，不修改 `CapacityRule` 与 `RegistrationPolicy`。
- 新数据库表、管理页、演示页均以新增方式提供。

**受约束的折中**

- 在不修改 `RegistrationService`、`PolicyType`、`RegistrationPolicyFactory` 的前提下，旧报名入口只能通过 `@Primary` 子类拦截。子类继承具体服务，属于“继承式扩展”，比理想的多态组合更耦合。
- 该子类依赖 `RegistrationService` 具体类，DIP 不如“新接口 + 新实现”纯粹；但新资格规则、仓库、观察者仍依赖接口。
- `@Primary` 是运行时装配覆盖，源码文件保持不变；需要在文档和验收中明确这一事实，避免被误认为修改了原服务。

**备选方案 B 的 DIP 更纯，但旧入口的年级约束需要额外处理**，详见第 6 节。

## 4. 潜在风险

### 4.1 数据迁移

- 新增 `student_grade_v2` 需要为已有学生回填年级；建议默认 1，再按演示数据设置 A=2、B=3，并补充大一/大四演示学生。
- 新表通过外键关联 `student`、`activity`；活动采用软删除，规则记录保留。
- 迁移名称需要满足 Flyway 解析；`V3__grade_and_email-v2.sql` 采用连字符描述，落地前用 `flyway validate` 验证。
- 回滚策略：新增表可保留；应用回退时旧代码忽略新表。迁移脚本采用纯新增，便于回退。

### 4.2 状态转换

- 年级检查发生在创建报名之前；拒绝时没有报名行和领域事件，状态机保持原样。
- 年级检查与后续名额检查分两次读取活动；规则变更或活动删除的并发窗口由原服务的事务和行锁兜底。
- 对已有有效报名的活动修改年级范围，可能造成“已报名学生不符合新规则”的历史不一致；建议在存在有效报名时拒绝修改，或明确只影响未来报名。

### 4.3 通知幂等与事务

- 邮件表唯一约束 `(source_event_id, student_id)` 是幂等底线；观察者发送前查询，重复事件直接跳过。
- 发送成功但记录写入前崩溃的窗口，可能造成重复发送；模拟邮件没有真实外部副作用，风险可控。若要求严格一次，需要“先占位 PENDING，再发送，再更新状态”的两阶段记录。
- 邮件观察者捕获发送异常并写 `FAILED`，不向 outbox 抛出；outbox 标记 `SENT`，邮件不自动重试。该行为满足“记录错误、报名不回滚”，但重试语义需要在验收中说明。
- 若邮件记录写入本身失败，异常会向上抛出，outbox 进入失败重试；站内消息随事务回滚后会在重试时重建，幂等约束仍有效。

### 4.4 回归范围

- `@Primary` 子类会影响所有注入 `RegistrationService` 的旧控制器；需要验证旧入口、审批入口、报名列表均正常。
- 原有 5 个测试类需要保持通过；新增测试覆盖年级和邮件。
- 旧模板固定学生 A；新演示页需要覆盖 B、C、D 年级场景。
- 统计、站内通知、outbox 重试、并发报名/审批均属于回归范围。

### 4.5 约束与评分风险

- Java 公共类文件名必须与类名一致，类名不能包含 `-`；文件名严格使用 `-v2` 与 Java 语法冲突。建议 Java 文件使用 `V2` 后缀，其他文件使用 `-v2`，需教师/用户确认。
- 旧页面导航无法加入新入口（修改旧模板会违反约束）；新页面通过直接 URL 访问，需确认验收方式。
- 旧活动详情页无法显示年级规则（修改旧模板会违反约束）；提示通过报名错误信息呈现，需确认是否满足“明确提示”。

## 5. 规模估算

| 项目 | 估算 |
|---|---|
| 新增 Java 类/接口 | 约 26—30 个（方案 A） |
| 修改现有核心业务类 | 0 个（源码保持不变；运行时由 `@Primary` 扩展接管） |
| 新增测试类 | 约 8—9 个 |
| 新增测试方法 | 约 26—32 个 |
| 新增数据库迁移 | 1 个（3 张新表） |
| 新增模板 | 3—4 个 |
| 新增 UML 源文件 | 4 个（用例、类图、年级报名时序、邮件通知时序） |
| 新增 UML 导出 | 8 个（4 张 × SVG/PNG） |
| 新增文档 | 本文件 `docs/13-change-impact-v2.md` |
| 预计总新增文件 | 约 40—45 个（方案 A） |

方案 B 采用 V2 纵向切片，预计新增 Java 类 35—40 个，模板与控制器更多。

## 6. 实现方案 A/B

### 方案 A：最小扩展 + 装饰器（推荐）

**做法**

1. 新增 `StudentGradeV2`、`ActivityGradeRuleV2` 及扩展表。
2. 新增 `GradeAwareRegistrationServiceV2`，标注 `@Primary` 并继承 `RegistrationService`；覆盖 `register`：
   - 查询活动年级规则；
   - 有规则时查询学生年级；
   - 不符合则抛出 `GradeRestrictionExceptionV2`；
   - 符合则调用 `super.register`，完整复用原名额、时间、重复报名和事件流程。
3. 新增年级规则管理页和演示页；旧页面入口保持不变。
4. 新增 `EmailNotificationObserverV2`，由 Spring 自动注入 `EventDispatcher` 的观察者列表；新增邮件表和查看页。
5. 旧站内通知、outbox、状态机、`PolicyType` 均保持原样。

**优点**

- 旧入口自动获得年级约束，覆盖 `ActivityController` 的报名路径。
- 原 `RegistrationService` 核心流程完整复用，回归风险较低。
- 新增文件少，交付快，适合当前“仅新增”约束。
- 旧页面、旧测试、旧 UML 均保持原样，便于对比。

**缺点**

- `@Primary` 子类依赖具体 `RegistrationService`，DIP 不够纯。
- 旧活动详情页不显示年级规则，提示只在报名失败时出现。
- 运行时装配被覆盖，需在文档中明确说明。
- 子类与父类方法签名强耦合，后续父类变化会影响子类。

### 方案 B：V2 纵向切片

**做法**

1. 新增 `/v2` 命名空间下的活动浏览、报名、管理控制器与服务；使用新的 `RegistrationUseCaseV2` 接口和实现。
2. 新流程显式组合年级规则、名额规则、策略和事件发布。
3. 新增独立的 V2 模板与页面，旧页面保持原样。
4. 邮件通知仍通过新观察者接入；或在新流程中显式调用通知端口。

**优点**

- 新流程接口清晰，DIP/OCP 更纯，便于讲解设计模式。
- 与基线完全隔离，便于对比和回退。
- 新页面可以完整展示年级规则和邮件记录。

**缺点**

- 旧入口仍可绕过年级规则；若要求全局生效，仍需给旧入口加 `@Primary` 装饰器，方案会退化为 A+B 混合。
- 代码量明显增加，活动浏览/报名/管理页面重复。
- 新旧两套流程共享同一数据库，容易出现行为分叉。
- 验收需要使用新 URL，旧导航保持不变，用户体验不连贯。

### 对比与建议

| 维度 | 方案 A | 方案 B |
|---|---|---|
| 旧入口年级约束 | 自动生效 | 需要额外装饰器或数据库触发器 |
| 代码量 | 较少 | 较多 |
| OCP/DIP 纯度 | 中等（继承式扩展） | 较高（新接口） |
| 回归风险 | 较低 | 中高（双流程） |
| 页面完整度 | 中（旧页不显示规则） | 高（新页完整） |
| 交付速度 | 快 | 慢 |

建议采用 **方案 A**，把“年级资格”作为独立规则对象，`@Primary` 只做薄装饰；这样既满足仅新增约束，又保持旧入口的全局约束。若课程评分更重视 DIP 纯度，可选择 B，但需要接受旧入口的额外防护或双流程成本。

## 7. 回归测试范围建议

以下按功能描述列出，不依赖隐藏验收编号。

### 7.1 既有自动化测试

- `HealthControllerTest`：健康检查返回正常。
- `HomeControllerTest`：首页视图正常。
- `DefaultCapacityRuleTest`：提交/审批名额规则。
- `RegistrationStateTest`：报名状态机转换。
- `ActivityTimeTest`：开始时间、软删除、容量下限。

### 7.2 既有功能回归

1. 活动创建：必填校验、容量、开始时间、策略类型。
2. 活动编辑：容量不低于已通过数、有效报名存在时策略类型限制、开始时间校验。
3. 活动软删除：存在待审核报名时拒绝；无待审核时删除后不可见。
4. 活动浏览与筛选：列表、关键字筛选、详情、剩余名额。
5. 直接报名：成功、满员拒绝、重复有效报名拒绝、已开始/已删除拒绝。
6. 审核报名：提交为待审核、满员时提交警告、批准成功、批准满额失败、拒绝、取消。
7. 站内通知：报名成功、审核通过、审核拒绝；审核提交/取消保持原规则；未读/已读。
8. outbox：事务内写入、定时派发、失败重试、重复派发幂等。
9. 统计：活动已通过/待审核/剩余名额。
10. 并发：报名与审批的名额一致性、状态更新条件。
11. 页面：首页、活动列表/详情、我的报名、通知、管理页、统计页。
12. 构建与启动：`mvn test`、打包、dev 配置启动、健康检查。

### 7.3 本次新增回归

- 年级 2/3 报名成功，1/4 拒绝且提示清晰。
- 年级规则与满员、已开始、重复报名叠加。
- 无年级规则时原直接/审核流程不变。
- 直接报名、审核批准、审核拒绝均产生站内 + 邮件记录。
- 邮件失败记录错误，报名状态保持，站内通知存在。
- 重复事件只产生一份邮件记录。
- 三个新表约束与迁移在空库、已有基线库上均通过。

## 8. 本轮待决策项与检查点

### 待决策项

1. **Java 文件命名**：Java 公共类不能含 `-`；拟用 `V2` 后缀（如 `GradeAwareRegistrationServiceV2.java`），其他文件用 `-v2`。请确认。
2. **实现方案**：选择方案 A（最小扩展 + 装饰器）或方案 B（V2 纵向切片）。
3. **学生年级存储**：采用独立 `student_grade_v2` 扩展表，或在 `student` 表增加 `grade` 列。
4. **年级规则变更**：存在有效报名时，拒绝修改年级范围，或允许修改并只影响未来报名。
5. **邮件收件人**：采用 `studentId@campus.example` 形式，或新增学生邮箱字段。
6. **邮件失败策略**：观察者捕获并记录 `FAILED`、outbox 标记成功；或抛出异常触发 outbox 重试。
7. **新页面入口**：新增页面通过直接 URL 访问，旧导航保持原样；确认验收方式。
8. **UML v2 范围**：仅更新受影响的 4 张图，或为全部 9 张图生成 v2 对照。
9. **迁移文件名**：`V3__grade_and_email-v2.sql` 是否符合课程对 Flyway 命名的要求。
10. **演示数据**：新增大一/大四演示学生，并将 A=2、B=3；确认是否接受。

### 建议检查点

- 确认 `baseline-v1` 标签或提交存在，v1 代码和 v1 UML 保持原样。
- 实现前用 `git status` 确认仅新增文件，无原文件修改。
- 先落数据库迁移和领域规则，再落服务扩展，最后接观察者与页面。
- 每次变更后运行既有 5 个测试类；新增测试覆盖年级边界和邮件失败。
- 验收时检查旧直接/审核流程、名额、时间、重复报名、站内通知均未回退。
- 检查邮件唯一约束、失败记录和日志证据。
- 导出 UML v2 时使用 UTF-8，保留 v1 导出图。
