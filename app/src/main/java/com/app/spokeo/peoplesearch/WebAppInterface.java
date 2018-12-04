package com.app.spokeo.peoplesearch;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.app.spokeo.peoplesearch.util.LocalNotification;
import com.app.spokeo.peoplesearch.util.Pref;
import com.app.spokeo.peoplesearch.util.ProgressDialogHelper;
import com.google.firebase.iid.FirebaseInstanceId;

import fr.quentinklein.slt.LocationTracker;
import fr.quentinklein.slt.TrackerSettings;
import hotchemi.android.rate.AppRate;

/**
 * Created by dragank on 2/20/2017.
 */

public class WebAppInterface {
    private Context mContext;
    private String productId;
    private WebView mWebview;
    private LocationTracker myTracker;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context mContext, WebView mWebview) {
        this.mContext = mContext;
        this.productId = productId;
        this.mWebview = mWebview;
    }

    @JavascriptInterface
    public String getOneSignalRegisteredId() {
        return Pref.getValue(mContext, Pref.ONESIGNAL_REGISTERED_ID, "");
    }

    @JavascriptInterface
    public String getFirebaseToken() {
        return  FirebaseInstanceId.getInstance().getToken();
    }

    @JavascriptInterface
    public boolean isProductPurchased() {
        return Pref.getValue(mContext, productId, false);
    }


    @JavascriptInterface
    public void createNotification(String displayName, String message) {
        LocalNotification.createNotification(mContext, displayName, message);
    }

    @JavascriptInterface
    public void showLoader() {
        ProgressDialogHelper.showProgress(mContext);
    }

    @JavascriptInterface
    public void hideLoader() {
        ProgressDialogHelper.dismissProgress();
    }

    @JavascriptInterface
    public void fontSizeNormal() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.NORMAL);
            }
        });
    }

    @JavascriptInterface
    public void fontSizeLarger() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.LARGER);
            }
        });
    }

    @JavascriptInterface
    public void fontSizeLargest() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.LARGEST);
            }
        });
    }

    @JavascriptInterface
    public void fontSizeSmaller() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.SMALLER);
            }
        });

    }

    @JavascriptInterface
    public void fontSizeSmallest() {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                WebSettings webSettings = mWebview.getSettings();
                webSettings.setTextSize(WebSettings.TextSize.SMALLEST);
            }
        });
    }

    @JavascriptInterface
    public void rateApp() {
        AppRate.with(mContext).showRateDialog((MainActivity)mContext);
    }

    @JavascriptInterface
    public void playSound(String fname) {
        int resID = mContext.getResources().getIdentifier(fname, "raw", mContext.getPackageName());
        if (resID > 0) {
            MediaPlayer mediaPlayer = MediaPlayer.create(mContext, resID);
            mediaPlayer.start();
        }
    }

    public void startGoogleTracker() {
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // You need to ask the user to enable the permissions
        } else {
            if (myTracker != null) {
                myTracker.startListening();
                return;
            }

            TrackerSettings settings = new TrackerSettings()
                    .setUseGPS(true)
                    .setUseNetwork(true)
                    .setUsePassive(true)
                    .setTimeBetweenUpdates(10 * 1000)
                    .setMetersBetweenUpdates(10);

            myTracker = new LocationTracker(mContext, settings) {
                @Override
                public void onLocationFound(@NonNull Location location) {
                    mWebview.loadUrl("javascript:locationTracker(" + location.getLongitude() + ", " +  location.getLatitude()+ ")");
                }

                @Override
                public void onTimeout() {
                }
            };
            myTracker.startListening();
        }
    }

    public void stopGoogleTracker() {
        if (myTracker != null) {
            myTracker.stopListening();
        }
    }

}

