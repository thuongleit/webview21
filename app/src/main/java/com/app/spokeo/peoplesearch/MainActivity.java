package com.app.spokeo.peoplesearch;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.app.spokeo.peoplesearch.configuration.Config;
import com.app.spokeo.peoplesearch.configuration.NotifyHelper;
import com.app.spokeo.peoplesearch.util.NetworkHandler;
import com.app.spokeo.peoplesearch.util.PermissionUtil;
import com.app.spokeo.peoplesearch.util.ProgressDialogHelper;
import com.app.spokeo.peoplesearch.util.UrlHander;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DownloadListener, SwipeRefreshLayout.OnRefreshListener,
    Observer {

  /* URL saved to be loaded after fb login */
  private static String target_url, target_url_prefix;

  private Context mContext;
  private WebView mWebview, mWebviewPop;
  private ValueCallback<Uri> mUploadMessage;
  public ValueCallback<Uri[]> uploadMessage;
  private String mCameraPhotoPath;
  private Uri mCapturedImageURI = null;
  private static final int FILE_CHOOSER_RESULT_CODE = 1;
  private static final int REQUEST_SELECT_FILE = 2;

  private FrameLayout mContainer;
  private ImageView mBack;
  private ImageView mForward;
  private SwipeRefreshLayout mSwipeToRefresh;
  private boolean show_content = true, showToolBar = true;

  private String urlData, currentUrl, contentDisposition, mimeType;
  private AdMob admob;

  //DATA FOR GEOLOCAION REQUEST
  String geoLocationOrigin;
  GeolocationPermissions.Callback geoLocationCallback;
  //
  private Config mConfig;
  private ImageView mImageRating;
  private AlertDialog mDialog;
  private FrameLayout mGoBackLayout;
  private FrameLayout mAdContainer;
  private AdView mAdView;

  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    NotifyHelper.getInstance().addObserver(this);
    mConfig = ((SuperViewWeb) getApplication()).mConfig;
    if (TextUtils.isEmpty(getString(R.string.pullToRefresh))) {
      setContentView(R.layout.content_main);
    } else {
      setContentView(R.layout.content_main_pull_to_refresh);
    }
    checkURL(getIntent());
    initComponents();
    initBrowser(savedInstanceState);
    if (savedInstanceState != null) {
      showContent();
    } else {
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          showContent();
        }
      }, 5000);
    }

  }

  private void checkURL(Intent intent) {
    if (intent != null) {
      if ("text/plain".equals(intent.getType()) && !TextUtils.isEmpty(intent.getStringExtra(Intent.EXTRA_TEXT))) {
        target_url = intent.getStringExtra(Intent.EXTRA_TEXT);
        target_url_prefix = Uri.parse(target_url).getHost();
        currentUrl = target_url;
        return;
      }
    }

    target_url = getString(R.string.target_url);

    if (TextUtils.isEmpty(target_url)) {
      target_url = "file:///android_asset/index.html";
      target_url_prefix = "android_asset";
    } else {
      target_url_prefix = Uri.parse(target_url).getHost();
    }

    currentUrl = target_url;

    if (mWebview != null) {
      if (mWebviewPop != null) {
        mWebviewPop.setVisibility(View.GONE);
        mContainer.removeView(mWebviewPop);
        mWebviewPop = null;
      }
      mWebview.setVisibility(View.VISIBLE);
    }
  }


  @Override
  protected void onResume() {
    super.onResume();
    SuperViewWeb.activityResumed();
    hideStatusBar();
    checkURL(getIntent());
  }

  @Override
  protected void onPause() {
    super.onPause();
    SuperViewWeb.activityPaused();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mWebview.saveState(outState);
  }

  private void removeAds() {
    mAdView.setVisibility(View.GONE);
    if (admob != null) {
      admob.stopRepeatingTask();
    }
  }

  private void initComponents() {
    mContext = this.getApplicationContext();
    mGoBackLayout = (FrameLayout) findViewById(R.id.fl_back);
    mAdContainer = (FrameLayout) findViewById(R.id.banner);
    initBannerAds();
    mGoBackLayout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        admob.showInterstitialAd();
        showExitDialog();
      }
    });
    mSwipeToRefresh = (SwipeRefreshLayout) findViewById(R.id.swipeToRefresh);
    if (mSwipeToRefresh != null) {
      mSwipeToRefresh.setOnRefreshListener(this);
    }

    mImageRating = (ImageView) findViewById(R.id.rating);
    mImageRating.setOnClickListener(this);
    if (TextUtils.isEmpty(getString(R.string.toolbar))) {
      showToolBar = false;
    }

    if (showToolBar) {
      mBack = (ImageView) findViewById(R.id.back);
      mForward = (ImageView) findViewById(R.id.forward);
      ImageView mRefresh = (ImageView) findViewById(R.id.refresh);

      mBack.setOnClickListener(this);
      mForward.setOnClickListener(this);
      mRefresh.setOnClickListener(this);
    } else {
      FrameLayout llToolbarContainer = (FrameLayout) findViewById(R.id.toolbar_footer);
      if (llToolbarContainer != null) {
        llToolbarContainer.setVisibility(View.GONE);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mAdView.getLayoutParams();
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
      }
    }
    mWebview = (WebView) findViewById(R.id.webview);
    mContainer = (FrameLayout) findViewById(R.id.webview_frame);
    boolean rated = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("rating_appeared", false);
    if (rated) mImageRating.setVisibility(View.INVISIBLE);
    if (!mConfig.isRatingUsed()) mImageRating.setVisibility(View.INVISIBLE);
  }


  private void hideStatusBar() {
    if (!TextUtils.isEmpty(getString(R.string.hide_status_bar))) {
      if (Build.VERSION.SDK_INT < 16) {
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
      } else {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
          actionBar.hide();
        }
      }
    }
  }

  public void showContent() {
    if (show_content) {
      PermissionUtil.checkPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO,
          android.Manifest.permission.CALL_PHONE,
          android.Manifest.permission.SEND_SMS,
          android.Manifest.permission.ACCESS_NETWORK_STATE,
          android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
          android.Manifest.permission.BLUETOOTH,
          android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.ACCESS_COARSE_LOCATION,
          android.Manifest.permission.INTERNET
      });
      show_content = false;
      admob.initInterstitialAd();
      mContainer.setVisibility(View.VISIBLE);
      ProgressDialogHelper.dismissProgress();
    }
  }

  public void initBannerAds() {
    MobileAds.initialize(mContext, mConfig.getAdmob_appId());
    if (mAdContainer != null) {
      String bannerId = mConfig.getAdmob_BannerId();
      mAdView = new AdView(this);
      mAdView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
      mAdView.setAdUnitId(bannerId);
      mAdContainer.addView(mAdView);
      admob = new AdMob(this, mAdView, mConfig);
      admob.requestBannerAds();
      mAdView.setVisibility(View.VISIBLE);
    }
  }

  @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
  private void initBrowser(Bundle savedInstanceState) {
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptCookie(true);

    WebSettings webSettings = mWebview.getSettings();
    webSettings.setJavaScriptEnabled(true);

    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
    webSettings.setSupportMultipleWindows(true);
    webSettings.setGeolocationEnabled(true);
    webSettings.setDomStorageEnabled(true);
    webSettings.setDatabaseEnabled(true);
    webSettings.setGeolocationEnabled(true);
    webSettings.setGeolocationDatabasePath(getFilesDir().getPath());
    webSettings.setLoadWithOverviewMode(true);
    webSettings.setAllowFileAccess(true);
    webSettings.setUserAgentString("Android");
    if (TextUtils.isEmpty(getString(R.string.cache))) {
      webSettings.setAppCacheEnabled(true);
      webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
      webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
    }
    int a = WebSettings.TextSize.SMALLER.ordinal();
    mWebview.setWebViewClient(new UriWebViewClient());
    mWebview.setWebChromeClient(new UriChromeClient());
    mWebview.setDownloadListener(this);
    mWebview.addJavascriptInterface(new WebAppInterface(this, mWebview), "android");

    if (Build.VERSION.SDK_INT >= 19) {
      mWebview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    } else if (Build.VERSION.SDK_INT >= 15 && Build.VERSION.SDK_INT < 19) {
      mWebview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    if (!TextUtils.isEmpty(getString(R.string.zoom))) {
      mWebview.getSettings().setSupportZoom(true);
      mWebview.getSettings().setBuiltInZoomControls(true);
      mWebview.getSettings().setDisplayZoomControls(false);
    }
    if (savedInstanceState != null) {
      mWebview.restoreState(savedInstanceState);
    } else {
      mWebview.loadUrl(target_url);
    }
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.back:
        int interRotation = PreferenceManager.getDefaultSharedPreferences(mContext).getInt("inter_rotation", 0);
        if (interRotation % 3 == 0) {
          admob.showInterstitialAd();
        }
        if (mWebview.canGoBack()) {
          mWebview.goBack();
        }
        admob.incrementInterCounter();
        break;
      case R.id.forward:
        if (mWebview.canGoForward()) {
          mWebview.goForward();
        }
        break;
      case R.id.refresh:
        mWebview.loadUrl(target_url);
        if (!show_content) {
          ProgressDialogHelper.showProgress(MainActivity.this);
        }
        break;
      case R.id.rating:
        Intent intent = new Intent(MainActivity.this, RateUsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        finish();
        break;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == FILE_CHOOSER_RESULT_CODE || requestCode == REQUEST_SELECT_FILE) {
      permissionSelectFile(requestCode, resultCode, data);
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    NotifyHelper.getInstance().deleteObserver(this);
    if (mWebview != null) {
      mWebview.destroy();
    }
    if (mWebviewPop != null) {
      mWebviewPop.destroy();
    }
    if (admob != null) {
      admob.stopRepeatingTask();
    }
  }

  @Override
  public void onBackPressed() {
    if (mWebview.canGoBack()) {
      mWebview.goBack();
    } else {
      super.onBackPressed();
    }
  }

  //This method will be called when the user will tap on allow or deny
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    //Checking the request code of our request
    if (requestCode == PermissionUtil.MY_PERMISSIONS_REQUEST_CALL) {
      //If permissionSelectFile is granted
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        UrlHander.call(MainActivity.this, urlData);
      }
    } else if (requestCode == PermissionUtil.MY_PERMISSIONS_REQUEST_SMS) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        UrlHander.sms(MainActivity.this, urlData);
      }
    } else if (requestCode == PermissionUtil.MY_PERMISSIONS_REQUEST_DOWNLOAD) {
      UrlHander.download(MainActivity.this, urlData, contentDisposition, mimeType);
    } else if (requestCode == PermissionUtil.MY_PERMISSIONS_REQUEST_GEOLOCATION) {
      if (geoLocationCallback != null) {
        geoLocationCallback.invoke(geoLocationOrigin, true, false);
      }
    }
  }

  @Override
  public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long l) {
    this.contentDisposition = contentDisposition;
    this.mimeType = mimeType;
    UrlHander.downloadLink(this, url, contentDisposition, mimeType);
  }

  private void setToolbarButtonColor() {
    if (showToolBar) {
      if (mWebview.canGoBack()) {
        mBack.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
        mGoBackLayout.setVisibility(View.GONE);
      } else {
        mBack.setColorFilter(ContextCompat.getColor(this, R.color.gray));
        mGoBackLayout.setVisibility(View.VISIBLE);
      }
      if (mWebview.canGoForward()) {
        mForward.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
      } else {
        mForward.setColorFilter(ContextCompat.getColor(this, R.color.gray));
      }
    }
  }

  private void showExitDialog() {
    LayoutInflater inflater = LayoutInflater.from(this);
    View view = inflater.inflate(R.layout.exit_dialog, null, false);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(true);
    builder.setView(view);
    mDialog = builder.create();
    view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mDialog.dismiss();
      }
    });
    view.findViewById(R.id.cancel_exit).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mDialog.dismiss();
        finish();
      }
    });
    mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
    mDialog.getWindow().setLayout(600, 400);
    mDialog.show();
  }


  @Override
  public void onRefresh() {
    mWebview.reload();
    mSwipeToRefresh.setRefreshing(false);
  }

  @Override
  public void update(Observable o, Object arg) {
    NotifyHelper.NotifyEvent notifyEvent = (NotifyHelper.NotifyEvent) arg;
    switch (notifyEvent.action) {
      case NotifyHelper.CONFIG_FETCHED:
        mConfig = (Config) notifyEvent.data;
        admob = new AdMob(this, mAdView, mConfig);
        admob.requestAdMob();
    }
  }


  private class UriWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      String host = Uri.parse(url).getHost();
      urlData = url;
      if (target_url_prefix.equals(host)) {
        if (mWebviewPop != null) {
          mWebviewPop.setVisibility(View.GONE);
          mContainer.removeView(mWebviewPop);
          mWebviewPop = null;
        }
        return false;
      }

      boolean result = UrlHander.checkUrl(MainActivity.this, url);
      if (result) {
        ProgressDialogHelper.dismissProgress();
      } else {
        currentUrl = url;
        if (!show_content) {
          ProgressDialogHelper.showProgress(MainActivity.this);
        }
      }
      return result;
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
      if (!NetworkHandler.isNetworkAvailable(view.getContext())) {
        view.loadUrl("file:///android_asset/NoInternet.html");
      }
      hideStatusBar();
      ProgressDialogHelper.dismissProgress();
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      super.onReceivedError(view, errorCode, description, failingUrl);
      if (!NetworkHandler.isNetworkAvailable(view.getContext())) {
        view.loadUrl("file:///android_asset/NoInternet.html");
      }
      hideStatusBar();
      ProgressDialogHelper.dismissProgress();
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
      super.onReceivedHttpError(view, request, errorResponse);
      if (!NetworkHandler.isNetworkAvailable(view.getContext())) {
        view.loadUrl("file:///android_asset/NoInternet.html");
      }
      hideStatusBar();
      ProgressDialogHelper.dismissProgress();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);
      showContent();
      setToolbarButtonColor();
      hideStatusBar();
      ProgressDialogHelper.dismissProgress();
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
      super.onPageCommitVisible(view, url);
      setToolbarButtonColor();
      hideStatusBar();
      ProgressDialogHelper.dismissProgress();
    }
  }

  class UriChromeClient extends WebChromeClient {

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog,
                                  boolean isUserGesture, Message resultMsg) {
      mWebviewPop = new WebView(mContext);
      mWebviewPop.setVerticalScrollBarEnabled(false);
      mWebviewPop.setHorizontalScrollBarEnabled(false);
      mWebviewPop.setWebViewClient(new UriWebViewClient());
      mWebviewPop.getSettings().setJavaScriptEnabled(true);
      mWebviewPop.getSettings().setSavePassword(false);

      mWebviewPop.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
      mWebviewPop.getSettings().setSupportMultipleWindows(true);
      mWebviewPop.getSettings().setGeolocationEnabled(true);
      mWebviewPop.getSettings().setDomStorageEnabled(true);
      mWebviewPop.getSettings().setDatabaseEnabled(true);
      mWebviewPop.getSettings().setGeolocationEnabled(true);
      mWebviewPop.getSettings().setGeolocationDatabasePath(getFilesDir().getPath());
      mWebviewPop.addJavascriptInterface(new WebAppInterface(MainActivity.this, mWebviewPop), "android");
      mWebviewPop.getSettings().setLoadWithOverviewMode(true);
      mWebviewPop.getSettings().setAllowFileAccess(true);
      mWebviewPop.getSettings().setUserAgentString("Android");

      if (TextUtils.isEmpty(getString(R.string.cache))) {
        mWebviewPop.getSettings().setAppCacheEnabled(true);
        mWebviewPop.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        mWebviewPop.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
      }

      mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT));
      mContainer.addView(mWebviewPop);
      mWebviewPop.setDownloadListener(MainActivity.this);

      if (Build.VERSION.SDK_INT >= 19) {
        mWebviewPop.setLayerType(View.LAYER_TYPE_HARDWARE, null);
      } else if (Build.VERSION.SDK_INT >= 15 && Build.VERSION.SDK_INT < 19) {
        mWebviewPop.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
      }

      WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
      transport.setWebView(mWebviewPop);
      resultMsg.sendToTarget();
      return true;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
      super.onProgressChanged(view, newProgress);

    }

    @Override
    public void onCloseWindow(WebView window) {
      Log.v("TEST", "onCloseWindow");
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(final String origin,
                                                   final GeolocationPermissions.Callback callback) {
      // Always grant permissionSelectFile since the app itself requires location
      // permissionSelectFile and the user has therefore already granted it
      MainActivity.this.geoLocationOrigin = origin;
      MainActivity.this.geoLocationCallback = callback;
      PermissionUtil.geoLocationPermission(MainActivity.this, origin, callback);
    }

    // openFileChooser for Android 3.0+
    protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
      mUploadMessage = uploadMsg;
      File imageStorageDir = new File(
          Environment.getExternalStoragePublicDirectory(
              Environment.DIRECTORY_PICTURES)
          , "AndroidExampleFolder");

      if (!imageStorageDir.exists()) {
        // Create AndroidExampleFolder at sdcard
        imageStorageDir.mkdirs();
      }

      // Create camera captured image file path and name
      File file = new File(
          imageStorageDir + File.separator + "IMG_"
              + String.valueOf(System.currentTimeMillis())
              + ".jpg");
      Log.d("File", "File: " + file);
      mCapturedImageURI = Uri.fromFile(file);

      // Camera capture image intent
      final Intent captureIntent = new Intent(
          android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

      captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

      Intent i = new Intent(Intent.ACTION_GET_CONTENT);
      i.addCategory(Intent.CATEGORY_OPENABLE);
      i.setType("image/*");

      // Create file chooser intent
      Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

      // Set camera intent to file chooser
      chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
          , new Parcelable[]{captureIntent});

      // On select image call onActivityResult method of activity
      startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);


    }

    // For Lollipop 5.0+ Devices
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback,
                                     WebChromeClient.FileChooserParams fileChooserParams) {
      if (uploadMessage != null) {
        uploadMessage.onReceiveValue(null);
        uploadMessage = null;
      }

      uploadMessage = filePathCallback;

      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        // Create the File where the photo should go
        File photoFile = null;
        try {
          photoFile = createImageFile();
          takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
        } catch (IOException ex) {
          // Error occurred while creating the File
          Log.e("TEST", "Unable to create Image File", ex);
        }

        // Continue only if the File was successfully created
        if (photoFile != null) {
          mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
          takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
              Uri.fromFile(photoFile));
        } else {
          takePictureIntent = null;
        }
      }

      Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
      contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
      contentSelectionIntent.setType("image/*");

      Intent[] intentArray;
      if (takePictureIntent != null) {
        intentArray = new Intent[]{takePictureIntent};
      } else {
        intentArray = new Intent[0];
      }

      Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
      chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
      chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
      chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

      startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);

      return true;
    }

    private File createImageFile() throws IOException {
      // Create an image file name
      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      String imageFileName = "JPEG_" + timeStamp + "_";
      File storageDir = Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_PICTURES);
      File imageFile = File.createTempFile(
          imageFileName,  /* prefix */
          ".jpg",         /* suffix */
          storageDir      /* directory */
      );
      return imageFile;
    }

    // openFileChooser for Android < 3.0
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
      openFileChooser(uploadMsg, "");
    }

    //For Android 4.1 only
    protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
      openFileChooser(uploadMsg, acceptType);
    }

  }


  public void permissionSelectFile(int requestCode, int resultCode, Intent data) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

      if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessage == null) {
        super.onActivityResult(requestCode, resultCode, data);
        return;
      }

      Uri[] results = null;

      // Check that the response is a good one
      if (resultCode == Activity.RESULT_OK) {
        if (data == null) {
          // If there is not data, then we may have taken a photo
          if (mCameraPhotoPath != null) {
            results = new Uri[]{Uri.parse(mCameraPhotoPath)};
          }
        } else {
          String dataString = data.getDataString();
          if (dataString != null) {
            results = new Uri[]{Uri.parse(dataString)};
          }
        }
      }

      uploadMessage.onReceiveValue(results);
      uploadMessage = null;

    } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
      if (requestCode != FILE_CHOOSER_RESULT_CODE || mUploadMessage == null) {
        super.onActivityResult(requestCode, resultCode, data);
        return;
      }

      if (requestCode == FILE_CHOOSER_RESULT_CODE) {

        if (null == this.mUploadMessage) {
          return;

        }

        Uri result = null;

        try {
          if (resultCode != RESULT_OK) {

            result = null;

          } else {

            // retrieve from the private variable if the intent is null
            result = data == null ? mCapturedImageURI : data.getData();
          }
        } catch (Exception e) {
          Toast.makeText(getApplicationContext(), "activity :" + e,
              Toast.LENGTH_LONG).show();
        }

        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;

      }
    }
  }
}

