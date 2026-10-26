-- Filtered project task and audit feeds, newest first.
CREATE INDEX idx_task_project_status_time
    ON task (project_id, status, created_at DESC, id DESC);

CREATE INDEX idx_task_project_assignee_time
    ON task (project_id, assignee_id, created_at DESC, id DESC);

CREATE INDEX idx_audit_project_action_time
    ON audit_log (project_id, action, created_at DESC, id DESC);
