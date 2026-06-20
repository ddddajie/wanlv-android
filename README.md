# 万旅 Android 客户端

<p align="center">
  <img src="app/src/main/res/drawable/logo.png" width="112" alt="万旅 Logo" />
</p>

<p align="center">
  面向景区游览场景的 Android 客户端，整合景区推荐、导游地图、在线预约、AI 智能问答、数字人播报与用户中心。
</p>

## 项目简介

万旅 Android 客户端采用 Kotlin 与 Jetpack Compose 开发。应用通过统一业务后端获取景区、地图、预约、用户和 AI 问答数据，通过 MapLibre 展示景区地图及游览路线，并使用 WebRTC 连接导游、客服两类数字人服务。

当前仓库只包含 Android 客户端，不包含业务后端、地图瓦片服务和数字人服务。完整联调需要提前准备相应服务，并在 `app/src/main/assets/config.yml` 中配置客户端可访问的地址。

## 主要功能

### 首页

- 展示热门景区推荐和景区基础信息。
- 启动后从后端分页获取景区列表。
- 接口不可用时保留本地推荐数据，避免首页完全空白。

### 导游地图

- 获取景区列表并切换当前景区。
- 使用 MapLibre 展示矢量瓦片、可选栅格底图和自定义地图样式。
- 展示景点点位、官方推荐路线与用户最新 AI 路线。
- 默认展示前两条带有效轨迹的推荐路线，并允许手动切换路线可见性。
- 支持定位到当前位置，需要用户授予精确或大致位置权限。
- 支持在地图内唤起数字导游，结合当前景区上下文提问并进行音视频播报。

### 景区预约

- 切换景区、选择开放景点和未来日期。
- 汇总各景点预约时段的总容量、剩余名额和可预约时段数。
- 选择预约时段、填写联系人及游客实名信息并提交预约订单。
- 提交成功后刷新当前时段和名额数据。
- 预约依赖登录状态；游客信息和后端实名要求需保持一致。

### AI 智能问答

- 登录后显示独立的“问答”底部导航入口。
- 支持全局问答与带景区上下文的问答。
- AI 响应可交给导游女数字人或客服男数字人播报。
- 支持数字人连接、切换角色、打断播报和主动断开。
- 对数字人画面进行绿幕抠图，以便叠加到 Compose 页面。

### 用户中心

- 支持手机号验证码登录和账号密码登录。
- Debug 构建下，如果验证码接口返回明文验证码，会自动填入输入框。
- 支持用户资料查看与修改、实名认证、退出登录。
- 支持查看个人预约订单和执行检票入园操作。
- 登录凭证使用 Android Keystore 与 AES-GCM 加密后保存。
- Access Token 失效时自动使用 Refresh Token 刷新；刷新失败后清理本地会话并引导重新登录。

### 开发者模式

- 在底部导航中连续点击“首页”7 次进入开发者模式。
- 可在不重新打包 APK 的情况下覆盖业务后端、数字人和地图服务地址。
- 保存后的地址仅作用于当前设备；“恢复默认”会重新使用打包时的配置。

## 技术栈

| 分类 | 技术或版本 |
| --- | --- |
| 开发语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose、Material 3 |
| 架构 | 单 Activity、Compose Navigation、ViewModel、Repository |
| Android Gradle Plugin | 8.9.2 |
| Gradle Wrapper | 8.11.1 |
| 编译与目标 SDK | Android 35 |
| 最低 Android 版本 | Android 9（API 28） |
| Java/Kotlin 字节码目标 | Java 11 |
| HTTP | OkHttp 4.12.0 |
| 图片加载 | Coil Compose 2.7.0 |
| 地图 | MapLibre Android SDK 11.8.0 |
| 毛玻璃效果 | Haze 1.1.1 |
| 实时音视频 | WebRTC Android 144.7559.09 |
| 单元测试 | JUnit 4.13.2 |

> 虽然项目字节码目标为 Java 11，但 Android Gradle Plugin 8.x 的构建环境建议使用 JDK 17。

## 应用架构

