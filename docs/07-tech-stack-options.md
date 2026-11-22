# CampusEvent 技术栈方案（第8批，待选择）

> 状态：已选择 **A Spring Boot + PostgreSQL + 服务端渲染**。后续决定：**使用 systemd + jar 部署、本地 PostgreSQL 测试**；本地用便携版 PostgreSQL，集成测试用本地 PostgreSQL，部署用 JRE 17 + `java -jar` + PostgreSQL 服务。
>
> 本文件保留选型对比记录；以 `docs/08-project-skeleton.md` 的实现状态为准。
>
> 已确认的设计约束会影响选型：
> - 事务性 outbox + 派发器（需要事务、定时任务/后台 worker、重启后恢复）。
> - 名额批准需要并发控制（行锁/条件更新）；重复报名需要数据库唯一约束。
> - PostgreSQL 的部分唯一索引、`FOR UPDATE`、`SKIP LOCKED` 最贴合当前设计。
> - 最终要部署云服务器；固定演示账号；不做注册/复杂权限。
> - 使用 outbox + 观察者。

---

## 1. 方案总览

> 以下 A/B/C 是选型时的方案对比；其中的 Docker 部署仅作为当时的比较项。**最终决定：A（Spring Boot + PostgreSQL + 服务端渲染），使用 systemd + jar 部署。**

### 方案 A：Spring Boot 3 + PostgreSQL

- 后端：Java 17/21、Spring Boot 3.x、Spring Web、Spring Data JPA（或 MyBatis）、Bean Validation、Spring Scheduling。
- 数据库：PostgreSQL 16；迁移用 Flyway。
- 前端：Thymeleaf 服务端渲染（或简单 REST + 静态页）。
- 测试：JUnit 5、Spring Boot Test、MockMvc、Testcontainers（PostgreSQL）。
- 部署：Docker 镜像 + PostgreSQL（同 VM 或托管）；JVM 内存占用较高。

**优点**

- 事务、行锁、DI、定时任务最成熟；`@Transactional` + `SELECT ... FOR UPDATE` 直接匹配名额/审批并发。
- Flyway 迁移可写部分唯一索引；Testcontainers 可跑真实 PostgreSQL 集成测试。
- 策略/仓储/观察者等接口用 Spring Bean 装配很自然。
- 生态成熟，排错资料多。

**缺点**

- 样板代码多，初始开发速度偏慢；JVM 内存/启动成本高。
- 测试容器/集成测试环境配置比脚本语言重。
- 云服务器 512MB 内存可能紧张，通常要 1GB 左右。

### 方案 B：NestJS + PostgreSQL

- 后端：Node.js 20 + TypeScript + NestJS 10；TypeORM 或 Prisma；class-validator；`@nestjs/schedule`。
- 数据库：PostgreSQL 16；TypeORM migrations 或 Prisma Migrate。
- 前端：EJS/Handlebars 服务端渲染，或 React/Vue SPA（二选一，见待决策）。
- 测试：Jest + Supertest + Testcontainers（PostgreSQL）。
- 部署：Docker + PostgreSQL；Node 运行时内存中等。

**优点**

- TypeScript + 模块/DI，结构清晰，开发速度快于 Spring。
- TypeORM 支持 `where` 部分索引、`FOR UPDATE`、事务；Prisma 的部分唯一索引需要 raw SQL migration。
- Jest/Supertest 测试体验好；Docker 部署简单。
- 适合前后端同语言（若选 React/Vue）。

**缺点**

- Node 的事务/行锁需要自律；Prisma 与 TypeORM 的能力边界不同。
- 若选 SPA，会多一套构建/部署/CORS。
- 后台 outbox 派发器需要额外的调度/租约设计。

### 方案 C：FastAPI + PostgreSQL（可降级 SQLite）

- 后端：Python 3.11/3.12、FastAPI、SQLAlchemy 2.0、Alembic、Pydantic；APScheduler 或后台任务。
- 数据库：PostgreSQL 16（推荐）；SQLite（仅本地/演示）。
- 前端：Jinja2 服务端渲染，或静态页 + fetch；也可 React/Vue SPA。
- 测试：pytest + httpx/TestClient + Testcontainers（PostgreSQL）。
- 部署：Docker + PostgreSQL；uvicorn/gunicorn；内存占用低。

**优点**

- 开发速度最快；Python 代码简洁；pytest 测试成本低。
- SQLAlchemy 事务、`FOR UPDATE`、Alembic 迁移都能覆盖当前设计。
- 内存/云成本低；Docker 部署简单。
- 若课程/团队 Python 最熟，迭代最快。

**缺点**

- DI 较轻（`Depends`），分层/接口约束需要团队自律；否则容易把逻辑写进路由。
- SQLite 变体：部分唯一索引可用，但单写者、无行级锁；并发审批/云持久化弱。
- 后台派发器需要额外设计租约/重启恢复。

---

## 2. 四项对比

> 评分是相对比较：**开发速度越高越好；测试成本越低越好；部署难度越低越好；重启一致性维护成本越低越好**。

