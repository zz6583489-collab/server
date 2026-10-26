package com.teamflow.task.entity;

public class TaskStatsEntity {
  private long total;
  private long todo;
  private long inProgress;
  private long inReview;
  private long done;
  private long overdue;

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public long getTodo() {
    return todo;
  }

  public void setTodo(long todo) {
    this.todo = todo;
  }

  public long getInProgress() {
    return inProgress;
  }

  public void setInProgress(long inProgress) {
    this.inProgress = inProgress;
  }

  public long getInReview() {
    return inReview;
  }

  public void setInReview(long inReview) {
    this.inReview = inReview;
  }

  public long getDone() {
    return done;
  }

  public void setDone(long done) {
    this.done = done;
  }

  public long getOverdue() {
    return overdue;
  }

  public void setOverdue(long overdue) {
    this.overdue = overdue;
  }
}
