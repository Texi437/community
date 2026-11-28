package com.example.neighborhood.ui.common;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import java.util.concurrent.*;

public final class ScreenModel<T> extends ViewModel {
  public final SavedStateHandle form;
  public final MutableLiveData<UiState<T>> state = new MutableLiveData<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private boolean running;
  private boolean cleared;

  public ScreenModel(SavedStateHandle form) {
    this.form = form;
  }

  public void execute(Callable<T> task, String message) {
    execute(null, task, message);
  }

  public void submit(Runnable action, Callable<T> refresh, String message) {
    execute(action, refresh, message);
  }

  private void execute(Runnable action, Callable<T> task, String message) {
    if (running) return;
    running = true;
    UiState<T> previous = state.getValue();
    T data = previous == null ? null : previous.data;
    state.setValue(new UiState<>(data, true, null, null));
    executor.execute(
        () -> {
          UiState<T> result;
          boolean saved = false;
          try {
            if (action != null) {
              action.run();
              saved = true;
            }
            result = new UiState<>(task.call(), false, null, message);
          } catch (Exception e) {
            android.util.Log.w("ScreenModel", "Task failed: " + e.getClass().getSimpleName());
            String error = e.getMessage() == null ? "操作失败，请重试" : e.getMessage();
            if (saved) error = "操作已完成，页面刷新失败，请刷新查看最新状态";
            result = new UiState<>(data, false, error, saved ? message : null);
          }
          UiState<T> completed = result;
          main.post(
              () -> {
                running = false;
                if (!cleared) state.setValue(completed);
              });
        });
  }

  @Override
  protected void onCleared() {
    cleared = true;
    executor.shutdownNow();
  }
}
