package com.example.android.sunshine.app.sync;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.TimeUnit;

//reference https://github.com/bourdibay/TestMessagesCommunicationWearPhone
public class MessageSender {
    private static final long CONNECTION_TIME_OUT = 1; // seconds

    private GoogleApiClient mGoogleApiClient;
    private String nodeID = null;

    public MessageSender(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
        getNodeID();
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
