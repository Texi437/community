package com.example.neighborhood.ui.interest;

import android.text.InputType;
import android.widget.Spinner;
import com.example.neighborhood.model.*;
import com.example.neighborhood.ui.common.*;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import java.util.*;

public final class AvailabilityFragment extends ScreenFragment<InterestProfile> {
  private List<TimeWindow> windows = new ArrayList<>();

  @Override
  protected String title() {
    return "近期参与安排";
  }

  @Override
  protected InterestProfile loadData() {
    return app.interests.get(session);
  }

  @Override
  protected void render(InterestProfile profile) {
    heading("安排你的空闲时间", null);
    Spinner intent = Views.spinner(binding.body, "近期参加意愿", Arrays.asList("暂未决定", "愿意参加", "近期不参加"));
    Integer savedIntent = model.form.get("intent");
    intent.setSelection(savedIntent == null ? profile.intent.ordinal() : savedIntent);
    intent.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(android.widget.AdapterView<?> p) {}

          public void onItemSelected(
              android.widget.AdapterView<?> p, android.view.View v, int position, long id) {
            model.form.set("intent", position);
          }
        });
    String saved = model.form.get("windows");
    windows =
        saved == null
            ? new ArrayList<>(profile.availability)
            : new ArrayList<>(Arrays.asList(new Gson().fromJson(saved, TimeWindow[].class)));
    TextInputEditText until =
        Views.field(
            binding.body,
            "安排有效至",
            "until",
            Times.format(
                profile.validUntil > System.currentTimeMillis()
                    ? profile.validUntil
                    : Times.future(7, 21)),
            model.form,
            InputType.TYPE_CLASS_TEXT);
    for (TimeWindow window : windows)
      Views.card(
          binding.body,
          "空闲时段",
          Times.range(window),
          "点击移除此时段",
          () -> {
            windows.remove(window);
            model.form.set("windows", new Gson().toJson(windows));
            refresh();
          });
    TextInputEditText start =
        Views.field(
            binding.body,
            "开始时间",
            "start",
            Times.format(Times.future(3, 9)),
            model.form,
            InputType.TYPE_CLASS_TEXT);
    TextInputEditText end =
        Views.field(
            binding.body,
            "结束时间",
            "end",
            Times.format(Times.future(3, 10)),
            model.form,
            InputType.TYPE_CLASS_TEXT);
    button(
        "添加时段",
        () -> {
          try {
            TimeWindow window =
                new TimeWindow(Times.parse(Views.value(start)), Times.parse(Views.value(end)));
            windows.add(window);
            model.form.set("windows", new Gson().toJson(windows));
            refresh();
          } catch (IllegalArgumentException e) {
            message(e.getMessage());
          }
        });
    button(
        "保存近期安排",
        () -> {
          try {
            long validUntil = Times.parse(Views.value(until));
            List<TimeWindow> selected = new ArrayList<>(windows);
            InterestProfile.Intent selectedIntent =
                InterestProfile.Intent.values()[intent.getSelectedItemPosition()];
            submit(
                () ->
                    app.interests.saveAvailability(
                        session, selectedIntent, selected, validUntil, profile.version),
                "近期安排已保存");
          } catch (IllegalArgumentException e) {
            message(e.getMessage());
          }
        });
  }
}
