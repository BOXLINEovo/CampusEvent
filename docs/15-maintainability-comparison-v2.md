# baseline-v1 与 change-v2 可维护性对照

> 对照基线：`baseline-v1`，提交 `90b1d14`。
>
> change-v2：工作区增量实现（第13—15批），本文件记录结构与构建证据。
>
> 证据边界：教师/课程验收结果未提供，本文件不填写验收通过/失败结论；本地 `mvn test` 仅作为构建阶段证据，不作为验收结论。

## 1. 对照表模板

| 指标 | baseline-v1 | change-v2 | 变化/说明 | 证据 |
|---|---:|---:|---|---|
| 主代码 Java 文件数 | 85 | 115 | +30 | `git ls-tree -r --name-only 90b1d14 -- src/main/java`；当前文件计数 |
| 新增主代码类/接口 | — | 30 | 全部为 `*V2.java` | 当前 `src/main/java` 文件计数 |
| 修改核心业务类数 | 0（初始建设） | 0 | 原 `RegistrationService`、`PolicyType`、`RegistrationPolicyFactory` 等源码保持不变 | `git diff --name-only -- src/main src/test` 为空 |
| 测试类数 | 5 | 8 | +3 | `src/test/java` 文件计数 |
| 测试方法数 | 16 | 26 | +10 | 现有测试文件与 `target/surefire-reports` 汇总 |
| 新增模板 | — | 3 | 年级规则、邮件记录、年级演示 | `templates/*-v2.html` |
| 新增数据库迁移 | — | 2 | 1 个主迁移 + 1 个 dev 演示数据 | `db/**/*-v2.sql` |
| 新增 UML 图 | 9 张 v1 | +4 张 v2 | v1 保持原样；v2 导出 8 个文件 | `docs/diagrams/*-v2.puml`、`export/*-v2.*` |
| 新增主代码行数 | — | 约 953 行 | 30 个 `*V2.java` 文件 | 文件行数统计 |
| 新增测试代码行数 | — | 约 270 行 | 3 个 `*V2Test.java` 文件 | 文件行数统计 |
| AI 交互轮次 | 12 批设计/实现 | 3 批（13 影响分析、14 实现、15 汇总） | 按批次计，非聊天消息数 | 第13—15批记录 |
| 修改总时长 | 2026-11-19—11-22（日志日期；精确工时未记录） | 实现轮约 20 分钟；分析与文档轮未单独计时 | 采用可核对的时间记录 | `AI_DEV_LOG-v2.md` |
| 本地自动化测试执行 | `docs/12` 记录为“未执行” | 26 个测试，0 失败，0 错误 | 构建阶段证据，非验收结论 | 第14批 `mvn test` 输出与 `target/surefire-reports` |
| 教师验收结果 | 未提供 | 未提供 | 本表不推断 | 待学生/教师补充 |

### 使用说明

- 课程报告若需要“验收结果”列，保持空白并附教师签字/截图；本文件只提供结构与构建证据。
- baseline 的“修改核心业务类数”指初始建设，不作为变更指标；change-v2 的 0 表示原源码文件变更数为 0。
- 新增文件数在实现轮为 52 个（30 Java + 3 测试 + 3 模板 + 2 迁移 + 4 UML + 8 导出 + 2 文档）；第15批报告材料另计。

## 2. 设计利弊的客观证据

### 2.1 策略模式的收益

**现有结构**

- `RegistrationPolicy` 只定义 `initialStatus(activity)` 与 `requiresApproval()`。
- `DirectRegistrationPolicy` 返回 `APPROVED`；`ReviewRegistrationPolicy` 返回 `PENDING`。
- `RegistrationPolicyFactory` 按 `PolicyType` 选择实现；`CapacityRule` 单独负责名额。

**可核对证据**

- 本次年级限定直接报名**没有新增** `PolicyType` 枚举值，也没有修改 `RegistrationPolicy`、`DirectRegistrationPolicy`、`ReviewRegistrationPolicy`。
- 年级资格作为独立规则 `ActivityGradeRuleV2` 接入；初始状态仍由 `DIRECT` 策略给出。
- `git diff --name-only -- src/main src/test` 为空，说明原策略类未被改动。

