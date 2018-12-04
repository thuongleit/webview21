package com.app.spokeo.peoplesearch;

import android.content.Context;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.app.spokeo.peoplesearch.configuration.Config;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

/**
 * Created by dragank on 10/1/2016.
 */
public class AdMob {
  private static final String TAG = "ADMOB";
  private InterstitialAd mInterstitialAd;
  private Context mContext;
  private AdView adView;
  private Handler mHandler;
  private Runnable mHandlerTask;
  private Config mConfig;
  private static int INTERVAL = 1000 * 60 * 2;

  public AdMob(Context mContext, AdView adView, Config config) {
    this.mContext = mContext;
    this.adView = adView;
    this.mConfig = config;
    String interval = mContext.getString(R.string.interval_time);
    if (!TextUtils.isEmpty(interval)) {
      try {
        INTERVAL = Integer.parseInt(interval);
      } catch (Exception ex) {
        Log.e(TAG, ex.getMessage());
      }
    }
    initInterstitialAd();
  }

  public AdMob(Context context, Config config) {
    this.mContext = context;
    this.mConfig = config;
    initInterstitialAd();
  }

  public InterstitialAd initInterstitialAd() {
    String interstitialId = mConfig.getAdmob_InterId();
    if (!TextUtils.isEmpty(interstitialId)) {
      mInterstitialAd = new InterstitialAd(mContext);
      mInterstitialAd.setAdUnitId(interstitialId);
      mInterstitialAd.loadAd(getAdRequest());
    }
    return mInterstitialAd;
  }

  public void showInterstitialAd() {
    if (mInterstitialAd.isLoaded()) {
      mInterstitialAd.show();
    }
    mInterstitialAd.loadAd(getAdRequest());
  }

  public void requestInterstitialAd() {
    if (mInterstitialAd != null) {
      String interstitialId = mConfig.getAdmob_InterId();
      if (!TextUtils.isEmpty(interstitialId)) {
        if (SuperViewWeb.isActivityVisible()) {
          mInterstitialAd.loadAd(getAdRequest());
        } else {
          if (mHandler != null) {
            mHandler.postDelayed(mHandlerTask, INTERVAL);
          }
        }
      }
    }
  }

  public void requestBannerAds() {
    if (adView != null) {
      String bannerId = mConfig.getAdmob_BannerId();
      if (!TextUtils.isEmpty(bannerId)) {
        adView.loadAd(getAdRequest());
      }
    }
  }

  public void incrementInterCounter() {
    int interRotation = PreferenceManager.getDefaultSharedPreferences(mContext).getInt("inter_rotation", 0);
    PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt("inter_rotation", interRotation + 1).apply();
  }


  private AdRequest getAdRequest() {
    return new AdRequest.Builder()
        .addTestDevice(mContext.getString(R.string.test_device_id))
        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
        .build();
  }

  public void requestAdMob() {
    mHandler = new Handler();
    requestBannerAds();
    mHandlerTask = new Runnable() {
      @Override
      public void run() {
        requestInterstitialAd();
        requestBannerAds();
      }
    };
    mHandler.postDelayed(mHandlerTask, INTERVAL);
    mHandlerTask.run();
  }

  public void stopRepeatingTask() {
    if (mHandler != null) {
      mHandler.removeCallbacks(mHandlerTask);
    }
  }


}