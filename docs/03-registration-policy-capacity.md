# CampusEvent 报名策略设计与名额检查归属论证（第4批）

> 状态：已确认 `D4-01=C`（本轮定义）；`D4-02`～`D4-06` 按建议落地；类图已同步；等待第5批通知设计。本轮只做策略/规则设计与论证，不写代码。
>
> **命名冲突的历史说明**：
> - 第3批你确认的 `D3-01=C` 是**混合方案 C'**：`RegistrationService.ensureNotOverCapacity(...)`（批准硬不变量）+ `RegistrationPolicy.preCheck(...)`（提交预检查）。
> - 第4批任务把 `C` 定义为**公共规则 + 策略只决定初始状态**，没有 `preCheck`。
> - 第4批已确认选择**本轮 C**；`docs/02-class-diagram-v1.md` 与 `class-diagram-v1.puml` 已移除 `preCheck`，改为 `CapacityRule`/`DefaultCapacityRule`/`CapacityDecision`。C' 仅作为备选方案保留在第4.2/4.3节，供设计报告对比。

---

## 1. 已确认前提

| 事项 | 已确认结论 |
|---|---|
| 报名方式 | 直接型、审核型两种；由 `Activity.policyType` 决定 |
| 直接型 | 报名即“已通过”；满员拒绝（B.2-1/B.2-3、C4） |
| 审核型 | 报名进入“待审核”；满员仍允许提交并提示（M-04=A） |
| 批准 | 批准时二次检查名额；名额不足保持待审核（C8、M-05=A） |
| 名额口径 | 只统计“已通过”；待审核不占名额 |
| 重复报名 | 同一学生同一活动最多一条“待审核/已通过” |
| 通知 | 只在直接通过、批准、拒绝时写站内消息（M-06=A）；本轮不展开通知设计 |
| 状态 | 待审核、已通过、已拒绝、已取消；本轮不新增状态 |

---

## 2. RegistrationPolicy 稳定抽象建议

### 2.1 核心接口（方案 A / 本轮 C 共用）

```text
interface RegistrationPolicy {
    RegistrationStatus initialStatus(Activity activity)
    boolean requiresApproval()
}
```

| 项 | 约定 |
|---|---|
| 输入 | `Activity`（只读）；策略不应查询仓储，也不应修改活动 |
| 输出 | `initialStatus`：`APPROVED` 或 `PENDING`；`requiresApproval`：是否需要管理员审批 |
| 纯函数 | 相同输入返回相同结果；不依赖时间、数据库、随机数 |
| 异常 | `IllegalArgumentException`：`activity == null`；`PolicyNotFoundException`：工厂无法解析 `policyType`（由工厂抛，不由策略抛） |
| 不应抛 | 满员、重复报名等业务结果不应由策略抛异常；这些由公共规则/服务以返回值或领域异常处理 |

> 为什么输入是 `Activity` 而不是无参：给未来策略留出读取活动属性的空间（例如按类别决定是否审核），但当前两个实现都只返回固定状态。若未来需要学生/时间上下文，再引入 `RegistrationContext`，不要现在把接口做大。
>
> `requiresApproval` 目前可由 `initialStatus == PENDING` 推导；保留它是为了显式表达“进入审批队列”的语义。若你希望接口最小化，可以删掉，服务改为判断初始状态；这不会影响本轮三方案的选择。

### 2.2 方案 B 的扩展接口

如果名额检查放进策略，策略就不止“决定初始状态”，接口需要扩展为：

```text
interface RegistrationPolicy {
    RegistrationDecision evaluate(RegistrationContext context)
}

class RegistrationContext {
    Activity activity
    Student student
    DateTime now
    int approvedCount
    Operation operation   // SUBMIT | APPROVE
}

class RegistrationDecision {
    boolean allowed
    RegistrationStatus initialStatus
    String message       // 拒绝原因或警告
    boolean warning      // 允许但提示
}
```

