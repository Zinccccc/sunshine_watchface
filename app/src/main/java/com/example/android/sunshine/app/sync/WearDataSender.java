package com.example.android.sunshine.app.sync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.concurrent.TimeUnit;

//reference https://github.com/bourdibay/TestMessagesCommunicationWearPhone
public class WearDataSender {
    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;
    private static final int INDEX_MAX_TEMP = 2;
    private static final int INDEX_MIN_TEMP = 3;

    private static final long CONNECTION_TIME_OUT = 1; // seconds

    private GoogleApiClient mGoogleApiClient;
    private String nodeID = null;

    Context context;

    public WearDataSender(Context c) {
        context = c;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
        getNodeID();
        getDataJson();
    }

    public String getDataJson(){
        Log.d(LOG_TAG, "getDataJson Called");
        String result;
        String location = Utility.getPreferredLocation(context);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor data = context.getContentResolver().query(weatherForLocationUri, FORECAST_COLUMNS, null,
                null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
        if (data == null) {
            return null;
        }
        if (!data.moveToFirst()) {
            data.close();
            return null;
        }
        // Extract the weather data from the Cursor
        int weatherId = data.getInt(INDEX_WEATHER_ID);
        double maxTemp = data.getDouble(INDEX_MAX_TEMP);
        double minTemp = data.getDouble(INDEX_MIN_TEMP);
        String formattedMaxTemperature = Utility.formatTemperature(context, maxTemp);
        String formattedMinTemperature = Utility.formatTemperature(context, minTemp);
        data.close();

        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray.put(0, weatherId);
            jsonArray.put(1, formattedMaxTemperature);
            jsonArray.put(2, formattedMinTemperature);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        result = jsonArray.toString();
        Log.d(LOG_TAG, "Json result : " + result);
        return result;
    }

    private void getNodeID() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConnectionResult connectionResult = mGoogleApiClient
                        .blockingConnect(CONNECTION_TIME_OUT, TimeUnit.SECONDS);
                if (connectionResult.isSuccess() == true) {
                    NodeApi.GetConnectedNodesResult result =
                            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    List<Node> nodes = result.getNodes();
                    if (nodes.size() > 0) {
                        nodeID = nodes.get(0).getId();
                    }
                    mGoogleApiClient.disconnect();
                }
            }
        }).start();
    }

    public void sendMessage(final String message) {
        if (nodeID != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT, TimeUnit.SECONDS);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeID, message, null);
                    mGoogleApiClient.disconnect();
                }
            }).start();
        }
    }
}
