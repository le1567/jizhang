package com.le1567.spring;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class FestivalReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int year = intent.getIntExtra("year", 2027);
        int kind = intent.getIntExtra("kind", 1);
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    "festival", "节日提醒", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("除夕与春节的守岁提醒");
            ch.enableLights(true);
            ch.setLightColor(0xFFE0B36A);
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(ctx, 100 + kind, open, flags);

        String title, body;
        if (kind == 0) {
            title = "今夜除夕 · 点灯守岁";
            body = "爆竹声中一岁除，明天就是 " + year + " 年春节。";
        } else {
            title = "新年快乐 · " + year + " 新春";
            body = "千门万户曈曈日，点开收下这一年的祝福。";
        }

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(ctx, "festival")
                : new Notification.Builder(ctx);
        b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setColor(0xFFE0473A);

        try {
            nm.notify(year * 10 + kind, b.build());
        } catch (Exception ignored) { }
    }
}
