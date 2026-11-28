package com.example.neighborhood.data;

import android.content.Context;
import com.example.neighborhood.model.Session;
import java.io.File;
import java.util.LinkedHashMap;

public final class DraftStore {
  public static final class Draft extends LinkedHashMap<String, String> {}

  private final File directory;

  public DraftStore(Context context) {
    this(context.getFilesDir());
  }

  public DraftStore(File filesDirectory) {
    directory = new File(filesDirectory, "drafts");
  }

  public Draft load(Session session, String kind) {
    Draft draft = file(session, kind).read();
    return draft == null ? new Draft() : draft;
  }

  public void save(Session session, String kind, Draft draft) {
    file(session, kind).write(draft);
  }

  public void clear(Session session, String kind) {
    file(session, kind).delete();
  }

  private JsonFile<Draft> file(Session session, String kind) {
    if (!kind.matches("[a-z_]+") || !session.userId.matches("[a-zA-Z0-9-]+"))
      throw new IllegalArgumentException("无效草稿标识");
    return new JsonFile<>(new File(directory, session.userId + "-" + kind + ".json"), Draft.class);
  }
}
