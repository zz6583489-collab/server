-- A rejected review is distinct from reopening a completed task.
ALTER TABLE user_notification DROP CHECK ck_notification_type;
ALTER TABLE user_notification ADD CONSTRAINT ck_notification_type CHECK (type IN (
    'MEMBER_ADDED', 'TASK_ASSIGNED', 'TASK_REVIEW_REQUESTED',
    'TASK_REVIEW_REJECTED', 'TASK_COMPLETED', 'TASK_REOPENED'));
