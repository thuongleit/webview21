package com.app.spokeo.peoplesearch;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.app.spokeo.peoplesearch.configuration.Config;
import com.app.spokeo.peoplesearch.configuration.NotifyHelper;
import com.app.spokeo.peoplesearch.configuration.RESTService;
import com.app.spokeo.peoplesearch.util.Pref;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by dragank on 9/16/2016.
 */

public class SuperViewWeb extends Application implements OneSignal.NotificationOpenedHandler {

  private static final String TAG = SuperViewWeb.class.getSimpleName();
  private static boolean activityVisible;
  public Config mConfig;

  @Override
  public void onCreate() {
    super.onCreate();
    OneSignal
        .startInit(this)
        .setNotificationOpenedHandler(this)
        .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
        .init();

    OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
      @Override
      public void idsAvailable(String userId, String registrationId) {
        Pref.setValue(getApplicationContext(), Pref.ONESIGNAL_REGISTERED_ID, userId);
      }
    });
    fetchConfig();
  }

  private void fetchConfig() {
    final Retrofit adapter = new Retrofit.Builder()
        .baseUrl(getString(R.string.config_base_url))
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    RESTService api = adapter.create(RESTService.class);
    Call<Config> call = api.getConfig(getString(R.string.config_url));
    call.enqueue(new Callback<Config>() {
      @Override
      public void onResponse(Call<Config> call, Response<Config> response) {
        Config config = response.body();
        mConfig = config;
        NotifyHelper.getInstance().notifyObservers(NotifyHelper.CONFIG_FETCHED, mConfig);
      }

      @Override
      public void onFailure(Call<Config> call, Throwable t) {
        Log.d("Error", t.getMessage());
      }
    });
  }


  @Override
  public void notificationOpened(OSNotificationOpenResult result) {
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (result.notification.payload.launchURL != null) {
      intent.putExtra(Intent.EXTRA_TEXT, result.notification.payload.launchURL);
      intent.setType("text/plain");
    }
    startActivity(intent);
  }

  public static boolean isActivityVisible() {
    return activityVisible;
  }

  public static void activityResumed() {
    activityVisible = true;
  }

  public static void activityPaused() {
    activityVisible = false;
  }


}
