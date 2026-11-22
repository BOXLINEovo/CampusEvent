# CampusEvent 初始功能实现（第9批）

> 状态：实现完成，待你确认后继续。**本轮未执行测试、未判定通过、未写测试实际结果。**
>
> 实现顺序：9.1 活动管理与查询筛选 → 9.2 报名/取消与重复约束 → 9.3 策略实现 → 9.4 审批 → 9.5 通知 → 9.6 统计。
>
> 接口契约依据：`docs/05-sequence-and-contracts.md`。**未修改第6批接口签名**。

---

## 1. 实现范围与需求映射

| 需求 | 实现落点 |
|---|---|
| P2-F01 活动管理 | `ActivityService`、`AdminActivityController`、`Activity`、`JpaActivityRepository`、活动表单/管理页 |
| P2-F02 活动查看/筛选 | `ActivityController`、`ActivityService.listVisible`、`JpaActivityRepository.findVisible`、活动列表/详情页 |
| P2-F03 报名/取消 | `RegistrationService.register/cancel`、`Registration` 状态机、`JpaRegistrationRepository` |
| P2-F04 报名策略 | `DirectRegistrationPolicy`、`ReviewRegistrationPolicy`、`RegistrationPolicyFactory` |
| P2-F05 审批 | `RegistrationService.approve/reject`、`AdminApprovalController`、`DefaultCapacityRule.checkApproval` |
| P2-F06 状态管理 | `Registration.approve/reject/cancel/isActive`、条件更新 `updateStatusIfCurrent` |
| P2-F07 站内通知 | `OutboxEventPublisher`、`EventDispatcher`、`InSiteNotificationService`、`Message` |
| P2-F08 统计 | `StatisticsService`、`ActivityStatistics`、`StatisticsController` 页面 |

业务规则落点：

- B.2-1 直接报名：`DirectRegistrationPolicy` + `CapacityRule.checkSubmission` + `REGISTERED` 事件。
- B.2-2 审核报名：`ReviewRegistrationPolicy` + `PENDING` + `SUBMITTED` 审计事件。
- B.2-3 名额：`RegistrationRepository.countApproved` + `CapacityRule.checkApproval`。
- B.2-4 重复报名：应用预检查 + `ux_registration_active` 部分唯一索引。
- B.2-5 取消/开始后限制：`Registration.cancel`、`Activity.hasStarted/isOpenForRegistration`。
- B.2-6 通知失败：outbox + 独立派发事务 + 重试/失败留痕。
- B.2-7 持久化/软删除：Flyway 表 + `Activity.deletedAt` + 待审核时拒绝软删除。

---

## 2. 新增文件

### 2.1 领域/应用

- `domain/exception/InvalidRegistrationStateException.java`
- `application/exception/`：`ActivityNotFoundException`、`ActivityNotOpenException`、`DuplicateActiveRegistrationException`、`CapacityExceededException`、`RegistrationNotFoundException`、`NotRegistrationOwnerException`、`ConcurrentStateChangeException`、`MessageNotFoundException`、`NotMessageOwnerException`、`DuplicateNotificationException`、`PolicyNotFoundException`、`ActivityNotDeletableException`、`InvalidActivityException`、`OutboxPersistenceException`
- `application/security/DemoAccounts.java`

### 2.2 持久化适配（D8-01=A：领域纯类 + infrastructure JPA）

- `infrastructure/persistence/jpa/entity/`：`ActivityJpaEntity`、`RegistrationJpaEntity`、`MessageJpaEntity`、`OutboxEventJpaEntity`
- `infrastructure/persistence/jpa/repository/`：`ActivityJpaRepository`、`RegistrationJpaRepository`、`MessageJpaRepository`、`OutboxEventJpaRepository`
- `infrastructure/persistence/jpa/mapper/`：`ActivityMapper`、`RegistrationMapper`、`MessageMapper`、`OutboxEventMapper`
- `infrastructure/persistence/jpa/adapter/`：`JpaActivityRepository`、`JpaRegistrationRepository`、`JpaMessageRepository`、`JpaOutboxRepository`

