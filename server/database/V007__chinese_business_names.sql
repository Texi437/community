-- V007: rename business identifiers; preserve column types, values, indexes and relationships.

-- Maintenance only: stop application writes. Remove CHECKs, rename columns INPLACE, restore equivalent CHECKs.

-- MySQL cannot combine these operations for all FK-dependent tables. Failure leaves V007 FAILED; do not restart the old backend.

ALTER TABLE `sys_user`
    DROP CHECK `ck_user_role`,
    DROP CHECK `ck_user_status`;

ALTER TABLE `sys_user`
    RENAME COLUMN `id` TO `用户编号`,
    RENAME COLUMN `username` TO `用户名`,
    RENAME COLUMN `password_hash` TO `密码摘要`,
    RENAME COLUMN `nickname` TO `昵称`,
    RENAME COLUMN `role` TO `角色`,
    RENAME COLUMN `status` TO `状态`,
    RENAME COLUMN `created_at` TO `创建时间`,
    RENAME INDEX `uk_user_username` TO `用户名唯一`,
    ALGORITHM=INPLACE;

ALTER TABLE `sys_user`
    ADD CONSTRAINT `用户角色范围` CHECK (`角色` IN ('RESIDENT', 'STAFF', 'ADMIN')),
    ADD CONSTRAINT `用户状态范围` CHECK (`状态` IN ('ACTIVE', 'DISABLED'));

ALTER TABLE `resident_profile`
    RENAME COLUMN `user_id` TO `用户编号`,
    RENAME COLUMN `building` TO `楼栋`,
    RENAME COLUMN `unit` TO `单元`,
    RENAME COLUMN `room` TO `门牌`,
    ALGORITHM=INPLACE;

ALTER TABLE `auth_session`
    RENAME COLUMN `token_hash` TO `凭证摘要`,
    RENAME COLUMN `user_id` TO `用户编号`,
    RENAME COLUMN `expires_at_epoch` TO `到期秒数`,
    RENAME COLUMN `created_at` TO `创建时间`,
    RENAME INDEX `idx_session_user` TO `会话用户`,
    RENAME INDEX `idx_session_expiry` TO `会话到期`,
    ALGORITHM=INPLACE;

ALTER TABLE `work_order`
    DROP CHECK `ck_work_order_status`,
    DROP CHECK `ck_work_order_category`;

ALTER TABLE `work_order`
    RENAME COLUMN `id` TO `工单编号`,
    RENAME COLUMN `reporter_id` TO `上报人`,
    RENAME COLUMN `client_request_id` TO `请求编号`,
    RENAME COLUMN `assignee_id` TO `处理人`,
    RENAME COLUMN `title` TO `标题`,
    RENAME COLUMN `category` TO `分类`,
    RENAME COLUMN `location` TO `位置`,
    RENAME COLUMN `description` TO `描述`,
    RENAME COLUMN `status` TO `状态`,
    RENAME COLUMN `version` TO `版本`,
    RENAME COLUMN `result` TO `结果`,
    RENAME COLUMN `created_at` TO `创建时间`,
    RENAME COLUMN `updated_at` TO `更新时间`,
    RENAME INDEX `uk_work_order_request` TO `工单去重`,
    RENAME INDEX `idx_work_order_reporter` TO `我的上报`,
    RENAME INDEX `idx_work_order_assignee` TO `我的处理`,
    RENAME INDEX `idx_work_order_status` TO `工单状态`,
    ALGORITHM=INPLACE;

ALTER TABLE `work_order`
    ADD CONSTRAINT `工单状态范围` CHECK (`状态` IN ('NEW','ASSIGNED','ACCEPTED','IN_PROGRESS','COMPLETED','REJECTED')),
    ADD CONSTRAINT `工单分类范围` CHECK (`分类` IN ('ENVIRONMENT','FACILITY','SECURITY','NOISE','OTHER'));

ALTER TABLE `work_order_log`
    RENAME COLUMN `id` TO `日志编号`,
    RENAME COLUMN `work_order_id` TO `工单编号`,
    RENAME COLUMN `actor_id` TO `操作人`,
    RENAME COLUMN `actor_role` TO `操作角色`,
    RENAME COLUMN `action` TO `操作`,
    RENAME COLUMN `from_status` TO `原状态`,
    RENAME COLUMN `to_status` TO `新状态`,
    RENAME COLUMN `note` TO `备注`,
    RENAME COLUMN `created_at` TO `创建时间`,
    RENAME INDEX `idx_work_order_log_order` TO `工单日志`,
    RENAME INDEX `fk_work_order_log_actor` TO `日志操作人`,
    ALGORITHM=INPLACE;

ALTER TABLE `file_asset`
    RENAME COLUMN `id` TO `附件编号`,
    RENAME COLUMN `work_order_id` TO `工单编号`,
    RENAME COLUMN `storage_key` TO `存储键`,
    RENAME COLUMN `content_type` TO `文件类型`,
    RENAME COLUMN `byte_size` TO `字节数`,
    RENAME COLUMN `created_at` TO `创建时间`,
    RENAME INDEX `uk_file_storage_key` TO `存储唯一`,
    RENAME INDEX `idx_file_order` TO `工单附件`,
    ALGORITHM=INPLACE;

