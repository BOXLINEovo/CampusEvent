# CampusEvent 云服务器部署方案（第11批）

> 状态：**文档交付**。服务器、域名、DNS、TLS 证书等由你自行部署；本文只提供方案、配置模板、迁移步骤、检查表与验证方法。
>
> 本文**不给最终可执行命令序列**，也不声称已部署/已验证；实际执行与结果由你负责。
>
> 已确认约束：**使用 `systemd` 运行** Spring Boot jar；PostgreSQL 本机/托管二选一。

---

## 0. 部署边界

| 项 | 由你负责 | 本文提供 |
|---|---|---|
| 云账号/VM | 选购、系统安装、网络/安全组 | 推荐 OS/规格/端口说明 |
| 域名/DNS/TLS | 域名、解析、证书签发/续期 | Nginx 模板、TLS 注意事项 |
| 数据库实例 | 安装/托管、备份策略执行 | 角色/库/权限、迁移、备份命令模板 |
| 应用运行 | 上传 jar、配置、启停、验收 | systemd/env 模板、检查表 |
| 访问控制 | 决定内网/演示范围、账号 | Nginx Basic Auth / IP 白名单模板；应用当前无登录 |
| 实际命令执行 | 全部由你执行 | 命令清单/占位模板，不代执行 |
| 验收结果 | 由你填写 | 重启一致性验证步骤模板 |

**部署拓扑（推荐）**

```text
Internet
   │ 80/443
   ▼
 Nginx (TLS, 反向代理)
   │ 127.0.0.1:8080
   ▼
 Spring Boot (systemd, 非 root)
   │ 127.0.0.1:5432
   ▼
 PostgreSQL 16 (仅本机)
```

> 不开放 8080/5432 到公网；云安全组与 UFW 同时限制。

---

## 1. 部署方案

### 1.1 推荐方案（课程项目，单 VM）

> 以下是**文档默认假设**；你的实际 OS/数据库/域名由你决定，替换对应项即可。

| 项 | 选择 |
|---|---|
| 操作系统 | **Ubuntu 24.04 LTS**（备选：Ubuntu 22.04 LTS / Debian 12） |
| 运行时 | **OpenJDK 17（Temurin/Ubuntu 包）**，`java -jar` 运行 Spring Boot |
| 数据库 | **PostgreSQL 16，与应用同一台云 VM**；仅监听 `127.0.0.1:5432` |
| 进程管理 | **systemd** |
| 反向代理 | **Nginx**，TLS 终止，转发到 `127.0.0.1:8080` |
| 访问控制 | **云端浏览器访问**；Nginx Basic Auth + IP 白名单；应用当前无真实登录 |
| 防火墙 | **UFW**：开放 22、80、443；**不开放 8080/5432** |
| 域名/TLS | 推荐域名 + Let's Encrypt；无域名时仅 HTTP + IP，仅用于课程演示 |
| 应用监听 | `server.address=127.0.0.1`、`server.port=8080` |
| 部署目录 | `/opt/campusevent/`（jar、版本文件） |
| 配置/密钥 | `/etc/campusevent/campusevent.env`（`root:campusevent`，600） |
| 日志 | systemd journal（`journalctl -u campusevent`），不单独写文件 |
| 备份 | `pg_dump` 每日 + 部署前强制备份；保留策略见决策项 |

### 1.2 备选方案

- **托管 PostgreSQL**（云数据库）：应用只连内网/白名单地址；运维更省，成本更高。
- **Ubuntu 22.04 LTS**：更常见、资料多；JDK 17/PostgreSQL 16 同样可用。
- **Nginx 仅 HTTP**：没有域名/证书时可用，但不建议长期暴露明文。
- **不用 Nginx，直接 8080**：不推荐；会暴露应用端口，缺少 TLS/限流/统一入口。

---

## 2. 环境变量与配置模板

> 以下均为模板，密码用 `__CHANGE_ME__` 占位；不要提交真实密码到 Git。

### 2.1 `/etc/campusevent/campusevent.env`

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://127.0.0.1:5432/campusevent
DB_USER=campusevent_app
DB_PASSWORD=__CHANGE_ME__
SERVER_ADDRESS=127.0.0.1
PORT=8080
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC
TZ=Asia/Shanghai

