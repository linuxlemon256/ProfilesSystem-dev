# ProfilesSystem-dev

**简体中文** · [English](README.md) · [← 公众版](../ProfilesSystem/README.zh-CN.md)

![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 4.1.2](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?logo=spring&logoColor=white)
![MySQL 8](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![ORM: MyBatis](https://img.shields.io/badge/ORM-MyBatis-DC382D)
![LLM: OpenAI / Ollama](https://img.shields.io/badge/LLM-OpenAI%20%2F%20Ollama-412991)
[![License: Non-Commercial](https://img.shields.io/badge/License-Non--Commercial-red)](./LICENSE)
![Version](https://img.shields.io/badge/version-v0.4.0-informational)
![Status](https://img.shields.io/badge/status-experimental-blue)
![部署环境: 内网](https://img.shields.io/badge/%E9%83%A8%E7%BD%B2%E7%8E%AF%E5%A2%83-%E5%86%85%E7%BD%91-0b7285)

在公众版基础上继续开发的分支：学习组、好友私信、以及带额度控制的大模型问答。其余功能与公众版一致。

---

开发版与公众版共用同一套骨架：Spring Boot 单体 + MyBatis + MySQL，31 个基础页面之外多了 3 个页面，数据表从 11 张扩到 17 张。新模块目前**没有写进 `CHANGELOG.md`**，版本号也仍是 `v0.4.0`，所以不要把这里的接口当成已发布契约。

## 与公众版的关系

本仓库是**开发版**，[ProfilesSystem](../ProfilesSystem/README.zh-CN.md) 是**公众版**。两边可以独立部署。数据库是超集关系：公众版可以直接跑在开发版的库上（多出来的表不用管），反过来不行——开发版需要额外的 6 张表和 3 个功能开关。

| | 公众版 | 开发版（本仓库） |
| --- | --- | --- |
| 学习组 | — | `/api/groups`：创建、加入、退出、成员、管理员审批 |
| 好友与私信 | — | `/api/social`：好友申请、私聊、组内消息 |
| 大模型问答 | — | `/api/llm`：OpenAI 兼容 / Ollama，按等级限额 |
| 页面 | 31 个 | 34 个（多 `groups.html`、`communication.html`、`LLMindex.html`） |
| 数据表 | 11 张 | 17 张 |
| 功能开关 | `notifications`、`community`、`storage` | 上述 3 个 + `llm`、`groups`、`friends` |
| 版本 | v0.4.0 | v0.4.0 + 未记录模块 |

## 功能

**公众版已有的全部功能**

- 个人主页、项目展示、账户与注册、管理员审核、身份码、三种登录方式、找回流程。
- 专业目录（`major_category` → `major`）、学习路线、学习资源、学习进度同步。
- 通知、社区反馈工单、功能开关、管理后台（资源管理、内容迁移、分层开放、通知、审批）。
- 详细说明见[公众版 README](../ProfilesSystem/README.zh-CN.md)，两版行为一致。

**本仓库新增**

- **学习组**：登录用户可以创建学习组（`POST /api/groups`），创建后 `approval_status = 0` 等待管理员审批；列表只显示已通过的组，组长是创建者（`role = OWNER`），创建者不能退出。注意加入接口只校验组是否存在，未审批的组直接调接口也能加入，但读消息会被拒绝。
- **好友与私信**：发起好友申请（`PENDING`），对方接受（`ACCEPTED`）后才能互发消息；重复申请会返回「好友申请已存在」。
- **消息**：组消息与私信共用一个 `chat_message` 表，私信时 `group_id = 0`；单条消息不能为空且不能超过 2000 字；列表按时间正序返回最近 200 条。
- **大模型问答**：`LLMindex.html` + `/api/llm`，支持 OpenAI 兼容接口和 Ollama 两种接入方式，按用户等级分配 token 额度，超额返回 `429`。

## 技术栈

与公众版相同：Java 17、Spring Boot 4.1.2（WebMVC / JDBC / MyBatis 4.1.0 starter）、MySQL 8、BCrypt、`HttpSession` + `AuthInterceptor`、原生 HTML。

新增两点：

- 大模型调用用 Spring 的 `RestClient`（`LlmClientService`），没有引入额外 SDK。
- 额度与模型配置存在数据库里（`llm_setting`、`llm_usage`），管理员可在后台改，环境变量只作为后备默认值。

## 目录结构

```
ProfilesSystem-dev/
├─ pom.xml
├─ mvnw / mvnw.cmd
├─ CHANGELOG.md / VERSION / LICENSE
├─ src/main/java/com/example/profilessystem/
│  ├─ auth/            # SessionKeys、AuthInterceptor（多 llm / groups / friends 判定）
│  ├─ config/          # AuthConfig（BCrypt）、WebConfig
│  ├─ controller/      # 16 个控制器，新增 Group、Social、Llm
│  ├─ server/          # 业务服务，新增 GroupService、SocialService、LlmClientService、LlmQuotaService
│  ├─ mapper/          # 注解 SQL，新增 GroupMapper、SocialMapper、LlmMapper
│  ├─ entity/          # 实体与请求体
│  └─ result/
├─ src/main/resources/
│  ├─ application.yaml
│  ├─ application-dev.yaml
│  └─ static/          # 34 个页面
└─ src/test/java/.../ProfilesSystemApplicationTests.java
```

## 快速开始

### 前置条件

| 依赖 | 说明 |
| --- | --- |
| JDK 17+ | `java -version` 确认 |
| MySQL 8 | 默认库 `profiles`，账号 `profiles` |
| Maven | 用自带的 `mvnw` |
| 模型服务 | 可选：OpenAI 兼容接口或 Ollama，未配置时 LLM 页显示「demo」模式 |

### 1. 建库与建表

```sql
CREATE DATABASE profiles
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'profiles'@'localhost' IDENTIFIED BY '换成你的密码';
GRANT ALL PRIVILEGES ON profiles.* TO 'profiles'@'localhost';
```

仓库**没有建表脚本和种子数据**，需要按 `mapper` 里的注解 SQL 自己建表。开发版比公众版多这 6 张：

| 表 | 关键列 | 用途 |
| --- | --- | --- |
| `study_group` | `id`、`name`、`description`、`owner_username`、`approval_status`、`created_at` | 学习组，`approval_status` 0 待审 / 1 通过 / 2 拒绝 |
| `study_group_member` | `group_id`、`username`、`role`、`member_level`、`joined_at` | 组成员，`role` 为 `OWNER` / `MEMBER` |
| `friendship` | `id`、`requester`、`addressee`、`status` | 好友关系，`PENDING` / `ACCEPTED` / `REJECTED` |
| `chat_message` | `id`、`group_id`、`sender_username`、`receiver_username`、`content`、`created_at` | 组消息与私信（私信 `group_id = 0`） |
| `llm_setting` | `setting_key`、`setting_value`、`updated_at` | 模型配置、额度、刷新间隔 |
| `llm_usage` | `username`、`used_tokens`、`refreshed_at` | 每个用户已用 token |

其余 11 张表见[公众版的数据表](../ProfilesSystem/README.zh-CN.md#数据表)。

### 2. 配置数据库

```powershell
$env:DB_PASSWORD = "换成你的密码"
.\mvnw.cmd spring-boot:run
```

`application.yaml` 默认激活 `dev`，端口 8080；连接信息在 `application-dev.yaml`。文件末尾的 `flask-url: 123` 是遗留的占位配置，代码里没有任何地方读取，可以删掉。

### 3. 初始化管理员与功能开关

管理员初始化与公众版相同（`/managerLogin.html` 或 `POST /api/manager/bootstrap`，仅在管理员表为空时可用）。

开发版有 6 个功能开关，默认都没有行，等于全部关闭：

```sql
INSERT INTO feature_toggle(feature_key, enabled, reason) VALUES
  ('notifications', 1, ''),
  ('community',     1, ''),
  ('storage',       1, ''),
  ('llm',           1, ''),
  ('groups',        1, ''),
  ('friends',       1, '');
```

不插 `llm` 行时 `/api/llm/**` 与 `LLMindex.html` 会直接 403 / 跳转，容易被误判成「LLM 没配好」。

### 4. 配置大模型（可选）

两种方式，数据库优先、环境变量兜底：

**A. 管理后台**

管理员进入 `managerPanel.html` 的模型设置，可以改：

- 三个等级的 token 额度（默认 2000 / 8000 / 20000）；
- 额度刷新间隔（默认 24 小时）；
- 接入方式：`openapi`、`ollama`、`ollama_external`；
- 模型名、服务地址、API Key。

**B. 环境变量**

```powershell
$env:LLM_PROVIDER = "openapi"                 # openapi | ollama | ollama_external
$env:LLM_BASE_URL = "https://api.deepseek.com"
$env:LLM_API_KEY = "sk-..."
$env:LLM_MODEL = "deepseek-flash"
$env:OLLAMA_BASE_URL = "http://127.0.0.1:11434"  # provider=ollama 时使用
```

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `LLM_PROVIDER` | `openapi` | `openapi` 走 `/chat/completions`；`ollama` 走本机 `OLLAMA_BASE_URL`；`ollama_external` 走数据库里的 `base_url` + `/api/chat` |
| `LLM_BASE_URL` | 空 | OpenAI 兼容服务的根地址 |
| `LLM_API_KEY` | 空 | `openapi` 必填；Ollama 不需要 |
| `LLM_MODEL` | `deepseek-flash` | 模型名 |
| `OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | 本机 Ollama 地址 |

### 5. 运行测试

```powershell
.\mvnw.cmd test
```

同样是 `contextLoads` 冒烟测试，运行前需要能连上 MySQL。

## 内网部署

开发版与公众版一样按**内网运行**设计：局域网浏览器访问内网服务器，不依赖公网入口。

- **访问方式**：Spring Boot 监听 `0.0.0.0:8080`，同网段打开 `http://<服务器IP>:8080/index.html`。
- **不依赖外网**：CSS/JS 全部内联，图标是 data URI，唯一的本地图片是 `img.png`，没有 CDN、外部字体或统计脚本；GitHub、Bilibili、教育部目录和备案链接只是外跳。
- **离线模型**：把 `provider` 设为 `ollama`、地址填内网 Ollama（后台占位示例 `http://192.168.3.20:11434`），整站可完全离线；`openapi` 模式要求服务器能访问外网。
- **端口放行**：应用 8080、模型机的 Ollama 端口（默认 11434）；MySQL 3306 只对应用服务器开放。
- **功能开关**：内网部署同样要在 `feature_toggle` 插入 `llm`、`groups`、`friends`，否则对应页面 403。
- **登录与会话**：登录态是 `JSESSIONID` Cookie，内网 HTTP 下可用；没有 HTTPS 与 CSRF Token，只适合可信内网。
- **不要直接暴露到公网**：确需公网访问时，先补 HTTPS、CSRF、限流与默认密码修改。

## 大模型问答

**调用链**

1. 用户登录后打开 `LLMindex.html`（等级 ≥ 1）。
2. `GET /api/llm/status` 返回是否已配置、接入方式、模型名、服务地址；未配置时 `mode = demo`。
3. `POST /api/llm/chat` 校验登录、消息非空且 ≤ 4000 字，按 `ceil(字符数 / 2.5)` 估算 token 并**先扣额度**，再调用模型。
4. 额度不足时不请求模型，返回统一结构 `{"code":429,"message":"今日 token 额度不足"}`。
5. 成功后返回 `reply`、`provider`、`modelName`、`totalTokens`。

**额度规则**

- 等级 0 / 1 / 2 分别对应 `level0_tokens` / `level1_tokens` / `level2_tokens`，默认 2000 / 8000 / 20000。
- `refresh_interval_hours` 到期后，下一次查询或对话会把该用户 `used_tokens` 清零。
- 扣的是**估算值**，不是模型返回的 `totalTokens`，两者会有偏差。

**管理接口**（`/api/llm/admin/**`，拦截器要求管理员会话）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/llm/status` | 当前配置与模式 |
| `POST` | `/api/llm/chat` | 提问 |
| `GET` | `/api/llm/quota` | 我的额度与已用量 |
| `GET/POST` | `/api/llm/admin/settings` | 读取 / 保存模型与额度配置 |
| `POST` | `/api/llm/admin/refresh` | 清零所有用户的已用 token |

**注意**

- 每次请求只发送**当前这一条**用户消息，没有多轮上下文，也不保存对话历史。
- `GET /api/llm/status` 会把服务地址返回给已登录用户，介意的话可以在返回里去掉 `baseUrl`。
- 模型请求失败（地址、Key、模型名、Ollama 未启动）时 HTTP 仍是 200，body 里 `code = 500` 并附中文提示——统一返回结构不走 HTTP 状态码。

## 学习组与好友

**学习组**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/groups` | 所有已通过审批的学习组 |
| `GET` | `/api/groups/mine` | 我加入的组 |
| `GET` | `/api/groups/{id}` | 组详情 |
| `POST` | `/api/groups` | 创建组，进入待审批状态 |
| `POST` | `/api/groups/{id}/join` | 加入 |
| `DELETE` | `/api/groups/{id}/leave` | 退出 |
| `GET` | `/api/groups/admin/pending` | 待审批组（管理员） |
| `POST` | `/api/groups/admin/{id}/approve` \| `/reject` | 审批（管理员） |

只有 `approval_status = 1` 且你是成员时，才能读取组消息；成员列表只要求你是成员。未通过审批的组，读消息会返回 `403`。

**好友与私信**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/social/friends` | 我的好友与申请状态 |
| `POST` | `/api/social/friends/request` | 发起申请（`username`） |
| `POST` | `/api/social/friends/{username}/{status}` | 接受 / 拒绝（`ACCEPTED` / `REJECTED`） |
| `GET` | `/api/social/direct/{username}` | 私聊消息（需已接受） |
| `POST` | `/api/social/direct/{username}` | 发送私聊 |
| `GET` | `/api/social/groups/{id}/messages` | 组消息 |
| `POST` | `/api/social/groups/{id}/messages` | 发组消息 |
| `GET` | `/api/social/groups/{id}/members` | 组成员 |

私聊要求 `friendship.status = ACCEPTED`，否则 `403`。消息没有 WebSocket 推送，页面通过接口刷新获取新内容。

## 与公众版一致的配置

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | 端口 |
| `spring.profiles.active` | `dev` | 必须激活 |
| `spring.datasource.*` | `localhost:3306/profiles` | 账号 `profiles`，密码 `${DB_PASSWORD:123456}` |
| `spring.data.redis.*` | `localhost:6379` | 预留，未使用 |
| `spring.rabbitmq.*` | `localhost:5672` | 预留，未使用 |
| `spring.batch.job.enabled` | `false` | Batch 未使用 |
| `mybatis.configuration.log-impl` | `StdOutImpl` | 打印 SQL |

## 权限模型

与公众版相同，另外扩展了这些判定：

| 类型 | 新增内容 |
| --- | --- |
| 管理员专属 | `/api/groups/admin/**`、`/api/llm/admin/**` |
| 需要登录 | `/api/groups`、`/api/social`、`/api/llm`（含 `/api/llm/status`） |
| 功能开关 | `llm`、`groups`、`friends`，未配置行时相关页面与接口 403 |
| 等级限制 | `LLMindex.html` 要求等级 ≥ 1；`storage.html`、`projectDemo.html` 要求 ≥ 2 |

## 前端页面

在公众版 31 个页面的基础上增加：

| 页面 | 说明 |
| --- | --- |
| `groups.html` | 学习组：我的组、全部组、创建、加入、成员 |
| `communication.html` | 好友与私信 |
| `LLMindex.html` | 大模型问答与额度显示 |

## 已知限制

- 新模块没有写进 `CHANGELOG.md`，版本号仍为 `v0.4.0`；接口可能随时调整。
- 没有建表脚本和种子数据，17 张表需要手工建。
- 6 个功能开关默认全关，少插一行就会出现「功能 403」。
- LLM 没有多轮上下文，额度按字符估算，`llm_usage` 只增不减到刷新点。
- 消息与好友状态没有实时推送，也没有未读数。
- 会话登录，没有 CSRF Token 与登录限流。
- `application-dev.yaml` 里的 `flask-url` 未被使用。
- 单实例部署，没有 Docker 与 CI。

## 许可证

本项目采用 [ProfilesSystem Non-Commercial License](./LICENSE)：允许个人、教育、研究等非商业用途；**禁止任何商业用途**。

## 变更记录

- 版本号见 [VERSION](./VERSION)，当前 `v0.4.0`。
- [CHANGELOG.md](./CHANGELOG.md) 记录到 v0.4.0，学习组、好友、LLM 三个模块尚未记录。
