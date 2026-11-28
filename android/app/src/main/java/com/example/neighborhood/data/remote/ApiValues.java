package com.example.neighborhood.data.remote;

import com.example.neighborhood.model.DictionaryOption;
import java.util.ArrayList;
import java.util.List;
import com.example.neighborhood.model.Session;
import com.google.gson.*;
import java.time.Instant;

final class ApiValues {
  private static final Gson GSON = new Gson();
  private ApiValues() {}

  static JsonObject body(Object... values) {
    JsonObject object = new JsonObject();
    for (int i = 0; i < values.length; i += 2)
      object.add((String) values[i], GSON.toJsonTree(values[i + 1]));
    return object;
  }

  static String text(JsonObject value, String key) {
    JsonElement element = value.get(key);
    return element == null || element.isJsonNull() ? "" : element.getAsString();
  }

  static long time(JsonObject value, String key) {
    String text = text(value, key);
    return text.isEmpty() ? 0 : Instant.parse(text).toEpochMilli();
  }

  static String instant(long value) {
    return value == 0 ? null : Instant.ofEpochMilli(value).toString();
  }

  static Session.Role role(String value) {
    return "STAFF".equals(value) ? Session.Role.WORKER : Session.Role.valueOf(value);
  }

  static List<DictionaryOption> options(JsonArray values, String labelKey) {
    List<DictionaryOption> result = new ArrayList<>();
    for (JsonElement item : values) {
      JsonObject value = item.getAsJsonObject();
      result.add(new DictionaryOption(text(value, "code"), text(value, labelKey)));
    }
    return result;
  }
}
