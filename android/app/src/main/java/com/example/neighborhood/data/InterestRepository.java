package com.example.neighborhood.data;

import com.example.neighborhood.model.*;
import java.util.List;
import java.util.Map;

public interface InterestRepository {
  List<DictionaryOption> topics(Session session);

  InterestProfile get(Session session);

  void saveInterests(Session session, Map<String, Integer> strengths);

  void saveAvailability(
      Session session,
      InterestProfile.Intent intent,
      List<TimeWindow> windows,
      long validUntil,
      int version);

  List<Opportunity> opportunities(Session session);
}
