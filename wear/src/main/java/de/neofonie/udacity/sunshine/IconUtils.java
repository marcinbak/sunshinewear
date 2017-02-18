/*
 * (c) Neofonie Mobile GmbH (2017)
 *
 * This computer program is the sole property of Neofonie Mobile GmbH (http://mobile.neofonie.de)
 * and is protected under the German Copyright Act (paragraph 69a UrhG).
 *
 * All rights are reserved. Making copies, duplicating, modifying, using or distributing
 * this computer program in any form, without prior written consent of Neofonie Mobile GmbH, is prohibited.
 * Violation of copyright is punishable under the German Copyright Act (paragraph 106 UrhG).
 *
 * Removing this copyright statement is also a violation.
 */
package de.neofonie.udacity.sunshine;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.Log;

/**
 * Created by marcinbak on 13/02/2017.
 */
public class IconUtils {

  private final static String LOG_TAG = "IconUtils";

  @DrawableRes
  public static int getArtResourceIdForWeatherCondition(int weatherId) {

    /*
     * Based on weather code data for Open Weather Map.
     */
    if (weatherId >= 200 && weatherId <= 232) {
      return R.drawable.ic_storm;
    } else if (weatherId >= 300 && weatherId <= 321) {
      return R.drawable.ic_light_rain;
    } else if (weatherId >= 500 && weatherId <= 504) {
      return R.drawable.ic_rain;
    } else if (weatherId == 511) {
      return R.drawable.ic_snow;
    } else if (weatherId >= 520 && weatherId <= 531) {
      return R.drawable.ic_rain;
    } else if (weatherId >= 600 && weatherId <= 622) {
      return R.drawable.ic_snow;
    } else if (weatherId >= 701 && weatherId <= 761) {
      return R.drawable.ic_fog;
    } else if (weatherId == 761 || weatherId == 771 || weatherId == 781) {
      return R.drawable.ic_storm;
    } else if (weatherId == 800) {
      return R.drawable.ic_clear;
    } else if (weatherId == 801) {
      return R.drawable.ic_light_clouds;
    } else if (weatherId >= 802 && weatherId <= 804) {
      return R.drawable.ic_cloudy;
    } else if (weatherId >= 900 && weatherId <= 906) {
      return R.drawable.ic_storm;
    } else if (weatherId >= 958 && weatherId <= 962) {
      return R.drawable.ic_storm;
    } else if (weatherId >= 951 && weatherId <= 957) {
      return R.drawable.ic_clear;
    }

    Log.e(LOG_TAG, "Unknown Weather: " + weatherId);
    return R.drawable.ic_storm;
  }

  @StringRes
  public static int getStringResourceIdForWeatherCondition(int weatherId) {

    /*
     * Based on weather code data for Open Weather Map.
     */
    if (weatherId >= 200 && weatherId <= 232) {
      return R.string.storm_label;
    } else if (weatherId >= 300 && weatherId <= 321) {
      return R.string.light_rain_label;
    } else if (weatherId >= 500 && weatherId <= 504) {
      return R.string.rain_label;
    } else if (weatherId == 511) {
      return R.string.snow_label;
    } else if (weatherId >= 520 && weatherId <= 531) {
      return R.string.rain_label;
    } else if (weatherId >= 600 && weatherId <= 622) {
      return R.string.snow_label;
    } else if (weatherId >= 701 && weatherId <= 761) {
      return R.string.fog_label;
    } else if (weatherId == 761 || weatherId == 771 || weatherId == 781) {
      return R.string.storm_label;
    } else if (weatherId == 800) {
      return R.string.clear_label;
    } else if (weatherId == 801) {
      return R.string.light_clouds_label;
    } else if (weatherId >= 802 && weatherId <= 804) {
      return R.string.cloudy_label;
    } else if (weatherId >= 900 && weatherId <= 906) {
      return R.string.storm_label;
    } else if (weatherId >= 958 && weatherId <= 962) {
      return R.string.storm_label;
    } else if (weatherId >= 951 && weatherId <= 957) {
      return R.string.clear_label;
    }

    Log.e(LOG_TAG, "Unknown Weather: " + weatherId);
    return R.string.storm_label;
  }
}
