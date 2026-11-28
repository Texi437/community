package com.example.neighborhood.ui.common;

import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import com.example.neighborhood.data.DraftStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class DraftFragment<T> extends ScreenFragment<T> {
  protected abstract String draftKind();

  private DraftStore.Draft saved = new DraftStore.Draft();
  private OnBackPressedCallback back;

  protected DraftStore.Draft loadDraft() {
    return app.drafts.load(session, draftKind());
  }

  protected void restore(DraftStore.Draft draft) {
    saved = new DraftStore.Draft();
    for (String key : draft.keySet()) if (!key.startsWith("_")) saved.put(key, draft.get(key));
    for (String key : draft.keySet())
      if (!model.form.contains(key)) model.form.set(key, draft.get(key));
  }

  protected DraftStore.Draft currentDraft() {
    DraftStore.Draft draft = new DraftStore.Draft();
    for (String key : model.form.keys()) {
      Object value = model.form.get(key);
      if (value instanceof String && !key.startsWith("_")) draft.put(key, (String) value);
    }
    return draft;
  }

  protected void saveDraft() {
    saveDraft("草稿已保存");
  }

  private void saveDraft(String message) {
    DraftStore.Draft draft = currentDraft();
    model.execute(
        () -> {
          app.drafts.save(session, draftKind(), draft);
          return null;
        },
        message);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle state) {
    super.onViewCreated(view, state);
    back =
        new OnBackPressedCallback(true) {
          @Override
          public void handleOnBackPressed() {
            if (currentDraft().equals(saved)
                || currentDraft().values().stream().allMatch(String::isEmpty)) {
              leave();
              return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("保存当前内容？")
                .setNegativeButton("继续编辑", null)
                .setNeutralButton("放弃修改", (dialog, which) -> leave())
                .setPositiveButton(
                    "保存并返回",
                    (dialog, which) -> {
                      saveDraft("已保存并返回");
                    })
                .show();
          }
        };
    requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
  }

  protected void leave() {
    back.setEnabled(false);
    host().getOnBackPressedDispatcher().onBackPressed();
  }

  @Override
  protected void onSuccess(String message) {
    if (message.equals("草稿已保存") || message.equals("已保存并返回")) saved = currentDraft();
    if (message.equals("已保存并返回")) leave();
  }
}
