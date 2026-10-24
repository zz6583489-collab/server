-- TeamFlow baseline schema for MySQL 8.0.16+. All DATETIME(3) values are UTC.
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT uk_sys_user_email UNIQUE (email),
    CONSTRAINT ck_sys_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_role_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_role_id (role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    description TEXT NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_project_owner_id (owner_id),
    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES sys_user (id),
    CONSTRAINT ck_project_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(16) NOT NULL,
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_project_member_project_user UNIQUE (project_id, user_id),
    KEY idx_project_member_user_id (user_id),
    CONSTRAINT fk_project_member_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_project_member_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT ck_project_member_role CHECK (member_role IN ('OWNER', 'MEMBER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    assignee_id BIGINT NULL,
    reporter_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    due_at DATETIME(3) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_task_project_status (project_id, status),
    KEY idx_task_assignee_status (assignee_id, status),
    KEY idx_task_project_assignee (project_id, assignee_id),
    KEY idx_task_reporter_id (reporter_id),
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES project (id),
    CONSTRAINT fk_task_assignee_member FOREIGN KEY (project_id, assignee_id)
        REFERENCES project_member (project_id, user_id),
    CONSTRAINT fk_task_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user (id),
    CONSTRAINT ck_task_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    CONSTRAINT ck_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_task_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE task_activity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    from_value VARCHAR(255) NULL,
    to_value VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_task_activity_task_time (task_id, created_at),
    KEY idx_task_activity_actor_id (actor_id),
    CONSTRAINT fk_task_activity_task FOREIGN KEY (task_id) REFERENCES task (id),
    CONSTRAINT fk_task_activity_actor FOREIGN KEY (actor_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    trace_id VARCHAR(36) NOT NULL,
    metadata_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_audit_project_time (project_id, created_at),
    KEY idx_audit_trace_id (trace_id),
    KEY idx_audit_actor_id (actor_id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES sys_user (id),
    CONSTRAINT fk_audit_project FOREIGN KEY (project_id) REFERENCES project (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_role (code, name) VALUES ('SYSTEM_ADMIN', '系统管理员');