**收益**

- 直接报名与审核报名的“初始状态”差异被隔离在策略实现内，新增报名结果类型时避免把条件分支堆进 `RegistrationService`。
- 名额规则 `CapacityRule` 独立复用，年级检查之后仍由原服务执行，避免资格规则与容量规则耦合。
- 单元测试可以直接验证 `DirectRegistrationPolicy` / `ReviewRegistrationPolicy` 的返回值，测试面小。

**代价与边界**

- `RegistrationPolicyFactory` 使用 `switch (type)` 固定枚举，新增 `PolicyType` 值需要修改工厂；本次为保持原文件不变，年级限定采用“DIRECT + 扩展规则表”，没有走“新增策略”的路径。
- 年级资格需要额外查询 `activity_grade_rule_v2` 与 `student_grade_v2`；原 `register` 流程因此多一次规则读取（无规则活动也有一次规则查询）。
- `GradeAwareRegistrationServiceV2` 继承具体 `RegistrationService`，并用 `@Primary` 接管旧入口。这属于继承式扩展，DIP 不如“新接口 + 新实现”纯粹；收益是旧控制器、旧页面和旧测试保持原样。
- 该方案的复杂度集中在“运行时装配覆盖”：`@Primary` 是隐式行为，需要在文档和答辩中主动说明，否则维护者容易误以为原服务被修改。

### 2.2 观察者/事件分发的收益

**现有结构**

- `EventDispatcher` 构造器注入 `List<RegistrationEventObserver>`，逐个调用 `onEvent`。
- `InSiteNotificationService` 是既有观察者；本次新增 `EmailNotificationObserverV2`，实现同一接口。
- 事件先写 `outbox_event`，提交后由 `EventDispatcher` 派发。

**可核对证据**

- `EventDispatcher`、`InSiteNotificationService`、`RegistrationEvent`、`OutboxEvent` 原文件无改动。
- 新增 `EmailNotificationObserverV2` 后，`EventDispatcher` 通过 Spring 列表注入自动获得第二个观察者；派发代码保持原样。
- 邮件表唯一键 `(source_event_id, student_id)` 在 `V3__grade_and_email-v2.sql` 中定义。
- 观察者处理 `REGISTERED`、`APPROVED`、`REJECTED`，跳过 `SUBMITTED`、`CANCELLED`；这些分支在新观察者内部完成。
- 发送异常在观察者内捕获并写入 `FAILED`，不向 `EventDispatcher` 抛出；报名事务在 outbox 派发前已提交。

**收益**

- 新增通知渠道只需要新增观察者与记录表，站内通知链路保持原样，符合 OCP 的典型扩展方式。
- outbox 把通知从报名事务中拆出；邮件失败记录错误，已提交报名结果保持不变，符合“通知失败不回滚报名”的要求。
- 唯一键提供幂等底线；重复投递时观察者先查询，重复事件跳过。
- `RegistrationEvent` 不新增事件类型，`EventDispatcher` 不新增分支，邮件订阅者复用现有事件契约。

**代价与边界**

- `EventDispatcher` 在一个事务内调用全部观察者；邮件观察者必须自行捕获发送异常，否则会把同一事务内的站内消息一起回滚。当前实现用 `try/catch` 解决，但这是观察者实现的责任，不是框架保证。
- 当前邮件失败策略是“记录 `FAILED` 并结束当前事件”，重试策略待决策；`FAILED` 记录不会自动再次发送。
- 事件负载没有邮箱字段，当前收件人采用 `studentId@campus.example` 的模拟地址；这是教学 mock 的简化，真实邮件需要补充学生邮箱或收件人映射。
- 通知成功与记录写入之间存在崩溃窗口；唯一键可以防止重复记录，但“严格一次发送”需要两阶段占位记录。
- 观察者列表顺序由 Spring 注入顺序决定；当前两个观察者都幂等，顺序不影响结果，但未来有强顺序要求的渠道需要显式排序。

## 3. 变更影响分析复盘

### 3.1 使变更容易的设计

