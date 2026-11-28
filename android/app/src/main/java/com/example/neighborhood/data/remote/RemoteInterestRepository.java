package com.example.neighborhood.data.remote;

import static com.example.neighborhood.data.remote.ApiValues.*;
import static com.example.neighborhood.domain.LegacyLabels.*;

import com.example.neighborhood.data.InterestRepository;
import com.example.neighborhood.model.*;
import com.google.gson.*;
import java.util.*;

public final class RemoteInterestRepository implements InterestRepository {
  private final ApiClient api;

  public RemoteInterestRepository(ApiClient api) {
    this.api = api;
  }

  @Override
  public List<DictionaryOption> topics(Session session) {
    return options(api.get("/api/interests/topics", session).getAsJsonArray(), "name");
  }

  @Override
  public InterestProfile get(Session session) {
    InterestProfile result = new InterestProfile();
    JsonObject strengths =
        api.get("/api/users/me/interests", session).getAsJsonObject().getAsJsonObject("strengths");
    for (Map.Entry<String, JsonElement> entry : strengths.entrySet())
      result.strengths.put(entry.getKey(), entry.getValue().getAsInt());
    JsonObject recent = api.get("/api/users/me/participation", session).getAsJsonObject();
    result.intent = InterestProfile.Intent.valueOf(text(recent, "intent"));
    result.validUntil = time(recent, "validUntil");
    result.version = recent.get("version").getAsInt();
    for (JsonElement item : recent.getAsJsonArray("slots")) {
      JsonObject slot = item.getAsJsonObject();
      result.availability.add(new TimeWindow(time(slot, "startsAt"), time(slot, "endsAt")));
    }
    return result;
  }

  @Override
  public void saveInterests(Session session, Map<String, Integer> strengths) {
    Map<String, Integer> codes = new LinkedHashMap<>();
    for (Map.Entry<String, Integer> entry : strengths.entrySet())
      codes.put(topicCode(entry.getKey()), entry.getValue());
    api.send("PUT", "/api/users/me/interests", body("strengths", codes), session);
  }

  @Override
  public void saveAvailability(
      Session session,
      InterestProfile.Intent intent,
      List<TimeWindow> windows,
      long until,
      int version) {
    JsonArray slots = new JsonArray();
    for (TimeWindow window : windows)
      slots.add(body("startsAt", instant(window.start), "endsAt", instant(window.end)));
    api.send(
        "PUT",
        "/api/users/me/participation",
        body(
            "intent",
            intent.name(),
            "validUntil",
            instant(until),
            "slots",
            slots,
            "version",
            version),
        session);
  }

  @Override
  public List<Opportunity> opportunities(Session session) {
    List<Opportunity> result = new ArrayList<>();
    List<DictionaryOption> topics = topics(session);
    for (JsonElement item : api.get("/api/activity-opportunities", session).getAsJsonArray()) {
      JsonObject value = item.getAsJsonObject();
      Opportunity opportunity = new Opportunity();
      opportunity.title = text(value, "title");
      opportunity.topicCode = text(value, "topicCode");
      opportunity.topic = DictionaryOption.label(topics, text(value, "topicCode"), topic(text(value, "topicCode")));
      opportunity.reason = text(value, "reason");
      opportunity.venueId = text(value, "venueId");
      opportunity.time = new TimeWindow(time(value, "startsAt"), time(value, "endsAt"));
      opportunity.updatedAt = time(value, "generatedAt");
      opportunity.showExactCount = value.get("showExactCount").getAsBoolean();
      if (opportunity.showExactCount) {
        opportunity.potentialCount = value.get("potentialCount").getAsInt();
        opportunity.willingCount = value.get("willingCount").getAsInt();
        opportunity.availableCount = value.get("availableCount").getAsInt();
      }
      result.add(opportunity);
    }
    return result;
  }
}