# 若采用“独立迁移角色 + 应用只读/读写角色”：
SPRING_FLYWAY_ENABLED=false
```

- 文件权限：`root:campusevent 0640` 或 `root:root 0600`；systemd 以 root 读取后再降权。
- 若采用“简化方案（应用角色 owner + Flyway 启动迁移）”：`SPRING_FLYWAY_ENABLED=true`，`DB_USER=campusevent`（owner）。

### 2.2 `application-prod.yml`（可选，放 jar 同级或 `config/`）

```yaml
server:
  address: ${SERVER_ADDRESS:127.0.0.1}
  port: ${PORT:8080}
  forward-headers-strategy: framework

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: ${SPRING_FLYWAY_ENABLED:false}
    locations: classpath:db/migration
  thymeleaf:
    cache: true

logging:
  level:
    root: INFO
    com.campusevent: INFO
```

> 生产不加载 `application-dev.yml`，不会执行 `db/dev/V2__demo_data.sql`。

### 2.3 `systemd` 单元模板

`/etc/systemd/system/campusevent.service`：

```ini
[Unit]
Description=CampusEvent Spring Boot application
After=network-online.target postgresql.service
Wants=network-online.target

[Service]
Type=simple
User=campusevent
Group=campusevent
WorkingDirectory=/opt/campusevent
EnvironmentFile=/etc/campusevent/campusevent.env
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/campusevent/campus-event.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ProtectKernelTunables=true
ProtectControlGroups=true
RestrictSUIDSGID=true
LockPersonality=true
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

- `ProtectSystem=strict` 使 `/opt` 只读；应用只读 jar 即可。
- 应用不写本地文件；日志走 journald。
- 若需要 JVM 诊断文件，另加可写的 `ReadWritePaths=`。

### 2.4 Nginx 模板

`/etc/nginx/sites-available/campusevent`：

```nginx
server {
    listen 80;
    server_name __YOUR_DOMAIN__;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name __YOUR_DOMAIN__;

    ssl_certificate     /etc/letsencrypt/live/__YOUR_DOMAIN__/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/__YOUR_DOMAIN__/privkey.pem;

    server_tokens off;
    client_max_body_size 1m;

    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy no-referrer always;
    add_header Strict-Transport-Security "max-age=31536000" always;

    location / {
        # 当前应用没有真实登录；至少用 Basic Auth 或 IP 白名单保护整个站点。
        auth_basic           "CampusEvent demo";
        auth_basic_user_file /etc/nginx/.htpasswd;

        # 可选：仅允许校园网/VPN 网段（按你的实际网段替换）
        # allow 203.0.113.0/24;
        # deny all;

        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
```

- 无域名时：仅保留 `listen 80`，`server_name _;`，直接 `proxy_pass`；仅课程演示使用。
- 证书用 Certbot/Let's Encrypt；续期由 timer 自动处理。
- 当前应用**没有真实登录**；用 `htpasswd` 生成 Basic Auth 文件（如 `sudo htpasswd -c /etc/nginx/.htpasswd demo`），再加 IP 白名单更稳。不要把 `/admin/**` 无保护地暴露到公网。
- **完全内网/校园网**：可只保留 IP 白名单（注释 Basic Auth）；**公网演示**：Basic Auth 必须保留。二者都不是应用内登录。

---

## 3. 数据库初始化/迁移步骤

### 3.1 角色与库（推荐最小权限）

```sql
-- 以 postgres 管理账号执行
CREATE ROLE campusevent_owner LOGIN PASSWORD '__OWNER_PASSWORD__';
CREATE ROLE campusevent_app   LOGIN PASSWORD '__APP_PASSWORD__';

CREATE DATABASE campusevent OWNER campusevent_owner;
```

```sql
-- 连接到 campusevent 库后
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO campusevent_app;

-- 迁移角色创建表后，给应用角色 DML 权限
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO campusevent_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO campusevent_app;

ALTER DEFAULT PRIVILEGES FOR ROLE campusevent_owner IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO campusevent_app;
ALTER DEFAULT PRIVILEGES FOR ROLE campusevent_owner IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO campusevent_app;
```

