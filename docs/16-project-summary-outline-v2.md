# CampusEvent 项目总结大纲（5—8 页）

> 用途：课程报告/答辩 PPT 的页面级大纲。
>
> 口径：验收结果由学生/教师填写；本大纲只列出应展示的材料与证据位置。

## 第 1 页：项目背景与范围

- 题目：校园活动报名与管理系统。
- 目标：活动管理、直接/审核报名、名额控制、通知、统计。
- 角色：学生、校园管理员。
- 范围边界：单体 Spring Boot、PostgreSQL、Thymeleaf 服务端渲染；课程演示身份固定。
- 关键约束：软删除、开始后不可报名、容量与重复报名约束、通知与报名解耦。
- 证据：`docs/01-use-cases.md`、`docs/07-tech-stack-options.md`、`README.md`。
- 本页图表：系统上下文简图或角色/功能矩阵。

## 第 2 页：需求分析与用例

- 用例清单：UC-01—UC-12；v2 增加 UC-13 年级限定报名、UC-14 配置年级规则、UC-15 查看模拟邮件。
- 核心用例规约：直接报名、审核报名、审批、取消、通知、统计。
- 业务规则：名额、时间、重复有效报名、软删除、审批满额、通知范围。
- 验收条件：引用 `docs/01` 与 `docs/13` 的表格；验收结果列留空，由学生/教师填写。
- 本页图表：用例图 v1 与 v2 对照（`use-case` / `use-case-v2`）。

## 第 3 页：初始设计（v1 UML 与架构）

- 分层：`domain` / `application` / `interfaces` / `infrastructure`。
- v1 类图：Activity、Registration、Student、Message、策略、容量规则、端口、适配器。
- v1 状态图：报名状态机 `PENDING/APPROVED/REJECTED/CANCELLED`。
- v1 时序图：直接报名、审核报名、批准、拒绝、取消。
- 关键取舍：策略只决定初始状态；容量规则独立；outbox + 观察者通知。
- 本页图表：`class-diagram-v1`、`state-registration`、`seq-register-direct`、`seq-register-review`。

## 第 4 页：设计模式决策与替代方案

- 策略模式：`RegistrationPolicy` 的两个实现；为什么把“初始状态”与“名额”拆开。
- 容量规则：`CapacityRule` 的提交检查与审批硬检查；与策略的职责边界。
- 观察者/事件：`RegistrationEventObserver` + outbox；为什么通知不直接写进 `RegistrationService`。
- 状态机：`Registration` 内部守卫；避免控制器层散落状态判断。
- 替代方案：策略工厂新增 `GRADE_LIMITED`（需改枚举/工厂）vs 本次“DIRECT + 扩展规则表 + 装饰器”。
- 证据：`docs/02`、`docs/03`、`docs/04`、`docs/05`。
- 本页图表：职责边界图/策略-容量-服务关系图。

## 第 5 页：change-v2 变更影响与实现

- CR-01：年级限定直接报名；学生年级 1—4；仅大二/大三；公共约束叠加。
- CR-02：模拟邮件；`REGISTERED/APPROVED/REJECTED` 三类事件；站内通知继续运行。
- 影响分析结论：稳定抽象、扩展点、风险与决策（`docs/13`）。
- 实现方式：方案 A；新增 30 个 Java 类/接口；0 个核心业务类修改；3 个新表/扩展表。
- v2 类图/时序：`GradeAwareRegistrationServiceV2`、`EmailNotificationObserverV2`、outbox 派发。
- 本页图表：`class-diagram-v2`、`seq-register-grade-v2`、`seq-email-notify-v2`。

## 第 6 页：关键代码与数据设计

- `RegistrationService.register`：原有公共约束与事件发布主干（引用行号）。
- `GradeAwareRegistrationServiceV2.register`：规则查询 → 年级校验 → `super.register`。
- `EventDispatcher.dispatchPending`：outbox 认领、观察者调用、失败重试。
- `EmailNotificationObserverV2.onEvent`：三类事件、失败记录、幂等查询。
- 数据表：`student_grade_v2`、`activity_grade_rule_v2`、`email_notification_v2`；唯一键 `(source_event_id, student_id)`。
- 本页图表：关键代码片段 + 表结构小图。
- 讲解重点：哪些代码没有改、为什么没有改、扩展点在哪里。

## 第 7 页：测试与回归

- 测试结构：原有 5 个测试类/16 个方法；新增 3 个测试类/10 个方法；合计 8 类/26 方法。
- 覆盖点：策略/容量/状态、年级边界、邮件事件与失败、Web 健康与首页。
- 本地执行证据：`mvn test` 的 26 个测试、0 失败、0 错误；注明这是构建阶段证据。
- 回归清单：活动 CRUD、浏览筛选、直接/审核报名、审批/拒绝/取消、站内通知、outbox、统计、并发、旧页面。
- 验收结果：留空，引用学生/教师实际记录；本大纲不推断。
- 本页表格：baseline vs v2 测试数量与覆盖矩阵。

## 第 8 页：部署、可维护性与 AI 协作总结

- 部署：JVM 17/21、便携 PostgreSQL 16、systemd、Nginx Basic Auth/IP 白名单；健康检查 `/health`。
- 运行脚本：`start-local.ps1`、`stop-local.ps1`、`status-local.ps1`。
- 可维护性对照：85→115 个主 Java 文件；+30 类；0 个核心业务类修改；5→8 个测试类；16→26 个测试方法。
- AI_DEV_LOG_v2：第13批影响分析、第14批实现、第15批报告；交互轮次与时长记录。
- 经验：观察者/端口使扩展容易；枚举工厂/具体服务依赖使扩展困难；`@Primary` 是约束下的折中。
- 后续改进：用例接口替代具体服务、真实邮件适配器、集成测试库、UML 全量 v2。
- 证据：`AI_DEV_LOG-v2.md`、`docs/15-maintainability-comparison-v2.md`、`docs/18-submission-checklist-v2.md`。

## 页数与材料控制

| 页 | 内容 | 主要材料 |
|---|---|---|
| 1 | 背景与范围 | `docs/01`、`README` |
| 2 | 需求与用例 | 用例图 v1/v2、`docs/01`、`docs/13` |
| 3 | 初始设计 | 类图 v1、状态图、时序图 v1 |
| 4 | 模式决策 | `docs/02`—`docs/05` |
| 5 | 变更实现 | `docs/13`、`docs/14`、v2 UML |
| 6 | 关键代码/数据 | 源码、`V3__grade_and_email-v2.sql` |
| 7 | 测试与回归 | 测试源码、Surefire 报告、`docs/10` |
| 8 | 部署与总结 | `docs/11`、`docs/15`、`AI_DEV_LOG-v2.md` |

> 若要求 5 页，可合并第 3+4 页、第 7+8 页；若要求 8 页，按上表展开。
