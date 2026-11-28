package com.example.neighborhood.data.remote;

import static com.example.neighborhood.data.remote.ApiValues.*;
import static com.example.neighborhood.domain.LegacyLabels.*;

import com.example.neighborhood.data.WorkOrderRepository;
import com.example.neighborhood.domain.WorkOrderPolicy;
import com.example.neighborhood.model.*;
import com.google.gson.*;
import java.io.File;
import java.util.*;

public final class RemoteWorkOrderRepository implements WorkOrderRepository {
  private final ApiClient api;
  private final File cache;

  public RemoteWorkOrderRepository(ApiClient api, File cache) {
    this.api = api;
    this.cache = cache;
  }

  @Override
  public List<DictionaryOption> categories(Session session) {
    return options(api.get("/api/work-orders/categories", session).getAsJsonArray(), "label");
  }

  @Override
  public List<WorkOrder> list(Session session, boolean workspace) {
    List<WorkOrder> result = new ArrayList<>();
    List<DictionaryOption> categories = categories(session);
    for (JsonObject value : api.pages("/api/work-orders", session)) {
      WorkOrder order = order(value, categories);
      if (workspace || session.userId.equals(order.ownerId)) result.add(order);
    }
    return result;
  }

  @Override
  public WorkOrder get(Session session, String id) {
    JsonObject detail = api.get("/api/work-orders/" + id, session).getAsJsonObject();
    WorkOrder order = order(detail.getAsJsonObject("workOrder"), categories(session));
    Map<String, WorkOrder.LogEntry> logs = new HashMap<>();
    for (JsonElement item : detail.getAsJsonArray("timeline")) {
      JsonObject value = item.getAsJsonObject();
      WorkOrder.LogEntry log =
          new WorkOrder.LogEntry(
              time(value, "createdAt"),
              actionLabel(text(value, "action")),
              text(value, "actorNickname"),
              text(value, "note"));
      order.timeline.add(log);
      logs.put(text(value, "id"), log);
    }
    Map<String, String> completed = new HashMap<>();
    for (JsonElement item :
        api.get("/api/work-orders/" + id + "/completion-attachments", session).getAsJsonArray()) {
      JsonObject value = item.getAsJsonObject();
      completed.put(text(value, "assetId"), text(value, "logId"));
    }
    File directory = new File(cache, session.userId);
    if (!directory.isDirectory() && !directory.mkdirs())
      throw new IllegalStateException("无法保存附件预览");
    for (JsonElement item : detail.getAsJsonArray("attachments")) {
      JsonObject value = item.getAsJsonObject();
      String assetId = text(value, "id");
      File image = new File(directory, UUID.fromString(assetId).toString() + ".image");
      if (!image.isFile()) api.download(text(value, "url"), session, image);
      WorkOrder.LogEntry log = logs.get(completed.get(assetId));
      if (log == null) order.imagePaths.add(image.getAbsolutePath());
      else log.imagePaths.add(image.getAbsolutePath());
    }
    return order;
  }

  @Override
  public String create(Session session, WorkOrder input) {
    if (input.clientRequestId == null) input.clientRequestId = UUID.randomUUID().toString();
    JsonObject response =
        api.upload(
                "/api/work-orders",
                body(
                    "clientRequestId",
                    input.clientRequestId,
                    "title",
                    input.title,
                    "category",
                    categoryCode(input.category),
                    "location",
                    input.location,
                    "description",
                    input.description),
                input.imagePaths,
                session)
            .getAsJsonObject();
    return text(response.getAsJsonObject("workOrder"), "id");
  }

  @Override
  public void transition(
      Session session,
      String id,
      int version,
      WorkOrderPolicy.Action action,
      String workerId,
      String note,
      List<String> images) {
    JsonObject data = body("version", version);
    if (action == WorkOrderPolicy.Action.ASSIGN)
      data.addProperty("assigneeId", Long.parseLong(workerId));
    if (action == WorkOrderPolicy.Action.REJECT || action == WorkOrderPolicy.Action.COMPLETE)
      data.addProperty("note", note);
    String path = "/api/work-orders/" + id + "/" + action.name().toLowerCase(Locale.ROOT);
    if (action == WorkOrderPolicy.Action.COMPLETE) api.upload(path, data, images, session);
    else api.send("POST", path, data, session);
  }

  private static WorkOrder order(JsonObject value, List<DictionaryOption> categories) {
    WorkOrder result = new WorkOrder();
    result.id = text(value, "id");
    result.ownerId = text(value, "reporterId");
    result.workerId = text(value, "assigneeId");
    result.title = text(value, "title");
    result.category = DictionaryOption.label(categories, text(value, "category"), category(text(value, "category")));
    result.location = text(value, "location");
    result.description = text(value, "description");
    String status = text(value, "status");
    if ("NEW".equals(status)) status = "PENDING";
    if ("IN_PROGRESS".equals(status)) status = "PROCESSING";
    result.status = WorkOrder.Status.valueOf(status);
    result.version = value.get("version").getAsInt();
    result.createdAt = time(value, "createdAt");
    return result;
  }

  private static String actionLabel(String action) {
    if ("CREATE".equals(action)) return "已上报";
    for (WorkOrderPolicy.Action value : WorkOrderPolicy.Action.values())
      if (value.name().equals(action)) return value.label;
    return action;
  }
}
