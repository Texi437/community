# 邻里共治 · Android 前端

Java/XML原生Android客户端，采用前后端分离：App通过HTTP接口调用Spring Boot后端，业务数据存储于MySQL。当前版本1.1.0，统一使用真实后端。

## 运行

1. 用Android Studio打开本目录，安装Android SDK Platform37，等待Gradle同步。最低API26，Java源码级别17，Gradle守护进程按工程配置使用JDK25。
2. 在配套后端运行 `scripts/start.ps1`，默认监听本机8080。
3. 启动Android Studio模拟器，配置ADB转发：

   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb devices
   & $adb reverse tcp:8080 tcp:8080
   ```

4. 运行 `app` 的 `debug` 变体。登录数据库中的账号或注册居民账号。多设备时给ADB加 `-s 设备编号`；ADB或模拟器重启后重新执行转发。

本机默认地址 `http://127.0.0.1:8080`。也可重新构建指定远程地址：

```powershell
.\scripts\build.ps1 -JavaHome $env:JAVA_HOME
.\scripts\build.ps1 -JavaHome $env:JAVA_HOME -BackendUrl 'https://你的服务域名'
```

构建脚本执行assembleDebug和lintDebug；Windows中文路径项目在英文临时目录构建，完成后回传产物和检查报告。临时目录本身含中文时，可用 `-BuildRoot C:\AndroidBuild` 指定英文目录。APK输出 `app/build/outputs/apk/debug/app-debug.apk`。仅debug允许HTTP；release需HTTPS和正式签名。`local.properties`由接收方配置，不包含在源码包。

## 数据与账号

账号、角色、资料、工单、场地、活动、报名、兴趣与近期安排均由后端和数据库管理。前端不内置账号、密码、示例工单或固定推荐人数，不提供角色快捷填充、演示标识和重置场景入口。本地只保留加密会话、草稿及必要附件缓存。

原前端初始记录已迁入配套数据库。后端 `scripts/import-stage-data.ps1` 负责显式、事务化的一次性导入；不会随App启动执行，也不会重置已有数据。生成的账号密码保存在后端 `.local/stage-accounts.json`，不进入源码提交；由项目维护人员单独提供。原本的统一演示密码不再使用。

## 已实现功能

- 注册、登录、退出、本人资料、授权住户查询。
- 报事分类、图片、草稿、列表、时间线、派单/驳回/接单/处理/办结。
- 兴趣强度、近期意愿、空闲时段、规则活动机会。
- 活动发布、审核、报名/退出、我的发起/报名、取消/结束。

公开身份仅展示昵称；私密资料由后端校验授权。Token使用Keystore加密，登录失效返回登录页。工单和活动使用请求ID防重复、版本号防并发覆盖。机会人数来自真实记录聚合，FCM尚未实现。

## 代码结构

```text
app/src/main/java/com/example/neighborhood/
  model/       业务模型
  domain/      页面操作权限规则
  data/        Repository接口、草稿、会话
  data/remote/ HTTP客户端及四个接口实现
  di/          数据源装配
  ui/          登录、工单、兴趣、活动、资料、工作台
app/src/main/res/  XML界面和资源
app/src/debug/    本地调试网络配置
scripts/build.ps1 构建入口
```

源码交付包含代码、资源、构建配置和README；不包含测试代码、编译产物、本机密钥、SDK路径或过程文件。完整项目源码包另包含配套后端及数据库迁移/数据导入代码。既有测试结果保留在工作区文档中。

提交GitHub时使用Git读取本目录的`.gitignore`。原始设计文件、历史验证记录、临时目录和旧交付包保留在本机并排除提交。`gradle/wrapper/gradle-wrapper.jar`是构建必需文件，应随源码提交。前端和配套后端位于两个独立目录，上传前端不会自动包含后端。