| 项 | 约定 |
|---|---|
| 输入 | `RegistrationContext`，其中 `approvedCount` 由 `RegistrationService` 查询后传入；策略**不得**自己查库 |
| 输出 | `RegistrationDecision`；`allowed=false` 表示拒绝提交/批准，`warning=true` 表示允许但提示 |
| 异常 | `IllegalArgumentException`（空上下文/非法操作）、`PolicyNotFoundException`（工厂）；满员不抛异常，用 `allowed=false` 表达 |
| 注意 | 策略必须保持纯函数；若策略内部开事务/查库，会破坏服务的事务边界 |

> 若你希望 A/B/C 三案共用同一个接口，可统一为 `evaluate(RegistrationContext)`；代价是 A/C 的策略会收到它不会读取的 `approvedCount`，且 C 的“策略只决定初始状态”语义会被稀释。本文建议按方案选择接口，保持接口精简。

### 2.3 直接型与审核型的职责差异

| 维度 | DirectRegistrationPolicy | ReviewRegistrationPolicy |
|---|---|---|
| 初始状态 | `APPROVED` | `PENDING` |
| 是否需要审批 | 否 | 是 |
| 提交时名额充足 | 允许，创建已通过 | 允许，创建待审核 |
| 提交时名额已满 | 拒绝（直接报名满员失败） | 允许，带“已满员，批准可能失败”警告（M-04=A） |
| 批准时名额检查 | 不适用 | 必须二次检查；不足则保持待审核（M-05=A） |
| 提交时通知 | 发 `REGISTERED` 事件（M-06=A） | 不发事件（M-06=A） |
| 审批结果通知 | 不适用 | 批准/拒绝发事件 |
| 共同边界 | 不持久化、不通知、不鉴权、不开事务、不查仓储 | 同左 |

### 2.4 异常语义建议

| 场景 | 表达方式 | 由谁产生 | 服务层处理 |
|---|---|---|---|
| 活动不存在 | `ActivityNotFoundException` | `RegistrationService` | 返回“活动不存在” |
| 活动已软删除/已开始 | `ActivityNotOpenException` | `RegistrationService` | 拒绝报名/审批 |
| 重复有效报名 | `DuplicateActiveRegistrationException` | `RegistrationService`（应用检查或唯一约束冲突翻译） | 提示重复 |
| 直接报名满员 | `CapacityExceededException` | `RegistrationService` 根据 `CapacityDecision.allowed=false` 抛出 | 拒绝并提示 |
| 审核申请满员 | 不抛异常；`RegistrationResult.warning` | `RegistrationService`/策略 | 允许提交并提示 |
| 批准时满员 | `CapacityExceededException` | `RegistrationService` | 批准失败，报名保持待审核 |
| 非法状态转换 | `InvalidRegistrationStateException` | `Registration` 实体 | 拒绝并提示刷新 |
| 未知报名方式 | `PolicyNotFoundException` | `RegistrationPolicyFactory` | 系统配置错误，拒绝请求 |

> 原则：**可预期的业务结果**（满员、重复）在规则/策略层用返回值表达，在服务边界翻译为领域异常或结果警告；**编程错误**（空参数、未知类型）用异常。

---

## 3. 名额检查归属的三种方案

### 3.1 方案 A：放在 RegistrationService 公共规则

```text
class RegistrationService {
    register(...) {
        policy = policyFactory.create(activity.policyType)
        status = policy.initialStatus(activity)
        decision = checkCapacity(activity, approvedCount, status)
        if (!decision.allowed) throw CapacityExceeded(decision.message)
        // 审核型满员时 decision.allowed=true 且 decision.warning=true
        ...
        return RegistrationResult(registration, decision.warning ? decision.message : null)
    }
    approve(...) {
        decision = checkCapacity(activity, approvedCount, APPROVED)
        if (!decision.allowed) throw CapacityExceeded(decision.message)
        ...
    }
    private CapacityDecision checkCapacity(activity, approvedCount, status) {
        // 满员：status=APPROVED 拒绝；status=PENDING 允许+警告
    }
}
```

