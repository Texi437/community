# 邻里共治 · 后端

Spring Boot / Java 17 后端，使用 MyBatis 访问 MySQL。完整环境安装、数据库迁移、初始数据导入和 Android 联调步骤见 [根目录 README](../README.md)。

```text
backend/src/main/  Java 业务代码与运行配置
backend/pom.xml   后端构建与依赖
database/         V001–V007 迁移、中文结构与阶段数据
scripts/          安装、初始化、迁移、构建、启停和健康检查
pom.xml           IntelliJ IDEA 的 Maven 工作区入口
```

首次使用按根目录 README 配置环境。完成后，IntelliJ IDEA 打开本目录 `pom.xml`，运行 `scripts/configure-ide.ps1` 生成本机运行配置；启动 `scripts/start-db.ps1`，再选择 **Linli Backend** 运行。

已配置环境的常用命令：

```powershell
.\scripts\maven.ps1 package
.\scripts\start.ps1
.\scripts\check.ps1
.\scripts\stop.ps1
```

修改代码后先重新打包，停止旧后端，再启动。需要保留数据库运行时，停止后端使用 `scripts/stop.ps1 -KeepDatabase`。

数据库、上传图片和本机生成的账号密码分别保存在 `.runtime/` 与 `.local/`，均不提交 Git。数据库结构和数据初始化通过脚本在接收方电脑生成，不随源码携带本机数据库或密码。
