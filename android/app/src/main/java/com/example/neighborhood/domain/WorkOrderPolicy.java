package com.example.neighborhood.domain;

import com.example.neighborhood.model.Session;
import com.example.neighborhood.model.WorkOrder;
import java.util.EnumSet;
import java.util.Set;

public final class WorkOrderPolicy {
  public enum Action {
    ASSIGN("派单"),
    REJECT("驳回"),
    ACCEPT("接单"),
    START("开始处理"),
    COMPLETE("提交办结");
    public final String label;

    Action(String label) {
      this.label = label;
    }
  }

  private WorkOrderPolicy() {}

  public static Set<Action> allowedActions(Session session, WorkOrder order) {
    Set<Action> actions = EnumSet.noneOf(Action.class);
    if (session.role == Session.Role.ADMIN && order.status == WorkOrder.Status.PENDING) {
      actions.add(Action.ASSIGN);
      actions.add(Action.REJECT);
    }
    if (session.role != Session.Role.WORKER || !session.userId.equals(order.workerId))
      return actions;
    switch (order.status) {
      case ASSIGNED:
        actions.add(Action.ACCEPT);
        break;
      case ACCEPTED:
        actions.add(Action.START);
        break;
      case PROCESSING:
        actions.add(Action.COMPLETE);
        break;
      default:
        break;
    }
    return actions;
  }
}
