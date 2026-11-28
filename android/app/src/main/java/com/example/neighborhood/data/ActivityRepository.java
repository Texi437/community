package com.example.neighborhood.data;

import com.example.neighborhood.model.*;
import java.util.List;

public interface ActivityRepository {
  List<CommunityActivity> list(Session session, String scope);

  CommunityActivity get(Session session, String id);

  List<Venue> venues();

  String publish(Session session, CommunityActivity input);

  void review(Session session, String id, int version, boolean approved);

  void updateStatus(Session session, String id, int version, CommunityActivity.Status status);

  List<PublicProfile> participants(Session session, String activityId);

  void enroll(Session session, String activityId);

  void withdraw(Session session, String activityId);
}
