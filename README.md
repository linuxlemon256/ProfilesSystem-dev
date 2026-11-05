# ProfilesSystem-dev

[简体中文](README.zh-CN.md) · **English** · [← Public edition](../ProfilesSystem/README.md)

![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 4.1.2](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?logo=spring&logoColor=white)
![MySQL 8](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![ORM: MyBatis](https://img.shields.io/badge/ORM-MyBatis-DC382D)
![LLM: OpenAI / Ollama](https://img.shields.io/badge/LLM-OpenAI%20%2F%20Ollama-412991)
[![License: Non-Commercial](https://img.shields.io/badge/License-Non--Commercial-red)](./LICENSE)
![Version](https://img.shields.io/badge/version-v0.4.0-informational)
![Status](https://img.shields.io/badge/status-experimental-blue)
![Deployment: Intranet](https://img.shields.io/badge/deployment-intranet-0b7285)

A branch that keeps building on the public edition: study groups, friend messaging, and LLM Q&A with token quotas. Everything else behaves the same.

---

The dev edition shares the same skeleton as the public edition: a Spring Boot monolith + MyBatis + MySQL, 31 base pages plus 3 new ones, and 17 tables instead of 11. The new modules are **not in `CHANGELOG.md`** yet and the version is still `v0.4.0`, so do not treat these endpoints as a released contract.

## Relationship to the public edition

This repository is the **dev edition**; [ProfilesSystem](../ProfilesSystem/README.md) is the **public edition**. Both can be deployed independently. The schemas are a superset relationship: the public edition runs fine on a dev-edition database (the extra tables are simply unused), but not the other way around — the dev edition needs 6 extra tables and 3 extra feature switches.

| | Public edition | Dev edition (this repo) |
| --- | --- | --- |
| Study groups | — | `/api/groups`: create, join, leave, members, admin approval |
| Friends and messaging | — | `/api/social`: friend requests, direct messages, group messages |
| LLM Q&A | — | `/api/llm`: OpenAI-compatible / Ollama, quota per level |
| Pages | 31 | 34 (adds `groups.html`, `communication.html`, `LLMindex.html`) |
| Tables | 11 | 17 |
| Feature switches | `notifications`, `community`, `storage` | the same 3 plus `llm`, `groups`, `friends` |
| Version | v0.4.0 | v0.4.0 + undocumented modules |

## Features

**Everything the public edition has**

- Personal homepage, project showcase, accounts and registration, admin review, identity codes, three sign-in flows and recovery.
- Major catalog (`major_category` → `major`), learning routes, learning resources and progress sync.
- Notifications, community tickets, feature switches, and the admin console (resource management, content transfer, layered access, notifications, approvals).
- See the [public edition README](../ProfilesSystem/README.md) for details; behavior is identical.

**New in this repository**

- **Study groups**: a signed-in user can create a group (`POST /api/groups`); new groups start as `approval_status = 0` and wait for an administrator. Lists only show approved groups, the creator becomes the `OWNER` and cannot leave. Note that the join endpoint only checks that the group exists — an unapproved group can be joined through the API, but reading its messages is rejected.
- **Friends and direct messages**: send a friend request (`PENDING`); only after it is `ACCEPTED` can both sides message each other. Duplicate requests answer "好友申请已存在".
- **Messages**: group and direct messages share `chat_message`, with `group_id = 0` for direct messages. A message must be non-empty and at most 2000 characters; lists return the latest 200 in ascending time order.
- **LLM Q&A**: `LLMindex.html` plus `/api/llm`, speaking to either an OpenAI-compatible API or Ollama, with per-level token quotas and a 429-style response when the quota runs out.

## Tech stack

Same as the public edition: Java 17, Spring Boot 4.1.2 (WebMVC / JDBC / MyBatis 4.1.0 starter), MySQL 8, BCrypt, `HttpSession` + `AuthInterceptor`, vanilla HTML.

Two additions:

- The model call uses Spring's `RestClient` (`LlmClientService`) with no extra SDK.
- Quotas and model settings live in the database (`llm_setting`, `llm_usage`), can be changed from the admin console, with environment variables as fallbacks.

## Project layout

```
ProfilesSystem-dev/
├─ pom.xml
├─ mvnw / mvnw.cmd
├─ CHANGELOG.md / VERSION / LICENSE
├─ src/main/java/com/example/profilessystem/
│  ├─ auth/            # SessionKeys, AuthInterceptor (adds llm / groups / friends checks)
│  ├─ config/          # AuthConfig (BCrypt), WebConfig
│  ├─ controller/      # 16 controllers, adds Group, Social, Llm
│  ├─ server/          # services, adds GroupService, SocialService, LlmClientService, LlmQuotaService
│  ├─ mapper/          # annotation SQL, adds GroupMapper, SocialMapper, LlmMapper
│  ├─ entity/          # entities and request bodies
│  └─ result/
├─ src/main/resources/
│  ├─ application.yaml
│  ├─ application-dev.yaml
│  └─ static/          # 34 pages
└─ src/test/java/.../ProfilesSystemApplicationTests.java
```

## Quick start

### Prerequisites

| Requirement | Notes |
| --- | --- |
| JDK 17+ | Check with `java -version` |
| MySQL 8 | Default database `profiles`, account `profiles` |
| Maven | Use the bundled `mvnw` |
| Model service | Optional: an OpenAI-compatible API or Ollama; without one the LLM page stays in demo mode |

### 1. Create the database and tables

```sql
CREATE DATABASE profiles
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'profiles'@'localhost' IDENTIFIED BY 'replace-with-your-password';
GRANT ALL PRIVILEGES ON profiles.* TO 'profiles'@'localhost';
```

The repository has **no schema script and no seed data**; create tables from the annotation SQL in `mapper`. Compared with the public edition, the dev edition adds 6 tables:

| Table | Key columns | Purpose |
| --- | --- | --- |
| `study_group` | `id`, `name`, `description`, `owner_username`, `approval_status`, `created_at` | Study groups; `approval_status` 0 pending / 1 approved / 2 rejected |
| `study_group_member` | `group_id`, `username`, `role`, `member_level`, `joined_at` | Group members; `role` is `OWNER` / `MEMBER` |
| `friendship` | `id`, `requester`, `addressee`, `status` | Friend relations: `PENDING` / `ACCEPTED` / `REJECTED` |
| `chat_message` | `id`, `group_id`, `sender_username`, `receiver_username`, `content`, `created_at` | Group and direct messages (direct uses `group_id = 0`) |
| `llm_setting` | `setting_key`, `setting_value`, `updated_at` | Model settings, quotas, refresh interval |
| `llm_usage` | `username`, `used_tokens`, `refreshed_at` | Tokens consumed per user |

The other 11 tables are listed in the [public edition's data tables](../ProfilesSystem/README.md#data-tables).

### 2. Configure the database

```powershell
$env:DB_PASSWORD = "replace-with-your-password"
.\mvnw.cmd spring-boot:run
```

`application.yaml` activates `dev` on port 8080; connection settings live in `application-dev.yaml`. The `flask-url: 123` line at the end of that file is a leftover placeholder that nothing reads, so it can be deleted.

### 3. Bootstrap the admin and feature switches

Admin bootstrap works exactly like the public edition (`/managerLogin.html` or `POST /api/manager/bootstrap`, only while the `manager` table is empty).

The dev edition has 6 feature switches, all absent by default, which means all off:

```sql
INSERT INTO feature_toggle(feature_key, enabled, reason) VALUES
  ('notifications', 1, ''),
  ('community',     1, ''),
  ('storage',       1, ''),
  ('llm',           1, ''),
  ('groups',        1, ''),
  ('friends',       1, '');
```

Without the `llm` row, `/api/llm/**` and `LLMindex.html` answer 403 or redirect, which is easy to mistake for a broken model configuration.

### 4. Configure the model (optional)

Two layers: the database wins, environment variables are the fallback.

**A. Admin console**

An administrator opens the model settings in `managerPanel.html` and can change:

- token quotas for the three levels (defaults 2000 / 8000 / 20000);
- the quota refresh interval (default 24 hours);
- the provider: `openapi`, `ollama`, `ollama_external`;
- model name, service URL and API key.

**B. Environment variables**

```powershell
$env:LLM_PROVIDER = "openapi"                 # openapi | ollama | ollama_external
$env:LLM_BASE_URL = "https://api.deepseek.com"
$env:LLM_API_KEY = "sk-..."
$env:LLM_MODEL = "deepseek-flash"
$env:OLLAMA_BASE_URL = "http://127.0.0.1:11434"  # used when provider=ollama
```

| Variable | Default | Notes |
| --- | --- | --- |
| `LLM_PROVIDER` | `openapi` | `openapi` calls `/chat/completions`; `ollama` calls the local `OLLAMA_BASE_URL`; `ollama_external` calls the database `base_url` + `/api/chat` |
| `LLM_BASE_URL` | empty | Root URL of an OpenAI-compatible service |
| `LLM_API_KEY` | empty | Required for `openapi`; Ollama does not need it |
| `LLM_MODEL` | `deepseek-flash` | Model name |
| `OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Local Ollama address |

### 5. Run the tests

```powershell
.\mvnw.cmd test
```

The `contextLoads` smoke test needs a reachable MySQL.

## Intranet deployment

The dev edition assumes the same **intranet** setup as the public edition: browsers on the LAN reach an intranet server, no public entry point.

- **How to reach it**: Spring Boot listens on `0.0.0.0:8080`; open `http://<server-ip>:8080/index.html` from the same network.
- **No internet dependency**: all CSS/JS is inline, icons are data URIs, the only local image is `img.png`; there are no CDNs, web fonts or analytics scripts. Outbound links (GitHub, Bilibili, MOE catalog, filing) need internet but nothing in the site depends on them.
- **Offline model**: set `provider` to `ollama` and point it at an intranet Ollama (the admin console hints at `http://192.168.3.20:11434`), and the whole site runs offline; the `openapi` provider needs outbound internet.
- **Ports**: 8080 for the app and the Ollama port (default 11434) for the model host; keep MySQL 3306 reachable only from the app server.
- **Feature switches**: an intranet deployment must still insert the `llm`, `groups` and `friends` rows into `feature_toggle`, otherwise those pages answer 403.
- **Sessions**: login state lives in a `JSESSIONID` cookie over plain HTTP; there is no HTTPS or CSRF token, so trusted networks only.
- **Never expose it directly to the public internet** without HTTPS, CSRF protection, rate limiting and a changed default password.

## LLM Q&A

**Call chain**

1. A signed-in user opens `LLMindex.html` (level ≥ 1).
2. `GET /api/llm/status` returns whether the model is configured, the provider, model name and service URL; when unconfigured it reports `mode = demo`.
3. `POST /api/llm/chat` validates the session, requires a non-empty message of at most 4000 characters, reserves `ceil(characters / 2.5)` tokens **before** calling the model.
4. When the quota is insufficient the model is never called and the response is `{"code":429,"message":"今日 token 额度不足"}`.
5. On success it returns `reply`, `provider`, `modelName` and `totalTokens`.

**Quota rules**

- Levels 0 / 1 / 2 map to `level0_tokens` / `level1_tokens` / `level2_tokens`, default 2000 / 8000 / 20000.
- After `refresh_interval_hours` the next status or chat call resets that user's `used_tokens`.
- The debit is the **estimate**, not the model's `totalTokens`, so the two drift apart.

**Endpoints** (`/api/llm/admin/**` requires an administrator session through the interceptor)

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/llm/status` | Current configuration and mode |
| `POST` | `/api/llm/chat` | Ask a question |
| `GET` | `/api/llm/quota` | My quota and usage |
| `GET/POST` | `/api/llm/admin/settings` | Read / save model and quota settings |
| `POST` | `/api/llm/admin/refresh` | Reset everyone's used tokens |

**Notes**

- Each request sends only the **current** user message: there is no conversation context and no history is stored.
- `GET /api/llm/status` exposes the service URL to any signed-in user; drop `baseUrl` from the payload if that matters.
- Model failures (bad URL, key, model name, Ollama not running) are returned with HTTP 200 and `code = 500` in the body plus a Chinese message — the shared `Result` envelope does not use HTTP status codes for errors.

## Study groups and friends

**Study groups**

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/groups` | All approved groups |
| `GET` | `/api/groups/mine` | Groups I joined |
| `GET` | `/api/groups/{id}` | Group detail |
| `POST` | `/api/groups` | Create a group (enters the pending queue) |
| `POST` | `/api/groups/{id}/join` | Join |
| `DELETE` | `/api/groups/{id}/leave` | Leave |
| `GET` | `/api/groups/admin/pending` | Pending groups (admin) |
| `POST` | `/api/groups/admin/{id}/approve` \| `/reject` | Approve / reject (admin) |

Group messages require `approval_status = 1` and membership; the member list only requires membership. Unapproved groups answer `403` when you try to read messages.

**Friends and direct messages**

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/social/friends` | My friends and request states |
| `POST` | `/api/social/friends/request` | Send a request (`username`) |
| `POST` | `/api/social/friends/{username}/{status}` | Accept / reject (`ACCEPTED` / `REJECTED`) |
| `GET` | `/api/social/direct/{username}` | Direct messages (must be accepted) |
| `POST` | `/api/social/direct/{username}` | Send a direct message |
| `GET` | `/api/social/groups/{id}/messages` | Group messages |
| `POST` | `/api/social/groups/{id}/messages` | Send a group message |
| `GET` | `/api/social/groups/{id}/members` | Group members |

Direct messages require `friendship.status = ACCEPTED`, otherwise `403`. Messages are not pushed over WebSocket; pages fetch new content through these endpoints.

## Shared configuration

| Property | Default | Notes |
| --- | --- | --- |
| `server.port` | `8080` | Port |
| `spring.profiles.active` | `dev` | Must stay active |
| `spring.datasource.*` | `localhost:3306/profiles` | Account `profiles`, password `${DB_PASSWORD:123456}` |
| `spring.data.redis.*` | `localhost:6379` | Reserved, unused |
| `spring.rabbitmq.*` | `localhost:5672` | Reserved, unused |
| `spring.batch.job.enabled` | `false` | Batch is unused |
| `mybatis.configuration.log-impl` | `StdOutImpl` | Prints SQL |

## Access model

Same as the public edition, with these extensions:

| Type | Additions |
| --- | --- |
| Admin only | `/api/groups/admin/**`, `/api/llm/admin/**` |
| Sign-in required | `/api/groups`, `/api/social`, `/api/llm` (including `/api/llm/status`) |
| Feature switches | `llm`, `groups`, `friends`; without rows the pages and APIs answer 403 |
| Level gates | `LLMindex.html` requires level ≥ 1; `storage.html` and `projectDemo.html` require ≥ 2 |

## Pages

On top of the public edition's 31 pages:

| Page | Purpose |
| --- | --- |
| `groups.html` | Study groups: mine, all, create, join, members |
| `communication.html` | Friends and direct messages |
| `LLMindex.html` | LLM Q&A and quota display |

## Known limitations

- New modules are not in `CHANGELOG.md` and the version stays `v0.4.0`; endpoints may change.
- No schema script or seed data: 17 tables must be created by hand.
- All 6 feature switches default to off; one missing row means a 403.
- The LLM has no multi-turn context, quotas are estimated from characters, and `llm_usage` only grows until the refresh point.
- Messages and friend states are not pushed in real time, and there are no unread counts.
- Session auth without CSRF tokens or login rate limiting.
- `application-dev.yaml` still contains an unused `flask-url` property.
- Single instance, no Docker, no CI.

## License

Released under the [ProfilesSystem Non-Commercial License](./LICENSE): personal, educational and research use is allowed; **commercial use of any kind is prohibited**.

## Changelog

- Version: see [VERSION](./VERSION), currently `v0.4.0`.
- [CHANGELOG.md](./CHANGELOG.md) stops at v0.4.0; the study-group, friend and LLM modules are not recorded there yet.
