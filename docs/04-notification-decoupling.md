# CampusEvent 通知解耦设计（第5批）

> 状态：已确认 `D5-02=C`，其余 `D5-01`、`D5-03`～`D5-08` 按推荐落地；类图已同步；等待第6批。
>
> **与已确认决策的关系**：
> - `M-06=A`：只对“直接报名成功、批准、拒绝”发站内通知；审核提交、取消不发通知。
> - `M-07=B`：同库 outbox + 观察者/事件分发，使用 outbox + 观察者。
> - `D4-01=C`：名额检查为公共规则 + 策略初始状态；与本批通知设计无直接耦合。
> - `D5-02=C`：outbox + 事务后派发；派发失败不回滚报名。
>
> 本批已按 C 落地，并同步 `RegistrationEventType`（5 值）、`NotificationService`（接口 + `InSiteNotificationService`）、`Message` 幂等键与 `OutboxEvent` 字段。

---

## 1. 结构方案：领域事件分发（outbox + dispatcher）

### 1.1 选择

**选择：领域事件分发（domain event dispatch）**，结构为：

```text
RegistrationService
  └─ 业务事务：保存报名/审批结果 + 写 OutboxEvent
        └─ 提交后：EventDispatcher 读取 PENDING 事件
              └─ 调用订阅者（观察者接口）
                    └─ NotificationService 写 Message
```

- **领域事件**：`RegistrationEvent` 表示“已经发生的报名事实”，如 `DirectRegistrationSucceeded`、`RegistrationApproved`。
- **outbox**：事件与业务数据在同一个数据库事务中写入；提交后由 `EventDispatcher` 派发。
- **订阅端**：`NotificationService` 实现 `RegistrationEventObserver`，只负责把事件转成站内消息。
- **无消息队列**：outbox 表就是持久化事件缓冲；派发器是进程内组件，可由请求后钩子或定时任务触发。

### 1.2 为什么不选纯观察者

纯观察者（Subject 直接持有 Observer 列表并同步调用）更简单，但存在三个问题：

1. **没有持久化**：进程崩溃或观察者抛异常时，事件丢失，报名已经提交但通知无声消失，和 B.2-6 冲突。
2. **没有重试/留痕**：失败只能靠调用方捕获；无法区分“通知失败”与“报名失败”。
3. **耦合调用时机**：Subject 必须知道何时通知、通知失败是否回滚；注册服务会被通知细节污染。

领域事件分发把“事实记录”和“通知投递”分开：报名事务只负责记录事件，通知失败由派发器重试/留痕。**观察者模式仍用于派发器内部**（订阅者实现 `RegistrationEventObserver`），但整体结构是领域事件分发，不是纯观察者。

### 1.3 组件职责

| 组件 | 职责 | 不负责 |
|---|---|---|
| `RegistrationService` | 业务状态变更；在同一事务写 outbox 事件 | 不直接调用 `NotificationService`；不写 `Message` |
| `RegistrationEventPublisher`（端口） | 发布事件抽象 | 不关心订阅者 |
| `OutboxEventPublisher` | 把事件写入 `OutboxEvent`，与业务同事务 | 不直接发消息、不调用观察者 |
| `EventDispatcher` | 读取 PENDING 事件；调用订阅者；标记 SENT/FAILED；重试 | 不做报名业务判断 |
| `RegistrationEventObserver`（端口） | 订阅者契约 | 不关心 outbox |
| `NotificationService` / `InSiteNotificationService` | 消费事件，幂等写 `Message`；查询/标记已读 | 不改报名状态；不发布事件 |
| `OutboxRepository` | outbox 持久化/状态更新 | 不解释事件语义 |
| `MessageRepository` | 消息持久化/查询 | 不解释报名规则 |

---

## 2. 事件定义

### 2.1 事件清单