```text
Compose Screen
      │
      ▼
   ViewModel
      │
      ▼
  Repository ───────────────┐
      │                     │
      ▼                     ▼
  ApiClient          DigitalHumanSessionManager
      │                     │
      ▼                     ▼
业务后端（HTTP/JSON）    数字人服务（HTTP + WebRTC）
```

- `ui/screens`：页面布局、交互和弹窗。
- `ui/components`：可复用 Compose 组件。
- `viewmodel`：页面状态、输入校验、协程任务和业务流程编排。
- `repository`：业务接口调用与 DTO 转换。
- `network`：HTTP 封装、统一响应解包、登录态和 Token 刷新。
- `digitalhuman`：WebRTC 会话、音视频轨道、绿幕抠图和播报文本清洗。
- `config`：构建期配置、运行期配置与开发者覆盖配置。

## 目录结构

```text
wanlv-android/
├── app/
│   ├── build.gradle.kts                 # Android 应用模块配置
│   ├── proguard-rules.pro               # ProGuard/R8 规则
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml       # 权限与应用入口
│       │   ├── assets/config.yml         # 服务地址和调试登录配置
│       │   ├── java/com/wanlv/app/
│       │   │   ├── config/               # 应用配置读取与覆盖
│       │   │   ├── data/                 # 本地数据与景区上下文
│       │   │   ├── digitalhuman/         # WebRTC 数字人会话
│       │   │   ├── model/                # UI 领域模型
│       │   │   ├── navigation/           # Compose 导航
│       │   │   ├── network/              # HTTP、鉴权和异常处理
│       │   │   ├── pojo/                 # 接口 DTO/VO
│       │   │   ├── repository/           # 数据仓库
│       │   │   ├── ui/                   # 页面、组件和主题
│       │   │   └── viewmodel/            # 页面状态与业务逻辑
│       │   └── res/                       # 图片、图标、主题和网络策略
│       ├── test/                          # JVM 单元测试
│       └── androidTest/                   # Android 仪器测试
├── gradle/libs.versions.toml              # 依赖版本目录
├── gradle/wrapper/                        # Gradle Wrapper
├── settings.gradle.kts
└── README.md
```

## 环境要求

开始构建前，请准备以下环境：

- Android Studio，建议使用支持 AGP 8.9.2 的稳定版本。
- JDK 17。
- Android SDK Platform 35。
- Android SDK Build-Tools 及 Platform-Tools。
- Android 9（API 28）或更高版本的模拟器/真机。
- 能访问 Google Maven 与 Maven Central 的依赖下载环境。
- 联调时所需的业务后端、地图瓦片和数字人服务。

可通过以下命令确认本机 Java 环境：

```powershell
java -version
```

## 快速开始

### 1. 获取代码

```bash
git clone <repository-url>
cd wanlv-android
```

也可以直接使用 Android Studio 打开仓库根目录，等待 Gradle Sync 完成。

### 2. 配置服务地址

编辑：

```text
app/src/main/assets/config.yml
```

局域网联调示例：

```yaml
WANLV_SERVER_PROTOCOL: http
WANLV_SERVER_HOST: 192.168.1.100
WANLV_SERVER_ORIGIN: ${WANLV_SERVER_PROTOCOL}://${WANLV_SERVER_HOST}

WANLV_API_BASE_URL: ${WANLV_SERVER_ORIGIN}:8080
WANLV_DIGITAL_HUMAN_GUIDE_API_URL: ${WANLV_SERVER_ORIGIN}:8011
WANLV_DIGITAL_HUMAN_SERVICE_API_URL: ${WANLV_SERVER_ORIGIN}:8010

WANLV_MAP_STYLE_URL:
WANLV_MAP_VECTOR_SOURCE_URL: ${WANLV_SERVER_ORIGIN}:3000/china
WANLV_MAP_RASTER_TILE_URL:
WANLV_MAP_TILE_ATTRIBUTION: Local Martin tiles

WANLV_DEBUG_USER_ID:
WANLV_DEBUG_TOKEN:
```

将示例 IP 替换为服务所在计算机在局域网中的实际 IP，并确保手机与服务端处于可互通网络。