1. **观察者扩展点**：`RegistrationEventObserver` 与 `List` 注入使邮件渠道以新增类接入；原站内通知、派发器、事件契约均保持原样。
2. **端口/适配器分层**：`ActivityRepository`、`RegistrationRepository`、`MessageRepository` 等接口稳定；新增 `StudentGradeRepositoryV2`、`ActivityGradeRuleRepositoryV2` 可以独立适配。
3. **容量规则独立**：`CapacityRule` 不参与年级判断，年级检查在 `super.register` 前完成，原名额逻辑直接复用。
4. **`@Primary` 运行时扩展**：旧控制器注入 `RegistrationService` 时自动获得年级感知子类，旧 URL、旧模板、旧测试保持原样。
5. **纯新增数据库表**：`student_grade_v2`、`activity_grade_rule_v2`、`email_notification_v2` 不改变基线表结构，迁移回退成本低。
6. **v2 命名隔离**：新增文件以 `V2` / `-v2` 区分，便于与 baseline 对照。

### 3.2 使变更困难的设计

1. **`PolicyType` 枚举 + 工厂 switch**：新增“年级限定”类型需要新增枚举值并修改工厂；为满足原文件不变，只能采用“DIRECT + 扩展规则表 + 装饰器”。
2. **`RegistrationService` 以具体类被控制器依赖**：没有 `RegistrationUseCase` 端口，扩展只能继承具体类并用 `@Primary` 覆盖；DIP 纯度下降。
3. **`Student` 只有 id/name，且没有学生仓储**：年级只能放到扩展表；旧页面固定学生 A，验收需要新增演示页选择学生。
4. **`Activity` 没有扩展字段**：年级规则放在旁表，旧活动详情页无法直接显示规则；提示只能通过报名错误信息呈现。
5. **事件负载字段固定**：`RegistrationEvent` 没有邮箱/收件人字段；邮件观察者只能根据 `studentId` 生成模拟地址。
6. **`EventDispatcher` 单事务调用全部观察者**：新增渠道要自行处理异常隔离，否则会影响站内消息。
7. **原测试以单元/Web 为主**：没有真实数据库集成测试；新表约束、Flyway、outbox 与观察者的组合需要补测。
8. **Java 命名约束**：Java 公共类不能含 `-`，新增 Java 文件采用 `V2` 后缀；这与“所有文件以 `-v2` 结尾”的字面要求存在冲突，需要在报告中说明。

### 3.3 复盘结论

本次变更的可维护性收益主要来自**观察者扩展点**和**端口分层**；主要成本来自**枚举工厂的封闭性**与**服务以具体类暴露**。在“原代码文件保持原样”的约束下，方案 A 是低回归风险的折中；若允许修改原文件，优先把 `RegistrationService` 抽象为用例接口，并将年级资格做成可组合规则，可进一步降低 `@Primary` 的隐式耦合。

## 4. 证据索引

- 原策略接口：`src/main/java/com/campusevent/domain/policy/RegistrationPolicy.java`
- 原派发器：`src/main/java/com/campusevent/infrastructure/outbox/EventDispatcher.java`
- 年级装饰器：`src/main/java/com/campusevent/application/service/GradeAwareRegistrationServiceV2.java`
- 邮件观察者：`src/main/java/com/campusevent/infrastructure/notification/EmailNotificationObserverV2.java`
- 新表迁移：`src/main/resources/db/migration/V3__grade_and_email-v2.sql`
- 测试报告：`target/surefire-reports/*.txt`
- 实现记录：`docs/14-change-implementation-v2.md`
- AI 日志：`AI_DEV_LOG-v2.md`

## 5. 数据口径说明

- Java/测试文件数来自文件系统与 git tree 统计；行数为 UTF-8 文本行数。
- 测试方法数来自现有测试源码与 Surefire 报告；第14批本地执行结果为 26 个测试、0 失败、0 错误。
- 教师验收结果未提供，报告中的“验收”列保持空白；本地测试结果仅证明构建阶段行为，不作为课程验收结论。
- 修改时长只记录可核对的时间段；“分析与文档轮”未单独计时，避免填入推测值。
