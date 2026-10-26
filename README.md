# TeamFlow Server

Java 21 / Spring Boot 3.5 模块化单体，使用 MySQL、MyBatis-Plus 与 Flyway。API、数据库与权限契约位于独立根仓库的 `docs/`，本仓库只维护服务端源码与运行脚本。

## 本地构建与测试

```text
mvn clean verify
```

使用隔离 MySQL 8.0.16+ 运行真实数据库测试时，设置 `TEAMFLOW_IT_DB_URL`、相同值的 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 后执行：

```text
mvn -Dtest=ReportingIT,Phase2TransactionIT test
```

有 Docker daemon 的环境可运行 `mvn -Dtest=ContainerMySqlIT test`；该用例从空 MySQL 8.4.13 容器验证 Flyway V1/V2/V3、CHECK 与跨账号项目授权。GitHub Actions 中的 `.github/workflows/ci.yml` 会执行常规验证和此容器用例；本机没有 Docker，不能把容器用例算作已通过。

## 启动与演示数据

常规启动使用 `mvn spring-boot:run`，本地测试值在 `application.yml`。生产镜像强制启用 `prod` profile，必须提供 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`；缺少配置时启动失败。首次管理员引导与 Cookie 配置见根仓库 `README.md`。

本地测试管理员账号已经存在后，可通过 `TEAMFLOW_DEMO_ADMIN_USERNAME`、`TEAMFLOW_DEMO_ADMIN_PASSWORD`、`TEAMFLOW_DEMO_USER_PASSWORD` 环境变量运行 `node scripts/seed-demo.mjs`。它只允许本机 API，创建两名普通用户、一个项目和四种状态的任务，随后核对统计；不在源码或输出中保存密码。`node scripts/bench-read.mjs` 用于受限的本机只读性能冒烟，需要提供测试账号、项目 ID 和密码，默认 100 请求/并发 5。真实跨账号安全流程可运行 `node scripts/phase2-smoke.mjs`，会写入隔离测试库。
