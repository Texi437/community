-- Approved additive migration. Run through the versioned migration tool.

CREATE TABLE sys_user_permission (
    `用户编号` BIGINT NOT NULL,
    `权限代码` VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    granted_by BIGINT NOT NULL,
    granted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at TIMESTAMP(6) NULL,
    PRIMARY KEY (`用户编号`,`权限代码`),
    KEY `权限到期` (`权限代码`,expires_at,`用户编号`),
    KEY `授权人` (granted_by),
    CONSTRAINT `授权用户关联` FOREIGN KEY (`用户编号`) REFERENCES sys_user(id),
    CONSTRAINT `授权人关联` FOREIGN KEY (granted_by) REFERENCES sys_user(id),
    CONSTRAINT `授权代码范围` CHECK (`权限代码`='RESIDENT_PROFILE_READ'),
    CONSTRAINT `授权到期顺序` CHECK (expires_at IS NULL OR expires_at > granted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE work_order_log_asset (
    `附件编号` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    log_id BIGINT NOT NULL,
    PRIMARY KEY (`附件编号`),
    KEY `日志附件` (log_id,`附件编号`),
    CONSTRAINT `办结附件关联` FOREIGN KEY (`附件编号`) REFERENCES file_asset(id) ON DELETE CASCADE,
    CONSTRAINT `办结日志关联` FOREIGN KEY (log_id) REFERENCES work_order_log(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

