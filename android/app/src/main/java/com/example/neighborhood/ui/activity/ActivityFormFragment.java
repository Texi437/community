package com.example.neighborhood.ui.activity;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import com.example.neighborhood.data.DraftStore;
import com.example.neighborhood.model.*;
import com.example.neighborhood.domain.LegacyLabels;
import com.example.neighborhood.ui.common.*;
import java.util.*;

public final class ActivityFormFragment extends DraftFragment<ActivityFormFragment.Data> {
  record Data(DraftStore.Draft draft, List<Venue> venues, List<DictionaryOption> topics, String error) {}
  public static ActivityFormFragment fromOpportunity(Opportunity opportunity) {
    ActivityFormFragment fragment = new ActivityFormFragment();
    Bundle args = new Bundle();
    args.putString("title", opportunity.title);
    args.putString("topic", opportunity.topicCode);
    args.putString("start", Times.format(opportunity.time.start));
    args.putString("end", Times.format(opportunity.time.end));
    args.putString("venue", opportunity.venueId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  protected Data loadData() {
    DraftStore.Draft draft = loadDraft();
    try {
      return new Data(draft, app.activities.venues(), app.interests.topics(session), null);
    } catch (IllegalStateException error) {
      return new Data(draft, List.of(), List.of(), error.getMessage());
    }
  }

  @Override
  protected String title() {
    return "发起活动";
  }

  @Override
  protected String draftKind() {
    return "activity";
  }

  @Override
  protected void render(Data data) {
    DraftStore.Draft draft = data.draft();
    if (getArguments() != null && !model.form.contains("_prefilled")) {
      for (String key : Arrays.asList("title", "topic", "start", "end", "venue"))
        if (getArguments().containsKey(key)) model.form.set(key, getArguments().getString(key));
      model.form.set("_prefilled", true);
    }
    restore(draft);
    if (!model.form.contains("request_id"))
      model.form.set("request_id", UUID.randomUUID().toString());
    heading("发起一次邻里相聚", null);
    Views.field(binding.body, "活动名称", "title", "", model.form, InputType.TYPE_CLASS_TEXT);
    Spinner topic = Views.options(binding.body, "活动主题", "topic", data.topics(), model.form, LegacyLabels::topicCode);
    Views.field(
            binding.body,
            "活动内容",
            "description",
            "",
            model.form,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        .setMinLines(3);
    Views.field(
        binding.body,
        "开始时间",
        "start",
        Times.format(Times.future(3, 9)),
        model.form,
        InputType.TYPE_CLASS_TEXT);
    Views.field(
        binding.body,
        "结束时间",
        "end",
        Times.format(Times.future(3, 10)),
        model.form,
        InputType.TYPE_CLASS_TEXT);
    List<Venue> venues = data.venues();
    Spinner venue = Views.spinner(binding.body, "活动场地", venues);
    List<String> venueIds = new ArrayList<>();
    for (Venue item : venues) venueIds.add(item.id);
    bindSpinner(venue, "venue", venueIds);
    Views.field(binding.body, "人数上限", "capacity", "15", model.form, InputType.TYPE_CLASS_NUMBER);
    CheckBox speaker = new CheckBox(requireContext());
    speaker.setText("需要使用音响");
    speaker.setChecked("true".equals(model.form.get("speaker")));
    binding.body.addView(speaker);
    speaker.setOnCheckedChangeListener(
        (button, checked) -> model.form.set("speaker", String.valueOf(checked)));
    if (!venues.isEmpty())
      button(
          "查看场地规则",
          () ->
              new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                  .setTitle(((Venue) venue.getSelectedItem()).name)
                  .setMessage(((Venue) venue.getSelectedItem()).rules)
                  .setPositiveButton("知道了", null)
                  .show());
    button("保存草稿", this::saveDraft);
    if (venues.isEmpty() || data.topics().isEmpty()) {
      Views.card(
          binding.body,
          "",
          "场地或主题暂不可用",
          data.error() == null ? "请联系社区工作人员配置场地和主题后再提交。" : data.error(),
          null);
      return;
    }
    button(
        "提交并检查",
        () -> {
          try {
            if (!data.topics().contains(topic.getSelectedItem())) {
              message("原活动主题已停用，请重新选择");
              return;
            }
            DraftStore.Draft values = currentDraft();
            CommunityActivity activity = new CommunityActivity();
            activity.clientRequestId = values.get("request_id");
            activity.title = values.get("title");
            activity.topic = ((DictionaryOption) topic.getSelectedItem()).code;
            activity.description = values.get("description");
            activity.time =
                new TimeWindow(Times.parse(values.get("start")), Times.parse(values.get("end")));
            activity.venueId = ((Venue) venue.getSelectedItem()).id;
            activity.capacity = Integer.parseInt(values.get("capacity"));
            activity.usesSpeaker = speaker.isChecked();
            submit(
                () -> {
                  app.activities.publish(session, activity);
                  app.drafts.clear(session, draftKind());
                },
                "活动已提交");
          } catch (IllegalArgumentException e) {
            message(e instanceof NumberFormatException ? "请填写有效的人数上限" : e.getMessage());
          }
        });
  }

  private void bindSpinner(Spinner field, String key, List<String> values) {
    String saved = model.form.get(key);
    if (saved != null && values.contains(saved)) field.setSelection(values.indexOf(saved));
    field.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> p) {}

          public void onItemSelected(AdapterView<?> p, android.view.View v, int position, long id) {
            model.form.set(key, values.get(position));
          }
        });
  }

  @Override
  protected void onSuccess(String message) {
    if (message.equals("活动已提交")) {
      com.example.neighborhood.ui.MainActivity activity = host();
      leave();
      activity.open(ActivityListFragment.create("created"));
      return;
    }
    super.onSuccess(message);
  }
}
