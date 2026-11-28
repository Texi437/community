package com.example.neighborhood.ui.workorder;

import android.os.Bundle;
import android.widget.Spinner;
import com.example.neighborhood.model.WorkOrder;
import com.example.neighborhood.ui.common.*;
import java.util.*;

public final class WorkOrderListFragment extends ScreenFragment<List<WorkOrder>> {
  public static WorkOrderListFragment create(boolean workspace) {
    WorkOrderListFragment fragment = new WorkOrderListFragment();
    Bundle args = new Bundle();
    args.putBoolean("workspace", workspace);
    fragment.setArguments(args);
    return fragment;
  }

  private boolean workspace() {
    return getArguments() != null && getArguments().getBoolean("workspace");
  }

  @Override
  protected String title() {
    return workspace() ? "工单工作台" : "我的工单";
  }

  @Override
  protected List<WorkOrder> loadData() {
    return app.workOrders.list(session, workspace());
  }

  @Override
  protected void render(List<WorkOrder> orders) {
    heading(title(), null);
    List<String> labels = new ArrayList<>();
    labels.add("全部状态");
    for (WorkOrder.Status status : WorkOrder.Status.values()) labels.add(status.label);
    Spinner filter = Views.spinner(binding.body, "筛选", labels);
    android.widget.LinearLayout list = new android.widget.LinearLayout(requireContext());
    list.setOrientation(android.widget.LinearLayout.VERTICAL);
    binding.body.addView(list);
    filter.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(android.widget.AdapterView<?> parent) {}

          public void onItemSelected(
              android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
            list.removeAllViews();
            int count = 0;
            for (WorkOrder order : orders)
              if (position == 0 || order.status.ordinal() == position - 1) {
                Views.card(
                    list,
                    order.status.label,
                    order.title,
                    order.category + " · " + Times.format(order.createdAt),
                    () -> open(WorkOrderDetailFragment.create(order.id)));
                count++;
              }
            if (count == 0) Views.card(list, "", "暂无相关工单", null, null);
          }
        });
    if (!workspace() && session.role == com.example.neighborhood.model.Session.Role.RESIDENT)
      button("＋ 新建报事", () -> open(new WorkOrderFormFragment()));
  }
}