### 3. 构建 Debug APK

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
```

macOS/Linux：

```bash
./gradlew assembleDebug
```

构建产物默认位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 4. 安装到已连接设备

```powershell
.\gradlew.bat installDebug
```

该命令只安装应用，不负责启动应用。也可以在 Android Studio 中选择目标设备后自行运行。

## 配置说明

### 配置项

| 配置项 | 默认值 | 用途 |
| --- | --- | --- |
| `WANLV_SERVER_PROTOCOL` | `http` | 公共服务协议，主要用于变量组合 |
| `WANLV_SERVER_HOST` | 当前配置中的局域网 IP | 公共服务主机地址 |
| `WANLV_SERVER_ORIGIN` | `${WANLV_SERVER_PROTOCOL}://${WANLV_SERVER_HOST}` | 公共服务 Origin |
| `WANLV_API_BASE_URL` | `http://127.0.0.1:8080` | 登录、用户、景区、地图、预约与 AI 问答接口 |
| `WANLV_DIGITAL_HUMAN_GUIDE_API_URL` | `http://127.0.0.1:8011` | 导游女数字人的信令与播报服务 |
| `WANLV_DIGITAL_HUMAN_SERVICE_API_URL` | `http://127.0.0.1:8010` | 客服男数字人的信令与播报服务 |
| `WANLV_MAP_STYLE_URL` | 空 | 可选的 MapLibre `style.json` 地址；留空时使用应用内置样式 |
| `WANLV_MAP_VECTOR_SOURCE_URL` | `http://10.0.2.2:3000/china` | 地图矢量瓦片源地址 |
| `WANLV_MAP_RASTER_TILE_URL` | 空 | 可选栅格瓦片模板；留空时不加载栅格底图 |
| `WANLV_MAP_TILE_ATTRIBUTION` | `Local Martin tiles` | 地图数据版权/来源说明 |
| `WANLV_DEBUG_USER_ID` | 空 | 本地联调用户 ID，留空时走正常登录流程 |
| `WANLV_DEBUG_TOKEN` | 空 | 本地联调 Access Token，需与调试用户 ID 配套使用 |

### 配置格式限制

`config.yml` 使用项目内置的轻量解析逻辑，并不是完整 YAML 解析器：

- 每行只支持 `KEY: VALUE` 或 `KEY=VALUE`。
- 支持 `${KEY}` 形式的变量引用，最多递归展开 4 次。
- 空行和以 `#` 开头的行会被忽略。
- 不支持 YAML 层级、数组、多行字符串等复杂语法。
- URL 值不要额外添加引号，避免引号被当作地址内容的一部分。

### 配置优先级

运行时从高到低的优先级如下：

1. 应用内开发者模式保存的本机覆盖值。
2. APK 中 `assets/config.yml` 的运行时配置。
3. 构建时生成到 `BuildConfig` 的配置或代码中的默认值。

开发者模式只开放服务 URL 的覆盖，不开放 `WANLV_DEBUG_USER_ID` 和 `WANLV_DEBUG_TOKEN`。修改这两个调试登录项后需要重新构建应用。

### 模拟器与真机地址

- Android 官方模拟器访问宿主机服务时，应使用 `10.0.2.2`，不能使用 `localhost`。
- 真机访问开发电脑时，应填写电脑的局域网 IP，例如 `192.168.1.100`。
- `localhost` 和 `127.0.0.1` 在数字人、地图地址中会被客户端转换为 `10.0.2.2`；业务后端地址建议仍显式配置为设备可访问地址。
- 使用真机时，需要检查系统防火墙、端口监听地址和 Wi-Fi 客户端隔离设置。

## 后端服务约定

### 服务划分

| 服务 | 示例端口 | 客户端用途 |
| --- | ---: | --- |
| 万旅业务后端 | `8080` | 用户、Token、景区、地图、预约、AI 问答 |
| 导游数字人服务 | `8011` | 导游女数字人 WebRTC 与播报 |
| 客服数字人服务 | `8010` | 客服男数字人 WebRTC 与播报 |
| 地图瓦片服务 | `3000` | MapLibre 矢量瓦片，可按部署方式调整 |

### 业务后端接口

