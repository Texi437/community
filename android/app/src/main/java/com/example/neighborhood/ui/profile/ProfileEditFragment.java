package com.example.neighborhood.ui.profile;

import android.os.Bundle;
import android.text.InputType;
import com.example.neighborhood.model.ResidentProfile;
import com.example.neighborhood.ui.common.*;
import com.google.android.material.textfield.TextInputEditText;

public final class ProfileEditFragment extends ScreenFragment<ResidentProfile> {
  public static ProfileEditFragment create(String id) {
    ProfileEditFragment fragment = new ProfileEditFragment();
    Bundle args = new Bundle();
    args.putString("id", id);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  protected String title() {
    return "住户资料";
  }

  @Override
  protected ResidentProfile loadData() {
    return app.accounts.profile(session, requireArguments().getString("id"));
  }

  @Override
  protected void render(ResidentProfile profile) {
    boolean own = session.userId.equals(profile.userId);
    heading(own ? "我的资料" : "授权查询", null);
    if (!own) {
      Views.card(
          binding.body,
          "私密资料",
          profile.nickname,
          profile.building + "号楼 · " + profile.unit + "单元 · " + profile.room,
          null);
      return;
    }
    TextInputEditText nickname =
        Views.field(
            binding.body,
            "昵称",
            "nickname",
            profile.nickname,
            model.form,
            InputType.TYPE_CLASS_TEXT);
    TextInputEditText building =
        Views.field(
            binding.body,
            "楼栋",
            "building",
            profile.building,
            model.form,
            InputType.TYPE_CLASS_TEXT);
    TextInputEditText unit =
        Views.field(
            binding.body, "单元", "unit", profile.unit, model.form, InputType.TYPE_CLASS_TEXT);
    TextInputEditText room =
        Views.field(
            binding.body, "门牌", "room", profile.room, model.form, InputType.TYPE_CLASS_TEXT);
    Views.text(binding.body, "除昵称外，个人资料不公开。", 14, com.example.neighborhood.R.color.muted);
    button(
        "保存资料",
        () -> {
          ResidentProfile updated =
              new ResidentProfile(
                  session.userId,
                  Views.value(nickname),
                  Views.value(building),
                  Views.value(unit),
                  Views.value(room));
          submit(
              () -> {
                app.accounts.saveProfile(session, updated);
                session.nickname = updated.nickname;
                app.signIn(session);
              },
              "资料已保存");
        });
  }
}