- `campusevent_app` 无 DDL/超级权限；应用启动时 `SPRING_FLYWAY_ENABLED=false`。
- `campusevent_owner` 仅用于迁移。
- 迁移后补执行 `GRANT ... ON ALL TABLES/SEQUENCES`；`ALTER DEFAULT PRIVILEGES` 可提前设置，保证后续表也有权限。

**PostgreSQL 监听/认证模板**

`postgresql.conf`：

```ini
listen_addresses = 'localhost'
password_encryption = 'scram-sha-256'
```

`pg_hba.conf`：

```ini
local   all             all                                     scram-sha-256
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
```

> 不开放 5432 到公网；云安全组同样只允许本机/内网。

### 3.2 简化方案

若课程时间紧，可让 `campusevent` 角色拥有数据库并启动 Flyway：

```sql
CREATE ROLE campusevent LOGIN PASSWORD '__CHANGE_ME__';
CREATE DATABASE campusevent OWNER campusevent;
```

- `DB_USER=campusevent`，`SPRING_FLYWAY_ENABLED=true`。
- 代价：应用角色拥有 DDL 权限；安全边界较弱。作为决策项 D11-05。

### 3.3 迁移执行

1. **部署前备份**（已有库）：
   ```bash
   pg_dump -Fc -h 127.0.0.1 -U campusevent_owner -d campusevent \
     -f /var/backups/campusevent-$(date +%F-%H%M).dump
   ```
   （密码用 `.pgpass` 或交互输入，不要写进命令历史。）
2. 首次部署：
   - 用 owner 角色执行 Flyway（推荐独立角色时）；或使用简化方案，由应用启动时执行。
   - 迁移文件来自 jar 的 `db/migration`（Flyway 默认）。
3. 验证：
   ```sql
   SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
   SELECT count(*) FROM activity;
   SELECT count(*) FROM registration;
   SELECT count(*) FROM message;
   SELECT count(*) FROM outbox_event;
   ```
4. 应用启动后 `spring.jpa.hibernate.ddl-auto=validate` 校验实体与表结构。
5. 失败处理：不要手工改表；保留日志，按回滚步骤恢复备份或修复迁移后重试。

> 若采用独立迁移角色，迁移命令的具体形式（Flyway CLI / 一次性迁移配置）由你按实际环境选择；本文给步骤与校验点。

---

## 4. 部署命令清单与回滚步骤

> 以下是**命令清单/占位模板**，不是最终执行序列；具体命令与参数由你按实际环境执行，本文不代执行、不声称部署结果。不要在生产直接复制未经确认的片段。

### 4.1 部署清单

1. **构建与校验**
   - 本地/CI：`mvn -DskipTests package`
   - 记录 `sha256sum`、版本号、Git tag（`baseline-v1`）。
2. **服务器准备**
   - OS 更新、时区、创建非 root 用户 `campusevent`、目录 `/opt/campusevent`、`/etc/campusevent`。
   - 安装 JDK 17、PostgreSQL 16、Nginx、UFW、Certbot。
3. **数据库**
   - 创建角色/库；配置 `listen_addresses='localhost'`、`pg_hba.conf` scram；执行迁移。
4. **应用**
   - 上传 jar 到 `/opt/campusevent/campus-event.jar`；保留上一版本为 `.prev`。
   - 创建 env 文件（600）；`systemd` 单元；`daemon-reload`。
5. **Nginx/TLS**
   - 配置站点、`nginx -t`、启用站点、申请/安装证书、reload。
6. **防火墙**
   - UFW 默认拒绝；允许 22（尽量限管理员 IP）、80、443；不开放 8080/5432。
7. **启动与检查**
   - `systemctl start campusevent`；`systemctl status`；`journalctl`；`curl /health`；页面检查。
8. **备份/监控**
   - 部署前 `pg_dump`；配置每日备份/保留；日志检查。

### 4.2 回滚步骤

1. **应用回滚**
   - `systemctl stop campusevent`
   - 将 `campus-event.jar.prev` 换回 `campus-event.jar`
   - `systemctl start campusevent`
   - `systemctl status` / `journalctl` / `/health`