| 事件类 | `eventType` | 触发时机 | 状态变化 | 是否发通知 | 说明 |
|---|---|---|---|---|---|
| `DirectRegistrationSucceeded` | `REGISTERED` | 直接型报名插入成功 | 无 → 已通过 | **是** | M-06=A；通知“报名成功” |
| `ReviewRegistrationSubmitted` | `SUBMITTED` | 审核型报名插入成功 | 无 → 待审核 | **否** | M-06=A；可作为审计事件，不产生消息 |
| `RegistrationApproved` | `APPROVED` | 管理员批准成功 | 待审核 → 已通过 | **是** | 通知“报名已通过” |
| `RegistrationRejected` | `REJECTED` | 管理员拒绝成功 | 待审核 → 已拒绝 | **是** | 通知“报名未通过”，可带理由 |
| `RegistrationCancelled` | `CANCELLED` | 学生取消成功 | 待审核/已通过 → 已取消 | **否** | M-06=A；可作为审计事件，不产生消息 |

> **已确认 `D5-03`：5 个都写**；后两个（`SUBMITTED`/`CANCELLED`）仅审计/扩展，通知处理器不订阅。这样事件流完整，未来加通知不用改业务。

### 2.2 载荷字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `eventId` | String (UUID/ULID) | 事件唯一 ID；outbox/消息幂等的根 |
| `eventType` | enum | 上表 5 种 |
| `schemaVersion` | int | 事件结构版本，V1=1，便于未来演进 |
| `registrationId` | String | 报名记录 ID |
| `activityId` | String | 活动 ID |
| `studentId` | String | 学生 ID |
| `policyType` | enum | `DIRECT`/`REVIEW`；消息内容/排查用 |
| `fromStatus` | enum? | 变更前状态；新建时为 null |
| `toStatus` | enum | 变更后状态 |
| `actorId` | String | 操作者 ID（学生或管理员） |
| `actorRole` | enum | `STUDENT`/`ADMIN` |
| `reason` | String? | 拒绝理由；仅拒绝事件 |
| `activityTitle` | String | 活动标题快照；消息在活动软删除后仍可读 |
| `studentName` | String | 学生姓名快照；消息内容稳定 |
| `occurredAt` | DateTime | 业务发生时间 |

> `activityTitle`/`studentName` 用快照而不是派发时再查库：消息是历史记录，活动软删除或改名后仍应保留当时语义；也避免 `NotificationService` 依赖 `ActivityRepository`。若你更希望“显示最新标题”，可改为派发时查询，但会增加依赖和失败点。

### 2.3 触发时机

| 场景 | 事务内动作 | 事件 | 事务后 |
|---|---|---|---|
| 直接报名 | 插入 `Registration(APPROVED)` + 写 outbox | `DirectRegistrationSucceeded` | 派发 → 通知 |
| 审核报名 | 插入 `Registration(PENDING)` + 写 outbox（若选 5 事件） | `ReviewRegistrationSubmitted` | 派发 → 无通知/仅审计 |
| 批准 | 条件更新 `PENDING → APPROVED` + 写 outbox | `RegistrationApproved` | 派发 → 通知 |
| 拒绝 | 条件更新 `PENDING → REJECTED` + 写 outbox | `RegistrationRejected` | 派发 → 通知 |
| 取消 | 条件更新 `PENDING/APPROVED → CANCELLED` + 写 outbox（若选 5 事件） | `RegistrationCancelled` | 派发 → 无通知/仅审计 |

**关键**：事件只在状态变更**实际发生**时写入；条件更新 0 行、插入冲突、事务回滚都不产生事件。这样“状态变更只发一次”与幂等从源头成立。

### 2.4 通知触发矩阵

| 事件 | 接收人 | 是否通知 | 消息类型 |
|---|---|---|---|
| `REGISTERED` | 学生 | 是 | `REGISTRATION_SUCCESS` |
| `SUBMITTED` | 学生 | 否 | — |
| `APPROVED` | 学生 | 是 | `REGISTRATION_APPROVED` |
| `REJECTED` | 学生 | 是 | `REGISTRATION_REJECTED` |
| `CANCELLED` | 学生 | 否 | — |
| 新待审核报名 | 管理员 | 否 | —（管理员用待审核列表，不建管理员消息） |

