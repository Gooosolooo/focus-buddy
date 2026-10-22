package io.github.gooosolooo.focusbuddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (ReminderScheduler.ACTION_REVIEW_SNOOZE.equals(action)) {
            if (ReminderScheduler.hasReviewSnooze(c)) {
                ReminderScheduler.cancelReviewSnooze(c);
                ReminderScheduler.notifyNow(c, true);
            }
            return;
        }
        if (action != null || !ReminderScheduler.enabled(c)) {
            return;
        }
        int sh = ReminderScheduler.prefs(c).getInt("sh", ReminderScheduler.DEFAULT_START_HOUR);
        int eh = ReminderScheduler.prefs(c).getInt("eh", ReminderScheduler.DEFAULT_END_HOUR);
        if (ReminderScheduler.inWindow(Calendar.getInstance(), sh, eh)) {
            if (!TodoStore.hasSnapshotToday(c)) {
                // 今天窗口内的第一次触发 = "开工时刻"：先存快照，再提醒你核对进度
                TodoStore.takeSnapshot(c);
                ReminderScheduler.cancelReviewSnooze(c);
                ReminderScheduler.notifyNow(c, true);
            } else if (!ReminderScheduler.hasReviewSnooze(c)) {
                ReminderScheduler.notifyNow(c, false);
            }
        }
        ReminderScheduler.scheduleNext(c, false);   // 链式：每次触发后排下一次
    }
}
