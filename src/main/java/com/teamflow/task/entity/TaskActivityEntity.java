package com.teamflow.task.entity;

import java.time.LocalDateTime;

public class TaskActivityEntity {
  private Long id;
  private Long taskId;
  private Long actorId;
  private String action;
  private String fromValue;
  private String toValue;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long actorId) {
    this.actorId = actorId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getFromValue() {
    return fromValue;
  }

  public void setFromValue(String fromValue) {
    this.fromValue = fromValue;
  }

  public String getToValue() {
    return toValue;
  }

  public void setToValue(String toValue) {
    this.toValue = toValue;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
