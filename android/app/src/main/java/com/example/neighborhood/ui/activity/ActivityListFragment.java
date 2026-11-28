package com.example.neighborhood.ui.activity;

import android.os.Bundle;
import com.example.neighborhood.R;
import com.example.neighborhood.model.*;
import com.example.neighborhood.ui.common.*;
import com.example.neighborhood.ui.interest.OpportunityFragment;
import java.util.*;

public final class ActivityListFragment extends ScreenFragment<List<CommunityActivity>> {

  public static ActivityListFragment create(String scope) {
    ActivityListFragment fragment = new ActivityListFragment();
    Bundle args = new Bundle();
    args.putString("scope", scope);
    fragment.setArguments(args);
    return fragment;
  }

  private String scope() {
    return requireArguments().getString("scope", "all");
  }

  @Override
  protected String title() {
    switch (scope()) {
      case "created":
        return "我发起的";
      case "enrolled":
        return "我的报名";
      case "review":
        return "活动例外";
      default:
        return "社区活动";
    }
  }

  @Override
  protected List<CommunityActivity> loadData() {
    return app.activities.list(session, scope());
  }

  @Override
  protected void render(List<CommunityActivity> activities) {
    heading(title(), null);
    if (scope().equals("all")) {
      Views.card(binding.body, "共同兴趣", "发现活动机会", null, () -> open(new OpportunityFragment()));
      button("＋ 发起活动", () -> open(new ActivityFormFragment()));
    }
    if (activities.isEmpty()) Views.card(binding.body, "", "这里暂时没有活动", null, null);
    for (CommunityActivity activity : activities) {
      String status = activity.statusLabel(System.currentTimeMillis());
      if (scope().equals("enrolled") && activity.enrollmentStatus != null) {
        switch (activity.enrollmentStatus) {
          case WITHDRAWN:
            status += " · 已退出";
            break;
          case CONFIRMED:
            status += " · 已报名";
            break;
          case CANCELLED:
            status += " · 报名已取消";
            break;
        }
      }
      String detail =
          Times.range(activity.time) + "\n" + activity.enrollmentCount + " / " + activity.capacity + " 人已报名";
      com.google.android.material.card.MaterialCardView card =
          Views.card(
              binding.body,
              activity.topic + " · " + status,
              activity.title,
              detail,
              () -> open(ActivityDetailFragment.create(activity.id)));
      card.setCardBackgroundColor(requireContext().getColor(R.color.warm));
    }
  }
}
