package com.example.neighborhood.ui.interest;

import android.widget.Spinner;
import com.example.neighborhood.model.InterestProfile;
import com.example.neighborhood.model.DictionaryOption;
import com.example.neighborhood.domain.LegacyLabels;
import com.example.neighborhood.ui.common.*;
import java.util.*;

public final class InterestFragment extends ScreenFragment<InterestFragment.Data> {
  record Data(InterestProfile profile, List<DictionaryOption> topics) {}
  @Override
  protected String title() {
    return "兴趣与近期安排";
  }

  @Override
  protected Data loadData() {
    return new Data(app.interests.get(session), app.interests.topics(session));
  }

  @Override
  protected void render(Data data) {
    InterestProfile profile = data.profile();
    heading("你对什么感兴趣？", null);
    Map<String, Spinner> fields = new LinkedHashMap<>();
    List<String> levels =
        Arrays.asList("未填写", "不感兴趣", "1 · 有一点兴趣", "2 · 比较感兴趣", "3 · 感兴趣", "4 · 很感兴趣", "5 · 非常感兴趣");
    for (DictionaryOption option : data.topics()) {
      String topic = option.code;
      Spinner field = Views.spinner(binding.body, option.label, levels);
      Integer strength = profile.strengths.get(topic);
      Integer saved = model.form.get(topic);
      if (saved == null) saved = model.form.get(LegacyLabels.topic(topic));
      int selection = strength == null ? 0 : strength + 1;
      if (saved != null) selection = saved;
      field.setSelection(selection);
      field.setOnItemSelectedListener(
          new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}

            public void onItemSelected(
                android.widget.AdapterView<?> p, android.view.View v, int position, long id) {
              model.form.set(topic, position);
            }
          });
      fields.put(topic, field);
    }
    button(
        "保存兴趣",
        () -> {
          Map<String, Integer> strengths = new LinkedHashMap<>();
          for (Map.Entry<String, Spinner> entry : fields.entrySet())
            if (entry.getValue().getSelectedItemPosition() > 0)
              strengths.put(entry.getKey(), entry.getValue().getSelectedItemPosition() - 1);
          submit(() -> app.interests.saveInterests(session, strengths), "兴趣已保存");
        });
    Views.card(binding.body, "近期安排", "参加意愿与空闲时间", null, () -> open(new AvailabilityFragment()));
    Views.card(binding.body, "", "发现共同兴趣", null, () -> open(new OpportunityFragment()));
  }
}
