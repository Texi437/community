package com.example.neighborhood.di;

import android.content.Context;
import com.example.neighborhood.data.*;
import com.example.neighborhood.model.Session;

public final class AppContainer {
  public AccountRepository accounts;
  public WorkOrderRepository workOrders;
  public ActivityRepository activities;
  public InterestRepository interests;
  public final DraftStore drafts;
  public final SessionStore sessions;
  public volatile Session session;
  private final java.io.File imageCache;

  public AppContainer(Context context) {
    imageCache = new java.io.File(context.getCacheDir(), "remote-images");
    drafts = new DraftStore(context);
    sessions = new SessionStore(context);
    session = sessions.load();
    DataSourceFactory.configure(context, this);
  }

  public synchronized void signIn(Session value) {
    sessions.save(value);
    session = value;
  }

  public synchronized void signOut() {
    Session previous = session;
    sessions.clear();
    session = null;
    if (previous != null)
      synchronized (previous) {
        java.io.File directory = new java.io.File(imageCache, previous.userId);
        java.io.File[] images = directory.listFiles();
        if (images != null) for (java.io.File image : images) image.delete();
        directory.delete();
      }
  }

  public synchronized void expire(Session expired) {
    if (session == expired) signOut();
  }
}