| 维度 | 影响 |
|---|---|
| 直接报名 | Service 在创建前检查；满员拒绝 |
| 审核申请 | 公共规则若只是“满员拒绝”，会与 M-04=A 冲突；Service 必须按 `status=PENDING` 特判“允许+警告” |
| 批准时 | Service 统一检查；满员拒绝，保持待审核 |
| 重复代码 | 只有一份检查；但提交阶段的“直接/审核不同结果”变成 Service 内的条件分支 |
| 扩展性 | 新策略若改变满员语义，需要改 Service；OCP 较弱 |
| 事务一致性 | Service 自己开事务/加锁，边界清晰；较好 |
| 主要风险 | “公共规则”在提交阶段并不真正公共；`if status == PENDING` 的分支会把策略差异又塞回 Service |

### 3.2 方案 B：放入策略

```text
interface RegistrationPolicy {
    RegistrationDecision evaluate(RegistrationContext context)
}
```

| 维度 | 影响 |
|---|---|
| 直接报名 | `DirectRegistrationPolicy` 在 `SUBMIT` 时检查；满员拒绝 |
| 审核申请 | `ReviewRegistrationPolicy` 在 `SUBMIT` 时允许满员+警告（M-04=A） |
| 批准时 | `ReviewRegistrationPolicy` 在 `APPROVE` 时检查；满员拒绝/保持待审核 |
| 重复代码 | 每个策略都要实现名额判断；两个策略容易重复，除非抽共享 helper（抽了就更接近 C） |
| 扩展性 | 最好；新增策略可封装自己的满员语义（候补、抽签等） |
| 事务一致性 | 策略必须纯函数、只接收 `approvedCount`；若策略查库/开事务，会破坏服务边界。Service 仍应保留批准兜底，否则新策略可能忘记硬不变量 |
| 主要风险 | “任何路径都不能超容量”这个硬不变量被分散到策略；需要 Service 兜底，实际会变成混合方案 |

### 3.3 方案 C：公共规则 + 策略只决定初始状态（本轮定义）

```text
interface RegistrationPolicy {
    RegistrationStatus initialStatus(Activity activity)
    boolean requiresApproval()
}

interface CapacityRule {
    CapacityDecision checkSubmission(Activity activity, int approvedCount, RegistrationStatus initialStatus)
    CapacityDecision checkApproval(Activity activity, int approvedCount)
}

class CapacityDecision {
    boolean allowed
    boolean warning
    String message
}
```

| 维度 | 影响 |
|---|---|
| 直接报名 | `initialStatus=APPROVED`；`checkSubmission` 满员时拒绝 |
| 审核申请 | `initialStatus=PENDING`；`checkSubmission` 满员时允许+警告（M-04=A） |
| 批准时 | `checkApproval` 统一检查；满员拒绝，保持待审核（C8/M-05=A） |
| 重复代码 | 一份 `CapacityRule`；直接/审核不重复实现容量逻辑 |
| 扩展性 | 中；新增状态（如候补 `WAITLISTED`）可能需要扩展 `CapacityRule` 的模式或新增规则实现 |
| 事务一致性 | `CapacityRule` 是纯函数，只接收 `approvedCount`；Service 持有事务/锁，边界清晰 |
| 主要风险 | 提交阶段的“满员时允许/拒绝”由 `initialStatus` 推导，规则隐含“状态语义”；未来第三种状态需要显式扩展 |

### 3.4 横向对比

| 维度 | A：Service 公共规则 | B：Policy 责任 | C：公共规则 + 策略初始状态 |
|---|---|---|---|
| 直接报名满员 | Service 拒绝 | 策略拒绝 | `CapacityRule` 按 `APPROVED` 拒绝 |
| 审核申请满员 | Service 需特判“允许+警告” | 策略允许+警告 | `CapacityRule` 按 `PENDING` 允许+警告 |
| 批准时名额 | Service 统一硬检查 | 策略检查；Service 可能需兜底 | `CapacityRule` 统一硬检查 |
| 重复代码 | 一份；提交特判在 Service | 策略间可能重复 | 一份公共规则 |
| 扩展性 | 较弱（改 Service） | 最强（新策略） | 中（可能扩展规则） |
| 事务一致性 | 好（Service 持有） | 需约束策略为纯函数 | 好（规则纯函数 + Service 事务） |
| 活动类型 if/else | 可能出现 `status` 条件 | 无 | 无（按 `initialStatus` 分支） |
| 与上一轮 C'（混合） | 不同 | 不同 | 不同（C' 有 `preCheck`） |

