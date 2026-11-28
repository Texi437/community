package com.example.neighborhood.ui.workorder;

import android.text.InputType;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.neighborhood.data.DraftStore;
import com.example.neighborhood.model.WorkOrder;
import com.example.neighborhood.model.DictionaryOption;
import com.example.neighborhood.domain.LegacyLabels;
import com.example.neighborhood.ui.common.*;
import java.util.*;

public final class WorkOrderFormFragment extends DraftFragment<WorkOrderFormFragment.Data> {
  record Data(DraftStore.Draft draft, List<DictionaryOption> categories, String error) {}
  private final ActivityResultLauncher<String> imagePicker =
      registerForActivityResult(
          new ActivityResultContracts.GetMultipleContents(),
          uris -> {
            if (uris.isEmpty()) return;
            String existing = model.form.get("images");
            List<String> paths = new ArrayList<>();
            if (existing != null && !existing.isEmpty())
              paths.addAll(Arrays.asList(existing.split("\n")));
            if (paths.size() + uris.size() > 3) {
              message("最多选择 3 张图片");
              return;
            }
            android.content.Context context = requireContext().getApplicationContext();
            model.execute(
                () -> {
                  for (android.net.Uri uri : uris) paths.add(ImageFiles.importImage(context, uri));
                  Data data = loadData();
                  data.draft().put("_imported_images", String.join("\n", paths));
                  return data;
                },
                "图片已添加");
          });

  @Override
  protected String title() {
    return "新建报事";
  }

  @Override
  protected String draftKind() {
    return "work_order";
  }

  @Override
  protected Data loadData() {
    DraftStore.Draft draft = loadDraft();
    try {
      return new Data(draft, app.workOrders.categories(session), null);
    } catch (IllegalStateException error) {
      return new Data(draft, List.of(), error.getMessage());
    }
  }

  @Override
  protected void render(Data data) {
    DraftStore.Draft draft = data.draft();
    if (draft.containsKey("_imported_images"))
      model.form.set("images", draft.remove("_imported_images"));
    restore(draft);
    if (!model.form.contains("request_id"))
      model.form.set("request_id", UUID.randomUUID().toString());
    heading("说说遇到的问题", null);
    Views.field(binding.body, "标题", "title", "", model.form, InputType.TYPE_CLASS_TEXT);
    Spinner category = Views.options(binding.body, "事项分类", "category", data.categories(), model.form, LegacyLabels::categoryCode);
    Views.field(binding.body, "问题地点", "location", "", model.form, InputType.TYPE_CLASS_TEXT);
    Views.field(
            binding.body,
            "描述问题",
            "description",
            "",
            model.form,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        .setMinLines(3);
    String imagePaths = model.form.get("images");
    if (imagePaths != null && !imagePaths.isEmpty())
      Views.images(binding.body, Arrays.asList(imagePaths.split("\n")));
    button("添加图片", () -> imagePicker.launch("image/*"));
    if (imagePaths != null && !imagePaths.isEmpty())
      button(
          "移除所有图片",
          () -> {
            model.form.set("images", "");
            refresh();
          });
    button("保存草稿", this::saveDraft);
    if (data.categories().isEmpty()) {
      Views.card(binding.body, "", "分类暂不可用", data.error() == null ? "请刷新后再提交" : data.error(), null);
      return;
    }
    button(
        "提交诉求",
        () -> {
          if (!data.categories().contains(category.getSelectedItem())) {
            message("原事项分类已停用，请重新选择");
            return;
          }
          DraftStore.Draft values = currentDraft();
          WorkOrder order = new WorkOrder();
          order.clientRequestId = values.get("request_id");
          order.title = values.get("title");
          order.category = ((DictionaryOption) category.getSelectedItem()).code;
          order.location = values.get("location");
          order.description = values.get("description");
          String paths = values.getOrDefault("images", "");
          if (!paths.isEmpty()) order.imagePaths.addAll(Arrays.asList(paths.split("\n")));
          submit(
              () -> {
                for (String path : order.imagePaths)
                  if (!new java.io.File(path).isFile())
                    throw new IllegalStateException("草稿图片已丢失，请重新选择");
                app.workOrders.create(session, order);
                app.drafts.clear(session, draftKind());
              },
              "诉求已提交");
        });
  }

  @Override
  protected void onSuccess(String message) {
    if (message.equals("诉求已提交")) {
      leave();
      return;
    }
    super.onSuccess(message);
  }
}
