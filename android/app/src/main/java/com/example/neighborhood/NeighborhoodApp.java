package com.example.neighborhood;

import android.app.Application;
import com.example.neighborhood.di.AppContainer;

public final class NeighborhoodApp extends Application {
  public AppContainer container;

  @Override
  public void onCreate() {
    super.onCreate();
    container = new AppContainer(this);
  }
}