### 2.3 Web/页面

- `interfaces/web/ActivityController.java`
- `interfaces/web/RegistrationController.java`
- `interfaces/web/AdminActivityController.java`
- `interfaces/web/AdminApprovalController.java`
- `interfaces/web/NotificationController.java`
- `interfaces/web/StatisticsController.java`
- `interfaces/web/form/ActivityForm.java`
- `interfaces/web/view/ActivityView.java`、`RegistrationView.java`、`StatisticsView.java`、`ActivityViewAssembler.java`
- `templates/activities/list.html`、`detail.html`
- `templates/registrations/my.html`
- `templates/admin/activities/list.html`、`form.html`
- `templates/admin/approvals.html`
- `templates/admin/statistics.html`
- `templates/notifications/list.html`
- `static/css/app.css`（共享样式：居中内容列、宽表/筛选同宽 1040、表单页窄栏 760、统一字阶/按钮/提示/响应式）

---

## 3. 修改文件

| 文件 | 修改 |
|---|---|
| `CampusEventApplication.java` | 增加 `@EnableScheduling`（outbox 派发器） |
| `domain/model/Activity.java` | 实现 `isDeleted/hasStarted/isOpenForRegistration/canReduceCapacityTo`、`updateDetails`、`softDelete` |
| `domain/model/Registration.java` | 实现状态机 `approve/reject/cancel/isActive` + `create` 工厂 |
| `domain/model/Message.java` | 实现 `markRead` + `create` 工厂 |
| `domain/event/RegistrationEvent.java` | 增加 `registered/submitted/approved/rejected/cancelled` 静态工厂 |
| `domain/rule/CapacityDecision.java` | 增加 `allow/allowWithWarning/reject` 工厂 |
| `domain/rule/DefaultCapacityRule.java` | 实现提交/批准容量规则 |
| `domain/policy/DirectRegistrationPolicy.java` | 实现 `APPROVED`/直接通过 |
| `domain/policy/ReviewRegistrationPolicy.java` | 实现 `PENDING`/需要审批 |
| `application/policy/RegistrationPolicyFactory.java` | 实现按 `PolicyType` 选择 |
| `application/dto/ActivityStatistics.java` | record 改为带 getter 的 DTO（便于 Thymeleaf；非接口变更） |
| `application/service/ActivityService.java` | 实现活动 CRUD/软删除/筛选/编辑校验 |
| `application/service/RegistrationService.java` | 实现报名/取消/批准/拒绝 |
| `application/service/StatisticsService.java` | 实现 P2-F08 三项统计 |
| `infrastructure/outbox/OutboxEventPublisher.java` | 实现事件序列化 + outbox 写入（MANDATORY） |
| `infrastructure/outbox/EventDispatcher.java` | 实现调度、领取、观察者分发、重试/失败标记 |
| `infrastructure/notification/InSiteNotificationService.java` | 实现 3 类通知、幂等、拒绝理由 |
| `templates/index.html` | 增加学生/管理员入口；改用共享 CSS |
| `interfaces/web/ActivityController.java`、`AdminActivityController.java`、`StatisticsController.java`、`RegistrationController.java` | 使用 `ActivityViewAssembler` 去除重复的统计映射代码 |
| `pom.xml` | 精简 `spring-boot-starter-validation`、`aspectjweaver`、`tomcat-embed-websocket` 依赖；jar 约 44.47 MB |
| `templates/activities/*.html`、`templates/registrations/my.html`、`templates/admin/*.html`、`templates/notifications/list.html` | 移除内联 style，改用 `app.css`，统一导航/表格/表单/提示样式 |

> 未修改：`RegistrationService`/`RegistrationPolicy`/`NotificationService`/仓储端口的方法签名；未新增端口方法。`ActivityStatistics` 从 record 改为 class 仅为模板可读性，属于 DTO 实现调整。

---

## 4. 模块实现说明

### 9.1 活动管理与查询筛选

