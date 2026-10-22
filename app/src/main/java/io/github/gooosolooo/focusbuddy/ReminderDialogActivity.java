package io.github.gooosolooo.focusbuddy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

/** 提醒确认入口：开工核对可稍后提醒，休息确认后启动五分钟悬浮计时。 */
public class ReminderDialogActivity extends AppCompatActivity {
    /** 休息计时时长，单位毫秒。 */
    private static final long REST_DURATION_MS = 5 * 60_000L;
    /** 休息计时申请悬浮窗权限的请求码。 */
    private static final int REQUEST_OVERLAY_PERMISSION = 1;
    private boolean review;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle state) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(state);
        setShowWhenLocked(true);
        review = getIntent().getBooleanExtra(ReminderScheduler.EXTRA_REVIEW, false);
        showReminder();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean incomingReview = intent.getBooleanExtra(ReminderScheduler.EXTRA_REVIEW, false);
        if (dialog != null && dialog.isShowing() && (review || !incomingReview)) {
            // 已展示的核对弹窗优先，不被周期休息提醒覆盖，也不堆叠同类弹窗。
            return;
        }
        setIntent(intent);
        review = incomingReview;
        showReminder();
    }

    private void showReminder() {
        if (dialog != null) {
            dialog.dismiss();
        }
        ReminderScheduler.dismissNotification(this, review);
        AppDialog.Builder builder = new AppDialog.Builder(this)
            .setTitle(review ? R.string.reminder_review_title : R.string.reminder_rest_title)
            .setMessage(review ? R.string.reminder_review_message : R.string.reminder_break_message)
            .setCancelable(false)
            .setPositiveAction(R.string.reminder_confirm, () -> {
                if (review) {
                    confirmReview();
                } else {
                    confirmRest();
                }
                // 授权未完成时保留弹窗，完成操作后由 Activity 的生命周期关闭。
                return false;
            });
        if (review) {
            builder.setNegativeButton(R.string.reminder_snooze, (view, which) -> {
                ReminderScheduler.snoozeReview(this);
                finish();
            });
        }
        dialog = builder.show();
    }

    private void confirmReview() {
        ReminderScheduler.cancelReviewSnooze(this);
        Intent open = new Intent(this, MainActivity.class)
            .putExtra(ReminderScheduler.EXTRA_REVIEW, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(open);
        finish();
    }

    private void confirmRest() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.reminder_overlay_required, Toast.LENGTH_LONG).show();
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())), REQUEST_OVERLAY_PERMISSION);
            return;
        }
        Intent timer = new Intent(this, FloatingTimerService.class)
            .putExtra(FloatingTimerService.EXTRA_TITLE, getString(R.string.reminder_rest_timer_title))
            .putExtra(FloatingTimerService.EXTRA_TOTAL, REST_DURATION_MS);
        startForegroundService(timer);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION && !review && Settings.canDrawOverlays(this)) {
            confirmRest();
        }
    }

    @Override
    protected void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
        }
        super.onDestroy();
    }
}
