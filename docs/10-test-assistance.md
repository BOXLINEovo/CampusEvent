# CampusEvent 通用测试辅助材料（第10批）

> 状态：测试辅助材料。**验收用例由你掌握和执行**；本轮未执行测试、未判定通过、未记录测试实际结果。
>
> 本文件不包含“通过数/失败数”，也不对任何验收结果作结论。

---

## 1. 通用测试关注点清单

> 下列是“建议检查”的通用关注点，不是你的验收用例清单；你可以按自己的用例取舍。

| 需求/规则 | 建议前置 | 建议操作 | 预期边界 |
|---|---|---|---|
| P2-F01 创建活动 | 管理员；空库/已有活动 | 提交合法字段；分别缺标题/类型/时间/地点、容量 0 或负数、过去开始时间 | 合法创建成功；非法字段拒绝并提示；不产生半条记录 |
| P2-F01 编辑活动 | 活动已有已通过报名/待审核报名/仅历史记录 | 修改标题；下调容量到低于已通过数；有有效报名时改 `policyType`；改开始时间 | 容量不得低于已通过数；有效报名时 `policyType` 不可改；开始时间不可改为过去（已开始活动的编辑边界见疑似缺陷） |
| P2-F01 删除活动 | 无报名 / 有待审核 / 仅有已通过或历史 | 软删除 | 无报名可删；有待审核拒绝；仅有已通过/历史时软删除，学生列表隐藏、历史保留 |
| P2-F02 浏览/筛选 | 有多个活动、含已开始/软删除 | 打开列表；按名称关键词筛选；空关键词；无匹配 | 软删除不展示；已开始展示但不可报名；空关键词不过滤；无匹配空态 |
| P2-F03 直接报名 | 直接型活动未开始、容量未满、学生无有效报名 | 报名 | 创建 APPROVED；通知；名额 +1 |
| P2-F03 直接报名满员 | 直接型活动已通过数=容量 | 报名 | 拒绝；不产生报名/事件；不超容量 |
| P2-F03 审核报名 | 审核型活动未开始、无有效报名 | 报名 | 创建 PENDING；不占名额；不产生通知（仅审计事件） |
| P2-F03 重复有效报名 | 学生已有 PENDING/APPROVED | 再次报名同一活动 | 拒绝；已有记录不被覆盖；已拒绝/已取消后可新建记录 |
| P2-F03 取消 | 本人 PENDING/APPROVED；非本人；已拒绝/已取消；已开始 | 取消 | 本人可取消；非本人拒绝；终态不可再取消；开始后仍允许；已通过取消后名额释放 |
| P2-F04 策略 | 直接型/审核型各一个 | 报名 | 直接型立即 APPROVED；审核型 PENDING；策略由活动 `policyType` 决定 |
| P2-F05 批准 | 待审核报名；活动未开始；名额充足/已满 | 批准 | 充足→APPROVED + 通知；已满→批准失败、保持 PENDING、无事件、无通知 |
| P2-F05 拒绝 | 待审核报名；活动已开始 | 拒绝（可带理由） | 变为 REJECTED；不占名额；开始后仍允许；理由进入消息 |
| P2-F06 非法状态转换 | 已通过/已拒绝/已取消 | 尝试非法批准/拒绝/取消 | 拒绝并提示；不产生状态变更；不产生事件 |
| P2-F07 通知触发 | 直接报名/审核提交/批准/拒绝/取消 | 完成对应动作 | 直接/批准/拒绝产生消息；审核提交/取消不产生消息 |
| P2-F07 通知幂等/失败 | 同一事件重复派发；模拟消息写入失败 | 重试/重复派发 | 不重复插入消息；报名/审批不回滚；outbox 有失败/重试痕迹 |
| P2-F08 统计 | 有不同状态报名；取消已通过；软删除活动 | 查看统计 | 已通过数、待审核数、剩余名额与状态一致；取消后同步；软删除活动默认排除 |
| 并发/持久化 | 最后名额；并发批准；重启应用 | 并发报名/批准；重启后查询 | 不超容量；不重复有效报名；重启后活动/报名/消息一致 |
| 页面/身份 | 固定演示学生 A；管理员路由 | 打开各页面；提交空/非法表单 | 页面可读；错误提示；固定演示身份符合 D9-02 |

---

## 2. 测试代码（D10-01：全部采用，未执行）

> **按 D10-01 已采用到 `src/test/java/...`；本轮未运行、未判定通过。**
>
> 重复的 `.java.txt` 草稿已删除，只保留 `src/test` 下的正式测试文件。

| 测试文件 | 覆盖点 |
|---|---|
| `src/test/java/com/campusevent/domain/rule/DefaultCapacityRuleTest.java` | 容量规则：未满、直接满员拒绝、审核满员警告、批准满员拒绝 |
| `src/test/java/com/campusevent/domain/model/RegistrationStateTest.java` | Registration 状态机：批准/拒绝/取消、非法转换 |
| `src/test/java/com/campusevent/domain/model/ActivityTimeTest.java` | 活动开始/开放/软删除/容量下调 |

