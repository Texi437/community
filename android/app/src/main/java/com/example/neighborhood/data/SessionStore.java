package com.example.neighborhood.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.example.neighborhood.model.Session;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SessionStore {
  private static final String ALIAS = "neighborhood_session";
  private final SharedPreferences preferences;

  public SessionStore(Context context) {
    preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE);
  }

  private SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (!store.containsAlias(ALIAS)) {
      KeyGenerator generator =
          KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
      generator.init(
          new KeyGenParameterSpec.Builder(
                  ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      return generator.generateKey();
    }
    return (SecretKey) store.getKey(ALIAS, null);
  }

  public void save(Session session) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key());
      byte[] encrypted =
          cipher.doFinal(new Gson().toJson(session).getBytes(StandardCharsets.UTF_8));
      if (!preferences
          .edit()
          .putString("value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
          .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
          .commit()) throw new IllegalStateException("会话保存失败");
    } catch (Exception e) {
      throw new IllegalStateException("无法安全保存登录状态", e);
    }
  }

  public Session load() {
    String value = preferences.getString("value", null);
    if (value == null) return null;
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key(),
          new GCMParameterSpec(
              128, Base64.decode(preferences.getString("iv", ""), Base64.NO_WRAP)));
      return new Gson()
          .fromJson(
              new String(
                  cipher.doFinal(Base64.decode(value, Base64.NO_WRAP)), StandardCharsets.UTF_8),
              Session.class);
    } catch (Exception e) {
      android.util.Log.w("SessionStore", "Session restore failed: " + e.getClass().getSimpleName());
      clear();
      return null;
    }
  }

  // Sign-out and cold-start recovery wait for this disk write.
  @SuppressLint("ApplySharedPref")
  public void clear() {
    preferences.edit().clear().commit();
  }
}
