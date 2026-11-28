-- 当前业务表结构：V007后的中文定义，仅供阅读，不作为迁移脚本重复执行。

-- 实际安装/升级由 scripts/migrate-db.ps1 按 V001–V007 执行。

CREATE TABLE `用户账号` (
  `用户编号` bigint NOT NULL AUTO_INCREMENT,
  `用户名` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `密码摘要` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `昵称` varchar(32) NOT NULL,
  `角色` varchar(20) NOT NULL DEFAULT 'RESIDENT',
  `状态` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `创建时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`用户编号`),
  UNIQUE KEY `用户名唯一` (`用户名`),
  CONSTRAINT `用户状态范围` CHECK ((`状态` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED'))),
  CONSTRAINT `用户角色范围` CHECK ((`角色` in (_utf8mb4'RESIDENT',_utf8mb4'STAFF',_utf8mb4'ADMIN')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `居民资料` (
  `用户编号` bigint NOT NULL,
  `楼栋` varchar(32) NOT NULL,
  `单元` varchar(32) NOT NULL,
  `门牌` varchar(32) NOT NULL,
  PRIMARY KEY (`用户编号`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`用户编号`) REFERENCES `用户账号` (`用户编号`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `登录会话` (
  `凭证摘要` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `用户编号` bigint NOT NULL,
  `到期秒数` bigint NOT NULL,
  `创建时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`凭证摘要`),
  KEY `会话用户` (`用户编号`),
  KEY `会话到期` (`到期秒数`),
  CONSTRAINT `fk_session_user` FOREIGN KEY (`用户编号`) REFERENCES `用户账号` (`用户编号`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `诉求工单` (
  `工单编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `上报人` bigint NOT NULL,
  `请求编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `处理人` bigint DEFAULT NULL,
  `标题` varchar(100) NOT NULL,
  `分类` varchar(20) NOT NULL,
  `位置` varchar(200) NOT NULL,
  `描述` varchar(2000) NOT NULL,
  `状态` varchar(20) NOT NULL DEFAULT 'NEW',
  `版本` int NOT NULL DEFAULT '0',
  `结果` varchar(1000) DEFAULT NULL,
  `创建时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `更新时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`工单编号`),
  UNIQUE KEY `工单去重` (`上报人`,`请求编号`),
  KEY `我的上报` (`上报人`,`创建时间`),
  KEY `我的处理` (`处理人`,`创建时间`),
  KEY `工单状态` (`状态`,`创建时间`),
  CONSTRAINT `fk_work_order_assignee` FOREIGN KEY (`处理人`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `fk_work_order_reporter` FOREIGN KEY (`上报人`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `工单分类范围` CHECK ((`分类` in (_utf8mb4'ENVIRONMENT',_utf8mb4'FACILITY',_utf8mb4'SECURITY',_utf8mb4'NOISE',_utf8mb4'OTHER'))),
  CONSTRAINT `工单状态范围` CHECK ((`状态` in (_utf8mb4'NEW',_utf8mb4'ASSIGNED',_utf8mb4'ACCEPTED',_utf8mb4'IN_PROGRESS',_utf8mb4'COMPLETED',_utf8mb4'REJECTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `工单日志` (
  `日志编号` bigint NOT NULL AUTO_INCREMENT,
  `工单编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `操作人` bigint NOT NULL,
  `操作角色` varchar(20) NOT NULL,
  `操作` varchar(20) NOT NULL,
  `原状态` varchar(20) DEFAULT NULL,
  `新状态` varchar(20) NOT NULL,
  `备注` varchar(1000) DEFAULT NULL,
  `创建时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`日志编号`),
  KEY `工单日志` (`工单编号`,`日志编号`),
  KEY `日志操作人` (`操作人`),
  CONSTRAINT `fk_work_order_log_actor` FOREIGN KEY (`操作人`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `fk_work_order_log_order` FOREIGN KEY (`工单编号`) REFERENCES `诉求工单` (`工单编号`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `文件附件` (
  `附件编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `工单编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `存储键` varchar(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `文件类型` varchar(30) NOT NULL,
  `字节数` bigint NOT NULL,
  `创建时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`附件编号`),
  UNIQUE KEY `存储唯一` (`存储键`),
  KEY `工单附件` (`工单编号`),
  CONSTRAINT `fk_file_order` FOREIGN KEY (`工单编号`) REFERENCES `诉求工单` (`工单编号`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `资料授权` (
  `用户编号` bigint NOT NULL,
  `权限代码` varchar(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `授权人` bigint NOT NULL,
  `授权时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `到期时间` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`用户编号`,`权限代码`),
  KEY `权限到期` (`权限代码`,`到期时间`,`用户编号`),
  KEY `授权人` (`授权人`),
  CONSTRAINT `授权人关联` FOREIGN KEY (`授权人`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `授权用户关联` FOREIGN KEY (`用户编号`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `授权代码范围` CHECK ((`权限代码` = _utf8mb4'RESIDENT_PROFILE_READ')),
  CONSTRAINT `授权到期顺序` CHECK (((`到期时间` is null) or (`到期时间` > `授权时间`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `办结附件` (
  `附件编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `日志编号` bigint NOT NULL,
  PRIMARY KEY (`附件编号`),
  KEY `日志附件` (`日志编号`,`附件编号`),
  CONSTRAINT `办结日志关联` FOREIGN KEY (`日志编号`) REFERENCES `工单日志` (`日志编号`) ON DELETE CASCADE,
  CONSTRAINT `办结附件关联` FOREIGN KEY (`附件编号`) REFERENCES `文件附件` (`附件编号`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `兴趣主题` (
  `主题代码` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `名称` varchar(32) NOT NULL,
  `排序` smallint NOT NULL DEFAULT '0',
  `启用` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`主题代码`),
  CONSTRAINT `主题启用范围` CHECK ((`启用` in (0,1))),
  CONSTRAINT `主题排序范围` CHECK ((`排序` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `用户兴趣` (
  `用户编号` bigint NOT NULL,
  `主题代码` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `强度` tinyint NOT NULL,
  `更新时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`用户编号`,`主题代码`),
  KEY `主题强度` (`主题代码`,`强度`,`用户编号`),
  CONSTRAINT `兴趣主题关联` FOREIGN KEY (`主题代码`) REFERENCES `兴趣主题` (`主题代码`),
  CONSTRAINT `兴趣用户关联` FOREIGN KEY (`用户编号`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `兴趣强度范围` CHECK ((`强度` between 0 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `参与意愿` (
  `用户编号` bigint NOT NULL,
  `意愿` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'UNKNOWN',
  `有效期` timestamp(6) NULL DEFAULT NULL,
  `版本` int NOT NULL DEFAULT '0',
  `更新时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`用户编号`),
  KEY `意愿到期` (`意愿`,`有效期`,`用户编号`),
  CONSTRAINT `意愿用户关联` FOREIGN KEY (`用户编号`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `意愿取值范围` CHECK ((`意愿` in (_utf8mb4'UNKNOWN',_utf8mb4'WILLING',_utf8mb4'NOT_NOW'))),
  CONSTRAINT `意愿版本范围` CHECK ((`版本` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `空闲时段` (
  `时段编号` bigint NOT NULL AUTO_INCREMENT,
  `用户编号` bigint NOT NULL,
  `开始时间` timestamp(6) NOT NULL,
  `结束时间` timestamp(6) NOT NULL,
  PRIMARY KEY (`时段编号`),
  UNIQUE KEY `用户时段` (`用户编号`,`开始时间`,`结束时间`),
  CONSTRAINT `时段意愿关联` FOREIGN KEY (`用户编号`) REFERENCES `参与意愿` (`用户编号`) ON DELETE CASCADE,
  CONSTRAINT `空闲时段顺序` CHECK ((`结束时间` > `开始时间`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `场地` (
  `场地编号` bigint NOT NULL AUTO_INCREMENT,
  `名称` varchar(100) NOT NULL,
  `容量` int NOT NULL,
  `开放时间` time NOT NULL,
  `关闭时间` time NOT NULL,
  `允许音响` tinyint(1) NOT NULL DEFAULT '0',
  `规则` varchar(1000) NOT NULL,
  `状态` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
  `创建时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `更新时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`场地编号`),
  KEY `场地状态` (`状态`,`场地编号`),
  CONSTRAINT `场地人数范围` CHECK ((`容量` > 0)),
  CONSTRAINT `场地开放顺序` CHECK ((`开放时间` < `关闭时间`)),
  CONSTRAINT `场地状态范围` CHECK ((`状态` in (_utf8mb4'ACTIVE',_utf8mb4'INACTIVE'))),
  CONSTRAINT `场地音响范围` CHECK ((`允许音响` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `社区活动` (
  `活动编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `发起人` bigint NOT NULL,
  `请求编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `主题代码` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `标题` varchar(100) NOT NULL,
  `描述` varchar(2000) NOT NULL,
  `场地编号` bigint NOT NULL,
  `开始时间` timestamp(6) NOT NULL,
  `结束时间` timestamp(6) NOT NULL,
  `容量` int NOT NULL,
  `使用音响` tinyint(1) NOT NULL DEFAULT '0',
  `状态` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `版本` int NOT NULL DEFAULT '0',
  `审核方式` varchar(16) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `审核原因` json NOT NULL,
  `审核人` bigint DEFAULT NULL,
  `审核时间` timestamp(6) NULL DEFAULT NULL,
  `创建时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `更新时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`活动编号`),
  UNIQUE KEY `提交去重` (`发起人`,`请求编号`),
  KEY `活动状态` (`状态`,`开始时间`,`活动编号`),
  KEY `我的发起` (`发起人`,`创建时间`,`活动编号`),
  KEY `场地占用` (`场地编号`,`状态`,`开始时间`,`结束时间`),
  KEY `活动主题` (`主题代码`),
  KEY `活动审核人` (`审核人`),
  CONSTRAINT `活动主题关联` FOREIGN KEY (`主题代码`) REFERENCES `兴趣主题` (`主题代码`),
  CONSTRAINT `活动发起人关联` FOREIGN KEY (`发起人`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `活动场地关联` FOREIGN KEY (`场地编号`) REFERENCES `场地` (`场地编号`),
  CONSTRAINT `活动审核人关联` FOREIGN KEY (`审核人`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `活动例外数组` CHECK ((json_type(`审核原因`) = _utf8mb4'ARRAY')),
  CONSTRAINT `活动审核方式` CHECK (((`审核方式` is null) or (`审核方式` in (_utf8mb4'AUTO',_utf8mb4'MANUAL')))),
  CONSTRAINT `活动容量范围` CHECK ((`容量` > 0)),
  CONSTRAINT `活动时间顺序` CHECK ((`结束时间` > `开始时间`)),
  CONSTRAINT `活动版本范围` CHECK ((`版本` >= 0)),
  CONSTRAINT `活动状态范围` CHECK ((`状态` in (_utf8mb4'PENDING_REVIEW',_utf8mb4'REJECTED',_utf8mb4'PUBLISHED',_utf8mb4'ENDED',_utf8mb4'CANCELLED'))),
  CONSTRAINT `活动音响范围` CHECK ((`使用音响` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `活动报名` (
  `活动编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `用户编号` bigint NOT NULL,
  `状态` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `创建时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `更新时间` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`活动编号`,`用户编号`),
  KEY `活动报名` (`活动编号`,`状态`,`用户编号`),
  KEY `我的报名` (`用户编号`,`状态`,`活动编号`),
  CONSTRAINT `报名活动关联` FOREIGN KEY (`活动编号`) REFERENCES `社区活动` (`活动编号`),
  CONSTRAINT `报名用户关联` FOREIGN KEY (`用户编号`) REFERENCES `用户账号` (`用户编号`),
  CONSTRAINT `报名状态范围` CHECK ((`状态` in (_utf8mb4'CONFIRMED',_utf8mb4'WITHDRAWN',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `活动机会` (
  `机会编号` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `主题代码` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `场地编号` bigint NOT NULL,
  `开始时间` timestamp(6) NOT NULL,
  `结束时间` timestamp(6) NOT NULL,
  `标题` varchar(100) NOT NULL,
  `推荐依据` varchar(500) NOT NULL,
  `兴趣人数` int NOT NULL,
  `意愿人数` int NOT NULL,
  `可用人数` int NOT NULL,
  `来源类型` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `规则版本` varchar(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `生成时间` timestamp(6) NOT NULL,
  `到期时间` timestamp(6) NOT NULL,
  PRIMARY KEY (`机会编号`),
  UNIQUE KEY `机会去重` (`主题代码`,`场地编号`,`开始时间`,`结束时间`,`来源类型`),
  KEY `机会有效期` (`到期时间`,`开始时间`),
  KEY `主题机会` (`主题代码`,`到期时间`),
  KEY `场地机会` (`场地编号`),
  CONSTRAINT `机会主题关联` FOREIGN KEY (`主题代码`) REFERENCES `兴趣主题` (`主题代码`),
  CONSTRAINT `机会场地关联` FOREIGN KEY (`场地编号`) REFERENCES `场地` (`场地编号`),
  CONSTRAINT `机会人数范围` CHECK (((`可用人数` >= 0) and (`意愿人数` >= `可用人数`) and (`兴趣人数` >= `意愿人数`))),
  CONSTRAINT `机会时段顺序` CHECK ((`结束时间` > `开始时间`)),
  CONSTRAINT `机会有效顺序` CHECK ((`到期时间` > `生成时间`)),
  CONSTRAINT `机会来源范围` CHECK ((`来源类型` in (_utf8mb4'RULE',_utf8mb4'FCM')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