客户端当前使用的主要接口如下：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/user/normal/login` | 账号密码登录 |
| `POST` | `/user/normal/code/send` | 发送手机验证码 |
| `POST` | `/user/normal/code/login` | 手机验证码登录 |
| `POST` | `/user/normal/token/refresh` | 刷新并轮换 Access/Refresh Token |
| `POST` | `/user/normal/logout` | 注销 Refresh Token |
| `GET` | `/user/normal/{userId}` | 查询当前用户资料 |
| `PUT` | `/user/normal/update` | 修改用户资料 |
| `POST` | `/user/normal/real-name/verify` | 实名认证 |
| `GET` | `/map/scenic-areas/page` | 分页查询启用景区 |
| `GET` | `/map/init/{scenicAreaId}` | 获取地图点位与官方路线 |
| `GET` | `/map/agent-route-geos/latest` | 获取用户最新 AI 路线 |
| `POST` | `/agent/chat` | AI 智能问答 |
| `GET` | `/reservation/spots/enabled` | 查询开放预约景点 |
| `GET` | `/reservation/slots` | 查询指定日期的预约时段 |
| `POST` | `/reservation/orders` | 创建预约订单 |
| `GET` | `/reservation/orders/my` | 查询当前用户预约订单 |
| `POST` | `/reservation/admin/orders/{reservationNo}/enter` | 预约检票入园 |

除数字人接口外，业务客户端默认兼容以下统一响应结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

需要鉴权的请求通过以下请求头传递 Access Token：

```http
Authorization: Bearer <access-token>
```

### 数字人接口

导游和客服数字人服务使用相同协议，但配置为两个独立的 Base URL：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/offer` | 提交本地 SDP Offer，获取远端 SDP 和 `sessionid` |
| `POST` | `/human` | 向指定数字人会话发送播报文本 |
| `POST` | `/interrupt_talk` | 打断当前播报 |
| `POST` | `/session/close` | 释放服务端会话 |

数字人服务需要返回可用于当前设备网络的 ICE 候选。若 SDP 中只有设备不可达的回环地址或错误网卡地址，即使 HTTP 接口正常，WebRTC 音视频也无法建立。

## 登录与会话机制

1. 用户通过手机号验证码或账号密码登录。
2. 后端同时返回用户 ID、Access Token 和 Refresh Token。
3. 客户端使用 Android Keystore 生成 AES 密钥，通过 AES/GCM/NoPadding 加密会话数据。
4. 普通业务请求自动附加 Access Token。
5. 请求返回 HTTP 401 或业务 `code = 401` 时，客户端协调并发请求，只执行一次 Token 刷新。
6. 刷新成功后同步保存后端轮换的新 Access Token 与 Refresh Token，并重试原请求一次。
7. 刷新失败或重试后仍为 401 时，客户端清理会话并跳转到个人页登录区域。

`WANLV_DEBUG_USER_ID` 与 `WANLV_DEBUG_TOKEN` 只适合本地临时联调，无法替代完整的 Refresh Token 登录会话。验证登录刷新、退出和持久化流程时应使用正常登录。

## 权限与网络安全

应用在 Manifest 中声明以下权限：

| 权限 | 用途 |
| --- | --- |
| `android.permission.INTERNET` | 调用业务接口、加载地图和连接数字人 |
| `android.permission.ACCESS_FINE_LOCATION` | 获取精确位置并在地图中定位 |
| `android.permission.ACCESS_COARSE_LOCATION` | 获取大致位置 |

当前工程为方便局域网联调，允许明文 HTTP：

```xml
<base-config cleartextTrafficPermitted="true" />
```

正式发布前建议：

- 所有公网服务切换到 HTTPS/WSS。
- 将 `android:usesCleartextTraffic` 和 Network Security Config 调整为禁止全局明文流量。
- 不要把真实账号、长期 Token、身份证信息或生产密钥提交到仓库。
- 为 Release 构建启用代码压缩与混淆，并补充必要的 R8 保留规则。

## 构建与验证

### Windows

```powershell
# 编译 Debug APK
.\gradlew.bat assembleDebug

# 运行 JVM 单元测试
.\gradlew.bat testDebugUnitTest

# 仅检查 Kotlin 编译
.\gradlew.bat :app:compileDebugKotlin

# 连接模拟器或真机后运行仪器测试
.\gradlew.bat connectedDebugAndroidTest

# 清理构建产物
.\gradlew.bat clean
```