---

## 4. 建议方案与理由（最终由你确认）

### 4.1 已采纳：本轮 C（公共规则 + 策略只决定初始状态）

理由：

1. **一份容量实现**：直接报名、审核申请、批准都走同一个 `CapacityRule`，不会在策略间复制容量逻辑。
2. **批准硬不变量集中**：`checkApproval` 是统一的安全边界，新策略不容易绕过“不超容量”。
3. **策略保持最小**：`RegistrationPolicy` 只回答“初始状态/是否需要审批”，符合策略模式“封装变化”的初衷；活动类型判断不再散落在 Service。
4. **事务一致性清晰**：`CapacityRule` 是纯函数，`approvedCount` 由 Service 在同一事务中查询/加锁后传入；规则不碰数据库。
5. **满足当前需求**：直接满员拒绝、审核满员允许+警告、批准满员保持待审核，都能在不写活动类型 `if/else` 的情况下表达。
6. **比 A 更稳**：A 的“公共规则”在提交阶段仍要按状态特判，实际上把策略差异塞回 Service；C 把这个差异收敛到 `CapacityRule` 的一个明确分支。

代价：

- `CapacityRule.checkSubmission` 需要根据 `initialStatus` 决定满员行为；这隐含了“状态语义”，未来新增 `WAITLISTED` 时需要扩展规则（见第7节）。
- 不如 B 那样让策略完全自定义满员语义；如果未来出现很多差异很大的报名方式，B 的 OCP 更强。

### 4.2 与上一轮混合 C' 的关系

- 上一轮 `C'`：`RegistrationService.ensureNotOverCapacity` + `RegistrationPolicy.preCheck`。提交策略显式放在 Policy，批准硬不变量在 Service。
- 本轮 `C`：`CapacityRule` 公共规则 + `RegistrationPolicy` 只决定初始状态；没有 `preCheck`。
- 两者都能满足当前需求。**C' 更显式**（策略直接表达提交时的满员行为），**C 更集中**（容量逻辑只有一份，策略更小）。
- 如果你认为“满员时允许审核申请”属于报名策略而不是容量规则，C' 更合适；如果你认为它是容量规则的一部分、只是按初始状态区分，C 更合适。**第4批已选本轮 C**；C' 保留为设计报告中的对比方案。

### 4.3 不推荐的方案

- **A**：提交阶段会出现 `if status == PENDING` 的条件，削弱“依赖抽象、少类型判断”的目标；扩展新策略要改 Service。
- **B（纯策略、无 Service 兜底）**：容量逻辑分散，任何新策略都可能忘记“批准不得超容量”；若加 Service 兜底，实质上就是 C'。

---

## 5. RegistrationService 主流程伪代码

> 以下按**本轮 C**写（假设 `RegistrationService` 注入 `CapacityRule`）；A/B/C' 的替换点见 5.4。伪代码不含框架/ORM 细节，`transaction` 与 `lock` 是设计意图。

### 5.1 报名

```text
RegistrationResult register(studentId, activityId):
    transaction:
        activity = activityRepo.findById(activityId) or throw ActivityNotFound
        if activity.isDeleted() or activity.hasStarted(now):
            throw ActivityNotOpen
        if registrationRepo.findActiveBy(activityId, studentId) != null:
            throw DuplicateActiveRegistration

        policy = policyFactory.create(activity.policyType) or throw PolicyNotFound
        status = policy.initialStatus(activity)
        approvedCount = registrationRepo.countApproved(activityId)

        decision = capacityRule.checkSubmission(activity, approvedCount, status)
        if not decision.allowed:
            throw CapacityExceeded(decision.message)

        registration = Registration.create(studentId, activityId, status, now)
        registrationRepo.save(registration)

        if status == APPROVED:
            eventPublisher.publish(RegistrationEvent.registered(registration))

        return RegistrationResult(registration, decision.message if decision.warning else null)
```

