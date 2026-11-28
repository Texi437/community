package com.example.neighborhood.ui.common;

import android.content.Context;
import android.graphics.Typeface;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.SavedStateHandle;
import com.example.neighborhood.R;
import com.example.neighborhood.databinding.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;

public final class Views {
  private Views() {}

  public static int dp(Context context, int value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }

  public static TextView text(LinearLayout parent, String value, int size, int color) {
    TextView text = new TextView(parent.getContext());
    text.setText(value);
    text.setTextSize(size);
    text.setTextColor(parent.getContext().getColor(color));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.bottomMargin = dp(parent.getContext(), 12);
    parent.addView(text, params);
    return text;
  }

  public static void heading(LinearLayout parent, String title, String subtitle) {
    text(parent, title, 28, R.color.ink).setTypeface(null, Typeface.BOLD);
    if (subtitle != null && !subtitle.isEmpty()) text(parent, subtitle, 14, R.color.muted);
  }

  public static MaterialButton button(LinearLayout parent, String label, Runnable action) {
    MaterialButton button = new MaterialButton(parent.getContext());
    button.setText(label);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.topMargin = dp(parent.getContext(), 8);
    params.bottomMargin = dp(parent.getContext(), 8);
    parent.addView(button, params);
    button.setOnClickListener(v -> action.run());
    return button;
  }

  public static MaterialCardView card(
      LinearLayout parent, String tag, String title, String detail, Runnable action) {
    ItemCardBinding card =
        ItemCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    card.tag.setText(tag);
    card.tag.setVisibility(tag == null || tag.isEmpty() ? View.GONE : View.VISIBLE);
    card.title.setText(title);
    card.detail.setText(detail);
    card.detail.setVisibility(detail == null || detail.isEmpty() ? View.GONE : View.VISIBLE);
    if (action != null) {
      card.getRoot().setClickable(true);
      card.getRoot().setFocusable(true);
      card.getRoot().setOnClickListener(v -> action.run());
    }
    parent.addView(card.getRoot());
    return card.getRoot();
  }

  public static TextInputEditText field(
      LinearLayout parent,
      String label,
      String key,
      String initial,
      SavedStateHandle state,
      int inputType) {
    FieldTextBinding field =
        FieldTextBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    field.getRoot().setHint(label);
    field.input.setInputType(inputType);
    boolean secret =
        (inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD)
            == InputType.TYPE_TEXT_VARIATION_PASSWORD;
    String saved = secret ? null : state.get(key);
    field.input.setText(saved == null ? initial : saved);
    if (!secret && saved == null) state.set(key, initial);
    field.input.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!secret) state.set(key, s.toString());
          }

          public void afterTextChanged(Editable value) {}
        });
    parent.addView(field.getRoot());
    return field.input;
  }

  public static <T> Spinner spinner(LinearLayout parent, String label, List<T> values) {
    text(parent, label, 14, R.color.ink);
    Spinner spinner = new Spinner(parent.getContext());
    spinner.setMinimumHeight(dp(parent.getContext(), 48));
    ArrayAdapter<T> adapter =
        new ArrayAdapter<>(
            parent.getContext(), android.R.layout.simple_spinner_dropdown_item, values);
    spinner.setAdapter(adapter);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
    params.bottomMargin = dp(parent.getContext(), 16);
    parent.addView(spinner, params);
    return spinner;
  }

  public static Spinner options(
      LinearLayout parent, String label, String key,
      List<com.example.neighborhood.model.DictionaryOption> available, SavedStateHandle state,
      java.util.function.UnaryOperator<String> legacyCode) {
    var values = new java.util.ArrayList<>(available);
    String saved = state.get(key);
    int selected = 0;
    if (saved != null) {
      selected = -1;
      String code = legacyCode.apply(saved);
      for (int i = 0; i < values.size(); i++)
        if (values.get(i).code.equals(code) || values.get(i).label.equals(saved)) { selected = i; break; }
      if (selected < 0) {
        selected = values.size();
        values.add(new com.example.neighborhood.model.DictionaryOption(code, "原选项已停用，请重新选择"));
      }
    }
    Spinner field = spinner(parent, label, values);
    if (!values.isEmpty()) {
      field.setSelection(selected);
      state.set(key, values.get(selected).code);
    }
    field.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      public void onNothingSelected(AdapterView<?> parent) {}
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        state.set(key, values.get(position).code);
      }
    });
    return field;
  }

  public static void illustration(LinearLayout parent, int width, int height) {
    ImageView image = new ImageView(parent.getContext());
    image.setImageResource(R.drawable.illustration_neighborhood);
    image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(
            dp(parent.getContext(), width), dp(parent.getContext(), height));
    params.bottomMargin = dp(parent.getContext(), 20);
    parent.addView(image, params);
  }

  public static String value(TextView input) {
    return input.getText().toString().trim();
  }

  public static void images(LinearLayout parent, List<String> paths) {
    for (String path : paths) {
      if (!new java.io.File(path).isFile()) {
        text(parent, "附件暂不可用", 14, R.color.muted);
        continue;
      }
      ImageView image = new ImageView(parent.getContext());
      image.setImageURI(android.net.Uri.fromFile(new java.io.File(path)));
      image.setAdjustViewBounds(true);
      image.setMaxHeight(dp(parent.getContext(), 220));
      image.setContentDescription("诉求附件");
      parent.addView(image);
    }
  }
}
