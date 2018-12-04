package com.app.spokeo.peoplesearch.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Config {
  @SerializedName("APP_ID")
  @Expose
  private String admob_appId;
  @SerializedName("BANNER")
  @Expose
  private String admob_BannerId;
  @SerializedName("INTERSTITIAL")
  @Expose
  private String admob_InterId;
  @SerializedName("Rating")
  @Expose
  private boolean ratingUsed;
  @SerializedName("Rate_Text")
  @Expose
  private String rateText;
  @SerializedName("Rate_Poupup_Text")
  @Expose
  private String ratePopUpText;
  @SerializedName("Rating_Link")
  @Expose
  private String ratingLink;

  public String getAdmob_BannerId() {
    return admob_BannerId;
  }

  public String getAdmob_appId() {
    return admob_appId;
  }

  public String getAdmob_InterId() {
    return admob_InterId;
  }

  public boolean isRatingUsed() {
    return ratingUsed;
  }

  public String getRateText() {
    return rateText;
  }

  public String getRatePopUpText() {
    return ratePopUpText;
  }

  public String getRatingLink() {
    return ratingLink;
  }
}