---

## 3. 通知失败是否影响报名事务

### 3.1 方案 A：同步同事务

```text
BEGIN
  写报名/审批结果
  写 Message
COMMIT
```

- **一致性**：强一致；要么报名和消息都成功，要么都失败。
- **失败影响**：`Message` 写入失败会导致整个事务回滚，报名不落库。
- **优点**：实现最简单；不会出现“报名成功但消息缺失”。
- **缺点**：
  - 通知表故障会阻塞报名，通知成为报名的硬依赖；
  - 违反“通知失败不应让已合法提交的报名消失”的意图（虽然会报错，不是“无声”，但业务被通知拖累）；
  - 消息写入与报名写入共享事务，锁竞争/表膨胀会影响报名吞吐。
- **结论**：不推荐。

### 3.2 方案 B：同步但独立事务/失败记录

```text
BEGIN
  写报名/审批结果
COMMIT
  BEGIN
    写 Message
  COMMIT
  失败 -> 写失败日志（独立事务）
```

- **一致性**：报名先提交；消息可能成功、失败或丢失。
- **失败影响**：通知失败不影响报名；失败日志留痕。
- **优点**：比 A 简单，报名不被消息表阻塞；通知通常立即可见。
- **缺点**：
  - 报名提交后、消息写入前进程崩溃：既没有消息也没有失败日志，存在**静默丢失窗口**；
  - 失败日志本身也可能写失败；
  - 没有自动重试；需要额外的补偿任务，实际上会向 C 演化。
- **结论**：可作为“通知是尽力而为”的简化方案，但不满足 B.2-6 的可靠性要求。

### 3.3 方案 C：事务后事件（outbox + dispatcher）

```text
BEGIN
  写报名/审批结果
  写 OutboxEvent(PENDING)
COMMIT
  派发器读取 PENDING
  调用 NotificationService
  成功 -> SENT
  失败 -> FAILED/重试；不影响已提交的报名
```

- **一致性**：报名与事件原子提交；消息最终一致（at-least-once + 幂等）。
- **失败影响**：通知**投递**失败不回滚报名；outbox 记录 `FAILED`/`retry_count`/`last_error`，可重试/排查。
- **优点**：
  - 满足 B.2-6：报名不会因通知失败消失，失败有痕迹；
  - 支持重试、幂等、审计；
  - 无消息队列，outbox 表 + 进程内派发器即可；
  - 与已确认 `M-07=B` 一致。
- **缺点**：
  - 多一张 outbox 表、派发器和状态管理；
  - 通知是最终一致，可能延迟；
  - outbox 写入与业务同事务：如果 outbox 写入失败，业务事务回滚并报错。这是**持久化失败**，不是“通知投递失败”；用户会看到失败并重试，不会出现“报名成功但事件丢失”。
- **结论：已确认 C**。可在事务提交后立即尝试一次派发（低延迟），失败再交给重试任务。

### 3.4 对比与结论

| 维度 | A 同事务 | B 独立事务/失败记录 | C outbox + 事务后 |
|---|---|---|---|
| 报名是否被通知失败阻塞 | 是 | 否 | 否（投递失败）；outbox 写入失败会回滚事务 |
| 静默丢失窗口 | 无 | 有（提交后崩溃） | 无（事件已持久化） |
| 重试 | 无 | 需另做 | 内建 |
| 失败留痕 | 事务回滚，无报名 | 失败日志（可能丢失） | outbox `FAILED` + `last_error` |
| 一致性 | 强一致 | 最终一致但无保证 | 最终一致、可保证 |
| 复杂度 | 低 | 中 | 中高 |
| 与 M-07=B | 不一致 | 不一致 | 一致 |

**已确认：C**。如果课程更强调“最小实现”，B 可作为简化版，但必须说明静默丢失窗口；A 不建议。

### 3.5 事务边界说明

