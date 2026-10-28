-- In-app notifications. Content is resolved only after current project membership is checked.
CREATE TABLE user_notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    type VARCHAR(32) NOT NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_notification_recipient_read_time (recipient_id, read_at, created_at DESC, id DESC),
    KEY idx_notification_project (project_id),
    KEY idx_notification_actor (actor_id),
    KEY idx_notification_task (task_id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES sys_user (id),
    CONSTRAINT fk_notification_actor FOREIGN KEY (actor_id) REFERENCES sys_user (id),
    CONSTRAINT fk_notification_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_notification_task FOREIGN KEY (task_id) REFERENCES task (id),
    CONSTRAINT ck_notification_type CHECK (type IN (
        'MEMBER_ADDED', 'TASK_ASSIGNED', 'TASK_REVIEW_REQUESTED',
        'TASK_COMPLETED', 'TASK_REOPENED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
