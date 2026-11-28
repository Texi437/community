package com.example.neighborhood.ui.common;

public final class UiState<T> {
  public final T data;
  public final boolean loading;
  public final String error;
  public String message;

  public UiState(T data, boolean loading, String error, String message) {
    this.data = data;
    this.loading = loading;
    this.error = error;
    this.message = message;
  }
}
