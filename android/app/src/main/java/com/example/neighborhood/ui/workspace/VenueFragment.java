package com.example.neighborhood.ui.workspace;

import com.example.neighborhood.model.Venue;
import com.example.neighborhood.ui.common.*;
import java.util.List;

public final class VenueFragment extends ScreenFragment<List<Venue>> {
  @Override
  protected String title() {
    return "场地规则";
  }

  @Override
  protected List<Venue> loadData() {
    return app.activities.venues();
  }

  @Override
  protected void render(List<Venue> venues) {
    heading("预置场地", null);
    for (Venue venue : venues)
      Views.card(binding.body, "最多 " + venue.capacity + " 人", venue.name, venue.rules, null);
  }
}
