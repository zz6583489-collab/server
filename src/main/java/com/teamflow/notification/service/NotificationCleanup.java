package com.teamflow.notification.service;

import com.teamflow.notification.repository.NotificationMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes old notices regardless of read state after the documented retention period. */
@Component
public class NotificationCleanup {
  private final NotificationMapper mapper;

  public NotificationCleanup(NotificationMapper mapper) {
    this.mapper = mapper;
  }

  @Scheduled(initialDelay = 3_600_000, fixedDelay = 86_400_000)
  public void purgeExpired() {
    mapper.deleteExpired();
  }
}
