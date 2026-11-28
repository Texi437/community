package com.example.neighborhood.ui.interest;

import com.example.neighborhood.model.*;
import com.example.neighborhood.ui.activity.ActivityFormFragment;
import com.example.neighborhood.ui.common.*;
import java.util.List;

public final class OpportunityFragment extends ScreenFragment<OpportunityFragment.Data> {
  record Data(List<Opportunity> opportunities, List<Venue> venues) {}

  @Override
  protected String title() {
    return "发现共同兴趣";
  }

  @Override
  protected Data loadData() {
    return new Data(app.interests.opportunities(session), app.activities.venues());
  }

  @Override
  protected void render(Data data) {
    List<Opportunity> opportunities = data.opportunities();
    heading("和邻居一起做喜欢的事", null);
    Views.card(binding.body, "", "更新兴趣与时间", null, () -> open(new InterestFragment()));
    if (opportunities.isEmpty())
      Views.card(
          binding.body, "", "暂时没有合适的建议", "你也可以自主发起活动", () -> open(new ActivityFormFragment()));
    for (Opportunity opportunity : opportunities) {
      String counts =
          opportunity.showExactCount
              ? "潜在需求 "
                  + opportunity.potentialCount
                  + " 人 · 近期有意愿 "
                  + opportunity.willingCount
                  + " 人\n完整时段可参与 "
                  + opportunity.availableCount
                  + " 人"
              : "有邻居对这个主题感兴趣";
      String venue =
          data.venues().stream()
              .filter(v -> v.id.equals(opportunity.venueId))
              .map(v -> v.name)
              .findFirst()
              .orElse("");
      Views.card(
          binding.body,
          opportunity.topic,
          opportunity.title,
          opportunity.reason
              + "\n"
              + Times.range(opportunity.time)
              + "\n"
              + venue
              + "\n"
              + counts
              + "\n更新于 "
              + Times.format(opportunity.updatedAt),
          () -> open(ActivityFormFragment.fromOpportunity(opportunity)));
    }
    button("自主发起活动", () -> open(new ActivityFormFragment()));
  }
}