- 创建：校验标题/类型/开始时间/地点/容量/报名方式；`startAt` 不得早于当前；容量 > 0。
- 编辑：
  - 容量不得低于当前“已通过”人数。
  - `policyType` 在有**有效报名**（待审核/已通过）时不可改。
  - `startAt` 不得早于当前。
- 软删除：存在待审核报名时拒绝；否则设置 `deletedAt`。
- 编辑/软删除：**D10-02：先用 `findByIdForUpdate` 锁活动行**，再检查容量/策略/待审核，避免与报名/审批并发冲突。
- 浏览/筛选：`findVisible` 排除软删除，按名称子串匹配，按 `startAt` 升序。
- 页面：`/activities`、`/activities/{id}`、`/admin/activities`、`/admin/activities/new`、`/admin/activities/{id}/edit`。

### 9.2 报名/取消与重复约束

- 报名前检查：活动存在/未删除/未开始、无“待审核/已通过”记录。
- **D10-02 并发修复**：报名事务先用 `ActivityRepository.findByIdForUpdate` 锁活动行，再执行 `countApproved` 与插入；同一活动的并发报名串行化。
- 插入后由数据库部分唯一索引 `ux_registration_active` 兜底；冲突翻译为 `DuplicateActiveRegistrationException`。
- 取消：校验本人、状态 `PENDING/APPROVED`；条件更新为 `CANCELLED`；开始后仍允许。
- 取消已通过：统计通过 `countApproved` 减少释放名额，不维护独立计数列。

### 9.3 策略实现

- `DirectRegistrationPolicy`：初始 `APPROVED`，直接通过。
- `ReviewRegistrationPolicy`：初始 `PENDING`，需要审批。
- `DefaultCapacityRule.checkSubmission`：
  - 未满 → 允许；
  - 满员 + `APPROVED` → 拒绝；
  - 满员 + `PENDING` → 允许 + “已满员，批准可能失败”。
- `DefaultCapacityRule.checkApproval`：满员 → 拒绝。

### 9.4 审批批准/拒绝

- 批准：报名必须 `PENDING`；活动未删除/未开始；**D10-02：先用 `findByIdForUpdate` 锁活动行**；`countApproved` 后 `checkApproval` 二次检查；条件更新 `PENDING→APPROVED`；发布 `APPROVED` 事件。
- 满额：抛 `CapacityExceededException`，不更新状态、不写事件；报名保持 `PENDING`。
- 拒绝：报名必须 `PENDING`；不查名额；开始后仍允许；条件更新 `PENDING→REJECTED`；发布 `REJECTED` 事件（带理由）。
- 条件更新返回 false → `ConcurrentStateChangeException`。

### 9.5 站内通知

- `RegistrationService` 只调用 `RegistrationEventPublisher`；`OutboxEventPublisher` 在业务事务内写 `outbox_event`。
- `EventDispatcher` 每 5 秒调度：领取 `PENDING`、反序列化、调用观察者、标记 `SENT/FAILED`；每个事件独立事务。
- `InSiteNotificationService` 只处理 `REGISTERED/APPROVED/REJECTED`；`SUBMITTED/CANCELLED` 跳过。
- 幂等：`Message` 唯一键 `(sourceEventId, studentId)` + `existsBySourceEventId`；重复视为成功。
- 失败：`markFailed` 增加 retry_count，1m/5m/15m/1h 退避；5 次后 `FAILED` 留痕。
- 崩溃恢复：`PROCESSING` 超过 5 分钟自动回收为 `PENDING`，避免 worker 崩溃后事件永久卡住。

### 9.6 统计

- 按活动返回：`approvedCount`、`pendingCount`、`remainingCapacity = max(0, capacity - approvedCount)`。
- 页面：`/admin/statistics`。
- 仅管理员页面；当前没有真实权限系统（固定演示账号）。

---

## 5. 异常与边界处理