- **业务事务**：报名/审批状态变更 + outbox 事件写入。二者必须同事务；否则会出现“报名成功但事件丢失”或“事件存在但报名未成功”。
- **派发事务**：派发器读取事件、调用 `NotificationService`、写 `Message`、更新 outbox 状态。这个事务**不包含**报名业务；失败不回滚报名。
- **outbox 写入失败**：属于业务事务失败，报名回滚并报错；这不是“通知投递失败”，而是事件持久化失败。若你要求“即使 outbox 写失败也必须保留报名”，那就需要 B 的独立事务，但会失去事件原子性；本设计不推荐。

---

## 4. 幂等设计

### 4.1 事件 ID

- 事件创建时生成 `eventId`（UUID/ULID），全局唯一。
- `OutboxEvent.event_id` 建唯一索引；同一事件不会被重复插入。
- 事件 ID 是消息幂等的根：重试/重复派发都使用同一个 `eventId`。

### 4.2 通知唯一键

- `Message` 增加 `source_event_id`。
- 唯一约束：`UNIQUE(source_event_id, student_id)`（当前一个事件只发给一个学生；加 `student_id` 为未来多接收人留余地）。
- `NotificationService.onEvent` 先查 `existsBySourceEventId(eventId)`；存在则直接返回（视为已处理）；数据库唯一约束是最终防线。

### 4.3 状态变更只发一次

- 直接报名：唯一有效报名约束 + 插入成功才写事件；重复请求不会产生第二条报名/事件。
- 批准/拒绝：`UPDATE registration SET status=? WHERE id=? AND status='PENDING'`；只有受影响行数=1 才写事件。
- 取消：`UPDATE ... WHERE id=? AND status IN ('PENDING','APPROVED')`；只有实际变更才写事件。
- 事件写入与状态变更同事务；事务回滚则事件不存在。
- 同一请求重试：状态已变更，条件更新 0 行，不再写事件。

### 4.4 重试与重复派发

- 派发器采用 **at-least-once**：可能重复投递，但消息唯一键保证不会重复插入。
- 事件状态：`PENDING → PROCESSING → SENT`；失败 `FAILED` 可重试；记录 `retry_count`、`next_attempt_at`、`last_error`。
- 处理流程：
  1. 原子领取：`UPDATE outbox SET status='PROCESSING' WHERE event_id=? AND status='PENDING'`（或 `SELECT ... FOR UPDATE SKIP LOCKED`）。
  2. 调用 `NotificationService.onEvent`。
  3. 成功/重复：`SENT`；异常：`FAILED` + `retry_count+1` + `next_attempt_at`。
- 已确认 `D5-05`：重试上限 5 次、指数退避（1m/5m/15m/1h）；超过上限标记 `FAILED` 并留日志，后续人工/管理端处理。

---

## 5. NotificationService 接口与站内消息模型

### 5.1 接口

```text
interface RegistrationEventObserver {
    void onEvent(RegistrationEvent event)
}

interface NotificationService extends RegistrationEventObserver {
    Message[] listByStudent(studentId: String)
    void markRead(messageId: String, studentId: String)
    int unreadCount(studentId: String)
}
```

- `onEvent`：
  - 成功写消息 → 正常返回；
  - 重复事件 → 正常返回（幂等，不抛异常）；
  - 非通知事件（`SUBMITTED`/`CANCELLED`）→ 正常返回（跳过）；
  - 基础设施失败（数据库不可用等）→ 抛 `NotificationDeliveryException`，由派发器标记 `FAILED`/重试。
- `listByStudent`：按 `created_at` 倒序；只返回本人消息。
- `markRead`：必须带 `studentId`，防止越权；只标记本人消息。
- `unreadCount`：未读数，供列表徽标。

> 实现类建议命名为 `InSiteNotificationService`，依赖 `MessageRepository`；`NotificationService` 是接口/端口，便于替换和测试。
>
> `MessageRepository` 需补 `countUnread(studentId)`（供 `unreadCount` 使用）；类图同步时补上。

### 5.2 站内消息模型

