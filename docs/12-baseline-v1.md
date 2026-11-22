# CampusEvent baseline-v1 封存与可维护性基线（第12批）

> 状态：封存材料。功能、测试材料、验收记录、部署由你确认/执行；本轮未运行测试、未执行 Git、未部署。
>
> 目标：把当前实现封存为 `baseline-v1`，作为后续变更（`change-v2`）的对照基线。

---

## 1. baseline-v1 封存检查表

> “状态”列留空/待你填写；“证据”列仅列文件与位置，不代表验收通过。

### 1.1 功能

| 检查项 | 证据 | 确认人 | 状态 |
|---|---|---|---|
| P2-F01 活动创建/编辑/软删除 | `ActivityService`、`AdminActivityController`、`V1__baseline.sql` |  |  |
| P2-F02 浏览/名称筛选 | `ActivityController`、`ActivityService.listVisible` |  |  |
| P2-F03 报名/取消/重复约束 | `RegistrationService`、`ux_registration_active` |  |  |
| P2-F04 直接/审核策略 | `Direct/ReviewRegistrationPolicy`、`RegistrationPolicyFactory` |  |  |
| P2-F05 批准/拒绝/二次名额检查 | `RegistrationService.approve/reject`、`DefaultCapacityRule` |  |  |
| P2-F06 状态转换 | `Registration` 状态机、`updateStatusIfCurrent` |  |  |
| P2-F07 站内通知/幂等/重试 | `OutboxEventPublisher`、`EventDispatcher`、`InSiteNotificationService` |  |  |
| P2-F08 统计 | `StatisticsService`、`/admin/statistics` |  |  |
| 最后名额并发 | `ActivityRepository.findByIdForUpdate` + 活动行锁 |  |  |
| 固定演示身份 | `DemoAccounts.STUDENT_A`、管理员路由开放 |  |  |
| 页面样式 | `static/css/app.css`、`templates/*.html` 居中内容列/统一字阶/响应式 |  |  |

### 1.2 测试材料与验收记录

| 检查项 | 证据 | 确认人 | 状态 |
|---|---|---|---|
| 领域单测 3 个 | `DefaultCapacityRuleTest`、`RegistrationStateTest`、`ActivityTimeTest` |  |  |
| Web 冒烟测试 2 个 | `HealthControllerTest`、`HomeControllerTest` |  |  |
| 测试数据与回归模板 | `docs/10-test-assistance.md` |  |  |
| 独立测试库配置/脚本 | `application-test.yml`、`scripts/create-test-db.ps1` |  |  |
| 测试执行记录 | 由你执行后填写 |  |  |
| 验收用例与实际结果 | 由你掌握/填写 |  |  |

> 本批未运行测试；不得把“文件存在”当作“测试通过”。

### 1.3 部署

| 检查项 | 证据 | 确认人 | 状态 |
|---|---|---|---|
| 部署方案/模板 | `docs/11-deployment-plan.md` |  |  |
| 环境变量 template | `docs/11` 第2节 |  |  |
| systemd/Nginx template | `docs/11` 第2节 |  |  |
| DB 迁移/权限步骤 | `docs/11` 第3节 |  |  |
| 实际云部署 | 由你执行 |  |  |
| 重启一致性验证 | 由你执行并填写 |  |  |

### 1.4 文档

| 检查项 | 证据 | 确认人 | 状态 |
|---|---|---|---|
| 需求/用例 | `docs/01-use-cases.md` |  |  |
| 类图/职责 | `docs/02-class-diagram-v1.md` |  |  |
| 策略/通知决策 | `docs/03`、`docs/04` |  |  |
| 时序图/契约 | `docs/05` |  |  |
| 设计评审包/检查表 | `docs/06`、`docs/review/design-review-checklist.md` |  |  |
| 技术栈/骨架 | `docs/07`、`docs/08` |  |  |
| 初始实现/测试辅助 | `docs/09`、`docs/10` |  |  |
| 部署方案 | `docs/11` |  |  |
| baseline 封存材料 | `docs/12-baseline-v1.md` |  |  |
| AI_DEV_LOG | `AI_DEV_LOG.md`（已导出 10 条，待你核对） |  |  |
| 同行评审签名 | `docs/review/design-review-checklist.md`（待签名） |  |  |
| README/构建文件 | `README.md`、`pom.xml`、`.gitignore` |  |  |

### 1.5 封存结论

| 项 | 内容 |
|---|---|
| 封存版本 | baseline-v1（待打标签） |
| 封存日期 |  |
| 功能确认 |  |
| 测试/验收确认 |  |
| 部署确认 |  |
| 文档确认 |  |
| 封存人/日期 |  |

---

## 2. Git 标签建议命令与提交说明

> **本轮未执行任何 Git 操作。** 以下命令由你按实际仓库执行。

### 2.1 当前检测