关键点：

- 没有 `if activity.policyType == DIRECT/REVIEW`；类型差异只通过 `policy.initialStatus` 和 `capacityRule` 表达。
- 审核型提交不发布事件（M-06=A）；直接型提交发布 `REGISTERED`。
- 重复检查 + 容量检查 + 插入在同一事务；容量计数要在锁内/同一快照内（见第6节）。
- 如果 `capacityRule` 返回警告（审核型满员），`RegistrationResult.warning` 带回界面，但报名仍创建为 `PENDING`。

### 5.2 批准

```text
Registration approve(registrationId):
    transaction:
        registration = registrationRepo.findById(registrationId) or throw NotFound
        if registration.status != PENDING:
            throw InvalidRegistrationState

        activity = activityRepo.findById(registration.activityId) or throw ActivityNotFound
        if activity.isDeleted() or activity.hasStarted(now):
            throw ActivityNotOpen   // M-03=C：开始后禁止批准

        approvedCount = registrationRepo.countApproved(activity.id)
        decision = capacityRule.checkApproval(activity, approvedCount)
        if not decision.allowed:
            throw CapacityExceeded(decision.message)   // 保持 PENDING（M-05=A）

        registration.approve(now)
        registrationRepo.save(registration)
        eventPublisher.publish(RegistrationEvent.approved(registration))
        return registration
```

### 5.3 拒绝 / 取消

```text
void reject(registrationId, reason):
    registration = load(registrationId)
    require status == PENDING
    registration.reject(now)
    registrationRepo.save(registration)
    eventPublisher.publish(RegistrationEvent.rejected(registration, reason))

void cancel(studentId, registrationId):
    registration = load(registrationId)
    require registration.studentId == studentId
    require status in {PENDING, APPROVED}
    registration.cancel(now)
    registrationRepo.save(registration)
    // M-06=A：取消不发通知
```

### 5.4 A/B/C/C' 的替换点

| 方案 | 报名阶段的容量调用 | 批准阶段的容量调用 |
|---|---|---|
| A | `this.checkCapacity(activity, approvedCount, status)`；内部按 `status` 决定满员时拒绝还是警告 | `this.checkCapacity(activity, approvedCount, APPROVED)` |
| B | `policy.evaluate(SUBMIT, activity, approvedCount, ...)`；策略返回 `allowed/warning/initialStatus` | `policy.evaluate(APPROVE, ...)`；Service 可另加兜底 |
| C（本轮） | `capacityRule.checkSubmission(activity, approvedCount, status)` | `capacityRule.checkApproval(activity, approvedCount)` |
| C'（上一轮混合） | `policy.preCheck(activity, approvedCount)`；策略返回 `allowed/warning` | `this.ensureNotOverCapacity(activityId)` |

> 四种写法都不应在 `RegistrationService` 里写 `if policyType == ...` 的活动类型分支；差异应由策略或规则表达。

---

## 6. 重复有效报名与并发/唯一约束

### 6.1 判定规则

- **有效报名**：同一 `activity_id` + `student_id`，`status ∈ {PENDING, APPROVED}`。
- **上限**：最多一条。
- **历史记录**：`REJECTED`、`CANCELLED` 保留，不阻止再次申请；再次申请创建新记录，不复活旧记录。
- **检查点**：报名创建前；批准不是创建，不触发重复检查（它只改状态）。
- **并发最终裁决**：数据库唯一约束，而不是应用层“先查再插”。

### 6.2 数据库唯一约束选项