- 依赖：JUnit 5 + AssertJ（`spring-boot-starter-test` 已包含）。
- 测试执行由你负责；本轮只做了测试源代码编译，未运行测试。
- 采用后的文件仅覆盖纯领域规则，不涉及数据库/Web 集成。

---

## 3. 测试数据构造建议

### 3.1 学生

- 学生 A：`11111111-1111-1111-1111-111111111111`（已有 dev seed）。
- 学生 B：`22222222-2222-2222-2222-222222222222`（已有 dev seed）。
- 学生 C：需要时补一条测试学生，避免 FK 失败；建议用独立测试库或 dev seed 扩展。

### 3.2 活动矩阵

| 活动 | 建议数据 | 用途 |
|---|---|---|
| 直接型-未满 | 未来开始，容量 2，0 已通过 | 直接报名成功 |
| 直接型-满员 | 未来开始，容量 2，2 已通过 | 直接报名失败 |
| 审核型-未满 | 未来开始，容量 1，0 已通过 | 审核申请 PENDING |
| 审核型-满员 | 未来开始，容量 1，1 已通过 | 审核申请允许+warning；批准失败 |
| 已开始 | `start_at < now` | 报名/批准拒绝；取消/拒绝允许 |
| 软删除 | `deleted_at` 非空 | 学生列表/统计排除；消息入口置灰 |
| 无报名 | 未来开始，容量 > 0 | 删除活动/创建基线 |

### 3.3 报名矩阵

- PENDING：审核型未批准。
- APPROVED：直接报名或审核批准。
- REJECTED：审核拒绝（可带理由）。
- CANCELLED：学生取消 PENDING/APPROVED。
- 重复有效：同一学生同一活动已有 PENDING/APPROVED；建议同时准备“已拒绝/已取消后重新申请”的数据。

### 3.4 消息/outbox

- Message：三类通知各一条；重复 `source_event_id` 用于幂等检查。
- Outbox：`PENDING`、`PROCESSING`、`SENT`、`FAILED` 各一条；可构造 `PROCESSING` 超过 5 分钟的记录检查回收。
- 通知失败：可用错误 payload 或临时不可写的消息表模拟；不要求破坏主业务。

### 3.5 固定时间与重置

- 活动时间建议使用相对测试日期的未来/过去固定值，避免“今天/明天”歧义。
- 测试库重置建议（**由你执行**）：
  ```sql
  DELETE FROM message;
  DELETE FROM outbox_event;
  DELETE FROM registration;
  DELETE FROM activity;
  DELETE FROM student;
  ```
  然后重新插入测试数据或重建测试库；不要在生产/演示库执行。
- 若保留 dev seed，注意 `V2__demo_data.sql` 不会自动重跑；重置后需手工补数据或重建库。

---

## 4. 回归测试表模板

> “实际”“通过/失败”由你填写；本模板不预填任何结果。

| 测试项 | 前置 | 操作 | 预期 | 实际 | 通过/失败 | 备注 |
|---|---|---|---|---|---|---|
| 创建活动-合法 | 管理员 | 填写合法字段提交 | 创建成功，列表可见 |  |  |  |
| 创建活动-非法 | 管理员 | 缺必填/容量≤0/过去时间 | 拒绝并提示，不落库 |  |  |  |
| 编辑活动-容量下限 | 有 2 条已通过 | 容量改为 1 | 拒绝 |  |  |  |
| 编辑活动-策略变更 | 有有效报名 | 修改 policyType | 拒绝（D9-01） |  |  |  |
| 删除活动-待审核 | 有 PENDING | 软删除 | 拒绝 |  |  |  |
| 直接报名-成功 | 直接型未满 | 学生报名 | APPROVED + 通知 |  |  |  |
| 直接报名-满员 | 已通过=容量 | 学生报名 | 拒绝，不超容量 |  |  |  |
| 审核报名-成功 | 审核型未满 | 学生报名 | PENDING，不占名额 |  |  |  |
| 审核报名-满员 | 已通过=容量 | 学生报名 | 允许 PENDING + warning |  |  |  |
| 重复报名 | 已有 PENDING/APPROVED | 再次报名 | 拒绝 |  |  |  |
| 取消-本人 | 本人 PENDING/APPROVED | 取消 | CANCELLED，统计同步 |  |  |  |
| 取消-非本人 | 他人报名 | 取消 | 拒绝 |  |  |  |
| 批准-成功 | PENDING，未满 | 批准 | APPROVED + 通知 |  |  |  |
| 批准-满额 | PENDING，已满 | 批准 | 失败，保持 PENDING，无事件 |  |  |  |
| 拒绝 | PENDING | 拒绝（可带理由） | REJECTED + 通知 |  |  |  |
| 通知-审核提交 | 审核型报名 | 提交 | 无消息 |  |  |  |
| 通知-取消 | 取消报名 | 取消 | 无消息 |  |  |  |
| 通知-重复派发 | 同一 eventId | 重复派发 | 不重复插入消息 |  |  |  |
| 统计 | 含各状态 | 查看统计 | 三项指标一致 |  |  |  |
| 重启一致性 | 已有数据 | 重启后查询 | 活动/报名/消息一致 |  |  |  |

