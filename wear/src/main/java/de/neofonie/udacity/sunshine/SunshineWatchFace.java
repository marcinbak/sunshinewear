/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.neofonie.udacity.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.*;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault());

  /**
   * Update rate in milliseconds for interactive mode. We update once a second since seconds are
   * displayed in interactive mode.
   */
  private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

  /**
   * Handler message id for updating the time periodically in interactive mode.
   */
  private static final int MSG_UPDATE_TIME = 0;

  @Override
  public Engine onCreateEngine() {
    return new Engine();
  }

  private static class EngineHandler extends Handler {
    private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

    public EngineHandler(SunshineWatchFace.Engine reference) {
      mWeakReference = new WeakReference<>(reference);
    }

    @Override
    public void handleMessage(Message msg) {
      SunshineWatchFace.Engine engine = mWeakReference.get();
      if (engine != null) {
        switch (msg.what) {
          case MSG_UPDATE_TIME:
            engine.handleUpdateTimeMessage();
            break;
        }
      }
    }
  }

  private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {
    final Handler mUpdateTimeHandler = new EngineHandler(this);
    boolean mRegisteredTimeZoneReceiver = false;
    boolean  mAmbient;
    Calendar mCalendar;
    final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        mCalendar.setTimeZone(TimeZone.getDefault());
        invalidate();
      }
    };

    private GoogleApiClient mGoogleApiClient;
    private Integer mWeather = null;
    private String  mTempMax = null;
    private String  mTempMin = null;

    /**
     * Whether the display supports fewer bits for each color in ambient mode. When true, we
     * disable anti-aliasing in ambient mode.
     */
    boolean mLowBitAmbient;
    private int      specW;
    private int      specH;
    private int      mPrimaryDark;
    private View     myLayout;
    private TextView mTimeTv, mDateTv, mTempMaxTv, mTempMinTv, mIconTv;
    private ColorFilter mBlackAndWhiteFilter;

    @Override
    public void onCreate(SurfaceHolder holder) {
      super.onCreate(holder);

      setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
          .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
          .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
          .setShowSystemUiTime(false)
          .setAcceptsTapEvents(true)
          .build());
      Resources resources = SunshineWatchFace.this.getResources();

      mCalendar = Calendar.getInstance();
      DATE_FORMAT.setTimeZone(mCalendar.getTimeZone());

      Point displaySize = new Point();
      LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      myLayout = inflater.inflate(R.layout.watchface, null);
      Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
      display.getSize(displaySize);

      mPrimaryDark = getColor(R.color.colorPrimaryDark);

      specW = View.MeasureSpec.makeMeasureSpec(displaySize.x, View.MeasureSpec.EXACTLY);
      specH = View.MeasureSpec.makeMeasureSpec(displaySize.y, View.MeasureSpec.EXACTLY);

      mTimeTv = (TextView) myLayout.findViewById(R.id.time_tv);
      mDateTv = (TextView) myLayout.findViewById(R.id.date_tv);
      mTempMaxTv = (TextView) myLayout.findViewById(R.id.temp_max_tv);
      mIconTv = (TextView) myLayout.findViewById(R.id.weather_icon_tv);
      mTempMinTv = (TextView) myLayout.findViewById(R.id.temp_min_tv);

      ColorMatrix matrix = new ColorMatrix();
      matrix.setSaturation(0);
      mBlackAndWhiteFilter = new ColorMatrixColorFilter(matrix);

      mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFace.this)
          .addApi(Wearable.API)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .build();
    }

    @Override
    public void onDestroy() {
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      releaseGoogleApiClient();
      super.onDestroy();
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
      super.onVisibilityChanged(visible);

      if (visible) {
        registerReceiver();
        mGoogleApiClient.connect();
        // Update time zone in case it changed while we weren't visible.
        mCalendar.setTimeZone(TimeZone.getDefault());
        invalidate();
      } else {
        unregisterReceiver();
        releaseGoogleApiClient();
      }

      // Whether the timer should be running depends on whether we're visible (as well as
      // whether we're in ambient mode), so we may need to start or stop the timer.
      updateTimer();
    }

    private void releaseGoogleApiClient() {
      if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
      }
    }

    private void registerReceiver() {
      if (mRegisteredTimeZoneReceiver) {
        return;
      }
      mRegisteredTimeZoneReceiver = true;
      IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
      SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
    }

    private void unregisterReceiver() {
      if (!mRegisteredTimeZoneReceiver) {
        return;
      }
      mRegisteredTimeZoneReceiver = false;
      SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
    }

    @Override
    public void onPropertiesChanged(Bundle properties) {
      super.onPropertiesChanged(properties);
      mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
    }

    @Override
    public void onTimeTick() {
      super.onTimeTick();
      invalidate();
    }

    @Override
    public void onAmbientModeChanged(boolean inAmbientMode) {
      super.onAmbientModeChanged(inAmbientMode);
      if (mAmbient != inAmbientMode) {
        mAmbient = inAmbientMode;
        if (!inAmbientMode) {
          myLayout.setBackgroundColor(mPrimaryDark);
          if (mIconTv.getCompoundDrawables()[0] != null) {
            mIconTv.getCompoundDrawables()[0].setColorFilter(null);
          }
        } else {
          myLayout.setBackgroundColor(Color.BLACK);
          if (mIconTv.getCompoundDrawables()[0] != null) {
            mIconTv.getCompoundDrawables()[0].setColorFilter(mBlackAndWhiteFilter);
          }
        }
        if (mLowBitAmbient) {
          mDateTv.getPaint().setAntiAlias(!inAmbientMode);
          mTimeTv.getPaint().setAntiAlias(!inAmbientMode);
          mTempMinTv.getPaint().setAntiAlias(!inAmbientMode);
          mTempMaxTv.getPaint().setAntiAlias(!inAmbientMode);
          mIconTv.setVisibility(inAmbientMode ? View.INVISIBLE : View.VISIBLE);
        }
        invalidate();
      }

      // Whether the timer should be running depends on whether we're visible (as well as
      // whether we're in ambient mode), so we may need to start or stop the timer.
      updateTimer();
    }

    @Override
    public void onDraw(Canvas canvas, Rect bounds) {
      // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
      long now = System.currentTimeMillis();
      mCalendar.setTimeInMillis(now);

      mDateTv.setText(DATE_FORMAT.format(mCalendar.getTime()));
      mTimeTv.setText(String.format("%d:%02d", mCalendar.get(Calendar.HOUR), mCalendar.get(Calendar.MINUTE)));

      if (mTempMax != null) {
        mTempMaxTv.setText(mTempMax);
      }

      if (mTempMin != null) {
        mTempMinTv.setText(mTempMin);
      }

      if (mWeather != null) {
        if (isInAmbientMode()) {
          int stringResId = IconUtils.getStringResourceIdForWeatherCondition(mWeather);
          mIconTv.setText(stringResId);
          mIconTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        } else {
          int iconResId = IconUtils.getArtResourceIdForWeatherCondition(mWeather);
          mIconTv.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
          mIconTv.setText(null);
        }

      }

      myLayout.measure(specW, specH);
      myLayout.layout(0, 0, myLayout.getMeasuredWidth(), myLayout.getMeasuredHeight());

      myLayout.draw(canvas);
    }

    /**
     * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
     * or stops it if it shouldn't be running but currently is.
     */
    private void updateTimer() {
      mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
      if (shouldTimerBeRunning()) {
        mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
      }
    }

    /**
     * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
     * only run when we're visible and in interactive mode.
     */
    private boolean shouldTimerBeRunning() {
      return isVisible() && !isInAmbientMode();
    }

    /**
     * Handle updating the time periodically in interactive mode.
     */
    private void handleUpdateTimeMessage() {
      invalidate();
      if (shouldTimerBeRunning()) {
        long timeMs = System.currentTimeMillis();
        long delayMs = INTERACTIVE_UPDATE_RATE_MS
            - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
        mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
      }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
      Log.i("WatchFace", "onConnected");
      Wearable.DataApi.addListener(mGoogleApiClient, this);
      Wearable.DataApi.getDataItems(mGoogleApiClient)
          .setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(@NonNull DataItemBuffer dataItems) {
              Iterator<DataItem> iter = dataItems.iterator();
              while (iter.hasNext()) {
                updateData(iter.next());
              }
            }
          });
    }

    @Override
    public void onConnectionSuspended(int i) {
      Log.i("WatchFace", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
      Log.i("WatchFace", "onConnectionFailed");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
      for (DataEvent event : dataEventBuffer) {
        if (event.getType() == DataEvent.TYPE_CHANGED) {
          updateData(event.getDataItem());
        }
      }
    }

    private void updateData(DataItem item) {
      if (item.getUri().getPath().compareTo("/weather-info") == 0) {
        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
        mWeather = dataMap.getInt("weather");
        mTempMax = getString(R.string.temperature, Math.round(dataMap.getDouble("temp-max")));
        mTempMin = getString(R.string.temperature, Math.round(dataMap.getDouble("temp-min")));
      }
    }
  }

}
