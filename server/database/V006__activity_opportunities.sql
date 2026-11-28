-- Approved additive migration. Run through the versioned migration tool.

CREATE TABLE activity_opportunity (
    `机会编号` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    topic_code VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    venue_id BIGINT NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    title VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    potential_count INT NOT NULL,
    willing_count INT NOT NULL,
    available_count INT NOT NULL,
    source_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_version VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    generated_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (`机会编号`),
    UNIQUE KEY `机会去重` (topic_code,venue_id,starts_at,ends_at,source_type),
    KEY `机会有效期` (expires_at,starts_at),
    KEY `主题机会` (topic_code,expires_at),
    KEY `场地机会` (venue_id),
    CONSTRAINT `机会主题关联` FOREIGN KEY(topic_code) REFERENCES interest_topic(`主题代码`),
    CONSTRAINT `机会场地关联` FOREIGN KEY(venue_id) REFERENCES venue(`场地编号`),
    CONSTRAINT `机会时段顺序` CHECK(ends_at>starts_at),
    CONSTRAINT `机会有效顺序` CHECK(expires_at>generated_at),
    CONSTRAINT `机会人数范围` CHECK(available_count>=0 AND willing_count>=available_count AND potential_count>=willing_count),
    CONSTRAINT `机会来源范围` CHECK(source_type IN('RULE','FCM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