---

## 5. 建议运行命令（由你执行）

> 以下命令**由你执行**；本轮不运行、不判定结果。

```bash
# 编译/打包（不执行测试）
mvn -DskipTests compile
mvn -DskipTests package

# 全部测试（由你执行）
mvn test

# 若采用某个测试草稿并移入 src/test 后
mvn -Dtest=DefaultCapacityRuleTest test
mvn -Dtest=RegistrationStateTest test
mvn -Dtest=ActivityTimeTest test
```

本地环境：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1
powershell -ExecutionPolicy Bypass -File scripts/status-local.ps1
powershell -ExecutionPolicy Bypass -File scripts/stop-local.ps1
```

状态检查（由你执行）：

```sql
SELECT registration_id, status FROM registration;
SELECT event_id, event_type, status, retry_count, last_error FROM outbox_event;
SELECT message_id, type, title, is_read, source_event_id FROM message;
SELECT activity_id, capacity, deleted_at FROM activity;
```

---

## 6. 疑似缺陷与建议检查点

> 以下仅为**疑似缺陷/检查点**，不是测试结论；是否成立由你验收判断。

1. **最后名额并发（D10-02 已处理，待你验收）**
   - 已在 `register`/`approve` 中先调用 `ActivityRepository.findByIdForUpdate` 锁活动行，再 `countApproved` 与插入/状态更新；同一活动的并发报名/审批串行化。
   - `ActivityService.update/softDelete` 也使用同一锁，避免与报名/审批并发冲突。
   - 仍需你按自己的并发用例验收；本轮未执行并发测试。
2. **软删除与并发报名（随 D10-02 已处理）**
   - `softDelete` 与 `register` 都先锁同一活动行；并发插入 PENDING 会在锁释放后看到 `deletedAt` 并拒绝。
   - 仍建议检查“活动已删但报名仍创建”的数据库最终状态。
3. **编辑已开始活动的开始时间**
   - 表单 `datetime-local` 默认分钟精度，数据库 `Instant` 可能带秒/纳秒；编辑已开始活动但未改开始时间时，`startAt.equals(existingStartAt)` 可能因精度不同而失败。
   - 建议检查：编辑已开始活动的标题/地点/容量是否被误拒绝。
4. **数据完整性异常被误报为重复**
   - `JpaRegistrationRepository.insert` / `JpaMessageRepository.save` 捕获所有 `DataIntegrityViolationException` 并翻译为“重复”；FK/其他约束失败可能被误报。
   - 建议检查：学生不存在、活动不存在等 FK 异常的错误提示。
5. **表单绑定错误**
   - `AdminActivityController` 使用 `BindingResult` 可改进；非法日期/类型错误可能直接 400，而不是页面友好提示。
   - 建议检查：空/非法表单提交的页面表现。
6. **outbox `PROCESSING` 回收**
   - 超过 5 分钟回收为 PENDING，若 handler 仍在运行可能重复处理；消息唯一键应吸收重复。
   - 建议检查：慢 handler/重启场景是否产生重复消息。
7. **统计范围**
   - `StatisticsService.getAll` 使用 `findVisible(null)`，软删除活动默认排除；若你希望统计包含已删除活动，需要调整口径。
   - 建议检查：软删除活动的已通过报名是否应计入统计。

---

## 7. 已确认决策与检查点

> 确认日期：2026-11-21。`D10-01`～`D10-05` 均按“第一选项”确认。

### 已确认

| 编号 | 结论 | 落地 |
|---|---|---|
| **D10-01** | **全部采用**三份测试草稿 | 已复制到 `src/test/java/...`；本轮只做测试源代码编译，未运行 |
| **D10-02** | **优先修复最后名额并发** | `ActivityRepository.findByIdForUpdate` 活动行悲观锁；`register/approve/update/softDelete` 使用；`05` 契约已补 |
| **D10-03** | **独立测试库** | 新增 `src/test/resources/application-test.yml` 与 `scripts/create-test-db.ps1`（`campusevent_test`） |
| **D10-04** | **固定学生 A** | 维持 `DemoAccounts.STUDENT_A`；管理员路由开放 |
| **D10-05** | **由你执行测试** | 本轮未运行测试、未判定 |

> D10-02 是第6批接口的**唯一新增方法**：`ActivityRepository.findByIdForUpdate`。影响：活动/报名适配器需实现；已在 `05` 契约与 `02` 类图同步。其余接口签名未改。

### 检查点

- [x] 关注点清单覆盖需求/边界。
- [x] 三份测试草稿已采用到 `src/test`，未运行。
- [x] 独立测试库配置/创建脚本已准备（未执行创建）。
- [x] 最后名额并发已加活动行锁；待你验收。
- [x] 由你执行测试、由你判定。

---

## 8. 说明

- 本轮**未执行测试**，不输出通过数/失败数，不代你判定验收结果。
- 未执行 Git 操作。
- 所有“疑似缺陷”仅为检查线索，不是结论。
