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
package com.example.android.sunshine;

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by marcinbak on 13/02/2017.
 */
public class SunshineApp extends Application implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  GoogleApiClient mGoogleApiClient;

  @Override
  public void onCreate() {
    super.onCreate();
    mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(Wearable.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build();
    mGoogleApiClient.connect();
  }

  public GoogleApiClient getGoogleApiClient() {
    return mGoogleApiClient;
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.i("SunshineApp", "Connected");
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.i("SunshineApp", "Connection suspended");
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.i("SunshineApp", "Connected failed");
  }
}
