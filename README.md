# AnimeGirlsDownloader Mobile

AnimeGirlsDownloader Mobile 是 AnimeGirlsCollection 的 Android 客户端，采用 Kotlin、Jetpack Compose 与 Material Design 3 构建，支持手机、平板、折叠屏及自由窗口模式。

## 主要功能

- 随机获取图片，并按图片 ID 或标签搜索。
- 支持 SFW、NSFW 与 AI 图片筛选。
- 使用服务端预览资源展示图片，仅在加入下载队列后请求原图。
- 提供持久化串行下载队列、下载进度、取消、失败保留与重试。
- 支持登录、账号激活、自动登录，以及账号名称和头像同步。
- 支持头像圆形裁剪、拖动定位和手势缩放。
- 以服务端用户 ID 隔离用户配置、令牌、头像和下载任务。
- 支持图片与目录混合选择上传；单图选择时可编辑标签并预览。
- 支持系统主题、浅色主题、深色主题和 Android 动态配色。
- 提供简体中文、英文和日文界面。
- 支持启动时静默检查更新及设置页手动检查更新。

## 系统要求

| 项目 | 要求 |
| --- | --- |
| 最低系统版本 | Android 8.0（API 26） |
| 目标 SDK | Android API 36 |
| 构建 JDK | JDK 17 或更高版本 |
| 开发环境 | Android Studio |

## 构建

1. 使用 Android Studio 打开项目根目录。
2. 通过 Android Studio 配置 Android SDK，或在 `local.properties` 中设置 `sdk.dir`。
3. 同步 Gradle 项目。
4. 选择 `app` 运行配置并启动应用，或执行以下命令生成 Debug APK：

```powershell
.\gradlew.bat :app:assembleDebug
```

输出文件位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

生成 Release APK：

```powershell
.\gradlew.bat :app:assembleRelease
```

Release 构建需要使用发布者自己的 Android 签名密钥完成签名。

## 配置

构建配置位于 `app/build.gradle.kts`：

| 字段 | 用途 |
| --- | --- |
| `API_BASE_URL` | AnimeGirlsCollection API 地址 |
| `PUBLIC_IMAGE_BASE_URL` | 公开图片资源地址 |
| `GITHUB_LATEST_RELEASE_URL` | GitHub 最新 Release API 地址 |
| `GITHUB_PROJECT_URL` | 移动端项目主页地址 |

Release 构建仅允许 HTTPS，不记录认证头、请求正文或图片数据。

## 项目结构

```text
app/src/main/java/top/kafuumiaki/animegirlsdownloader/
├─ core/       网络、数据库、偏好设置、安全存储与领域模型
├─ data/       数据仓储与外部数据访问
├─ download/   下载队列、后台任务与通知
└─ ui/         Compose 页面、主题与状态管理
```

## 数据与权限

- 登录令牌使用 Android Keystore 管理的 AES-GCM 密钥加密。
- 用户头像和应用数据保存在应用私有目录。
- 图片默认保存至 `Pictures/AnimeGirlsDownloader`，也可通过 Storage Access Framework 选择目录。
- 应用不申请完整文件系统管理权限。
- 图片浏览与复制链接不会下载原图。

## 服务端

本客户端配合 AnimeGirlsCollection 使用，兼容其账号、图片、下载与上传接口。服务端地址通过构建配置指定。

## 版本记录

版本变更参见 [CHANGELOG.md](CHANGELOG.md)。

## 许可证

本项目基于 [MIT License](LICENSE.txt) 发布。