| 维度 | A Spring Boot + PostgreSQL | B NestJS + PostgreSQL | C FastAPI + PostgreSQL |
|---|---|---|---|
| **开发速度** | 中偏慢：DI/配置/样板多，但结构稳 | 快：TS + Nest 模块化 | 最快：Python 简洁，路由/服务上手快 |
| **测试成本** | 中高：JUnit/Testcontainers 配置重，但集成测试真实 | 中：Jest/Supertest + Testcontainers | 低中：pytest + httpx/Testcontainers，写测试快 |
| **云服务器部署难度** | 中：jar/Docker + PostgreSQL；JVM 内存较高 | 低中：Docker + Node + PostgreSQL | 低：Docker + Python + PostgreSQL；内存低 |
| **重启后数据一致性维护成本** | 低：ACID/行锁/Flyway 成熟；需处理 `PROCESSING` 事件租约 | 低中：PostgreSQL 一致；TypeORM/Prisma 迁移与锁需选型 | 低中：PostgreSQL 一致；Alembic + SQLAlchemy 锁需自律 |
| **事务/行锁适配** | 最强：`@Transactional`、`FOR UPDATE`、隔离级别 | 强：QueryRunner/TypeORM；Prisma 需 raw SQL | 强：SQLAlchemy Session/`FOR UPDATE` |
| **DI/策略/仓储适配** | 最强：Spring Bean 天然适合接口/策略 | 强：Nest Provider/Module | 中：`Depends` 较轻，需手动装配 |
| **内存/云成本** | 高（JVM） | 中（Node） | 低（Python） |
| **学习曲线** | 高 | 中 | 低 |
| **最适合** | Java 背景、重事务/重设计 | TS 背景、速度与结构平衡 | Python 背景、快速原型 |

### 2.1 数据库选择

| 数据库 | 部分唯一索引 | 行锁/并发 | 云部署/持久化 | 结论 |
|---|---|---|---|---|
| **PostgreSQL 16** | 支持 | `FOR UPDATE`、`SKIP LOCKED` | 成熟；托管/同 VM 均可 | **推荐**；最贴合当前设计 |
| MySQL 8 | 不支持部分索引；可用生成列/唯一键 | 行锁可用 | 成熟 | 可行；重复报名唯一约束要换写法 |
| SQLite | 支持部分索引 | 单写者；`BEGIN IMMEDIATE`；无行级锁 | 文件卷；单实例；备份要自己做 | 仅本地/演示；不建议作为最终云主库 |

> 当前设计需要“重复有效报名”的部分唯一约束、审批最后名额的行锁/条件更新、outbox 的 `PROCESSING` 租约；PostgreSQL 最省心。SQLite 可跑通功能，但并发审批和云持久化运维较弱。

### 2.2 前端形态

| 形态 | 优点 | 缺点 | 建议 |
|---|---|---|---|
| 服务端渲染（Thymeleaf/EJS/Jinja2） | 最少移动部件；部署简单；12 个用例够用 | 交互不如 SPA | **初始版本推荐** |
| SPA（React/Vue）+ REST | 交互好；前后端分离 | 多一套构建/部署/CORS；测试面更大 | 若你重视前端体验再选 |

### 2.3 认证

- 固定演示账号；学生/管理员角色。
- 可用 Session Cookie 或 HTTP Basic；不做注册/复杂权限。
- 角色校验放接口层，不进入领域模型。

---

## 3. 关键设计约束的匹配

| 约束 | A Spring Boot | B NestJS | C FastAPI |
|---|---|---|---|
| outbox 派发器 | `@Scheduled` + 事务 | `@nestjs/schedule` + 事务 | APScheduler/后台任务 + 事务 |
| 重启后 `PROCESSING` 回收 | 定时扫描 + `updatedAt` 超时 | 同左 | 同左 |
| 部分唯一索引 | Flyway 原生 SQL | TypeORM `where` / Prisma raw SQL | Alembic 原生 SQL |
| 名额行锁 | `SELECT ... FOR UPDATE` | QueryRunner/TypeORM | SQLAlchemy `with_for_update()` |
| 策略/仓储接口 | Spring Bean | Nest Provider | `Depends`/手工装配 |
| 集成测试 | Testcontainers | Testcontainers | Testcontainers |
| 云部署 | Docker + Postgres | Docker + Postgres | Docker + Postgres |

> 三种栈都能实现当前设计；差异主要在**开发速度、DI 约束强度、测试/部署成本**，不是“能不能做”。

---

## 4. 建议

- **如果团队 Java 背景强、最看重事务/DI 正确性**：选 **A Spring Boot + PostgreSQL**。最稳，但最重。
- **如果团队 TS/JS 背景、想要结构与速度平衡**：选 **B NestJS + PostgreSQL**。综合风险较低。
- **如果团队 Python 最熟、想最快出可运行版本**：选 **C FastAPI + PostgreSQL**。开发最快，但需要自律保持分层/事务边界。
- **数据库统一建议 PostgreSQL**；SQLite 只用于本地/演示，不作为最终云主库。
- **前端初始建议服务端渲染**；SPA 是额外成本，不是当前 12 个用例的必需品。

> 没有团队技术背景信息时，我不会替你定；请按“最熟的语言 + 是否重视事务/DI”选择。

---

## 5. 待你选择

请选择：

1. 后端栈：**A Spring Boot / B NestJS / C FastAPI**。
2. 数据库：**PostgreSQL / MySQL / SQLite**。
3. 前端：**服务端渲染 / SPA**。
4. 部署默认：**使用 systemd + jar 部署；JRE 17 + `java -jar` + PostgreSQL 服务**（systemd/NSSM/任务计划）；如你想用托管数据库/其他方式，请说明。

选择后我再输出：项目目录结构、四层划分、接口空实现/骨架、数据库表草案与唯一约束、测试目录建议、Git 分支/标签计划。
