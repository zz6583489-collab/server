-- Refresh credentials and login throttling. Times use the UTC database session.
CREATE TABLE auth_session (
    id CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    refresh_hash CHAR(64) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_auth_session_user (user_id),
    KEY idx_auth_session_expiry (expires_at),
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE auth_login_attempt (
    key_hash CHAR(64) NOT NULL,
    failures INT NOT NULL,
    window_started_at DATETIME(3) NOT NULL,
    PRIMARY KEY (key_hash),
    KEY idx_auth_login_attempt_window (window_started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