| 字段 | 类型 | 说明 |
|---|---|---|
| `messageId` | String | 主键 |
| `studentId` | String | 接收人 |
| `activityId` | String | 关联活动（当前必填） |
| `sourceEventId` | String | 来源事件 ID；幂等键 |
| `type` | enum | `REGISTRATION_SUCCESS` / `REGISTRATION_APPROVED` / `REGISTRATION_REJECTED` |
| `title` | String | 标题，如“报名成功” |
| `content` | String | 正文，含活动标题/理由 |
| `createdAt` | DateTime | 创建时间 |
| `isRead` | boolean | 默认 false |
| `readAt` | DateTime? | 已读时间 |

- 唯一约束：`UNIQUE(source_event_id, student_id)`。
- 索引：`(student_id, created_at)` 用于列表；`(student_id, is_read)` 用于未读数。
- 不建议把消息内容做成可编辑模板表；V1 用代码内的固定模板即可，模板表是后续扩展。

### 5.3 内容模板建议

| 类型 | 标题 | 正文 |
|---|---|---|
| `REGISTRATION_SUCCESS` | 报名成功 | 你已成功报名《{activityTitle}》。 |
| `REGISTRATION_APPROVED` | 报名已通过 | 你报名的《{activityTitle}》已通过审核。 |
| `REGISTRATION_REJECTED` | 报名未通过 | 你报名的《{activityTitle}》未通过审核。{reason ? "原因：" + reason : ""} |

> 文案可按你的要求调整；本轮只定结构，不做最终 UI 文案。

### 5.4 已读/查询

- 点击消息详情才标记已读；列表显示未读数（D-06）。
- `listByStudent` 只返回本人；`markRead` 校验 `studentId`；越权返回拒绝。
- 软删除活动：消息仍显示，活动入口置灰（沿用第2批规则）。

---

## 6. 与 RegistrationService 的依赖方向（DIP）

### 6.1 依赖图

```text
RegistrationService
   │  depends on
   ▼
RegistrationEventPublisher (interface)
   ▲
   │ implements
OutboxEventPublisher ──► OutboxRepository (interface)

EventDispatcher ──► OutboxRepository (interface)
EventDispatcher ──► RegistrationEventObserver (interface)
                          ▲
                          │ implements
                 NotificationService (interface)
                          │
                          ▼
                 MessageRepository (interface)
```

- `RegistrationService` **只**依赖 `RegistrationEventPublisher`，不知道 `NotificationService`、`Message`、`MessageRepository`。
- `NotificationService` 只知道 `RegistrationEvent` 和 `MessageRepository`，不知道 `RegistrationService`/`RegistrationRepository`。
- `EventDispatcher` 是装配/派发组件，依赖 outbox 和观察者抽象。
- `RegistrationEvent` 是共享契约；双方都依赖它，但它不依赖任何服务。

### 6.2 为什么满足 DIP

- 高层策略（报名/审批）依赖事件发布抽象，不依赖通知实现。
- 低层通知实现依赖消息仓储抽象，不依赖报名服务。
- 具体实现（`OutboxEventPublisher`、`InSiteNotificationService`、数据库仓储）在组合根装配；业务代码不 `new` 具体类。
- 替换通知渠道（站内 → 邮件/短信）只需新增 `RegistrationEventObserver` 实现；`RegistrationService` 不改。
- 替换 outbox 存储（同库表 → 其他持久化）只需换 `OutboxRepository` 实现；业务服务不改。

### 6.3 组合根

- 应用启动时装配：`RegistrationService(policyFactory, capacityRule, eventPublisher=OutboxEventPublisher, ...)`。
- `EventDispatcher(outboxRepository, observers=[InSiteNotificationService])`。
- 业务服务不通过服务定位器拿 `NotificationService`；依赖在构造时注入。

---

## 7. 与已确认决策的对齐

