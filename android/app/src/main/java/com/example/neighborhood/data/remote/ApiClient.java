package com.example.neighborhood.data.remote;

import com.example.neighborhood.model.Session;
import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** HTTP transport; repositories own payloads and model mapping. */
public final class ApiClient {
  private final String baseUrl;
  private final Supplier<Session> current;
  private final Consumer<Session> expire;

  public ApiClient(String baseUrl, Supplier<Session> current, Consumer<Session> expire) {
    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))
      throw new IllegalArgumentException("服务地址必须使用HTTP或HTTPS");
    this.baseUrl = baseUrl.replaceAll("/+$", "");
    this.current = current;
    this.expire = expire;
  }

  public Session session() {
    Session value = current.get();
    requireSession(value);
    return value;
  }

  private void requireSession(Session value) {
    if (value == null
        || value.accessToken == null
        || value.expiresAt <= System.currentTimeMillis()) {
      if (value != null) expire.accept(value);
      throw new ApiException(401, "INVALID_TOKEN", "登录已失效，请重新登录");
    }
  }

  public JsonElement get(String path, Session session) {
    return send("GET", path, null, session);
  }

  public JsonElement send(String method, String path, JsonObject body, Session session) {
    return request(method, path, body, Collections.emptyList(), session, null);
  }

  public JsonElement upload(String path, JsonObject body, List<String> images, Session session) {
    if (images.size() > 3) throw new IllegalArgumentException("最多选择3张图片");
    for (String pathName : images) {
      File file = new File(pathName);
      if (!file.isFile()) throw new IllegalArgumentException("图片已丢失，请重新选择");
      if (file.length() > 5L * 1024 * 1024) throw new IllegalArgumentException("单张图片不能超过5MiB");
    }
    return request("POST", path, body, images, session, null);
  }

  public void download(String path, Session session, File target) {
    request("GET", path, null, Collections.emptyList(), session, target);
  }

  public List<JsonObject> pages(String path, Session session) {
    List<JsonObject> result = new ArrayList<>();
    for (int page = 1; ; page++) {
      JsonObject response =
          get(path + (path.contains("?") ? "&" : "?") + "page=" + page + "&size=50", session)
              .getAsJsonObject();
      JsonArray items = response.getAsJsonArray("items");
      for (JsonElement item : items) result.add(item.getAsJsonObject());
      if (items.isEmpty() || result.size() >= response.get("total").getAsLong()) return result;
    }
  }

  private JsonElement request(
      String method,
      String path,
      JsonObject body,
      List<String> images,
      Session session,
      File target) {
    if (!path.startsWith("/api/") || path.contains(".."))
      throw new IllegalArgumentException("无效接口路径");
    if (session != null) requireSession(session);
    HttpURLConnection connection = null;
    try {
      if (Thread.currentThread().isInterrupted()) throw new IOException("请求已取消");
      connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(15000);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod(method);
      connection.setRequestProperty("Accept", target == null ? "application/json" : "image/*");
      if (session != null)
        connection.setRequestProperty("Authorization", "Bearer " + session.accessToken);
      if (body != null) {
        connection.setDoOutput(true);
        if (images.isEmpty()) {
          byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
          connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
          connection.setFixedLengthStreamingMode(bytes.length);
          try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
          }
        } else {
          String boundary = "linli-" + UUID.randomUUID();
          connection.setRequestProperty(
              "Content-Type", "multipart/form-data; boundary=" + boundary);
          connection.setChunkedStreamingMode(16384);
          try (OutputStream output = new BufferedOutputStream(connection.getOutputStream())) {
            write(
                output,
                "--"
                    + boundary
                    + "\r\n"
                    + "Content-Disposition: form-data; name=\"data\"\r\n"
                    + "Content-Type: application/json\r\n\r\n"
                    + body
                    + "\r\n");
            for (String image : images) {
              boolean png = image.toLowerCase(Locale.ROOT).endsWith(".png");
              write(
                  output,
                  "--"
                      + boundary
                      + "\r\nContent-Disposition: form-data; name=\"images\"; filename=\"photo."
                      + (png ? "png" : "jpg")
                      + "\"\r\nContent-Type: image/"
                      + (png ? "png" : "jpeg")
                      + "\r\n\r\n");
              try (InputStream input = new FileInputStream(image)) {
                copy(input, output, 5L * 1024 * 1024);
              }
              write(output, "\r\n");
            }
            write(output, "--" + boundary + "--\r\n");
          }
        }
      }
      int status = connection.getResponseCode();
      if (status < 200 || status >= 300) {
        if (status == 401 && session != null) expire.accept(session);
        String code = "HTTP_" + status;
        String message = status == 401 ? "登录已失效，请重新登录" : "请求失败（" + status + "），请稍后重试";
        try (InputStream error = connection.getErrorStream()) {
          if (error != null) {
            JsonObject value = JsonParser.parseString(read(error)).getAsJsonObject();
            String errorCode = ApiValues.text(value, "code");
            String errorMessage = ApiValues.text(value, "message");
            if (!errorCode.trim().isEmpty()) code = errorCode;
            if (!errorMessage.trim().isEmpty()) message = errorMessage;
          }
        } catch (JsonParseException | IllegalStateException ignored) {
          /* Keep the HTTP status message. */
        }
        throw new ApiException(status, code, message);
      }
      if (status == 204) return JsonNull.INSTANCE;
      try (InputStream input = connection.getInputStream()) {
        if (target == null) {
          String text = read(input);
          return text.isEmpty() ? JsonNull.INSTANCE : JsonParser.parseString(text);
        }
        File temporary = File.createTempFile("attachment-", ".part", target.getParentFile());
        try {
          try (OutputStream output = new FileOutputStream(temporary)) {
            copy(input, output, 5L * 1024 * 1024);
          }
          synchronized (session) {
            if (current.get() != session)
              throw new ApiException(401, "SESSION_CHANGED", "登录状态已变更，请重新打开页面");
            if (!temporary.renameTo(target)) throw new IOException("图片保存失败");
          }
        } finally {
          if (temporary.exists()) temporary.delete();
        }
        return JsonNull.INSTANCE;
      }
    } catch (IOException ex) {
      throw new IllegalStateException("无法连接服务，请检查网络和后端是否已启动", ex);
    } catch (JsonParseException ex) {
      throw new IllegalStateException("服务返回的数据格式不正确", ex);
    } finally {
      if (connection != null) connection.disconnect();
    }
  }

  private static String read(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    copy(input, output, 4L * 1024 * 1024);
    return output.toString(StandardCharsets.UTF_8.name());
  }

  private static void write(OutputStream output, String value) throws IOException {
    output.write(value.getBytes(StandardCharsets.UTF_8));
  }

  private static void copy(InputStream input, OutputStream output, long limit) throws IOException {
    byte[] buffer = new byte[8192];
    long total = 0;
    for (int size; (size = input.read(buffer)) != -1; ) {
      if (Thread.currentThread().isInterrupted()) throw new IOException("请求已取消");
      total += size;
      if (total > limit) throw new IOException("数据超过大小限制");
      output.write(buffer, 0, size);
    }
  }

  public static final class ApiException extends IllegalStateException {
    public final int status;
    public final String code;

    ApiException(int status, String code, String message) {
      super(message);
      this.status = status;
      this.code = code;
    }
  }
}
