package com.app.spokeo.peoplesearch;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.app.spokeo.peoplesearch.configuration.Config;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

public class RateUsActivity extends AppCompatActivity {
  private Config mConfig;
  private TextView mTextViewRate;
  private RatingBar mRatingBar;
  private AlertDialog mDialog;
  private AdMob admob;
  private InterstitialAd mInterstitialAd;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mConfig = ((SuperViewWeb) getApplication()).mConfig;
    admob = new AdMob(this, mConfig);
    mInterstitialAd = admob.initInterstitialAd();
    mInterstitialAd.setAdListener(new AdListener() {
      @Override
      public void onAdClosed() {
        super.onAdClosed();
        Intent intent = new Intent(RateUsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
      }
    });
    setContentView(R.layout.activity_rate_us);
    mTextViewRate = (TextView) findViewById(R.id.tvRateUs);
    mTextViewRate.setText(mConfig.getRateText());
    mRatingBar = (RatingBar) findViewById(R.id.ratingBar);
    mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
      @Override
      public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        int rate = (int) rating;
        switch (rate) {
          case 4:
            PreferenceManager.getDefaultSharedPreferences(RateUsActivity.this).edit().putBoolean("rating_appeared", true).apply();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mConfig.getRatingLink())));
            break;
          case 5:
            PreferenceManager.getDefaultSharedPreferences(RateUsActivity.this).edit().putBoolean("rating_appeared", true).apply();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mConfig.getRatingLink())));
            break;
          case 1:
            showThankYouDialog();
            break;
          case 3:
            showThankYouDialog();
            break;
          case 2:
            showThankYouDialog();
            break;
        }
      }
    });
  }

  private void showThankYouDialog() {
    LayoutInflater inflater = LayoutInflater.from(this);
    View view = inflater.inflate(R.layout.thank_you_dialog, null, false);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(true);
    builder.setView(view);
    mDialog = builder.create();
    view.findViewById(R.id.cancel_exit).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mDialog.dismiss();
        Intent intent = new Intent(RateUsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
      }
    });
    mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
    mDialog.getWindow().setLayout(600, 400);
    mDialog.show();
  }

  public void skipButton(View view) {
    mInterstitialAd.show();
  }
}