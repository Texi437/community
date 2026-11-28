package com.example.neighborhood.data.remote;

import static com.example.neighborhood.data.remote.ApiValues.*;
import static com.example.neighborhood.domain.LegacyLabels.*;

import com.example.neighborhood.data.ActivityRepository;
import com.example.neighborhood.model.*;
import com.google.gson.*;
import java.util.*;

public final class RemoteActivityRepository implements ActivityRepository {
  private final ApiClient api;

  public RemoteActivityRepository(ApiClient api) {
    this.api = api;
  }

  @Override
  public List<CommunityActivity> list(Session session, String scope) {
    String path;
    switch (scope) {
      case "created":
      case "enrolled":
        path = "/api/users/me/activities?scope=" + scope;
        break;
      case "review":
        path = "/api/activities?scope=review";
        break;
      default:
        path = "/api/activities";
    }
    List<CommunityActivity> result = new ArrayList<>();
    List<DictionaryOption> topics = options(api.get("/api/interests/topics", session).getAsJsonArray(), "name");
    for (JsonObject value : api.pages(path, session)) result.add(activity(value, session, topics));
    return result;
  }

  @Override
  public CommunityActivity get(Session session, String id) {
    JsonObject value = api.get("/api/activities/" + id, session).getAsJsonObject();
    List<DictionaryOption> topics = options(api.get("/api/interests/topics", session).getAsJsonArray(), "name");
    return activity(value, session, topics);
  }

  @Override
  public List<Venue> venues() {
    List<Venue> result = new ArrayList<>();
    for (JsonElement item : api.get("/api/venues", api.session()).getAsJsonArray()) {
      JsonObject value = item.getAsJsonObject();
      result.add(
          new Venue(
              text(value, "id"),
              text(value, "name"),
              value.get("capacity").getAsInt(),
              value.get("allowsSpeaker").getAsBoolean(),
              "开放时间："
                  + text(value, "opensAt")
                  + "—"
                  + text(value, "closesAt")
                  + "\n"
                  + text(value, "ruleText")));
    }
    return result;
  }

  @Override
  public String publish(Session session, CommunityActivity input) {
    if (input.clientRequestId == null) input.clientRequestId = UUID.randomUUID().toString();
    JsonObject response =
        api.send(
                "POST",
                "/api/activities",
                body(
                    "clientRequestId",
                    input.clientRequestId,
                    "topicCode",
                    topicCode(input.topic),
                    "title",
                    input.title,
                    "description",
                    input.description,
                    "venueId",
                    Long.parseLong(input.venueId),
                    "startsAt",
                    instant(input.time.start),
                    "endsAt",
                    instant(input.time.end),
                    "capacity",
                    input.capacity,
                    "usesSpeaker",
                    input.usesSpeaker),
                session)
            .getAsJsonObject();
    return text(response, "id");
  }

  @Override
  public void review(Session session, String id, int version, boolean approved) {
    api.send(
        "POST",
        "/api/activities/" + id + "/review",
        body("version", version, "approved", approved),
        session);
  }

  @Override
  public void updateStatus(
      Session session, String id, int version, CommunityActivity.Status status) {
    String action;
    if (status == CommunityActivity.Status.CANCELLED) action = "cancel";
    else if (status == CommunityActivity.Status.ENDED) action = "end";
    else throw new IllegalArgumentException("无效活动操作");
    api.send("POST", "/api/activities/" + id + "/" + action, body("version", version), session);
  }

  @Override
  public List<PublicProfile> participants(Session session, String id) {
    List<PublicProfile> result = new ArrayList<>();
    for (JsonObject value : api.pages("/api/activities/" + id + "/participants", session))
      result.add(new PublicProfile(text(value, "nickname")));
    return result;
  }

  @Override
  public void enroll(Session session, String id) {
    api.send("POST", "/api/activities/" + id + "/enroll", null, session);
  }

  @Override
  public void withdraw(Session session, String id) {
    api.send("POST", "/api/activities/" + id + "/withdraw", null, session);
  }

  private static CommunityActivity activity(JsonObject value, Session session, List<DictionaryOption> topics) {
    CommunityActivity result = new CommunityActivity();
    result.id = text(value, "id");
    result.title = text(value, "title");
    result.topicCode = text(value, "topicCode");
    result.topic = DictionaryOption.label(topics, text(value, "topicCode"), topic(text(value, "topicCode")));
    result.description = text(value, "description");
    result.venueId = text(value, "venueId");
    result.venueName = text(value, "venueName");
    result.time = new TimeWindow(time(value, "startsAt"), time(value, "endsAt"));
    result.capacity = value.get("capacity").getAsInt();
    result.usesSpeaker = value.get("usesSpeaker").getAsBoolean();
    result.status = CommunityActivity.Status.valueOf(text(value, "status"));
    result.version = value.get("version").getAsInt();
    result.organizerId = value.get("organizer").getAsBoolean() ? session.userId : null;
    result.organizerNickname = text(value, "organizerNickname");
    result.enrollmentCount = value.get("enrollmentCount").getAsInt();
    String enrolled = text(value, "enrollmentStatus");
    result.enrollmentStatus = enrolled.isEmpty() ? null : Enrollment.Status.valueOf(enrolled);
    String method = text(value, "reviewMethod");
    result.reviewMethod =
        "AUTO".equals(method) ? "自动检查通过" : "MANUAL".equals(method) ? "人工协调" : null;
    result.reviewedAt = time(value, "reviewedAt");
    return result;
  }
}
