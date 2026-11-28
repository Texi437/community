package com.example.neighborhood.ui.home;

import android.widget.LinearLayout;
import com.example.neighborhood.R;
import com.example.neighborhood.model.*;
import com.example.neighborhood.ui.activity.*;
import com.example.neighborhood.ui.common.*;
import com.example.neighborhood.ui.interest.*;
import com.example.neighborhood.ui.workorder.*;
import com.example.neighborhood.ui.workspace.WorkspaceFragment;
import java.util.List;

public final class HomeFragment extends ScreenFragment<List<WorkOrder>> {
  @Override
  protected String title() {
    return "邻里共治";
  }

  @Override
  protected List<WorkOrder> loadData() {
    app.accounts.refresh(session);
    return app.workOrders.list(session, false);
  }

  @Override
  protected void render(List<WorkOrder> orders) {
    heading("你好，" + session.nickname, null);
    com.google.android.material.card.MaterialCardView welcome =
        Views.card(binding.body, "", "共建更好的家", null, null);
    welcome.setCardBackgroundColor(requireContext().getColor(R.color.brand_soft));
    LinearLayout content = welcome.findViewById(R.id.card_content);
    Views.illustration(content, 192, 82);
    if (session.role == Session.Role.RESIDENT)
      Views.card(
          binding.body, "社区服务", "我要报事", "记录问题 · 跟进处理", () -> open(new WorkOrderFormFragment()));
    Views.card(
        binding.body, "邻里生活", "社区活动", "查看活动与共同兴趣", () -> open(ActivityListFragment.create("all")));
    Views.text(binding.body, "我的最新工单", 18, R.color.ink);
    if (orders.isEmpty())
      Views.card(
          binding.body,
          "",
          "还没有提交的诉求",
          null,
          session.role == Session.Role.RESIDENT ? () -> open(new WorkOrderFormFragment()) : null);
    for (WorkOrder order : orders.subList(0, Math.min(2, orders.size())))
      Views.card(
          binding.body,
          order.status.label,
          order.title,
          order.location,
          () -> open(WorkOrderDetailFragment.create(order.id)));
    if (session.role != Session.Role.RESIDENT) button("进入工作台", () -> open(new WorkspaceFragment()));
  }
}
