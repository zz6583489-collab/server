package com.teamflow.notification.service;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.common.response.PageResult;
import com.teamflow.notification.dto.NotificationDtos;
import com.teamflow.notification.entity.NotificationEntity;
import com.teamflow.notification.repository.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationMapper mapper;

  public NotificationService(NotificationMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void memberAdded(long recipientId, long actorId, long projectId) {
    emit(recipientId, actorId, projectId, null, "MEMBER_ADDED");
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void taskAssigned(long recipientId, long actorId, long projectId, long taskId) {
    emit(recipientId, actorId, projectId, taskId, "TASK_ASSIGNED");
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void reviewRequested(long recipientId, long actorId, long projectId, long taskId) {
    emit(recipientId, actorId, projectId, taskId, "TASK_REVIEW_REQUESTED");
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void taskCompleted(long recipientId, long actorId, long projectId, long taskId) {
    emit(recipientId, actorId, projectId, taskId, "TASK_COMPLETED");
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void reviewRejected(long recipientId, long actorId, long projectId, long taskId) {
    emit(recipientId, actorId, projectId, taskId, "TASK_REVIEW_REJECTED");
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void taskReopened(long recipientId, long actorId, long projectId, long taskId) {
    emit(recipientId, actorId, projectId, taskId, "TASK_REOPENED");
  }

  private void emit(long recipientId, long actorId, long projectId, Long taskId, String type) {
    if (recipientId == actorId) return;
    NotificationEntity notification = new NotificationEntity();
    notification.setRecipientId(recipientId);
    notification.setActorId(actorId);
    notification.setProjectId(projectId);
    notification.setTaskId(taskId);
    notification.setType(type);
    mapper.insert(notification);
  }

  @Transactional(readOnly = true)
  public PageResult<NotificationDtos.View> page(
      long recipientId, int page, int pageSize, boolean unreadOnly) {
    long offset = PageResult.offset(page, pageSize);
    return new PageResult<>(
        mapper.pageVisible(recipientId, unreadOnly, pageSize, offset).stream()
            .map(NotificationDtos.View::from)
            .toList(),
        mapper.countVisible(recipientId, unreadOnly),
        page,
        pageSize);
  }

  @Transactional(readOnly = true)
  public NotificationDtos.UnreadCount unreadCount(long recipientId) {
    return new NotificationDtos.UnreadCount(mapper.countVisible(recipientId, true));
  }

  @Transactional
  public void markRead(long id, long recipientId) {
    if (mapper.findVisible(id, recipientId) == null) {
      throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
    }
    mapper.markRead(id, recipientId);
  }

  @Transactional
  public void markAllRead(long recipientId) {
    mapper.markAllRead(recipientId);
  }
}
