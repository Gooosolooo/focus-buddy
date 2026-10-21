package io.github.gooosolooo.focusbuddy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

/**
 * 悬浮窗进度条：前台服务 + WindowManager 叠加层
 */
public class FloatingTimerService extends Service {
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TOTAL = "total";
    private static final String CH = "timer";
    private static final int NOTI_ID = 200;

    private WindowManager wm;
    private WindowManager.LayoutParams lp;
    private LinearLayout root;
    private TextView tvTitle, tvTime;
    private ProgressBar bar;
    private long startAt, totalMs;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String title = intent.getStringExtra(EXTRA_TITLE);
        totalMs = intent.getLongExtra(EXTRA_TOTAL, 60 * 60000L);
        startAt = System.currentTimeMillis();   // 开始计时的时刻

        startAsForeground();
        if (root == null) buildOverlay();
        tvTitle.setText(title == null || title.isEmpty() ? "专注中" : title);
        bar.setProgressTintList(ColorStateList.valueOf(0xFF4FC3A8));

        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_NOT_STICKY;
    }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CH, "悬浮计时", NotificationManager.IMPORTANCE_LOW));
        }
        PendingIntent open = PendingIntent.getActivity(this, 3,
            new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("悬浮计时进行中")
            .setContentText("点这里回到 app")
            .setOngoing(true)
            .setContentIntent(open)
            .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTI_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTI_ID, n);
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void buildOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6202B27);
        bg.setCornerRadius(dp(16));
        root.setBackground(bg);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        tvTitle = new TextView(this);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(14);
        tvTitle.setSingleLine(true);
        top.addView(tvTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(0xFFB8C7C0);
        close.setTextSize(20);
        close.setPadding(dp(10), 0, 0, 0);
        close.setOnClickListener(v -> stopSelf());
        top.addView(close);
        root.addView(top);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
        blp.topMargin = dp(8);
        root.addView(bar, blp);

        tvTime = new TextView(this);
        tvTime.setTextColor(0xFFB8C7C0);
        tvTime.setTextSize(12);
        tvTime.setPadding(0, dp(6), 0, 0);
        root.addView(tvTime);

        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(dp(220),
            WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = dp(24);
        lp.y = dp(160);

        // 拖动
        root.setOnTouchListener(new View.OnTouchListener() {
            int x0, y0;
            float tx, ty;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x0 = lp.x;
                        y0 = lp.y;
                        tx = e.getRawX();
                        ty = e.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        lp.x = x0 + (int) (e.getRawX() - tx);
                        lp.y = y0 + (int) (e.getRawY() - ty);
                        wm.updateViewLayout(root, lp);
                        return true;
                }
                return false;
            }
        });
        wm.addView(root, lp);
    }

    private void update() {
        long el = System.currentTimeMillis() - startAt;     // 已过时间 = 当前时间 - 开始时间
        float r = Math.min(1f, (float) el / totalMs);
        bar.setProgress((int) (r * 1000));
        if (el > totalMs) {
            bar.setProgressTintList(ColorStateList.valueOf(0xFFF0A070));
            tvTime.setText("已超时 " + fmt(el - totalMs));
        } else {
            tvTime.setText(fmt(el) + " / " + fmt(totalMs) + "  (" + Math.round(r * 100) + "%)");
        }
    }

    private static String fmt(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format("%02d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        if (root != null && wm != null) {
            try {
                wm.removeView(root);
            } catch (Exception ignored) {}
            root = null;
        }
        super.onDestroy();
    }
}
