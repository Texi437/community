CREATE TABLE IF NOT EXISTS work_order (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 reporter_id BIGINT NOT NULL,
 client_request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 assignee_id BIGINT NULL,
 title VARCHAR(100) NOT NULL,
 category VARCHAR(20) NOT NULL,
 location VARCHAR(200) NOT NULL,
 description VARCHAR(2000) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'NEW',
 version INT NOT NULL DEFAULT 0,
 result VARCHAR(1000) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_work_order_request (reporter_id, client_request_id),
 KEY idx_work_order_reporter (reporter_id, created_at),
 KEY idx_work_order_assignee (assignee_id, created_at),
 KEY idx_work_order_status (status, created_at),
 CONSTRAINT fk_work_order_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user(id),
 CONSTRAINT fk_work_order_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id),
 CONSTRAINT ck_work_order_status CHECK (status IN ('NEW','ASSIGNED','ACCEPTED','IN_PROGRESS','COMPLETED','REJECTED')),
 CONSTRAINT ck_work_order_category CHECK (category IN ('ENVIRONMENT','FACILITY','SECURITY','NOISE','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS work_order_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 work_order_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 actor_id BIGINT NOT NULL,
 actor_role VARCHAR(20) NOT NULL,
 action VARCHAR(20) NOT NULL,
 from_status VARCHAR(20) NULL,
 to_status VARCHAR(20) NOT NULL,
 note VARCHAR(1000) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_work_order_log_order (work_order_id,id),
 CONSTRAINT fk_work_order_log_order FOREIGN KEY (work_order_id) REFERENCES work_order(id) ON DELETE CASCADE,
 CONSTRAINT fk_work_order_log_actor FOREIGN KEY (actor_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS file_asset (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 work_order_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 storage_key VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 content_type VARCHAR(30) NOT NULL,
 byte_size BIGINT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_file_storage_key (storage_key),
 KEY idx_file_order (work_order_id),
 CONSTRAINT fk_file_order FOREIGN KEY (work_order_id) REFERENCES work_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
