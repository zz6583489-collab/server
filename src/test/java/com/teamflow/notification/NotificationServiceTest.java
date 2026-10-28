package com.teamflow.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.notification.repository.NotificationMapper;
import com.teamflow.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {
  private NotificationMapper mapper;
  private NotificationService service;

  @BeforeEach
  void setUp() {
    mapper = org.mockito.Mockito.mock(NotificationMapper.class);
    service = new NotificationService(mapper);
  }

  @Test
  void doesNotNotifyActorAboutOwnAction() {
    service.memberAdded(7, 7, 5);
    service.taskAssigned(7, 7, 5, 10);
    verify(mapper, never()).insert(any());
  }

  @Test
  void emitsRecipientScopedTaskAssignment() {
    service.taskAssigned(8, 7, 5, 10);
    org.mockito.ArgumentCaptor<com.teamflow.notification.entity.NotificationEntity> captor =
        org.mockito.ArgumentCaptor.forClass(
            com.teamflow.notification.entity.NotificationEntity.class);
    verify(mapper).insert(captor.capture());
    assertEquals(8L, captor.getValue().getRecipientId());
    assertEquals(10L, captor.getValue().getTaskId());
    assertEquals("TASK_ASSIGNED", captor.getValue().getType());
  }

  @Test
  void anotherRecipientsNotificationIsInvisible() {
    when(mapper.findVisible(12, 7)).thenReturn(null);
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.markRead(12, 7));
    assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.errorCode());
    verify(mapper, never()).markRead(12, 7);
  }
}
