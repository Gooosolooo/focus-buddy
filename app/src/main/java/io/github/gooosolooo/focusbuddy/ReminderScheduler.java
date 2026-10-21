package io.github.gooosolooo.focusbuddy;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class ReminderScheduler {
    static final String CHANNEL = "reminder";

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("cfg", Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c) {
        return prefs(c).getBoolean("on", false);
    }

    /**
     * 开始/结束按"整点"算：startHour <= 当前小时 < endHour
     */
    static boolean inWindow(Calendar t, int sh, int eh) {
        int h = t.get(Calendar.HOUR_OF_DAY);
        return sh <= eh ? (h >= sh && h < eh) : (h >= sh || h < eh);
    }

    /**
     * 计算下一次触发时间。firstRun=true 时，若当前在时段内则 1 分钟后触发，方便立刻看到效果
     */
    static long computeNext(Context c, boolean firstRun) {
        SharedPreferences p = prefs(c);
        int sh = p.getInt("sh", 14), eh = p.getInt("eh", 17), iv = p.getInt("iv", 30);

        Calendar now = Calendar.getInstance();
        Calendar t = (Calendar) now.clone();
        t.add(Calendar.MINUTE, firstRun ? 1 : iv);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (inWindow(t, sh, eh)) {
            return t.getTimeInMillis();
        }

        // 否则排到下一个时段的开始
        Calendar s = (Calendar) now.clone();
        s.set(Calendar.HOUR_OF_DAY, sh);
        s.set(Calendar.MINUTE, 0);
        s.set(Calendar.SECOND, 0);
        s.set(Calendar.MILLISECOND, 0);
        if (!s.after(now)) {
            s.add(Calendar.DAY_OF_MONTH, 1);
        }
        return s.getTimeInMillis();
    }

    static long scheduleNext(Context c, boolean firstRun) {
        long at = computeNext(c, firstRun);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1,
            new Intent(c, ReminderReceiver.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);   // 没授权精确闹钟：允许有几分钟误差
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        return at;
    }

    static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1,
            new Intent(c, ReminderReceiver.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
    }

    static void notifyNow(Context c) {
        notifyNow(c, false);
    }

    /**
     * review=true：每天开工时刻的那条，点开后进入"核对进度"模式
     */
    static void notifyNow(Context c, boolean review) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "待办提醒", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent open = new Intent(c, MainActivity.class);
        open.putExtra("review", review);
        PendingIntent pi = PendingIntent.getActivity(c, 2, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(review ? "新的一天，先核对一下进度" : "该开始了")
            .setContentText(review ? "点开拖一拖各待办的进度条" : "选一个待办，现在就开始吧")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi);
        try {
            nm.notify(100, b.build());
        } catch (SecurityException ignored) {}
    }
}
