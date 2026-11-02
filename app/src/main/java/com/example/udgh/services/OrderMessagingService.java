package com.example.udgh.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.udgh.R;
import com.example.udgh.activities.ShipperOrderDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class OrderMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL = "new_orders";

    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        PushTokenManager.sync();
    }

    @Override public void onMessageReceived(RemoteMessage message) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        if (!"NEW_ORDER".equals(message.getData().get("type"))) return;
        String orderId = message.getData().get("orderId");
        if (orderId == null || orderId.isEmpty()) return;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Đơn hàng mới", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent intent = new Intent(this, ShipperOrderDetailActivity.class);
        intent.putExtra("orderId", orderId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, orderId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Có đơn hàng mới")
                .setContentText("Đơn " + orderId + " đang chờ nhận")
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        manager.notify(orderId.hashCode(), notification.build());
    }
}
