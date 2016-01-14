package com.example.android.sunshine.app;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;


//reference : https://github.com/bourdibay/TestMessagesCommunicationWearPhone
public class ListenerService extends WearableListenerService {
    private String receivedString;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        receivedString = messageEvent.getPath();
        Toast.makeText(getApplicationContext(), "receive!", Toast.LENGTH_SHORT).show();
        savePreferences();
        Log.d("ListenerService", receivedString);
    }

    private void savePreferences(){
        SharedPreferences pref = getSharedPreferences("WEAR_RECEIVED_MESSAGE", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("MESSAGE", receivedString);
        editor.commit();
    }
}