| 方案 | 做法 | 适用 | 注意 |
|---|---|---|---|
| 部分唯一索引 | `UNIQUE (activity_id, student_id) WHERE status IN ('PENDING','APPROVED')` | PostgreSQL、SQLite（支持 partial index） | 最直接；状态值变化时要同步索引条件 |
| 生成列/活跃键 | `active_key = activity_id || ':' || student_id`，仅有效状态非空；`UNIQUE(active_key)` | MySQL 等不支持部分索引的库 | 需要生成列/触发器维护；空值不参与唯一 |
| 独立活跃表 | `active_registration(activity_id, student_id)` 主键；有效报名插入，取消/拒绝删除 | 任意关系库 | 多一张表；事务内维护，防止与主表不一致 |
| 应用锁 | 对 `(activity_id, student_id)` 加锁/可串行化 | 无唯一约束时的兜底 | 性能/死锁风险；不如唯一约束可靠 |

**建议**：优先部分唯一索引；MySQL 用生成列或独立活跃表；应用层仍先查一次以给出友好提示，但最终捕获唯一冲突并翻译为 `DuplicateActiveRegistrationException`。

### 6.3 最后名额与审批并发

容量不能用普通唯一约束表达（它是“计数 < capacity”），需要事务/锁：

| 方案 | 做法 | 优点 | 缺点 |
|---|---|---|---|
| 活动行锁 | `SELECT ... FOR UPDATE` 锁 `activity` 行；count 已通过；插入/更新；提交 | 简单、语义清晰；同一活动的报名/审批串行化 | 热点活动会排队；需要事务 |
| 条件更新计数 | `UPDATE activity SET approved_count = approved_count + 1 WHERE id=? AND approved_count < capacity`；再写报名 | 原子、性能好 | 需要维护 `approved_count` 与报名表一致；取消/拒绝要回减 |
| 可串行化 + 重试 | `SERIALIZABLE` 事务；冲突重试 | 不依赖行锁语法 | 重试逻辑复杂；高冲突下吞吐低 |
| 数据库约束 | 触发器/断言 | 强 | 可移植性差；调试难 |

**建议（初始版本）**：活动行锁 + 事务内 `countApproved`；批准和直接报名都走同一锁顺序（先锁活动，再写报名），避免死锁。若后续压测发现热点，再考虑 `approved_count` 条件更新。

> 若最终选 SQLite：它没有 `SELECT ... FOR UPDATE`，可用 `BEGIN IMMEDIATE` 让写事务串行化；具体写法等数据库确认后再定，本批不绑定技术栈。

**幂等/重复点击**：唯一约束保证不会产生两条有效报名；重复请求捕获唯一冲突后返回已有记录或 `DuplicateActiveRegistration`。是否需要请求幂等键（idempotency key）由你后续定；当前唯一约束已足够防止数据重复。

---

## 7. 策略扩展点：未来“先到先得 + 候补”

### 7.1 “先到先得”本身

- 如果“先到先得”就是当前直接型（名额够就通过），使用现有直接型策略；可把 `DirectRegistrationPolicy` 作为它的实现，或在工厂里加一个别名。
- 如果“先到先得”需要额外的排序/排队语义，才需要新策略；不要为同义规则重复建类。

### 7.2 新增“候补”需要改的地方

| 类/接口 | 改动 | 原因 |
|---|---|---|
| `RegistrationStatus` | 新增 `WAITLISTED`（或独立 `WaitlistEntry` 实体） | 当前四状态没有候补；这是领域状态扩展 |
| `Registration` | 新增 `WAITLISTED -> APPROVED`（转正）、`WAITLISTED -> CANCELLED` 转换 | 候补生命周期 |
| `RegistrationPolicy` | 新增 `WaitlistRegistrationPolicy`（初始 `WAITLISTED`）+ 工厂映射 | 报名方式扩展 |
| `CapacityRule` | 新增“满员 -> 候补”的模式/实现；或新增 `WaitlistCapacityRule` | 满员时不再一律拒绝/允许待审核 |
| `RegistrationService` | 新增 `joinWaitlist`、`promoteNext`；取消/拒绝后触发转正 | 候补转正需要业务编排 |
| `RegistrationRepository` | 新增 `findWaitlistByActivityOrdered`、`nextWaitlist`、`countWaitlist` | 候补排序/查询 |
| `RegistrationEventType` | 新增 `WAITLISTED`、`PROMOTED` | 通知扩展（第5批设计） |
| `NotificationService` | 处理新事件类型 | 通知内容/模板 |
| `StatisticsService` / `ActivityStatistics` | 增加候补数（若 P2-F08 扩展获批） | 统计口径变化 |
| `Activity` | 可选：`waitlistCapacity` / `waitlistEnabled` | 是否允许候补、候补上限 |
| 数据库 | 唯一约束的“有效状态”加入 `WAITLISTED`；候补排序字段/索引 | 防重复 + 先到先得顺序 |
| 用例/文档/测试 | 新增“加入候补/候补转正”用例与回归 | 需求范围扩展 |