### macOS/Linux

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :app:compileDebugKotlin
./gradlew connectedDebugAndroidTest
./gradlew clean
```

当前 JVM 单元测试主要覆盖：

- 手机号和 6 位验证码校验。
- 验证码有效期默认值。
- 并发 401 请求只触发一次 Token 刷新。
- 已被其他请求刷新的 Token 能被后续重试直接复用。

## 常见问题

### Gradle 使用了错误的 JDK

如果出现 “Android Gradle plugin requires Java 17” 或类似错误，请在 Android Studio 的 Gradle JDK 设置中选择 JDK 17，或在当前终端设置 `JAVA_HOME` 后重新构建。

### 手机上无法连接电脑服务

依次检查：

1. 配置中是否仍使用 `127.0.0.1` 或 `localhost`。
2. 手机与电脑是否在同一局域网且可以互相访问。
3. 服务是否监听 `0.0.0.0`，而不是只监听电脑自身的回环地址。
4. Windows 防火墙是否允许 8080、8010、8011、3000 等实际使用端口。
5. 修改 `config.yml` 后是否重新构建并安装了 APK，或是否已在开发者模式中保存覆盖地址。

### 模拟器无法连接宿主机服务

Android 官方模拟器应使用 `10.0.2.2:<port>` 访问宿主机。业务后端地址不会统一自动重写，建议直接在配置或开发者模式中填写完整的 `10.0.2.2` 地址。

### 地图页面空白

- 确认 `WANLV_MAP_VECTOR_SOURCE_URL` 可从设备访问。
- 如果使用远程 `style.json`，检查样式中的 source、sprite、glyphs URL 是否也可从设备访问。
- 暂时清空 `WANLV_MAP_STYLE_URL`，验证应用内置样式能否正常加载。
- 检查瓦片服务路径和返回的 Content-Type 是否正确。
- 确认业务后端的 `/map/init/{scenicAreaId}` 返回有效的中心点、景点和路线数据。

### 数字人 HTTP 正常但没有画面或声音

- 确认设备能访问数字人服务的 `/offer`、`/human` 和 `/session/close`。
- 检查 `/offer` 返回的 SDP 是否包含设备可达的局域网 ICE Candidate。
- 检查服务端 WebRTC UDP 端口是否被防火墙拦截。
- 确认设备媒体音量没有静音；应用会让实体音量键默认调节媒体音量。
- 切换数字人角色时会先释放旧会话，频繁操作时需要等待新会话完成协商。

### 登录成功后仍然被要求重新登录

- 检查登录响应是否同时包含有效的用户 ID、`token` 和 `refreshToken`。
- 检查刷新接口是否返回统一响应结构，并在 `data` 中同时返回新 `token` 和新 `refreshToken`。
- 如果使用调试 Token，确认 Token 尚未过期；调试配置本身不包含可用于续期的 Refresh Token。

### 预约按钮不可用

需要依次满足：已登录、已选择景区、已选择开放景点、已选择日期、存在可预约时段、游客数量和游客信息合法。若后端要求实名，请先在个人中心完成实名认证。

## 开发约定

- 项目源码与文档统一使用 UTF-8 编码。
- 业务重点、并发控制、登录态和资源释放逻辑使用中文注释说明。
- 新增接口优先放入对应 `Repository`，页面不直接拼接 HTTP 请求。
- 页面状态和异步流程放入 `ViewModel`，Composable 主要负责状态展示和事件转发。
- 新增配置项时，需要同步检查 `app/build.gradle.kts`、`config.yml` 与 `AppConfig.kt`。
- 数字人会话必须覆盖成功、失败、取消和页面退出路径，确保服务端 `sessionid` 被释放。
- 修改后至少执行 Kotlin 编译和相关单元测试；无需为验证而自动启动应用或外部服务。

## 版本信息

- `applicationId`：`com.wanlv.app`
- `namespace`：`com.wanlv.app`
- `versionName`：`0.1`
- `versionCode`：`1`
- 根项目名：`WanLv-app`

## License

当前仓库未提供独立的开源许可证文件。若计划公开分发或允许第三方复用，请在发布前补充明确的 `LICENSE` 和版权说明。
