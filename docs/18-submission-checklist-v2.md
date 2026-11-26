# 提交材料清单核对表（change-v2）

> 核对对象：`实验/CampusEvent`。验收结果列由学生/教师填写，本文件不预填。

## 1. 代码与构建

| 项目 | 路径/命令 | 状态 | 备注 |
|---|---|---|---|
| Maven 构建 | `pom.xml` | [ ] | Java 17 release；当前环境 JDK 21 可编译 |
| 主代码 | `src/main/java` | [ ] | baseline 85 + v2 30 = 115 个 Java 文件 |
| 测试代码 | `src/test/java` | [ ] | 5 + 3 = 8 个测试类 |
| 数据库迁移 | `src/main/resources/db/migration` | [ ] | V1、V3；V3 为 v2 新表 |
| dev 演示数据 | `src/main/resources/db/dev` | [ ] | V2、V4；V4 为年级演示数据 |
| 模板 | `src/main/resources/templates` | [ ] | 3 个 v2 页面 |
| 样式 | `src/main/resources/static/css/app.css` | [ ] | baseline 样式沿用 |
| 本地构建 | `mvn test` | [ ] | 本地记录 26 个测试、0 失败、0 错误；验收另填 |
| 打包 | `mvn -DskipTests package` | [ ] | jar 路径 `target/campus-event-0.0.1-SNAPSHOT.jar` |

## 2. 文档

| 材料 | 路径 | 状态 | 备注 |
|---|---|---|---|
| baseline 文档 01—12 | `docs/01`—`docs/12` | [ ] | v1 设计/实现/部署 |
| 变更影响分析 | `docs/13-change-impact-v2.md` | [ ] | CR-01/CR-02、A/B 方案 |
| 实现记录 | `docs/14-change-implementation-v2.md` | [ ] | 文件清单、diff 计划、测试、指标 |
| 可维护性对照 | `docs/15-maintainability-comparison-v2.md` | [ ] | baseline/v2 客观证据 |
| 项目总结大纲 | `docs/16-project-summary-outline-v2.md` | [ ] | 5—8 页 |
| 答辩问题清单 | `docs/17-defense-qa-v2.md` | [ ] | 设计取舍与关键代码 |
| 提交清单 | `docs/18-submission-checklist-v2.md` | [ ] | 本文件 |
| baseline AI 日志 | `AI_DEV_LOG.md` | [ ] | baseline 10 条代表性事件 |
| v2 AI 日志 | `AI_DEV_LOG-v2.md` | [ ] | 第13—15批汇总 |
| README | `README.md` | [ ] | baseline 运行说明；v2 说明以 docs 为准 |

## 3. UML

### v1（保持原样）

| 图 | 源文件 | 导出 | 状态 |
|---|---|---|---|
| 用例图 | `docs/diagrams/use-case.puml` | `export/use-case.svg/png` | [ ] |
| 类图 V1 | `class-diagram-v1.puml` | `export/class-diagram-v1.svg/png` | [ ] |
| 状态图 | `state-registration.puml` | `export/state-registration.svg/png` | [ ] |
| 直接报名 | `seq-register-direct.puml` | `export/seq-register-direct.svg/png` | [ ] |
| 审核报名 | `seq-register-review.puml` | `export/seq-register-review.svg/png` | [ ] |
| 批准成功 | `seq-approve-success.puml` | `export/seq-approve-success.svg/png` | [ ] |
| 批准满额 | `seq-approve-full.puml` | `export/seq-approve-full.svg/png` | [ ] |
| 拒绝 | `seq-reject.puml` | `export/seq-reject.svg/png` | [ ] |
| 取消 | `seq-cancel.puml` | `export/seq-cancel.svg/png` | [ ] |

### v2（新增）

| 图 | 源文件 | 导出 | 状态 |
|---|---|---|---|
| 用例图 v2 | `use-case-v2.puml` | `export/use-case-v2.svg/png` | [ ] |
| 增量类图 v2 | `class-diagram-v2.puml` | `export/class-diagram-v2.svg/png` | [ ] |
| 年级报名时序 | `seq-register-grade-v2.puml` | `export/seq-register-grade-v2.svg/png` | [ ] |
| 邮件通知时序 | `seq-email-notify-v2.puml` | `export/seq-email-notify-v2.svg/png` | [ ] |

## 4. 部署与运维

| 项目 | 路径/命令 | 状态 | 备注 |
|---|---|---|---|
| 启动脚本 | `scripts/start-local.ps1` | [ ] | 便携 PostgreSQL + jar |
| 停止脚本 | `scripts/stop-local.ps1` | [ ] | 停止 8080/5432 |
| 状态脚本 | `scripts/status-local.ps1` | [ ] | 检查端口 |
| 测试库脚本 | `scripts/create-test-db.ps1` | [ ] | 独立测试库 |
| 健康检查 | `GET /health` | [ ] | 本地返回 `{"status":"UP"}` |
| 部署说明 | `docs/11-deployment-plan.md` | [ ] | systemd、Nginx、Basic Auth/IP 白名单 |
| 配置 | `src/main/resources/application*.yml` | [ ] | DB_URL/DB_USER/DB_PASSWORD/PORT 可用环境变量覆盖 |

## 5. Git 与提交范围

| 检查项 | 命令/标准 | 状态 |
|---|---|---|
| 当前分支 | `git branch --show-current` 为 `main` | [ ] |
| baseline 提交 | `90b1d14 baseline-v1: initial CampusEvent implementation` | [ ] |
| baseline 标签 | `git tag --list` 当前为空；验收后补 `baseline-v1` | [ ] |
| 原源码差异 | `git diff --name-only -- src/main src/test` 为空 | [ ] |
| v1 UML 差异 | v1 源文件与导出图无 diff | [ ] |
| v2 新文件 | `git status --short` 中为 `??`，路径以 `V2`/`-v2` 区分 | [ ] |
| 非本轮改动 | `AI_DEV_LOG.md` 修改、两份文档删除不纳入 v2 提交 | [ ] |
| v2 提交 | 建议 `feat: implement change-v2 grade rules and mock email` | [ ] |
| v2 标签 | 建议验收后 `git tag -a change-v2 -m "CampusEvent change-v2"` | [ ] |

## 6. 待学生/教师补充

- [ ] 课程验收实际结果（通过/不通过、问题记录）。
- [ ] 教师签字/日期。
- [ ] 小组成员姓名、分工、答辩分工。
- [ ] 实际回归执行记录（命令、时间、截图）。
- [ ] 若课程要求，补充验收截图与部署截图。
- [ ] 若要求 README 索引 v2 文档，需单独批准修改 `README.md`；当前保持 baseline。

## 7. 提交前红线检查

- [ ] 不提交 `target/`、IDE 配置、日志、临时文件。
- [ ] 不提交数据库密码/密钥；dev 默认密码仅用于本地演示。
- [ ] 不把教师专用材料/隐藏验收材料放入提交包。
- [ ] 不在报告中填写未提供的验收结果。
- [ ] 不修改 v1 UML 源文件与导出图。
- [ ] 不把 `AI_DEV_LOG.md` 的既有非本轮改动混入 v2 提交。
- [ ] Java 文件使用 `V2` 后缀，非 Java 文件使用 `-v2`；在报告中解释命名约束。