> 注意：新增 `WAITLISTED` 会改变第2批确认的“四状态”范围，属于未来 CR，不在当前批次实现。当前设计只需保证**扩展点存在**：策略接口、容量规则、事件类型、状态机都可扩展。

### 7.3 扩展时保持不变的类

- `Student`、`Message` 的基本结构保持不变。
- `OutboxEventPublisher`/`EventDispatcher` 的结构保持不变；只增加事件类型和观察者处理。
- `ActivityService` 的活动 CRUD 保持不变（除非增加候补容量字段）。

---

## 8. 已确认决策与后续检查点

### 8.1 已确认

| 编号 | 事项 | 结论 | 落地 |
|---|---|---|---|
| **D4-01** | **名额检查归属** | **C（公共规则 + 策略初始状态）** | `CapacityRule.checkSubmission` + `checkApproval`；策略只决定初始状态；已同步到类图 |
| D4-02 | 策略接口 | `initialStatus(Activity)` + `requiresApproval()` | 已落地 |
| D4-03 | `CapacityRule` | 独立注入 | 已落地 |
| D4-04 | 满员表达 | `CapacityDecision` 返回值 | 已落地；Service 翻译为异常/警告 |
| D4-05 | 重复报名唯一约束 | 部分唯一索引优先；具体 SQL 等数据库 | 设计已定 |
| D4-06 | 未来候补 | `WAITLISTED` 或独立实体 | 等 CR |

### 8.2 后续检查点

- [x] `RegistrationPolicy` 只做“初始状态/审批需求”，容量逻辑已移出。
- [x] `RegistrationService` 伪代码没有 `if policyType == ...` 的活动类型分支。
- [x] 容量检查只接收 `approvedCount`、不查库、不开事务。
- [x] 批准路径有独立的硬不变量，不依赖“提交时已经查过”。
- [x] 重复有效报名有数据库唯一约束方案。
- [x] 最后名额/审批并发明确了行锁/条件更新方向。
- [ ] 第5批：通知设计（事件类型、消息内容、失败重试/幂等、已读规则）。
- [ ] 后续：报名/审批时序图、设计原则说明、评审检查表与同行签名。
- [ ] 第7节候补扩展清单是否覆盖你预期的变更范围。

---

## 9. 本轮文件变更

| 文件 | 操作 | 说明 |
|---|---|---|
| `实验/CampusEvent/docs/03-registration-policy-capacity.md` | 新增/修订 | 本文：策略抽象、三方案对比、已采纳 C、伪代码、并发约束、扩展点 |
| `实验/CampusEvent/docs/02-class-diagram-v1.md` | 修订 | 移除 `preCheck`/`ensureNotOverCapacity`，改为 `CapacityRule`/`DefaultCapacityRule`/`CapacityDecision` |
| `实验/CampusEvent/docs/diagrams/class-diagram-v1.puml` | 修订 | 类图同步 D4-01=C；`RegistrationPolicy` 只保留 `initialStatus(Activity)`/`requiresApproval()` |

> 本轮未执行 Git 操作；未生成业务代码、数据库脚本、接口实现或部署文档。

---

## 10. 第4批收口

`D4-01=C`（本轮定义）与 `D4-02`～`D4-06` 已按建议确认并落入文档/类图。第4批收口，等待你发布**第5批通知设计**任务；在收到第5批任务前，我不做通知设计、不写代码。
