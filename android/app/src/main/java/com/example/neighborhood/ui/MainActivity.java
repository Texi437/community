package com.example.neighborhood.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.fragment.app.*;
import com.example.neighborhood.*;
import com.example.neighborhood.databinding.ActivityMainBinding;
import com.example.neighborhood.di.AppContainer;
import com.example.neighborhood.ui.activity.ActivityListFragment;
import com.example.neighborhood.ui.auth.LoginFragment;
import com.example.neighborhood.ui.home.HomeFragment;
import com.example.neighborhood.ui.profile.ProfileFragment;
import com.example.neighborhood.ui.workorder.WorkOrderListFragment;

public final class MainActivity extends AppCompatActivity {
  private ActivityMainBinding binding;
  private AppContainer app;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    androidx.core.view.WindowCompat.getInsetsController(getWindow(), binding.getRoot())
        .setAppearanceLightNavigationBars(true);
    app = ((NeighborhoodApp) getApplication()).container;
    getSupportFragmentManager()
        .addOnBackStackChangedListener(
            () ->
                binding.navigation.setVisibility(
                    app.session != null && !canGoBack() ? View.VISIBLE : View.GONE));
    ViewCompat.setOnApplyWindowInsetsListener(
        binding.root,
        (view, insets) -> {
          androidx.core.graphics.Insets bars =
              insets.getInsets(WindowInsetsCompat.Type.systemBars());
          view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    binding.navigation.setOnItemSelectedListener(
        item -> {
          if (app.session == null) return false;
          Fragment fragment;
          if (item.getItemId() == R.id.nav_orders) fragment = WorkOrderListFragment.create(false);
          else if (item.getItemId() == R.id.nav_activities)
            fragment = ActivityListFragment.create("all");
          else if (item.getItemId() == R.id.nav_profile) fragment = new ProfileFragment();
          else fragment = new HomeFragment();
          root(fragment);
          return true;
        });
    if (state == null) {
      if (app.session == null) showLogin();
      else showHome();
    } else
      binding.navigation.setVisibility(
          app.session != null && !canGoBack() ? View.VISIBLE : View.GONE);
  }

  public void open(Fragment fragment) {
    getSupportFragmentManager()
        .beginTransaction()
        .setReorderingAllowed(true)
        .replace(R.id.content, fragment)
        .addToBackStack(null)
        .commit();
  }

  private void root(Fragment fragment) {
    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
  }

  public void showHome() {
    binding.navigation.setVisibility(View.VISIBLE);
    binding.navigation.setSelectedItemId(R.id.nav_home);
  }

  public void showLogin() {
    binding.navigation.setVisibility(View.GONE);
    root(new LoginFragment());
  }

  public boolean canGoBack() {
    return getSupportFragmentManager().getBackStackEntryCount() > 0;
  }
}
