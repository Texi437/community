package com.example.neighborhood.ui.auth;

import android.text.InputType;
import com.example.neighborhood.model.ResidentProfile;
import com.example.neighborhood.ui.common.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public final class RegisterFragment extends ScreenFragment<Boolean> {
  @Override
  protected String title() {
    return "居民注册";
  }

  @Override
  protected boolean requiresSession() {
    return false;
  }

  @Override
  protected Boolean loadData() {
    return true;
  }

  @Override
  protected void render(Boolean data) {
    heading("成为邻里的一员", null);
    TextInputEditText account =
        Views.field(binding.body, "用户名", "account", "", model.form, InputType.TYPE_CLASS_TEXT);
    TextInputEditText nickname =
        Views.field(binding.body, "昵称", "nickname", "", model.form, InputType.TYPE_CLASS_TEXT);
    TextInputEditText password =
        Views.field(
            binding.body,
            "密码（至少 8 位）",
            "password",
            "",
            model.form,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    TextInputEditText building =
        Views.field(binding.body, "楼栋", "building", "", model.form, InputType.TYPE_CLASS_TEXT);
    TextInputEditText unit =
        Views.field(binding.body, "单元", "unit", "", model.form, InputType.TYPE_CLASS_TEXT);
    TextInputEditText room =
        Views.field(binding.body, "门牌", "room", "", model.form, InputType.TYPE_CLASS_TEXT);
    Views.text(binding.body, "只有昵称会对其他居民公开。", 14, com.example.neighborhood.R.color.muted);
    button(
        "创建账号",
        () -> {
          String name = Views.value(account), secret = password.getText().toString();
          ResidentProfile profile =
              new ResidentProfile(
                  "",
                  Views.value(nickname),
                  Views.value(building),
                  Views.value(unit),
                  Views.value(room));
          submit(() -> app.accounts.register(name, secret, profile), "注册成功");
        });
  }

  @Override
  protected void onSuccess(String message) {
    model.form.remove("password");
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle("欢迎，新邻居")
        .setMessage("账号已创建，请使用刚才的账号登录。")
        .setPositiveButton("去登录", (dialog, which) -> host().showLogin())
        .setCancelable(false)
        .show();
  }

  @Override
  public void onStop() {
    if (model != null) model.form.remove("password");
    super.onStop();
  }
}
