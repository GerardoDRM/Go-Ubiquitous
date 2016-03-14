package com.udacity.gerardo.app;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.udacity.gerardo.app.data.WeatherContract;

// Code reference from http://developer.android.com/training/wearables/data-layer/data-items.html
public class WatchFaceService extends IntentService implements
       GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String KEY_PATH = "/wearable-weather";
    private static final String INDEX_WEATHER_ID = "KEY_WEATHER_ID";
    private static final String INDEX_MAX_TEMP = "KEY_MAX_TEMP";
    private static final String INDEX_MIN_TEMP = "KEY_MIN_TEMP";
    public static final String UPDATE_WATCHFACE = "UPDATE_WATCHFACE";
    private GoogleApiClient mGoogleApiClient;

    public WatchFaceService() {
        super("WatchFaceService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mGoogleApiClient = new GoogleApiClient.Builder(com.udacity.gerardo.app.WatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        // Get today's data from the ContentProvider
        String location = Utility.getPreferredLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor data = getContentResolver().query(weatherForLocationUri, new String[]{WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP}, null,
                null, null);
        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        // Fetch the cursor and send to the WatchFace the extracted weather by the DataApi
        if (data.moveToFirst()) {
            int weatherId = data.getInt(data.getColumnIndex(
                    WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            String maxTemp = Utility.formatTemperature(this, data.getDouble(
                    data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)));
            String minTemp = Utility.formatTemperature(this, data.getDouble(
                    data.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)));

            PutDataMapRequest mapRequest = PutDataMapRequest.create(KEY_PATH);
            mapRequest.getDataMap().putInt(INDEX_WEATHER_ID, weatherId);
            mapRequest.getDataMap().putString(INDEX_MAX_TEMP, maxTemp);
            mapRequest.getDataMap().putString(INDEX_MIN_TEMP, minTemp);
            mapRequest.setUrgent();

            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, mapRequest.asPutDataRequest());

            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(final DataApi.DataItemResult result) {
                    if(result.getStatus().isSuccess()) {
                        Log.d("Success", "Data item set: " + result.getDataItem().getUri());
                    }
                }
            });
        }
        data.close();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
