package com.example.neighborhood.data;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class JsonFile<T> {
  private final File file;
  private final Class<T> type;
  private final Gson gson = new Gson();

  public JsonFile(File file, Class<T> type) {
    this.file = file;
    this.type = type;
  }

  public synchronized T read() {
    if (!file.exists()) return null;
    try {
      return gson.fromJson(
          new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), type);
    } catch (IOException | RuntimeException e) {
      throw new IllegalStateException("本地资料读取失败，请重试", e);
    }
  }

  public synchronized void write(T value) {
    File temp = new File(file.getParentFile(), file.getName() + ".tmp");
    try {
      Files.createDirectories(file.getParentFile().toPath());
      Files.write(temp.toPath(), gson.toJson(value).getBytes(StandardCharsets.UTF_8));
      Files.move(
          temp.toPath(),
          file.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new IllegalStateException("保存失败，请检查可用存储空间", e);
    }
  }

  public synchronized void delete() {
    try {
      Files.deleteIfExists(file.toPath());
    } catch (IOException e) {
      throw new IllegalStateException("草稿清理失败", e);
    }
  }
}
