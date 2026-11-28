package com.example.neighborhood.model;

public final class CommunityActivity {
  public enum Status {
    PENDING_REVIEW("待协调"),
    REJECTED("未通过"),
    PUBLISHED("报名中"),
    ENDED("已结束"),
    CANCELLED("已取消");
    public final String label;

    Status(String label) {
      this.label = label;
    }
  }

  public String id;
  public String clientRequestId;
  public int version;
  public int enrollmentCount;
  public Enrollment.Status enrollmentStatus;
  public String venueName;
  public String organizerId;
  public String organizerNickname;
  public String title;
  public String topic;
  public String topicCode;
  public String description;
  public String venueId;
  public TimeWindow time;
  public int capacity;
  public boolean usesSpeaker;
  public Status status;
  public String reviewMethod;
  public long reviewedAt;

  public String statusLabel(long now) {
    if (status == Status.PUBLISHED && time.start <= now) return "报名已截止";
    return status.label;
  }
}
