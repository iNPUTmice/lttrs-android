package rs.ltt.android.service;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull final String token) {

    }

    @Override
    public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {

    }
}