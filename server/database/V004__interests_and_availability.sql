-- Approved additive migration. Run through the versioned migration tool.

CREATE TABLE interest_topic (
    `主题代码` VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(32) NOT NULL,
    sort_order SMALLINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`主题代码`),
    CONSTRAINT `主题排序范围` CHECK (sort_order>=0),
    CONSTRAINT `主题启用范围` CHECK (is_active IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_interest (
    `用户编号` BIGINT NOT NULL,
    `主题代码` VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    strength TINYINT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`用户编号`,`主题代码`),
    KEY `主题强度` (`主题代码`,strength,`用户编号`),
    CONSTRAINT `兴趣用户关联` FOREIGN KEY (`用户编号`) REFERENCES sys_user(id),
    CONSTRAINT `兴趣主题关联` FOREIGN KEY (`主题代码`) REFERENCES interest_topic(`主题代码`),
    CONSTRAINT `兴趣强度范围` CHECK (strength BETWEEN 0 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_participation (
    `用户编号` BIGINT NOT NULL,
    intent VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'UNKNOWN',
    valid_until TIMESTAMP(6) NULL,
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`用户编号`),
    KEY `意愿到期` (intent,valid_until,`用户编号`),
    CONSTRAINT `意愿用户关联` FOREIGN KEY (`用户编号`) REFERENCES sys_user(id),
    CONSTRAINT `意愿取值范围` CHECK (intent IN ('UNKNOWN','WILLING','NOT_NOW')),
    CONSTRAINT `意愿版本范围` CHECK (version>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_available_slot (
    `时段编号` BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (`时段编号`),
    UNIQUE KEY `用户时段` (user_id,starts_at,ends_at),
    CONSTRAINT `时段意愿关联` FOREIGN KEY (user_id) REFERENCES user_participation(`用户编号`) ON DELETE CASCADE,
    CONSTRAINT `空闲时段顺序` CHECK (ends_at>starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO interest_topic (`主题代码`,name,sort_order) VALUES
('VOLUNTEERING','志愿服务',1),('READING','阅读交流',2),('FITNESS','文体健身',3),
('DANCE','文艺舞蹈',4),('ENVIRONMENT','户外环保',5),('MUTUAL_HELP','邻里互助',6);

