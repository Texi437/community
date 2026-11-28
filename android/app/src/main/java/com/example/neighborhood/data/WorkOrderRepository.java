package com.example.neighborhood.data;

import com.example.neighborhood.domain.WorkOrderPolicy;
import com.example.neighborhood.model.*;
import java.util.List;

public interface WorkOrderRepository {
  List<DictionaryOption> categories(Session session);

  List<WorkOrder> list(Session session, boolean workspace);

  WorkOrder get(Session session, String id);

  String create(Session session, WorkOrder input);

  void transition(
      Session session,
      String id,
      int version,
      WorkOrderPolicy.Action action,
      String workerId,
      String note,
      List<String> imagePaths);
}
