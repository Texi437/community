package com.example.neighborhood.di;

import android.content.Context;
import com.example.neighborhood.BuildConfig;
import com.example.neighborhood.data.remote.*;
import java.io.File;

final class DataSourceFactory {
  static void configure(Context context, AppContainer container) {
    if (container.session != null
        && (container.session.accessToken == null
            || container.session.expiresAt <= System.currentTimeMillis())) container.signOut();
    ApiClient api =
        new ApiClient(BuildConfig.API_BASE_URL, () -> container.session, container::expire);
    container.accounts = new RemoteAccountRepository(api);
    container.workOrders =
        new RemoteWorkOrderRepository(api, new File(context.getCacheDir(), "remote-images"));
    container.activities = new RemoteActivityRepository(api);
    container.interests = new RemoteInterestRepository(api);
  }
}
