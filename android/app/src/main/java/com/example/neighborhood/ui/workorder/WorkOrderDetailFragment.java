package com.example.neighborhood.ui.workorder;

import android.os.Bundle;
import android.text.InputType;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.neighborhood.R;
import com.example.neighborhood.data.DraftStore;
import com.example.neighborhood.domain.WorkOrderPolicy;
import com.example.neighborhood.model.*;
import com.example.neighborhood.ui.common.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.*;

public final class WorkOrderDetailFragment extends ScreenFragment<WorkOrderDetailFragment.Data> {
  record Data(WorkOrder order, List<Session> workers, String importedImages) {}

  private final ActivityResultLauncher<String> imagePicker =
      registerForActivityResult(
          new ActivityResultContracts.GetMultipleContents(),
          uris -> {
            if (uris.isEmpty()) return;
            List<String> paths = completionImages();
            if (paths.size() + uris.size() > 3) {
              message("最多选择 3 张图片");
              return;
            }
            android.content.Context context = requireContext().getApplicationContext();
            model.execute(
                () -> {
                  for (android.net.Uri uri : uris) paths.add(ImageFiles.importImage(context, uri));
                  Data data = loadData();
                  return new Data(data.order(), data.workers(), String.join("\n", paths));
                },
                "办结图片已添加");
          });

  public static WorkOrderDetailFragment create(String id) {
    WorkOrderDetailFragment fragment = new WorkOrderDetailFragment();
    Bundle args = new Bundle();
    args.putString("id", id);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  protected String title() {
    return "工单详情";
  }

  @Override
  protected Data loadData() {
    WorkOrder order = app.workOrders.get(session, requireArguments().getString("id"));
    List<Session> workers =
        session.role == Session.Role.ADMIN
            ? app.accounts.workers(session)
            : Collections.emptyList();
    return new Data(order, workers, null);
  }

  @Override
  protected void render(Data data) {
    WorkOrder order = data.order();
    if (data.importedImages() != null) model.form.set("completion_images", data.importedImages());
    heading(order.title, null);
    Views.card(
        binding.body,
        order.status.label,
        order.category,
        order.location + "\n" + order.description,
        null);
    Views.images(binding.body, order.imagePaths);
    Views.text(binding.body, "处理进度", 18, R.color.ink);
    for (int i = order.timeline.size() - 1; i >= 0; i--) {
      WorkOrder.LogEntry entry = order.timeline.get(i);
      Views.card(
          binding.body,
          Times.format(entry.time),
          entry.label,
          entry.actorNickname + (entry.note.isEmpty() ? "" : "\n" + entry.note),
          null);
      Views.images(binding.body, entry.imagePaths);
    }
    Set<WorkOrderPolicy.Action> actions = WorkOrderPolicy.allowedActions(session, order);
    if (!actions.isEmpty())
      Views.field(
              binding.body,
              "处理说明",
              "note",
              "",
              model.form,
              InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE)
          .setMinLines(2);
    if (actions.contains(WorkOrderPolicy.Action.COMPLETE)) {
      Views.images(binding.body, completionImages());
      button("添加办结图片", () -> imagePicker.launch("image/*"));
      if (!completionImages().isEmpty())
        button(
            "移除办结图片",
            () -> {
              model.form.set("completion_images", "");
              refresh();
            });
    }
    for (WorkOrderPolicy.Action action : actions) button(action.label, () -> act(data, action));
    if (order.status == WorkOrder.Status.REJECTED && session.userId.equals(order.ownerId))
      button(
          "重新上报",
          () -> {
            DraftStore.Draft draft = new DraftStore.Draft();
            draft.put("title", order.title);
            draft.put("category", order.category);
            draft.put("location", order.location);
            draft.put("description", order.description);
            android.content.Context context = requireContext().getApplicationContext();
            model.execute(
                () -> {
                  List<String> copies = new ArrayList<>();
                  for (String path : order.imagePaths)
                    copies.add(
                        ImageFiles.importImage(
                            context, android.net.Uri.fromFile(new java.io.File(path))));
                  draft.put("images", String.join("\n", copies));
                  app.drafts.save(session, "work_order", draft);
                  return null;
                },
                "已复制到草稿");
          });
  }

  private void act(Data data, WorkOrderPolicy.Action action) {
    if (action == WorkOrderPolicy.Action.ASSIGN) {
      String[] names = data.workers().stream().map(w -> w.nickname).toArray(String[]::new);
      new MaterialAlertDialogBuilder(requireContext())
          .setTitle("选择网格员")
          .setItems(
              names,
              (dialog, index) -> transition(data.order(), action, data.workers().get(index).userId))
          .show();
      return;
    }
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle("确认" + action.label + "？")
        .setNegativeButton("返回", null)
        .setPositiveButton("确认", (dialog, which) -> transition(data.order(), action, null))
        .show();
  }

  private List<String> completionImages() {
    String paths = model.form.get("completion_images");
    return paths == null || paths.isEmpty()
        ? new ArrayList<>()
        : new ArrayList<>(Arrays.asList(paths.split("\n")));
  }

  private void transition(WorkOrder order, WorkOrderPolicy.Action action, String worker) {
    String note = model.form.get("note");
    List<String> images =
        action == WorkOrderPolicy.Action.COMPLETE ? completionImages() : Collections.emptyList();
    submit(
        () ->
            app.workOrders.transition(
                session, order.id, order.version, action, worker, note == null ? "" : note, images),
        "工单状态已更新");
  }

  @Override
  protected void onSuccess(String message) {
    if (message.equals("已复制到草稿")) open(new WorkOrderFormFragment());
    if (message.equals("工单状态已更新")) {
      model.form.remove("note");
      model.form.remove("completion_images");
      refresh();
    }
  }
}
