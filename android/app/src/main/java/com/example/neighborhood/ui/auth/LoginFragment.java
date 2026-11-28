package com.example.neighborhood.ui.auth;

import android.text.InputType;
import com.example.neighborhood.ui.common.*;
import com.google.android.material.textfield.TextInputEditText;

public final class LoginFragment extends ScreenFragment<Boolean> {
  @Override
  protected String title() {
    return "邻里共治";
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
    binding.refresh.setVisibility(android.view.View.GONE);
    Views.illustration(binding.body, 294, 126);
    heading("邻里有事，一起解决。", null);
    TextInputEditText account =
        Views.field(binding.body, "用户名", "account", "", model.form, InputType.TYPE_CLASS_TEXT);
    TextInputEditText password =
        Views.field(
            binding.body,
            "密码",
            "password",
            "",
            model.form,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    button(
        "登录",
        () -> {
          String name = Views.value(account), secret = password.getText().toString();
          submit(() -> app.signIn(app.accounts.login(name, secret)), "登录成功");
        });
    Views.card(binding.body, "", "注册居民账号", null, () -> open(new RegisterFragment()));
  }

  @Override
  protected void onSuccess(String message) {
    model.form.remove("password");
    host().showHome();
  }

  @Override
  public void onStop() {
    if (model != null) model.form.remove("password");
    super.onStop();
  }
}