| 场景 | 当前处理 |
|---|---|
| 重复有效报名 | 预检查 + DB 部分唯一索引；冲突翻译为 `DuplicateActiveRegistrationException` |
| 直接报名满员 | `CapacityRule` 返回拒绝 → `CapacityExceededException`，事务回滚 |
| 审核报名满员 | 允许提交 `PENDING` + warning；不占名额 |
| 批准时满额 | `CapacityExceededException`，不更新状态、不写事件；保持 `PENDING` |
| 非法状态转换 | `Registration.approve/reject/cancel` 抛 `InvalidRegistrationStateException` |
| 并发状态变化 | `updateStatusIfCurrent` 0 行 → `ConcurrentStateChangeException` |
| 取消后统计 | 统计每次实时 `countApproved`/`findPendingByActivity`，取消后自然变化 |
| 取消已通过 | 状态 `CANCELLED`，`countApproved` 减少，剩余名额增加 |
| 活动开始后 | 报名/批准拒绝；取消/拒绝仍允许 |
| 软删除有待审核 | `ActivityNotDeletableException` |
| 通知重复 | `Message` 唯一键 + 预检查；重复视为成功 |
| 通知失败 | outbox 保留/重试，不回滚报名/审批 |
| 活动标题缺失 | 拒绝/取消事件使用兜底标题“活动” |

> 已知实现口径（D9-01）：`policyType` 只在存在“待审核/已通过”有效报名时不可改；仅有已拒绝/已取消历史时不阻止修改。若要求“任何报名记录都不可改”，需要给 `RegistrationRepository` 增加按活动计数的方法（接口扩展，待你确认）。

---

## 6. 验收关注点（不判定通过）

建议你在验收时重点看：

1. 直接报名：满员、重复、活动已开始、取消后再次报名。
2. 审核报名：满员时提交是否允许并带 warning；待审核不占名额。
3. 批准：满额时是否保持 `PENDING`、无事件、无通知；并发批准最后名额。
4. 拒绝：开始后仍可拒绝；理由是否进入站内消息。
5. 取消：本人/非本人；已通过取消后统计剩余名额是否增加。
6. 通知：直接/批准/拒绝是否有消息；审核提交/取消是否不产生消息；重复派发是否不重复插入。
7. 通知失败：outbox 是否留痕/重试，报名是否不受影响。
8. 活动编辑：容量低于已通过数、有有效报名时改 policyType、软删除待审核活动。
9. 统计：三项指标是否与报名状态一致；软删除活动是否排除。
10. 页面：表单校验错误是否可读；空列表/满员/已开始提示是否清晰。

---

## 7. 已确认决策与检查点

> 确认日期：2026-11-21。`D9-01`～`D9-05` 均按“默认”确认。

### 已确认

| 编号 | 事项 | 结论 |
|---|---|---|
| **D9-01** | `policyType` 不可改范围 | **仅有效报名（待审核/已通过）**；历史已拒绝/已取消不阻止修改 |
| **D9-02** | 固定演示身份 | **学生固定 A；管理员路由开放**；无真实登录/权限 |
| **D9-03** | outbox 派发延迟 | **5 秒** |
| **D9-04** | 页面范围 | **当前最小页面**（列表/详情/报名/审批/通知/统计） |
| **D9-05** | 接口 | **沿用第6批接口签名**；D9-01 不采用严格方案，因此沿用现有接口 `RegistrationRepository` |

### 检查点

- [x] 9.1～9.6 的代码覆盖本批范围。
- [x] D9-01 有效报名口径已确认；不扩展接口。
- [x] D9-02 固定演示身份已确认。
- [x] 页面/表单覆盖最小操作。
- [x] 继续保持“不执行测试、不判定通过”的本批约束。
- [ ] 等你发布下一批任务；本轮未执行 Git 操作。

---

## 8. 说明

- 本轮**未执行测试**，未给出通过/失败结论；第6节仅为验收关注点。
- 未执行 Git 操作。
- 未实现候补、签到、退款、管理员站内消息、真实权限系统。
- 通知失败/重试、并发、软删除等边界已在代码中处理，但需你按第6节自行验收。