ALTER TABLE `sys_user_permission`
    DROP CHECK `授权代码范围`,
    DROP CHECK `授权到期顺序`;

ALTER TABLE `sys_user_permission`
    RENAME COLUMN `granted_by` TO `授权人`,
    RENAME COLUMN `granted_at` TO `授权时间`,
    RENAME COLUMN `expires_at` TO `到期时间`,
    ALGORITHM=INPLACE;

ALTER TABLE `sys_user_permission`
    ADD CONSTRAINT `授权代码范围` CHECK (`权限代码`='RESIDENT_PROFILE_READ'),
    ADD CONSTRAINT `授权到期顺序` CHECK (`到期时间` IS NULL OR `到期时间` > `授权时间`);

ALTER TABLE `work_order_log_asset`
    RENAME COLUMN `log_id` TO `日志编号`,
    ALGORITHM=INPLACE;

ALTER TABLE `interest_topic`
    DROP CHECK `主题排序范围`,
    DROP CHECK `主题启用范围`;

ALTER TABLE `interest_topic`
    RENAME COLUMN `name` TO `名称`,
    RENAME COLUMN `sort_order` TO `排序`,
    RENAME COLUMN `is_active` TO `启用`,
    ALGORITHM=INPLACE;

ALTER TABLE `interest_topic`
    ADD CONSTRAINT `主题排序范围` CHECK (`排序`>=0),
    ADD CONSTRAINT `主题启用范围` CHECK (`启用` IN (0,1));

ALTER TABLE `user_interest`
    DROP CHECK `兴趣强度范围`;

ALTER TABLE `user_interest`
    RENAME COLUMN `strength` TO `强度`,
    RENAME COLUMN `updated_at` TO `更新时间`,
    ALGORITHM=INPLACE;

ALTER TABLE `user_interest`
    ADD CONSTRAINT `兴趣强度范围` CHECK (`强度` BETWEEN 0 AND 5);

ALTER TABLE `user_participation`
    DROP CHECK `意愿取值范围`,
    DROP CHECK `意愿版本范围`;

ALTER TABLE `user_participation`
    RENAME COLUMN `intent` TO `意愿`,
    RENAME COLUMN `valid_until` TO `有效期`,
    RENAME COLUMN `version` TO `版本`,
    RENAME COLUMN `updated_at` TO `更新时间`,
    ALGORITHM=INPLACE;

ALTER TABLE `user_participation`
    ADD CONSTRAINT `意愿取值范围` CHECK (`意愿` IN ('UNKNOWN','WILLING','NOT_NOW')),
    ADD CONSTRAINT `意愿版本范围` CHECK (`版本`>=0);

ALTER TABLE `user_available_slot`
    DROP CHECK `空闲时段顺序`;

ALTER TABLE `user_available_slot`
    RENAME COLUMN `user_id` TO `用户编号`,
    RENAME COLUMN `starts_at` TO `开始时间`,
    RENAME COLUMN `ends_at` TO `结束时间`,
    ALGORITHM=INPLACE;

ALTER TABLE `user_available_slot`
    ADD CONSTRAINT `空闲时段顺序` CHECK (`结束时间`>`开始时间`);

ALTER TABLE `venue`
    DROP CHECK `场地人数范围`,
    DROP CHECK `场地开放顺序`,
    DROP CHECK `场地音响范围`,
    DROP CHECK `场地状态范围`;

ALTER TABLE `venue`
    RENAME COLUMN `name` TO `名称`,
    RENAME COLUMN `capacity` TO `容量`,
    RENAME COLUMN `opens_at` TO `开放时间`,
    RENAME COLUMN `closes_at` TO `关闭时间`,
    RENAME COLUMN `allows_speaker` TO `允许音响`,
    RENAME COLUMN `rule_text` TO `规则`,
    RENAME COLUMN `status` TO `状态`,
    RENAME COLUMN `created_at` TO `创建时间`,
    RENAME COLUMN `updated_at` TO `更新时间`,
    ALGORITHM=INPLACE;

ALTER TABLE `venue`
    ADD CONSTRAINT `场地人数范围` CHECK (`容量`>0),
    ADD CONSTRAINT `场地开放顺序` CHECK (`开放时间`<`关闭时间`),
    ADD CONSTRAINT `场地音响范围` CHECK (`允许音响` IN(0,1)),
    ADD CONSTRAINT `场地状态范围` CHECK (`状态` IN('ACTIVE','INACTIVE'));

ALTER TABLE `community_activity`
    DROP CHECK `活动时间顺序`,
    DROP CHECK `活动容量范围`,
    DROP CHECK `活动版本范围`,
    DROP CHECK `活动音响范围`,
    DROP CHECK `活动状态范围`,
    DROP CHECK `活动审核方式`,
    DROP CHECK `活动例外数组`;

