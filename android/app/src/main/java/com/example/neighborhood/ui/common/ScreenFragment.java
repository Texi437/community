package com.example.neighborhood.ui.common;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import com.example.neighborhood.NeighborhoodApp;
import com.example.neighborhood.databinding.FragmentScreenBinding;
import com.example.neighborhood.di.AppContainer;
import com.example.neighborhood.model.Session;
import com.example.neighborhood.ui.MainActivity;
import com.google.android.material.snackbar.Snackbar;

public abstract class ScreenFragment<T> extends Fragment {
  protected FragmentScreenBinding binding;
  protected ScreenModel<T> model;
  protected AppContainer app;
  protected Session session;
  private T renderedData;

  protected abstract String title();

  protected abstract T loadData();

  protected abstract void render(T data);

  protected boolean requiresSession() {
    return true;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
    binding = FragmentScreenBinding.inflate(inflater, parent, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle state) {
    app = ((NeighborhoodApp) requireActivity().getApplication()).container;
    session = app.session;
    if (requiresSession() && session == null) {
      host().showLogin();
      return;
    }
    model =
        new ViewModelProvider(
                this,
                new AbstractSavedStateViewModelFactory(this, getArguments()) {
                  @Override
                  @NonNull
                  @SuppressWarnings("unchecked")
                  protected <V extends ViewModel> V create(
                      @NonNull String key,
                      @NonNull Class<V> type,
                      @NonNull SavedStateHandle handle) {
                    return (V) new ScreenModel<T>(handle);
                  }
                })
            .get(ScreenModel.class);
    binding.toolbarTitle.setText(title());
    binding.back.setOnClickListener(v -> host().getOnBackPressedDispatcher().onBackPressed());
    binding.back.setVisibility(host().canGoBack() ? View.VISIBLE : View.GONE);
    binding.refresh.setOnClickListener(v -> refresh());
    model.state.observe(
        getViewLifecycleOwner(),
        result -> {
          if (requiresSession() && app.session == null && !result.loading) {
            host().showLogin();
            return;
          }
          binding.progress.setVisibility(result.loading ? View.VISIBLE : View.GONE);
          binding.error.setVisibility(result.error == null ? View.GONE : View.VISIBLE);
          binding.error.setText(result.error);
          if (result.data != null && result.data != renderedData) {
            renderedData = result.data;
            binding.body.removeAllViews();
            render(result.data);
          }
          setEnabled(binding.body, !result.loading);
          if (result.message != null) {
            String message = result.message;
            result.message = null;
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            onSuccess(message);
          }
        });
    UiState<T> current = model.state.getValue();
    if (current == null || (!current.loading && current.message == null)) refresh();
  }

  protected void onSuccess(String message) {}

  protected void refresh() {
    model.execute(this::loadData, null);
  }

  protected void submit(Runnable action, String message) {
    model.submit(action, this::loadData, message);
  }

  protected MainActivity host() {
    return (MainActivity) requireActivity();
  }

  protected void open(Fragment fragment) {
    host().open(fragment);
  }

  protected void message(String text) {
    Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG).show();
  }

  protected void heading(String title, String subtitle) {
    Views.heading(binding.body, title, subtitle);
  }

  protected void button(String title, Runnable action) {
    Views.button(binding.body, title, action);
  }

  private void setEnabled(View view, boolean enabled) {
    view.setEnabled(enabled);
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
        setEnabled(((ViewGroup) view).getChildAt(i), enabled);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    renderedData = null;
  }
}
