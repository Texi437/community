package com.example.neighborhood.ui.workspace;

import com.example.neighborhood.domain.PrivacyPolicy;
import com.example.neighborhood.model.Session;
import com.example.neighborhood.ui.activity.ActivityListFragment;
import com.example.neighborhood.ui.common.*;
import com.example.neighborhood.ui.interest.OpportunityFragment;
import com.example.neighborhood.ui.workorder.WorkOrderListFragment;

public final class WorkspaceFragment extends ScreenFragment<WorkspaceFragment.Access> {
  record Access(Session.Role role, boolean canQueryResidents) {}

  @Override
  protected String title() {
    return "工作台";
  }

  @Override
  protected Access loadData() {
    app.accounts.refresh(session);
    PrivacyPolicy.require(session.role != Session.Role.RESIDENT);
    return new Access(session.role, session.canQueryResidents);
  }

  @Override
  protected void render(Access data) {
    heading("社区工作台", null);
    Views.card(binding.body, "", "工单处理", null, () -> open(WorkOrderListFragment.create(true)));
    if (data.role() == Session.Role.ADMIN) {
      Views.card(
          binding.body, "", "活动例外处理", null, () -> open(ActivityListFragment.create("review")));
      Views.card(binding.body, "", "需求与活动机会", null, () -> open(new OpportunityFragment()));
      Views.card(binding.body, "", "预置场地规则", null, () -> open(new VenueFragment()));
    }
    if (data.canQueryResidents())
      Views.card(binding.body, "", "授权住户查询", null, () -> open(new ResidentSearchFragment()));
  }
}