| 事项 | 已确认状态 | 本批结论 | 同步状态 |
|---|---|---|---|
| 通知事务 | `M-07=B` outbox + 观察者 | `D5-02=C`（outbox + 事务后派发） | 一致；已同步 |
| 通知事件 | 3 个（直接/批准/拒绝） | `D5-03=5 个领域事件`（2 个仅审计） | 已同步 |
| `RegistrationEventType` | 类图原 3 个值 | 增加 `SUBMITTED`/`CANCELLED` | 已同步类图 |
| `NotificationService` | 类图中原具体类 | 接口 + `InSiteNotificationService` | 已同步类图 |
| 取消/审核提交通知 | `M-06=A` 不发 | 保持不发 | 无变化 |
| 通知失败 | B.2-6 要留痕 | outbox `FAILED` + `lastError` | 已同步 |

> `class-diagram-v1.puml` 与 `02-class-diagram-v1.md` 已按本批结论更新；类图 `-checkonly` 通过。

---

## 8. 已确认决策与后续检查点

### 8.1 已确认

| 编号 | 事项 | 结论 | 落地 |
|---|---|---|---|
| **D5-01** | 解耦结构 | **领域事件分发（outbox + dispatcher）** | `RegistrationService` 只依赖 `RegistrationEventPublisher` |
| **D5-02** | 通知事务 | **C（outbox + 事务后派发）** | 派发失败不回滚报名；见 3.3/4.4 |
| **D5-03** | 事件范围 | **5 个领域事件（3 通知 + 2 审计）** | `SUBMITTED/CANCELLED` 不发通知 |
| **D5-04** | 幂等键 | **`eventId` + `UNIQUE(source_event_id, student_id)`** | `Message` 唯一键 + `existsBySourceEventId` |
| **D5-05** | 重试策略 | **5 次、指数退避（1m/5m/15m/1h）** | 超过上限 `FAILED` + `lastError` |
| **D5-06** | `onEvent` 失败语义 | **抛异常** | 派发器标记失败/重试；重复/跳过正常返回 |
| **D5-07** | 消息模型 | **`sourceEventId/type/title/content/isRead/readAt`** | 唯一键 `(sourceEventId, studentId)` |
| **D5-08** | 事件载荷快照 | **`activityTitle`/`studentName` 快照** | 活动删除/改名后消息仍可读 |

### 8.2 后续检查点

- [x] `RegistrationService` 只依赖 `RegistrationEventPublisher`，没有 `NotificationService`/`MessageRepository` 依赖。
- [x] 事件只在状态变更实际发生时写入；条件更新/唯一约束覆盖重复。
- [x] outbox 写入与业务同事务；派发失败不回滚报名。
- [x] `Message` 有 `(sourceEventId, studentId)` 唯一键；重复派发不会重复插入。
- [x] `NotificationService` 只处理 3 类通知事件；`SUBMITTED`/`CANCELLED` 跳过。
- [x] 通知查询/已读校验 `studentId`，避免越权。
- [x] 类图已同步事件枚举、`NotificationService` 接口、`InSiteNotificationService`、`Message` 字段。
- [ ] 第6批任务（由你发布）；在第6批前不写代码。
- [ ] 后续：报名/审批时序图、设计原则说明、评审检查表与签名。
- [ ] 后续批次：技术栈/数据库/部署；教师 CR 后再变更。

---

## 9. 本轮文件变更

| 文件 | 操作 | 说明 |
|---|---|---|
| `实验/CampusEvent/docs/04-notification-decoupling.md` | 新增/修订 | 本文：结构选择、事件定义、事务方案、幂等、接口/消息模型、DIP |
| `实验/CampusEvent/docs/02-class-diagram-v1.md` | 修订 | 同步 5 事件、`NotificationService` 接口、`InSiteNotificationService`、`Message/OutboxEvent` 字段 |
| `实验/CampusEvent/docs/diagrams/class-diagram-v1.puml` | 修订 | 类图同步 D5-02=C；`-checkonly` 通过 |

> 本轮未执行 Git 操作；未生成业务代码、数据库脚本或部署文档。

---

## 10. 第5批收口

`D5-02=C` 与其余推荐项已确认并落入文档/类图。第5批收口，等待你发布**第6批**任务；在第6批前，我不写代码、不生成实现文档。
