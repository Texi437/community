package com.example.neighborhood.data.remote;

import static com.example.neighborhood.data.remote.ApiValues.*;

import com.example.neighborhood.data.AccountRepository;
import com.example.neighborhood.model.*;
import com.google.gson.*;
import java.util.*;

public final class RemoteAccountRepository implements AccountRepository {
  private final ApiClient api;

  public RemoteAccountRepository(ApiClient api) {
    this.api = api;
  }

  @Override
  public Session login(String account, String password) {
    JsonObject response =
        api.send("POST", "/api/auth/login", body("username", account, "password", password), null)
            .getAsJsonObject();
    Session session = identity(response.getAsJsonObject("user"));
    session.accessToken = text(response, "accessToken");
    session.expiresAt = time(response, "expiresAt");
    permissions(session);
    return session;
  }

  @Override
  public Session register(String account, String password, ResidentProfile profile) {
    JsonObject response =
        api.send(
                "POST",
                "/api/auth/register",
                body(
                    "username",
                    account,
                    "password",
                    password,
                    "nickname",
                    profile.nickname,
                    "building",
                    profile.building,
                    "unit",
                    profile.unit,
                    "room",
                    profile.room),
                null)
            .getAsJsonObject();
    return identity(response);
  }

  @Override
  public void logout(Session session) {
    api.send("POST", "/api/auth/logout", null, session);
  }

  @Override
  public Session refresh(Session session) {
    JsonObject profile = api.get("/api/users/me", session).getAsJsonObject();
    session.nickname = text(profile, "nickname");
    session.role = role(text(profile, "role"));
    permissions(session);
    return session;
  }

  private void permissions(Session session) {
    session.canQueryResidents =
        api.get("/api/users/me/permissions", session)
            .getAsJsonObject()
            .get("canQueryResidents")
            .getAsBoolean();
  }

  @Override
  public ResidentProfile profile(Session session, String userId) {
    JsonObject value =
        api.get(
                session.userId.equals(userId) ? "/api/users/me" : "/api/residents/" + userId,
                session)
            .getAsJsonObject();
    return profile(value);
  }

  @Override
  public void saveProfile(Session session, ResidentProfile profile) {
    if (!session.userId.equals(profile.userId)) throw new IllegalArgumentException("只能修改本人资料");
    api.send(
        "PUT",
        "/api/users/me",
        body(
            "nickname",
            profile.nickname,
            "building",
            profile.building,
            "unit",
            profile.unit,
            "room",
            profile.room),
        session);
  }

  @Override
  public List<ResidentProfile> searchResidents(Session session, String query) {
    List<ResidentProfile> result = new ArrayList<>();
    String term = query.trim().toLowerCase(Locale.ROOT);
    for (JsonObject item : api.pages("/api/residents", session))
      if (text(item, "nickname").toLowerCase(Locale.ROOT).contains(term)) result.add(profile(item));
    return result;
  }

  @Override
  public List<Session> workers(Session session) {
    List<Session> result = new ArrayList<>();
    for (JsonElement item : api.get("/api/work-orders/staff", session).getAsJsonArray()) {
      JsonObject value = item.getAsJsonObject();
      result.add(
          new Session(text(value, "id"), text(value, "nickname"), Session.Role.WORKER, false));
    }
    return result;
  }

  private static Session identity(JsonObject value) {
    return new Session(
        text(value, "id"), text(value, "nickname"), role(text(value, "role")), false);
  }

  private static ResidentProfile profile(JsonObject value) {
    return new ResidentProfile(
        text(value, "id"),
        text(value, "nickname"),
        text(value, "building"),
        text(value, "unit"),
        text(value, "room"));
  }
}
