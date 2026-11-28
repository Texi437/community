package com.example.neighborhood.ui.activity;

import android.os.Bundle;
import com.example.neighborhood.R;
import com.example.neighborhood.data.DraftStore;
import com.example.neighborhood.model.*;
import com.example.neighborhood.ui.common.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public final class ActivityDetailFragment extends ScreenFragment<ActivityDetailFragment.Data> {
  record Data(
      CommunityActivity activity,
      String venue,
      int count,
      boolean enrolled,
      List<PublicProfile> participants) {}

  public static ActivityDetailFragment create(String id) {
    ActivityDetailFragment fragment = new ActivityDetailFragment();
    Bundle args = new Bundle();
    args.putString("id", id);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  protected String title() {
    return "活动详情";
  }

  @Override
  protected Data loadData() {
    CommunityActivity activity = app.activities.get(session, requireArguments().getString("id"));
    String venue = activity.venueName;
    boolean enrolled = activity.enrollmentStatus == Enrollment.Status.CONFIRMED;
    boolean organizer =
        session.userId.equals(activity.organizerId) || session.role == Session.Role.ADMIN;
    List<PublicProfile> participants =
        organizer
            ? app.activities.participants(session, activity.id)
            : java.util.Collections.emptyList();
    return new Data(activity, venue, activity.enrollmentCount, enrolled, participants);
  }

  @Override
  protected void render(Data data) {
    CommunityActivity activity = data.activity();
    com.google.android.material.card.MaterialCardView hero =
        Views.card(binding.body, activity.topic, activity.title, null, null);
    hero.setCardBackgroundColor(requireContext().getColor(R.color.warm));
    Views.card(
        binding.body,
        activity.statusLabel(System.currentTimeMillis()),
        Times.range(activity.time),
        data.venue() + "\n" + data.count() + " / " + activity.capacity + " 人已报名",
        null);
    Views.text(binding.body, activity.description, 16, R.color.ink);
    Views.text(binding.body, "发起人：" + activity.organizerNickname, 14, R.color.muted);
    if (activity.status == CommunityActivity.Status.PUBLISHED
        && activity.time.start > System.currentTimeMillis()) {
      if (data.enrolled())
        button(
            "退出报名",
            () -> confirm("退出这场活动？", () -> app.activities.withdraw(session, activity.id), "已退出报名"));
      else button("报名参加", () -> submit(() -> app.activities.enroll(session, activity.id), "报名成功"));
    }
    boolean organizer =
        session.userId.equals(activity.organizerId) || session.role == Session.Role.ADMIN;
    if (organizer) {
      Views.card(
          binding.body,
          "发布检查",
          activity.reviewMethod == null
              ? activity.statusLabel(System.currentTimeMillis())
              : activity.reviewMethod,
          null,
          null);
      button(
          "查看报名昵称",
          () -> {
            List<PublicProfile> people = data.participants();
            StringBuilder names = new StringBuilder();
            for (PublicProfile person : people) names.append(person.nickname).append('\n');
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("报名名单")
                .setMessage(people.isEmpty() ? "暂无报名" : names.toString().trim())
                .setPositiveButton("关闭", null)
                .show();
          });
      if (activity.status == CommunityActivity.Status.PUBLISHED
          || activity.status == CommunityActivity.Status.PENDING_REVIEW)
        button(
            "取消活动",
            () ->
                confirm(
                    "取消后将释放名额和场地",
                    () ->
                        app.activities.updateStatus(
                            session,
                            activity.id,
                            activity.version,
                            CommunityActivity.Status.CANCELLED),
                    "活动已取消"));
      if (activity.status == CommunityActivity.Status.PUBLISHED
          && activity.time.end <= System.currentTimeMillis())
        button(
            "标记活动结束",
            () ->
                confirm(
                    "确认活动已结束？",
                    () ->
                        app.activities.updateStatus(
                            session, activity.id, activity.version, CommunityActivity.Status.ENDED),
                    "活动已结束"));
      if (activity.status == CommunityActivity.Status.CANCELLED
          || activity.status == CommunityActivity.Status.REJECTED)
        button("复制并重新发起", () -> copy(activity));
    }
    if (session.role == Session.Role.ADMIN
        && activity.status == CommunityActivity.Status.PENDING_REVIEW) {
      button(
          "协调通过",
          () ->
              confirm(
                  "确认通过此活动？",
                  () -> app.activities.review(session, activity.id, activity.version, true),
                  "活动已发布"));
      button(
          "不通过",
          () ->
              confirm(
                  "确认不通过此活动？",
                  () -> app.activities.review(session, activity.id, activity.version, false),
                  "已处理"));
    }
  }

  private void confirm(String title, Runnable action, String result) {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(title)
        .setNegativeButton("返回", null)
        .setPositiveButton("确认", (dialog, which) -> submit(action, result))
        .show();
  }

  private void copy(CommunityActivity activity) {
    DraftStore.Draft draft = new DraftStore.Draft();
    draft.put("title", activity.title);
    draft.put("description", activity.description);
    draft.put("topic", activity.topicCode);
    draft.put("venue", activity.venueId);
    draft.put("capacity", String.valueOf(activity.capacity));
    submit(() -> app.drafts.save(session, "activity", draft), "已复制为新草稿");
  }

  @Override
  protected void onSuccess(String message) {
    if (message.equals("已复制为新草稿")) open(new ActivityFormFragment());
  }
}
