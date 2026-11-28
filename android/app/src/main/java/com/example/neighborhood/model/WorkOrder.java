package com.example.neighborhood.model;

import java.util.ArrayList;
import java.util.List;

public final class WorkOrder {
  public enum Status {
    PENDING("待审核"),
    ASSIGNED("已派单"),
    ACCEPTED("已接单"),
    PROCESSING("处理中"),
    COMPLETED("已办结"),
    REJECTED("已驳回");
    public final String label;

    Status(String label) {
      this.label = label;
    }
  }

  public String id;
  public String clientRequestId;
  public String ownerId;
  public String workerId;
  public String title;
  public String category;
  public String location;
  public String description;
  public Status status = Status.PENDING;
  public int version;
  public long createdAt;
  public List<String> imagePaths = new ArrayList<>();
  public List<LogEntry> timeline = new ArrayList<>();

  public static final class LogEntry {
    public long time;
    public String label;
    public String actorNickname;
    public String note;
    public List<String> imagePaths = new ArrayList<>();

    public LogEntry() {}

    public LogEntry(long time, String label, String actorNickname, String note) {
      this.time = time;
      this.label = label;
      this.actorNickname = actorNickname;
      this.note = note;
    }
  }
}
