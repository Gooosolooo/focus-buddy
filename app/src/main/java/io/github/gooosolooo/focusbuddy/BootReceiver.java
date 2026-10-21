package io.github.gooosolooo.focusbuddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 手机重启后闹钟会丢失，这里重新排上
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        if (ReminderScheduler.enabled(c)) ReminderScheduler.scheduleNext(c, false);
    }
}
