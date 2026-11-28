package com.example.neighborhood.data;

import com.example.neighborhood.model.*;
import java.util.List;

public interface AccountRepository {
  void logout(Session session);

  Session refresh(Session session);

  Session login(String account, String password);

  Session register(String account, String password, ResidentProfile profile);

  ResidentProfile profile(Session session, String userId);

  void saveProfile(Session session, ResidentProfile profile);

  List<ResidentProfile> searchResidents(Session session, String query);

  List<Session> workers(Session session);
}