- 当前环境检测到 Git 仓库根为 `C:\`，分支 `master`，尚无提交。
- **不建议**直接在 `C:\` 根仓库提交（会把整个盘/无关文件纳入范围）。
- 建议为项目建独立仓库：`实验/CampusEvent`（或在你的实际项目根）。

### 2.2 建议命令（独立仓库）

```bash
cd 实验/CampusEvent

# 1. 如果还没有独立仓库
git init
git branch -M main

# 2. 提交前检查
git status
git diff --cached

# 3. 只添加项目文件（.gitignore 已排除 target/、.env、IDE 等）
git add .
git commit -m "baseline-v1: initial CampusEvent implementation"

# 4. 打注解标签
git tag -a baseline-v1 -m "CampusEvent baseline-v1"

# 5. 关联远程并推送（替换为你的仓库地址）
git remote add origin <YOUR_REPO_URL>
git push -u origin main
git push origin baseline-v1
```

### 2.3 提交说明建议

```text
baseline-v1: initial CampusEvent implementation

- Spring Boot 3.3 + PostgreSQL 16 + Thymeleaf server-side rendering
- Activity management, browsing/filtering, direct/review registration, cancel
- Approval with second capacity check; activity row lock for last-slot concurrency
- Outbox + observer notification with idempotency/retry
- Statistics (approved/pending/remaining)
- Domain unit tests adopted; execution/acceptance/deployment pending
```

> 不要写“测试通过/部署成功”，除非你已实际执行并确认。

### 2.4 后续 change-v2

```bash
# 收到 CR 并完成影响分析后
git switch -c change-v2 baseline-v1
# 实现、回归、提交后
git tag -a change-v2 -m "CampusEvent change-v2"
```

---

## 3. 可维护性基线指标表模板

> 基线值来自当前代码的**静态统计**，不代表测试/验收结果；“验收结果”列留空，由你执行后填写。

| 指标 | 基线值（静态参考） | 验收结果（留空） |
|---|---|---|
| 类总数（`src/main/java` Java 源文件） | 85 |  |
| 核心业务类数 | 15（定义见下） |  |
| 接口数（`public interface`） | 13 |  |
| 测试数 | 5 个测试类 / 16 个 `@Test` 方法（静态统计，未运行） |  |
| 可执行 jar 大小 | 44.47 MB（`mvn clean package -DskipTests`） |  |
| AI 交互轮次 | 待你按 `AI_DEV_LOG.md` 统计 |  |
| 累计修改时长 | 待你统计 |  |

**核心业务类定义（15）**

- 领域模型 4：`Student`、`Activity`、`Registration`、`Message`
- 应用服务 3：`ActivityService`、`RegistrationService`、`StatisticsService`
- 策略/规则 3：`DirectRegistrationPolicy`、`ReviewRegistrationPolicy`、`DefaultCapacityRule`
- 通知/outbox 5：`RegistrationEvent`、`OutboxEvent`、`OutboxEventPublisher`、`EventDispatcher`、`InSiteNotificationService`

**接口 13**

- 策略/规则 2：`RegistrationPolicy`、`CapacityRule`
- 应用端口 7：`ActivityRepository`、`RegistrationRepository`、`MessageRepository`、`OutboxRepository`、`RegistrationEventPublisher`、`RegistrationEventObserver`、`NotificationService`
- JPA 仓储 4：`ActivityJpaRepository`、`RegistrationJpaRepository`、`MessageJpaRepository`、`OutboxEventJpaRepository`

> 统计口径：`src/main/java` 下 `.java` 文件；`src/test/java` 下测试类/`@Test`。控件、异常、DTO、JPA 实体、控制器、模板未计入“核心业务类”。

---

## 4. 初版设计利弊如实说明

### 4.1 策略模式（RegistrationPolicy）

**收益**

- 直接/审核的差异集中在策略实现；`RegistrationService` 不写 `if policyType`。
- 新增报名方式（例如候补）可新增策略 + 工厂映射，主流程改动小。
- 策略可单测，不依赖数据库。

**成本**

- 多出接口、两个实现、工厂；调用链更长。
- 名额检查与策略分离后，读者需要同时看 `RegistrationPolicy` 与 `CapacityRule` 才能理解完整规则。
- 如果永远只有两种策略且不变，简单 `if/else` 更短。

### 4.2 公共容量规则（CapacityRule）

**收益**

- 提交检查、批准硬检查各一处；批准不会超容量。
- 纯函数，易测试。
- 与策略分离，避免两个策略各写一遍容量逻辑。

**成本**

- `checkSubmission` 按 `initialStatus` 区分满员行为，隐含“状态语义”；未来 `WAITLISTED` 需要扩展规则。
- 多一个概念，业务规则被拆成“策略 + 规则”两处理解。

### 4.3 观察者 + outbox（通知解耦）

**收益**

- 报名/审批不直接依赖通知；通知失败不回滚报名（满足 B.2-6）。
- outbox 持久化 + 重试 + 幂等（`sourceEventId`），支持崩溃恢复与重复派发。
- 新增通知渠道只需新增观察者。

**成本**

- 多出 outbox 表、dispatcher、事件 schema、重试/幂等逻辑。
- 通知是最终一致（默认 5 秒轮询），不是同步立即可见。
- 调试链路长：Service → Publisher → Outbox → Dispatcher → Notification → Message。
- 需要处理 `PROCESSING` 超时回收、重复消息等边界。

### 4.4 仓储/DIP 与状态机

**收益**

- 领域不依赖 JPA/Web；可单测、可替换持久化。
- 状态转换集中在 `Registration`，非法转换可拒绝。
- 活动行锁把最后名额并发串行化（D10-02）。

**成本**

- JPA 实体/映射/适配器增加类数量（84 个 Java 文件）。
- 领域与 JPA 实体双模型，存在映射维护成本。
- 仓储接口 + 适配器比直接 DAO 多一层。

### 4.5 如实结论

- 本项目的模式收益主要在“可变报名规则 + 通知可靠性 + 可测试性”，代价是类数量、调用链、最终一致性和测试面增加。
- 对只有两种策略、通知永远成功、需求永不变化的小项目，这套结构偏重；实验要求变更适应与 B.2-6 可靠性，因此当前取舍可解释。
- 后续 `change-v2` 应重点观察：新增变更时修改了多少核心类、是否只扩展策略/观察者、回归失败数、AI 交互轮次与时长。

---

## 5. 下一阶段变更影响分析模板（不实现变更）

> 收到教师 CR 后填写；本批不实现任何变更。

| 项 | 内容 |
|---|---|
| CR 编号与描述 |  |
| 变更类型 | 新增规则/修改规则/新增通知/新增状态/其他 |
| 受影响需求/用例 |  |
| 受影响领域模型 |  |
| 受影响应用服务/策略/容量规则 |  |
| 受影响通知/outbox |  |
| 受影响接口契约 |  |
| 数据库迁移需求 |  |
| Web/页面影响 |  |
| 新增/修改测试 |  |
| 回归范围 |  |
| 兼容性/数据迁移风险 |  |
| 回滚方案 |  |
| 预计新增类数/修改核心类数 |  |
| 预计 AI 交互轮次/工时 |  |
| 需要学生决策的设计点 |  |
| Git 分支/标签 | `change-v2` from `baseline-v1` |

### 影响分析步骤

1. 先复述 CR，确认范围与验收口径（由你掌握）。
2. 标出受影响的需求/用例/类/接口/表。
3. 判断是“扩展策略/观察者”还是“修改核心流程”。
4. 评估是否需要接口变更/数据库迁移/兼容处理。
5. 列出新增测试与回归范围；不提前写实现。
6. 从 `baseline-v1` 建 `change-v2`，实现后再回归并打标签。

---

## 6. 本轮待你决策项/建议检查点

| 编号 | 事项 | 选项 | 说明 |
|---|---|---|---|
| **D12-01** | 仓库位置 | **独立仓库 `实验/CampusEvent`（已按建议）** / 现有 `C:\` 仓库 | 当前 `C:\` 根仓库无提交，不建议直接使用 |
| **D12-02** | 标签时机 | **验收/部署确认后打 baseline-v1（按建议）** / 现在打 | 建议先完成封存检查表再打标签 |
| **D12-03** | AI_DEV_LOG | **已导出 10 条（待你核对）** | `AI_DEV_LOG.md` + `docs/AI_DEV_LOG.csv` |
| **D12-04** | 同行评审签名 | 已签 / **待签** | 实验二 3.3 任务 D 要求 |
| **D12-05** | 部署状态 | 已部署 / **未部署（由你执行）** | 文档已备 |
| **D12-06** | 验收记录 | **由你执行后填写** | 本批不填通过情况 |

### 检查点

- [ ] 封存检查表已由你确认功能/测试/部署/文档。
- [ ] 测试与验收记录已由你填写。
- [ ] 同行评审签名已完成（如需要）。
- [x] `AI_DEV_LOG.md` 已导出 10 条（`docs/AI_DEV_LOG.csv` 为 CSV 版），待你核对。
- [ ] Git 仓库位置/分支已确认。
- [ ] 确认后再打 `baseline-v1` 标签；本轮未执行 Git。
- [ ] 后续 CR 只做影响分析，不提前实现。

---

## 7. 说明

- 本批未运行测试、未执行 Git、未部署；不声称任何通过/失败或部署成功。
- 基线指标为静态统计；测试数/类数不代表质量，需你验收后填写结果。
- 等待后续变更描述；在收到 CR 前不实现变更。
