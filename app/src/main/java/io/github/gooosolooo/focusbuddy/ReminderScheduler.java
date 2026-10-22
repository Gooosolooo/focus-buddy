package io.github.gooosolooo.focusbuddy;

import android.app.AlarmManager;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class ReminderScheduler {
    /** 弹窗携带的提醒类型，true 表示开工核对。 */
    static final String EXTRA_REVIEW = "review";
    /** 开工核对稍后提醒的专用广播动作。 */
    static final String ACTION_REVIEW_SNOOZE = "io.github.gooosolooo.focusbuddy.REVIEW_SNOOZE";
    /** 稍后提醒的延迟，单位毫秒。 */
    private static final long REVIEW_SNOOZE_DELAY_MS = 10 * 60_000L;
    /** 独立于周期提醒的稍后提醒请求码。 */
    private static final int REVIEW_SNOOZE_REQUEST_CODE = 4;
    /** 开工核对通知编号，休息提醒使用下一个编号。 */
    private static final int REMINDER_NOTIFICATION_ID = 100;
    private static final String REVIEW_SNOOZE_AT = "review_snooze_at";
    /** 未保存开始时间时使用的默认整点小时。 */
    static final int DEFAULT_START_HOUR = 14;
    /** 未保存结束时间时使用的默认整点小时。 */
    static final int DEFAULT_END_HOUR = 17;
    /** 未保存提醒间隔时使用的默认分钟数。 */
    static final int DEFAULT_INTERVAL_MINUTES = 60;
    /** 提醒间隔允许的最小分钟数。 */
    static final int MIN_INTERVAL_MINUTES = 5;
    /** 提醒间隔允许的最大分钟数。 */
    static final int MAX_INTERVAL_MINUTES = 600;
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
        int sh = p.getInt("sh", DEFAULT_START_HOUR);
        int eh = p.getInt("eh", DEFAULT_END_HOUR);
        int iv = p.getInt("iv", DEFAULT_INTERVAL_MINUTES);

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
        scheduleAlarm(am, at, pi);
        return at;
    }

    private static void scheduleAlarm(AlarmManager manager, long at, PendingIntent intent) {
        if (manager.canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent);
                return;
            } catch (SecurityException ignored) {
                // 权限可能在检查后被撤销，继续使用允许空闲模式的普通闹钟。
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent);
    }

    private static PendingIntent reviewSnoozeIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ACTION_REVIEW_SNOOZE);
        return PendingIntent.getBroadcast(context, REVIEW_SNOOZE_REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** 延后核对提醒，不改变正常的休息提醒周期，也不重复记录快照。 */
    static void snoozeReview(Context context) {
        long at = System.currentTimeMillis() + REVIEW_SNOOZE_DELAY_MS;
        prefs(context).edit().putLong(REVIEW_SNOOZE_AT, at).apply();
        restoreReviewSnooze(context);
    }

    static boolean hasReviewSnooze(Context context) {
        return prefs(context).getLong(REVIEW_SNOOZE_AT, 0L) > 0;
    }

    /** 手机重启后恢复尚未确认的稍后提醒，过期的提醒会尽快触发。 */
    static void restoreReviewSnooze(Context context) {
        long at = prefs(context).getLong(REVIEW_SNOOZE_AT, 0L);
        if (at > 0) {
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            scheduleAlarm(manager, at, reviewSnoozeIntent(context));
        }
    }

    static void cancelReviewSnooze(Context context) {
        prefs(context).edit().remove(REVIEW_SNOOZE_AT).apply();
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(reviewSnoozeIntent(context));
    }

    static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1,
            new Intent(c, ReminderReceiver.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
        cancelReviewSnooze(c);
        dismissNotification(c, true);
        dismissNotification(c, false);
    }

    static void notifyNow(Context c) {
        notifyNow(c, false);
    }

    /**
     * 优先展示确认弹窗；缺少后台弹窗权限时，通知点击后进入同一确认弹窗。
     */
    static void notifyNow(Context c, boolean review) {
        if (c instanceof Activity || Settings.canDrawOverlays(c)) {
            try {
                c.startActivity(dialogIntent(c, review));
                dismissNotification(c, review);
                return;
            } catch (SecurityException ignored) {
                // 后台启动被系统拒绝时保留可点击的提醒入口。
            }
        }
        showFallbackNotification(c, review);
    }

    private static Intent dialogIntent(Context context, boolean review) {
        return new Intent(context, ReminderDialogActivity.class)
            .setData(Uri.parse("focusbuddy://reminder/" + (review ? "review" : "break")))
            .putExtra(EXTRA_REVIEW, review)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    static void dismissNotification(Context context, boolean review) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(REMINDER_NOTIFICATION_ID + (review ? 0 : 1));
    }

    private static void showFallbackNotification(Context c, boolean review) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, c.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH));
        PendingIntent pi = PendingIntent.getActivity(c, review ? 2 : 5, dialogIntent(c, review),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(c.getString(review ? R.string.reminder_review_title : R.string.reminder_rest_title))
            .setContentText(c.getString(review ? R.string.reminder_review_message : R.string.reminder_break_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi);
        try {
            nm.notify(REMINDER_NOTIFICATION_ID + (review ? 0 : 1), b.build());
        } catch (SecurityException ignored) {}
    }
}