2. **Nginx 回滚**
   - 恢复上一份 `sites-available` 配置；`nginx -t`；`systemctl reload nginx`
3. **数据库回滚**
   - Flyway 迁移是前向的；若新版本 schema 不兼容：
     - 优先：部署前 `pg_dump` 备份恢复（会丢失部署后数据，需评估）。
     - 或：编写 compensating migration（不删除已有数据）。
   - 不要在未备份时手工 drop 表。
4. **配置回滚**
   - env/unit 文件保留 `.bak`；恢复后 `daemon-reload` + restart。
5. **记录**
   - 记录回滚原因、版本、时间、数据影响；更新部署记录。

---

## 5. 重启一致性验证的操作步骤（由你执行）

> 以下只给操作步骤；**不声称已执行、不声称通过**，实际结果由你填写。

### 5.1 重启前快照

在数据库执行并保存输出：

```sql
SELECT count(*) AS activities FROM activity;
SELECT status, count(*) FROM registration GROUP BY status ORDER BY status;
SELECT count(*) AS messages FROM message;
SELECT status, count(*) FROM outbox_event GROUP BY status ORDER BY status;

-- 可选：按稳定排序导出关键字段做比对
SELECT activity_id, title, capacity, policy_type, deleted_at
FROM activity ORDER BY activity_id;

SELECT registration_id, activity_id, student_id, status
FROM registration ORDER BY registration_id;

SELECT message_id, type, title, is_read, source_event_id
FROM message ORDER BY message_id;
```

- 建议同时保存 `pg_dump -Fc` 或 `--data-only` 备份。
- 注意：outbox 派发器每 5 秒运行，`PENDING/SENT` 计数可能在快照间变化；比对时说明时间窗。

### 5.2 重启

```bash
sudo systemctl restart campusevent
sudo systemctl status campusevent --no-pager
sudo journalctl -u campusevent -n 100 --no-pager
curl -fsS http://127.0.0.1:8080/health
```

- 若通过 Nginx：`curl -fsS https://__YOUR_DOMAIN__/health`。
- 等待应用启动完成（日志出现 `Started CampusEventApplication`）。

### 5.3 重启后比对

重新执行 5.1 的 SQL，逐项比对：

- 活动数/ID/标题/容量/策略/删除标记。
- 报名按状态计数；重点看 PENDING/APPROVED 是否变化。
- 消息数/类型/已读状态/来源事件 ID。
- outbox 状态；是否有异常 `FAILED`/重试。
- 页面：活动列表、详情、我的报名、通知、审批、统计是否可打开。

### 5.4 结果记录

由你填写：

| 项 | 重启前 | 重启后 | 是否一致 | 备注 |
|---|---|---|---|---|
| 活动数 |  |  |  |  |
| 报名状态计数 |  |  |  |  |
| 消息数 |  |  |  |  |
| outbox 状态 |  |  |  |  |
| 页面可访问 |  |  |  |  |

---

## 6. 安全与最小权限注意事项

- **SSH**：仅密钥登录；禁用 root 密码登录；尽量限制 22 端口来源 IP；可选 fail2ban。
- **系统用户**：应用用非 root 的 `campusevent` 用户；systemd  hardening（`NoNewPrivileges`、`ProtectSystem`、`PrivateTmp` 等）。
- **数据库**：
  - 只监听 `127.0.0.1`；`pg_hba.conf` 仅 local/127.0.0.1 + scram-sha-256。
  - 应用角色最小权限（DML，无 DDL）；迁移角色单独；不要用 postgres 超级用户跑应用。
  - 定期备份；备份文件加密/限权；验证恢复。
- **密钥**：`/etc/campusevent/campusevent.env` 600；不提交 Git；轮换密码；不在日志打印密码。
- **应用**：`server.address=127.0.0.1`；不直接暴露 8080；生产 profile；不加载 dev seed；错误页不暴露堆栈。当前应用**没有真实登录**：必须用 Nginx Basic Auth/IP 白名单保护整站，不要公开 `/admin/**`。
- **Nginx**：TLS、HSTS、`server_tokens off`、安全头、请求体限制；可选限流。
- **防火墙**：默认 deny；只开 22/80/443；不开放 8080/5432；云安全组同样限制。
- **更新**：JDK/PostgreSQL/Nginx/OS 安全更新；关注 Flyway 迁移兼容性。
- **日志**：journald 限制保留；不记录密码/token；监控 `FAILED` outbox 与 5xx。
- **最小权限**：应用只读 jar；不授予 sudo；数据库角色只访问本库。

