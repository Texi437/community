package com.example.neighborhood.ui.profile;

import com.example.neighborhood.model.*;
import com.example.neighborhood.ui.activity.ActivityListFragment;
import com.example.neighborhood.ui.common.*;
import com.example.neighborhood.ui.interest.InterestFragment;
import com.example.neighborhood.ui.workspace.WorkspaceFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class ProfileFragment extends ScreenFragment<ResidentProfile> {
  @Override
  protected String title() {
    return "我的";
  }

  @Override
  protected ResidentProfile loadData() {
    return app.accounts.profile(session, session.userId);
  }

  @Override
  protected void render(ResidentProfile profile) {
    heading("我的", null);
    Views.card(
        binding.body,
        "本人资料",
        profile.nickname,
        "管理昵称与居住信息",
        () -> open(ProfileEditFragment.create(profile.userId)));
    Views.card(binding.body, "", "兴趣与近期安排", null, () -> open(new InterestFragment()));
    Views.card(binding.body, "", "我的报名", null, () -> open(ActivityListFragment.create("enrolled")));
    Views.card(
        binding.body, "", "我发起的活动", null, () -> open(ActivityListFragment.create("created")));
    Views.card(binding.body, "", "本地草稿", null, () -> open(new DraftsFragment()));
    if (session.role != Session.Role.RESIDENT)
      Views.card(binding.body, "", "工作台", null, () -> open(new WorkspaceFragment()));
    button(
        "退出登录",
        () ->
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("退出当前账号？")
                .setNegativeButton("继续使用", null)
                .setPositiveButton(
                    "退出",
                    (dialog, which) ->
                        model.execute(
                            () -> {
                              try {
                                app.accounts.logout(session);
                              } finally {
                                app.signOut();
                              }
                              return null;
                            },
                            "已退出登录"))
                .show());
  }
}
