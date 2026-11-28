# 邻里共治 · 阶段开发

本包为前后端分离项目源码，客户端版本1.1.0。

本次提交记录当前阶段实现，后续继续设计与完善推荐算法、业务数据和前端界面。

```text
android/  Java/XML Android客户端
server/   Spring Boot后端、MySQL迁移和数据导入代码
```

## 运行环境

Android Studio、Android SDK Platform37；最低运行Android8.0/API26。Java源码级别17，Android工程的Gradle守护进程使用配置中的JDK25。
后端Java17、Spring Boot3.5.16、MyBatis3.0.5、MySQL8.4。后端提供Windows本地环境安装及启动脚本，工具版本与下载校验值在 `server/scripts/config/tools.json`。

建议解压至英文路径，例如 `D:/Projects/neighborhood-fullstack`，以避免Windows中文路径工具兼容问题。

## 启动后端

首次在PowerShell进入server目录：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-tools.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\init-db.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\migrate-db.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\configure-ide.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\maven.ps1 package
powershell -ExecutionPolicy Bypass -File .\scripts\import-stage-data.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1
```

默认MySQL端口3307，服务端口8080。若电脑上已经运行另一套项目环境，请先停止它，或在首次初始化时使用 `init-db.ps1 -DbPort 3308 -ServerPort 8081` 并同步Android访问地址及ADB转发端口。已有环境只需打包并启动；修改已运行后端前先执行stop.ps1。脚本拒绝覆盖已存在的数据库或不属于本项目的服务。

数据库16张业务表及字段使用中文，Java/JSON接口保持英文。V001–V007按顺序校验和执行；`current-schema.sql`仅供阅读，不与迁移重复执行。后端数据库配置由本机 `.local/environment.json` 管理，未包含在包内。

## 初始记录

原前端固定数据改为数据库记录：两个居民、三个工作人员角色账号、三个场地、一个工单及日志、五个活动、一条报名、两条兴趣和一条明确住户查询授权。由 `import-stage-data.ps1` 显式导入，一次事务完成；重复执行不重置、不复制，不覆盖已有同名账号。

账号密码在接收方运行导入时独立生成，保存在 `server/.local/stage-accounts.json`；不是前端固定密码。前端登录页不列出账号、不自动填入密码。普通管理员不自动获得住户查询权限。

`database/seeds/stage-data.sql`保留阶段数据快照，活动时间不会随应用启动滚动。旧前端虚构的机会人数未导入，活动机会由后端根据实际记录聚合；FCM尚未实现。

## 启动Android

Android Studio打开android目录，配置本机SDK并完成同步。启动模拟器后：

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb reverse tcp:8080 tcp:8080
```

运行app的debug变体。当前只有一个联网版本，默认地址 `http://127.0.0.1:8080`，通过ADB转发访问电脑后端。多设备时加 `-s 设备编号`，模拟器或ADB重启后重新转发。

命令行构建：

```powershell
cd android
.\scripts\build.ps1 -JavaHome $env:JAVA_HOME
```

使用远程服务时添加 `-BackendUrl 'https://服务域名'`。只有debug允许HTTP；release需HTTPS及正式签名。

## 阶段完成范围

账号与私密资料授权、工单上报及处理、图片附件、兴趣与近期安排、规则机会、活动发布/审核/报名/退出等流程已经接入后端。个人公开信息只展示昵称。离线保留本地草稿，业务数据通过接口读取。

当前源码不包含本地演示数据源、账号快捷选择、演示横幅、重置场景、独立测试代码或测试依赖。前后端业务校验、状态与权限控制、数据库迁移守卫、健康检查以及构建启动能力保留。

包内仅提供源码、资源、构建配置、数据脚本和README；不含APK/JAR构建产物、本机数据库、密码/Token、SDK路径、历史备份或过程测试文件。验证记录单独保留于原工作区。
