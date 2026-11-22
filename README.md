# CampusEvent

校园活动报名与管理系统 — 初始功能实现。

> 状态：初始功能已实现；测试执行与验收由你负责。
>
> 设计评审包：`docs/06-design-review-package.md`；初始实现说明：`docs/09-initial-implementation.md`。

## 技术栈

- Java 17
- Spring Boot 3.3.x
- Spring MVC + Thymeleaf 服务端渲染
- 共享样式：`src/main/resources/static/css/app.css`
- Spring Data JPA
- PostgreSQL 16
- Flyway
- JUnit 5 / Spring Boot Test

## 运行

本地使用便携版 PostgreSQL + JDK：

```powershell
# 启动 PostgreSQL + 应用（dev profile 加载演示学生/活动）
powershell -ExecutionPolicy Bypass -File scripts/start-local.ps1

# 状态 / 停止
powershell -ExecutionPolicy Bypass -File scripts/status-local.ps1
powershell -ExecutionPolicy Bypass -File scripts/stop-local.ps1
```

> `db/dev/V2__demo_data.sql` 只在 dev profile 加载；生产 profile 使用正式数据。

访问：

- 首页：http://localhost:8080/
- 活动列表：http://localhost:8080/activities
- 我的报名：http://localhost:8080/my/registrations
- 站内通知：http://localhost:8080/notifications
- 活动管理：http://localhost:8080/admin/activities
- 待审核：http://localhost:8080/admin/approvals
- 统计：http://localhost:8080/admin/statistics
- 健康检查：http://localhost:8080/health

## 访问与权限

- 初始版本使用固定演示身份：学生为演示学生 A；`/admin/**` 使用固定管理员路径。
- 给其他人使用时建议云端访问 + Nginx Basic Auth/IP 白名单；其他人使用浏览器即可。
- 当前应用使用固定演示身份；正式多用户登录/角色属于后续阶段。

## 测试

```bash
mvn test
```

当前测试文件：

- `HealthControllerTest`、`HomeControllerTest`（Web 冒烟）
- `DefaultCapacityRuleTest`、`RegistrationStateTest`、`ActivityTimeTest`（领域单测）

> 测试执行由你负责；本仓库不预填通过/失败结果。独立测试库：`src/test/resources/application-test.yml` + `scripts/create-test-db.ps1`（`campusevent_test`）。

## 当前实现范围

- 活动创建/编辑/软删除，浏览/名称筛选。
- 直接/审核报名、取消、重复有效报名约束。
- 批准/拒绝、批准时二次名额检查。
- 站内通知（outbox + 观察者 + 幂等/重试）。
- 活动统计（已通过、待审核、剩余名额）。
- JPA 实体/仓储适配、Thymeleaf 页面、共享 CSS、固定演示身份；页面使用居中内容列、宽表/筛选同宽 1040、表单页 760、统一字阶/按钮/提示样式。
- 同一活动的报名/审批/编辑/软删除使用活动行悲观锁（D10-02）。
- 包体与复用：精简 validation/aspectjweaver/websocket 依赖，jar 约 44.47 MB；使用 `ActivityViewAssembler` 统一活动视图映射；测试只保留 `src/test` 正式文件。
- 后续阶段：真实登录/权限、候补、签到、退款、管理员站内消息。

## 目录

```text
src/main/java/com/campusevent/
  domain/         领域模型、策略、容量规则、领域事件
  application/    用例服务、端口、DTO、策略工厂
  interfaces/     Web 入口
  infrastructure/ 持久化、outbox、通知适配
```

## UML 图

源文件在 `docs/diagrams/*.puml`；导出的 SVG/PNG 在 `docs/diagrams/export/`。

- 用例图：`use-case`
- 类图 V1：`class-diagram-v1`
- 状态机：`state-registration`
- 直接报名：`seq-register-direct`
- 审核报名：`seq-register-review`
- 批准成功：`seq-approve-success`
- 批准满额失败：`seq-approve-full`
- 拒绝：`seq-reject`
- 取消：`seq-cancel`

## 文档

- 需求与用例：`docs/01-use-cases.md`
- 类图与职责：`docs/02-class-diagram-v1.md`
- 策略/名额决策：`docs/03-registration-policy-capacity.md`
- 通知决策：`docs/04-notification-decoupling.md`
- 时序图与契约：`docs/05-sequence-and-contracts.md`
- 设计评审包：`docs/06-design-review-package.md`
- 技术栈方案：`docs/07-tech-stack-options.md`
- 项目骨架：`docs/08-project-skeleton.md`
- 初始功能实现：`docs/09-initial-implementation.md`
- 测试辅助材料：`docs/10-test-assistance.md`
- 云服务器部署方案：`docs/11-deployment-plan.md`
- baseline-v1 封存：`docs/12-baseline-v1.md`

## Git 仓库

- 本地独立仓库，分支 `main`。
- 首次提交：`baseline-v1: initial CampusEvent implementation`。
- `baseline-v1` 标签待验收/部署确认后创建；后续变更从该标签切 `change-v2`。
