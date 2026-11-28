package com.example.neighborhood.ui.workspace;

import android.text.InputType;
import com.example.neighborhood.domain.PrivacyPolicy;
import com.example.neighborhood.model.ResidentProfile;
import com.example.neighborhood.ui.common.*;
import com.example.neighborhood.ui.profile.ProfileEditFragment;
import java.util.List;

public final class ResidentSearchFragment extends ScreenFragment<List<ResidentProfile>> {
  @Override
  protected String title() {
    return "授权住户查询";
  }

  @Override
  protected List<ResidentProfile> loadData() {
    PrivacyPolicy.require(session.canQueryResidents);
    String query = model.form.get("query");
    return app.accounts.searchResidents(session, query == null ? "" : query);
  }

  @Override
  protected void render(List<ResidentProfile> residents) {
    heading("住户查询", null);
    Views.field(binding.body, "昵称", "query", "", model.form, InputType.TYPE_CLASS_TEXT);
    button("查询", this::refresh);
    if (residents.isEmpty()) Views.card(binding.body, "", "没有匹配的住户", null, null);
    for (ResidentProfile profile : residents)
      Views.card(
          binding.body,
          "",
          profile.nickname,
          "查看授权资料",
          () -> open(ProfileEditFragment.create(profile.userId)));
  }
}