ALTER TABLE `community_activity`
    RENAME COLUMN `organizer_id` TO `发起人`,
    RENAME COLUMN `client_request_id` TO `请求编号`,
    RENAME COLUMN `topic_code` TO `主题代码`,
    RENAME COLUMN `title` TO `标题`,
    RENAME COLUMN `description` TO `描述`,
    RENAME COLUMN `venue_id` TO `场地编号`,
    RENAME COLUMN `starts_at` TO `开始时间`,
    RENAME COLUMN `ends_at` TO `结束时间`,
    RENAME COLUMN `capacity` TO `容量`,
    RENAME COLUMN `uses_speaker` TO `使用音响`,
    RENAME COLUMN `status` TO `状态`,
    RENAME COLUMN `version` TO `版本`,
    RENAME COLUMN `review_method` TO `审核方式`,
    RENAME COLUMN `review_reasons` TO `审核原因`,
    RENAME COLUMN `reviewed_by` TO `审核人`,
    RENAME COLUMN `reviewed_at` TO `审核时间`,
    RENAME COLUMN `created_at` TO `创建时间`,
    RENAME COLUMN `updated_at` TO `更新时间`,
    ALGORITHM=INPLACE;

ALTER TABLE `community_activity`
    ADD CONSTRAINT `活动时间顺序` CHECK (`结束时间`>`开始时间`),
    ADD CONSTRAINT `活动容量范围` CHECK (`容量`>0),
    ADD CONSTRAINT `活动版本范围` CHECK (`版本`>=0),
    ADD CONSTRAINT `活动音响范围` CHECK (`使用音响` IN(0,1)),
    ADD CONSTRAINT `活动状态范围` CHECK (`状态` IN('PENDING_REVIEW','REJECTED','PUBLISHED','ENDED','CANCELLED')),
    ADD CONSTRAINT `活动审核方式` CHECK (`审核方式` IS NULL OR `审核方式` IN('AUTO','MANUAL')),
    ADD CONSTRAINT `活动例外数组` CHECK (JSON_TYPE(`审核原因`)='ARRAY');

ALTER TABLE `activity_enrollment`
    DROP CHECK `报名状态范围`;

ALTER TABLE `activity_enrollment`
    RENAME COLUMN `status` TO `状态`,
    RENAME COLUMN `created_at` TO `创建时间`,
    RENAME COLUMN `updated_at` TO `更新时间`,
    ALGORITHM=INPLACE;

ALTER TABLE `activity_enrollment`
    ADD CONSTRAINT `报名状态范围` CHECK (`状态` IN('CONFIRMED','WITHDRAWN','CANCELLED'));

ALTER TABLE `activity_opportunity`
    DROP CHECK `机会时段顺序`,
    DROP CHECK `机会有效顺序`,
    DROP CHECK `机会人数范围`,
    DROP CHECK `机会来源范围`;

ALTER TABLE `activity_opportunity`
    RENAME COLUMN `topic_code` TO `主题代码`,
    RENAME COLUMN `venue_id` TO `场地编号`,
    RENAME COLUMN `starts_at` TO `开始时间`,
    RENAME COLUMN `ends_at` TO `结束时间`,
    RENAME COLUMN `title` TO `标题`,
    RENAME COLUMN `reason` TO `推荐依据`,
    RENAME COLUMN `potential_count` TO `兴趣人数`,
    RENAME COLUMN `willing_count` TO `意愿人数`,
    RENAME COLUMN `available_count` TO `可用人数`,
    RENAME COLUMN `source_type` TO `来源类型`,
    RENAME COLUMN `source_version` TO `规则版本`,
    RENAME COLUMN `generated_at` TO `生成时间`,
    RENAME COLUMN `expires_at` TO `到期时间`,
    ALGORITHM=INPLACE;

ALTER TABLE `activity_opportunity`
    ADD CONSTRAINT `机会时段顺序` CHECK (`结束时间`>`开始时间`),
    ADD CONSTRAINT `机会有效顺序` CHECK (`到期时间`>`生成时间`),
    ADD CONSTRAINT `机会人数范围` CHECK (`可用人数`>=0 AND `意愿人数`>=`可用人数` AND `兴趣人数`>=`意愿人数`),
    ADD CONSTRAINT `机会来源范围` CHECK (`来源类型` IN('RULE','FCM'));

RENAME TABLE
    `sys_user` TO `用户账号`,
    `resident_profile` TO `居民资料`,
    `auth_session` TO `登录会话`,
    `work_order` TO `诉求工单`,
    `work_order_log` TO `工单日志`,
    `file_asset` TO `文件附件`,
    `sys_user_permission` TO `资料授权`,
    `work_order_log_asset` TO `办结附件`,
    `interest_topic` TO `兴趣主题`,
    `user_interest` TO `用户兴趣`,
    `user_participation` TO `参与意愿`,
    `user_available_slot` TO `空闲时段`,
    `venue` TO `场地`,
    `community_activity` TO `社区活动`,
    `activity_enrollment` TO `活动报名`,
    `activity_opportunity` TO `活动机会`;
