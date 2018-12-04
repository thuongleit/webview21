package com.app.spokeo.peoplesearch;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.app.spokeo.peoplesearch.configuration.Config;
import com.app.spokeo.peoplesearch.configuration.NotifyHelper;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import java.util.Observable;
import java.util.Observer;

public class SplashActivity extends AppCompatActivity implements Observer {

  private Config mConfig;
  private AdMob admob;
  private InterstitialAd mInterstitialAd;
  private boolean mRatingAppeared;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);
    NotifyHelper.getInstance().addObserver(this);
    mConfig = ((SuperViewWeb) getApplication()).mConfig;
    if (mConfig != null) {
      admob = new AdMob(SplashActivity.this, mConfig);
      mInterstitialAd = admob.initInterstitialAd();
    }
    mRatingAppeared = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("rating_appeared", false);
  }

  public void startButton(View view) {

    admob.showInterstitialAd();
    mInterstitialAd.setAdListener(new AdListener() {
      @Override
      public void onAdClosed() {
        super.onAdClosed();
        if (mConfig.isRatingUsed()&& !mRatingAppeared) {
          Intent intent = new Intent(SplashActivity.this, RateUsActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
          startActivity(intent);
          finish();
        } else {
          Intent intent = new Intent(SplashActivity.this, MainActivity.class);
          startActivity(intent);
          finish();
        }
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    NotifyHelper.getInstance().deleteObserver(this);
  }

  @Override
  public void update(Observable o, Object arg) {
    NotifyHelper.NotifyEvent notifyEvent = (NotifyHelper.NotifyEvent) arg;
    switch (notifyEvent.action) {
      case NotifyHelper.CONFIG_FETCHED:
        mConfig = (Config) notifyEvent.data;
        admob = new AdMob(this, mConfig);
        mInterstitialAd = admob.initInterstitialAd();
        mInterstitialAd.setAdListener(new AdListener() {
          @Override
          public void onAdClosed() {
            super.onAdClosed();
            if (mConfig.isRatingUsed() && !mRatingAppeared) {
              Intent intent = new Intent(SplashActivity.this, RateUsActivity.class);
              intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
              startActivity(intent);
              finish();
            } else {
              Intent intent = new Intent(SplashActivity.this, MainActivity.class);
              startActivity(intent);
              finish();
            }
          }
        });
    }
  }

}
