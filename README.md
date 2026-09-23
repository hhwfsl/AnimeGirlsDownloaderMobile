# AnimeGirlsDownloader Mobile

AnimeGirlsDownloader 的 Android 原生客户端，对接 AnimeGirlsCollection 服务。应用使用 Material Design 3，并针对手机、横屏、平板、折叠屏和自由窗口提供响应式布局。

## 功能

- 随机获取图片，或按图片 ID、标签搜索。
- SFW/NSFW 和 AI 图片筛选。
- 只加载服务端预览图；用户确认下载后才请求原图。
- 复制公开图片链接。
- 持久化串行下载队列，支持进度、取消、失败保留和重试。
- 登录、受控账号激活、Token 自动登录，以及账号名称和头像的跨设备同步。
- 在设置页修改账号名称，并在上传头像前进行圆形裁剪；支持拖动定位以及双指、滚轮或触控板缩放。
- 以服务端用户 ID 隔离令牌、配置、头像和下载任务。
- 多图片和目录混合上传；仅单独选择一张图片时启用标签与预览。
- 跟随系统/浅色/深色主题、Android 动态色。
- 简体中文、英文和日文平滑切换，不需要重建当前页面。
- 启动静默检查 GitHub Release，设置页支持手动检查。

## 技术栈

- Kotlin 2.1
- Jetpack Compose、Material 3
- Room、DataStore、WorkManager、JobScheduler UIDT
- Retrofit、OkHttp、Kotlin Serialization
- Coil
- Android Keystore

## 系统要求

- Android 8.0（API 26）或更高版本
- Android SDK 36
- JDK 17 或更高版本

## 构建

1. 使用 Android Studio 打开项目目录。
2. 在 `local.properties` 中配置 Android SDK，或设置 `ANDROID_HOME`。
3. 构建 Debug APK：

```powershell
.\gradlew.bat :app:assembleDebug
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

正式发布前应配置独立签名，并执行：

```powershell
.\gradlew.bat :app:bundleRelease
```

## 配置

服务地址和 GitHub 地址位于 `app/build.gradle.kts` 的 BuildConfig 字段：

- `API_BASE_URL`
- `PUBLIC_IMAGE_BASE_URL`
- `GITHUB_LATEST_RELEASE_URL`
- `GITHUB_PROJECT_URL`

正式版本只允许 HTTPS。Release 构建不会记录 Authorization Header、图片数据或请求正文。

## 项目结构

```text
app/src/main/java/top/kafuumiaki/animegirlsdownloader/
├─ core/       # 网络、数据库、偏好、密钥和领域模型
├─ data/       # 仓储实现
├─ download/   # 串行下载处理器和系统后台调度
└─ ui/         # Compose 页面、主题和 ViewModel
```

完整产品、架构、服务端配合和验收方案参见同级目录的 [`DESIGN.md`](../DESIGN.md)。

## 服务端兼容性

客户端兼容 AnimeGirlsCollection 当前的 PascalCase JSON、数字枚举和 Base64 JSON 上传接口。移动端会把相对的预览和下载地址解析到正式 API Base URL。

当前服务端上传接口限制为 30MB JSON 请求。客户端将原始图片限制在约 22MB，以预留 Base64 和 JSON 开销。正式批量上传建议服务端增加 multipart 流式接口。

## 隐私与存储

- JWT 使用 Android Keystore 的 AES-GCM 密钥加密。
- 用户头像保存在应用私有目录。
- 默认图片下载到 `Pictures/AnimeGirlsDownloader`；也可以选择 SAF 授权目录。
- 应用不申请完整文件系统管理权限。
- 浏览和复制链接不会下载原图；只有加入下载队列后才请求原图。

## 许可证

许可证以 AnimeGirlsDownloader 主项目的授权文件为准。