---

## 7. 部署检查表

- [ ] 域名/DNS 已指向服务器（或确认仅 IP + HTTP 演示）。
- [ ] OS 更新、时区、非 root 用户、目录权限。
- [ ] JDK 17 已安装并验证 `java -version`。
- [ ] PostgreSQL 16 已安装；仅监听 localhost；scram 认证。
- [ ] 数据库/角色/权限已创建；迁移成功；`flyway_schema_history` 正确。
- [ ] 应用 jar 已上传；sha256/版本记录；上一版本保留。
- [ ] env 文件 600；生产 profile；dev seed 不会加载。
- [ ] systemd 单元已启用；`systemctl status` 正常；重启策略。
- [ ] Nginx 配置 `nginx -t` 通过；TLS 证书有效；安全头。
- [ ] 访问控制：Basic Auth/IP 白名单已生效；`/admin/**` 未公开。
- [ ] UFW/云安全组只开 22/80/443。
- [ ] `/health`、首页、活动/报名/审批/通知/统计页面可访问。
- [ ] 部署前备份完成；恢复步骤已演练/确认。
- [ ] 回滚 jar/配置/DB 步骤明确。
- [ ] 日志无异常；outbox 无异常 FAILED。
- [ ] 重启一致性验证步骤已执行并记录（由你填写）。

---

## 8. 文档默认假设与可替换项

> 服务器/域名由你自行部署；以下不再是待确认阻塞项，而是**文档默认值**。若你的实际环境不同，告诉我具体 OS/DB/代理，我再改文档。

| 编号 | 文档默认 | 可替换 |
|---|---|---|
| D11-01 | Ubuntu 24.04 LTS | Ubuntu 22.04 / Debian 12 / 其他 |
| D11-02 | systemd | 你的进程管理器；使用 systemd |
| D11-03 | 同 VM PostgreSQL 16 | 托管 PostgreSQL |
| D11-04 | 域名 + Let's Encrypt（若你提供域名） | IP + HTTP 演示（不建议长期） |
| D11-05 | 独立 owner/app 角色（最小权限） | 简化：应用角色 owner + Flyway |
| D11-06 | 本地/CI 构建后上传 jar | 服务器上构建 |
| D11-07 | 备份保留 30 天 + 部署前强制备份 | 7 天/其他 |
| D11-08 | 域名/证书由你自行部署 | — |
| D11-09 | 云端浏览器访问；应用无登录；Nginx Basic Auth + IP 白名单 | 仅 IP 白名单 / 仅 Basic Auth；加应用登录属于后续变更 |

### 检查点

- [ ] 你的实际 OS/数据库/反向代理是否与默认不同；如不同，告知我调整文档。
- [ ] 是否采用最小权限数据库角色；若采用，迁移步骤需按 3.1/3.3 执行。
- [ ] 域名/证书由你处理；本文只给 Nginx 模板。
- [ ] 部署前备份、回滚 jar、回滚 Nginx 配置的路径已确认。
- [ ] 重启一致性验证由你执行并填写结果；本文不代填。

---

## 9. 说明

- 本批**未执行任何服务器操作**，未声称部署成功或通过。
- 未提供真实密码；模板中的 `__CHANGE_ME__` 需你替换并妥善保管。
- 部署命令与重启一致性结果由你执行/填写。
- 服务器、域名、DNS、TLS 由你自行部署；本文只提供文档、模板和检查表。
- **Windows 使用 JDK + 便携 PostgreSQL**：本地用便携版 PostgreSQL + `scripts/start-local.ps1`；云端 Linux 用 systemd。
- **使用者只需浏览器**；当前应用无登录，必须用 Nginx Basic Auth/IP 白名单保护，不要公开 `/admin/**`。
