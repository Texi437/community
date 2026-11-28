package com.example.neighborhood.ui.profile;

import com.example.neighborhood.data.DraftStore;
import com.example.neighborhood.ui.activity.ActivityFormFragment;
import com.example.neighborhood.ui.common.*;
import com.example.neighborhood.ui.workorder.WorkOrderFormFragment;

public final class DraftsFragment extends ScreenFragment<DraftStore.Draft[]> {
  @Override
  protected String title() {
    return "本地草稿";
  }

  @Override
  protected DraftStore.Draft[] loadData() {
    return new DraftStore.Draft[] {
      app.drafts.load(session, "work_order"), app.drafts.load(session, "activity")
    };
  }

  @Override
  protected void render(DraftStore.Draft[] drafts) {
    heading("本地草稿", null);
    if (drafts[0].isEmpty() && drafts[1].isEmpty())
      Views.card(binding.body, "", "暂无保存的草稿", null, null);
    if (!drafts[0].isEmpty())
      Views.card(
          binding.body,
          "诉求",
          drafts[0].getOrDefault("title", "未命名诉求"),
          "继续编辑",
          () -> open(new WorkOrderFormFragment()));
    if (!drafts[1].isEmpty())
      Views.card(
          binding.body,
          "活动",
          drafts[1].getOrDefault("title", "未命名活动"),
          "继续编辑",
          () -> open(new ActivityFormFragment()));
  }
}
