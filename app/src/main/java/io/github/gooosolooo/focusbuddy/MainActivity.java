package io.github.gooosolooo.focusbuddy;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntConsumer;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_EXPORT = 11, REQ_IMPORT = 12;

    private int tab = 0;                                   // 0 待办 / 1 已完成 / 2 设置
    private boolean review = false;                        // 是否处于"开工核对"模式
    private final Set<String> expanded = new HashSet<>();  // 当前展开的待办
    private LinearLayout body;
    private final Button[] tabBtns = new Button[3];

    private EditText etStart, etEnd, etIv;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);

        TextView title = Ui.text(this, "FocusBuddy", 24, Ui.INK, true);
        title.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 6));
        root.addView(title);

        LinearLayout tabs = Ui.row(this);
        tabs.setPadding(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 8));
        String[] names = {"待办", "已完成", "设置"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            Button b = Ui.button(this, names[i], Ui.SAND, v -> { tab = idx; render(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            p.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            tabs.addView(b, p);
            tabBtns[i] = b;
        }
        root.addView(tabs);

        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 24));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        // 从"开工核对"通知点进来：展开所有未归档的待办，方便一次拖完
        if (getIntent() != null && getIntent().getBooleanExtra("review", false)) {
            review = true;
            for (Todo t : TodoStore.all(this)) if (t.completedAt == 0) expanded.add(t.id);
        }
        render();
    }

    // ================= 总渲染 =================
    private void render() {
        int doneN = 0;
        for (Todo t : TodoStore.all(this)) if (t.completedAt > 0) doneN++;
        tabBtns[1].setText(doneN > 0 ? "已完成 " + doneN : "已完成");
        for (int i = 0; i < 3; i++) Ui.setFill(tabBtns[i], i == tab ? Ui.SAGE : Ui.SAND);

        body.removeAllViews();
        if (tab == 0) renderTodos();
        else if (tab == 1) renderDone();
        else renderSettings();
    }

    // ================= 待办页 =================
    private void renderTodos() {
        if (review) {
            LinearLayout rc = Ui.card(this);
            rc.addView(Ui.text(this, "开工核对", 17, Ui.INK, true));
            rc.addView(Ui.text(this, "拖一拖各个待办（或子项目）的进度条，对齐到现在的真实进度。已经替你存好了今天的起点快照。", 13, Ui.SUB, false), Ui.lp(this, 4));
            rc.addView(Ui.button(this, "核对完了", Ui.SAGE, v -> { review = false; render(); }), Ui.lp(this, 10));
            body.addView(rc, Ui.lp(this, 0));
        }

        LinearLayout add = Ui.card(this);
        add.setOrientation(LinearLayout.HORIZONTAL);
        add.setGravity(Gravity.CENTER_VERTICAL);
        EditText et = Ui.edit(this, "新待办，例如：简历完善", "", false);
        add.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button ab = Ui.button(this, "添加", Ui.SAGE, v -> {
            String s = et.getText().toString().trim();
            if (s.isEmpty()) return;
            Todo t = new Todo();
            t.title = s;
            TodoStore.all(this).add(0, t);
            TodoStore.save(this);
            render();
        });
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.leftMargin = Ui.dp(this, 10);
        add.addView(ab, ap);
        body.addView(add, Ui.lp(this, review ? 12 : 0));

        boolean any = false;
        for (Todo t : new ArrayList<>(TodoStore.all(this))) {
            if (t.completedAt > 0) continue;
            any = true;
            body.addView(todoCard(t), Ui.lp(this, 12));
        }
        if (!any) {
            TextView hint = Ui.text(this, "还没有待办，先添加一条吧", 14, Ui.SUB, false);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, Ui.dp(this, 40), 0, 0);
            body.addView(hint, Ui.lp(this, 0));
        }
    }

    private View todoCard(Todo t) {
        boolean open = expanded.contains(t.id);
        LinearLayout c = Ui.card(this);

        LinearLayout head = Ui.row(this);
        TextView title = Ui.text(this, (open ? "▾ " : "▸ ") + t.title, 17, Ui.INK, true);
        title.setOnClickListener(v -> {
            if (!expanded.add(t.id)) expanded.remove(t.id);
            render();
        });
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView pct = Ui.text(this, t.progress() + "%", 15, Ui.DEEP, true);
        head.addView(pct);
        c.addView(head);

        if (open && t.subs.isEmpty()) {
            // 最小一级且展开：可以拖动，10% 一档
            c.addView(seek(t.progress(), pct, val -> setTodoProgress(t, val)), Ui.lp(this, 4));
        } else {
            // 收起，或有子项目：只读进度条
            ProgressBar pb = Ui.bar(this, Ui.SAGE);
            pb.setProgress(t.progress());
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 8));
            pp.topMargin = Ui.dp(this, 10);
            c.addView(pb, pp);
        }

        String meta = t.subs.isEmpty() ? "无子项目" : t.doneCount() + "/" + t.subs.size() + " 个子项目已完成（总进度=子项目平均）";
        Integer base = TodoStore.todayBase(this, t.id);
        if (base != null && t.progress() > base) meta += " · 今天 +" + (t.progress() - base) + "%";
        c.addView(Ui.text(this, meta + (open ? "" : " · 点标题展开"), 12, Ui.SUB, false), Ui.lp(this, 6));

        if (open) addExpanded(c, t);
        return c;
    }

    private void addExpanded(LinearLayout c, Todo t) {
        for (Todo.Sub s : new ArrayList<>(t.subs)) {
            LinearLayout top = Ui.row(this);
            TextView tv = Ui.text(this, s.text, 15, s.progress >= 100 ? Ui.SUB : Ui.INK, false);
            if (s.progress >= 100) tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            top.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView sp = Ui.text(this, s.progress + "%", 13, Ui.DEEP, true);
            top.addView(sp);
            TextView x = Ui.text(this, "×", 22, Ui.SUB, false);
            x.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 4), 0);
            x.setOnClickListener(v -> removeSub(t, s));
            top.addView(x);
            c.addView(top, Ui.lp(this, 10));
            c.addView(seek(s.progress, sp, val -> setSubProgress(t, s, val)), Ui.lp(this, 0));
        }

        // 添加子项目
        LinearLayout addRow = Ui.row(this);
        EditText es = Ui.edit(this, "添加子项目", "", false);
        addRow.addView(es, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button sb = Ui.button(this, "添加", Ui.SAND, v -> {
            String txt = es.getText().toString().trim();
            if (txt.isEmpty()) return;
            Todo.Sub sub = new Todo.Sub();
            sub.text = txt;
            t.subs.add(sub);
            TodoStore.save(this);
            render();
        });
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbp.leftMargin = Ui.dp(this, 8);
        addRow.addView(sb, sbp);
        c.addView(addRow, Ui.lp(this, 12));

        // 悬浮计时
        LinearLayout tr = Ui.row(this);
        tr.addView(Ui.text(this, "计时(分钟)", 13, Ui.SUB, false));
        EditText em = Ui.edit(this, "60", "60", true);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(Ui.dp(this, 80), ViewGroup.LayoutParams.WRAP_CONTENT);
        mp.leftMargin = Ui.dp(this, 8);
        mp.rightMargin = Ui.dp(this, 8);
        tr.addView(em, mp);
        tr.addView(Ui.button(this, "开始悬浮计时", Ui.BLUE, v -> startFloating(t.title, num(em, 60, 1, 24 * 60))));
        c.addView(tr, Ui.lp(this, 12));

        Button del = Ui.button(this, "删除这个待办", Ui.ROSE, v ->
                new AlertDialog.Builder(this)
                        .setMessage("删除“" + t.title + "”？")
                        .setPositiveButton("删除", (d, w) -> {
                            TodoStore.all(this).remove(t);
                            TodoStore.save(this);
                            render();
                        })
                        .setNegativeButton("取消", null).show());
        c.addView(del, Ui.lp(this, 12));
    }

    /** 10% 一档的滑块：拖动中只更新数字，松手才提交 */
    private SeekBar seek(int pct, TextView label, IntConsumer onCommit) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(10);
        sb.setProgress(pct / 10);
        sb.setProgressTintList(ColorStateList.valueOf(Ui.DEEP));
        sb.setThumbTintList(ColorStateList.valueOf(Ui.DEEP));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.TRACK));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && label != null) label.setText((p * 10) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { onCommit.accept(s.getProgress() * 10); }
        });
        return sb;
    }

    private void setTodoProgress(Todo t, int val) {
        int prev = t.progress;
        if (val == prev) return;
        t.progress = val;
        afterChange(t, () -> t.progress = prev);
    }

    private void setSubProgress(Todo t, Todo.Sub s, int val) {
        int prev = s.progress;
        if (val == prev) return;
        s.progress = val;
        afterChange(t, () -> s.progress = prev);
    }

    private void removeSub(Todo t, Todo.Sub s) {
        int idx = t.subs.indexOf(s);
        t.subs.remove(s);
        afterChange(t, () -> t.subs.add(Math.min(idx, t.subs.size()), s));
    }

    /** 进度变化后统一处理：保存；到 100% 就归档，并给几秒钟的"撤销" */
    private void afterChange(Todo t, Runnable undo) {
        if (t.progress() == 100) {
            t.completedAt = System.currentTimeMillis();
            TodoStore.save(this);
            render();
            Snackbar.make(findViewById(android.R.id.content), "🎉 已归档到【已完成】", Snackbar.LENGTH_LONG)
                    .setAction("撤销", v -> {
                        undo.run();
                        t.completedAt = 0;
                        TodoStore.save(this);
                        render();
                    }).show();
        } else {
            TodoStore.save(this);
            render();
        }
    }

    // ================= 已完成页 =================
    private void renderDone() {
        List<Todo> list = new ArrayList<>();
        for (Todo t : TodoStore.all(this)) if (t.completedAt > 0) list.add(t);
        Collections.sort(list, (a, b) -> Long.compare(b.completedAt, a.completedAt));

        if (list.isEmpty()) {
            TextView hint = Ui.text(this, "还没有完成的事项。进度到 100% 会自动出现在这里", 14, Ui.SUB, false);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, Ui.dp(this, 40), 0, 0);
            body.addView(hint, Ui.lp(this, 0));
            return;
        }
        SimpleDateFormat f = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        for (Todo t : list) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, t.title, 17, Ui.INK, true));
            String sub = t.subs.isEmpty() ? "" : " · " + t.subs.size() + " 个子项目";
            c.addView(Ui.text(this, "完成于 " + f.format(new Date(t.completedAt)) + sub, 12, Ui.SUB, false), Ui.lp(this, 4));

            LinearLayout r = Ui.row(this);
            r.addView(Ui.button(this, "恢复到待办", Ui.SAGE, v -> {
                t.completedAt = 0;
                if (t.subs.isEmpty()) t.progress = 0;
                else for (Todo.Sub s : t.subs) s.progress = 0;   // 恢复时进度清零
                TodoStore.save(this);
                render();
            }));
            Button del = Ui.button(this, "删除", Ui.ROSE, v ->
                    new AlertDialog.Builder(this)
                            .setMessage("彻底删除“" + t.title + "”？")
                            .setPositiveButton("删除", (d, w) -> {
                                TodoStore.all(this).remove(t);
                                TodoStore.save(this);
                                render();
                            })
                            .setNegativeButton("取消", null).show());
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.leftMargin = Ui.dp(this, 8);
            r.addView(del, dlp);
            c.addView(r, Ui.lp(this, 10));
            body.addView(c, Ui.lp(this, 12));
        }
    }

    // ================= 设置页 =================
    private void renderSettings() {
        LinearLayout rc = Ui.card(this);
        rc.addView(Ui.text(this, "时段提醒", 18, Ui.INK, true));
        rc.addView(Ui.text(this, "每天时段开始的那一刻，会自动记录各待办的进度快照，并提醒你核对。", 13, Ui.SUB, false), Ui.lp(this, 4));
        SharedPreferences p = ReminderScheduler.prefs(this);
        etStart = labeledEdit(rc, "开始（整点，0-23）", String.valueOf(p.getInt("sh", 14)));
        etEnd = labeledEdit(rc, "结束（整点，0-23）", String.valueOf(p.getInt("eh", 17)));
        etIv = labeledEdit(rc, "每隔几分钟提醒一次", String.valueOf(p.getInt("iv", 30)));

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.leftMargin = Ui.dp(this, 8);
        LinearLayout row1 = Ui.row(this);
        row1.addView(Ui.button(this, "保存并开启", Ui.SAGE, v -> enable()));
        row1.addView(Ui.button(this, "关闭", Ui.SAND, v -> disable()), bp);
        rc.addView(row1, Ui.lp(this, 14));

        LinearLayout row2 = Ui.row(this);
        row2.addView(Ui.button(this, "测试通知", Ui.MAUVE, v -> ReminderScheduler.notifyNow(this)));
        row2.addView(Ui.button(this, "模拟开工核对", Ui.MAUVE, v -> {
            TodoStore.takeSnapshot(this);
            ReminderScheduler.notifyNow(this, true);
            Toast.makeText(this, "已存快照并发出核对通知", Toast.LENGTH_SHORT).show();
        }), bp);
        rc.addView(row2, Ui.lp(this, 8));

        tvStatus = Ui.text(this, "", 13, Ui.SUB, false);
        rc.addView(tvStatus, Ui.lp(this, 10));
        body.addView(rc, Ui.lp(this, 0));
        refreshStatus(-1);

        LinearLayout bc = Ui.card(this);
        bc.addView(Ui.text(this, "备份与恢复", 18, Ui.INK, true));
        bc.addView(Ui.text(this, "把所有待办（含子项目、已完成、每日快照）导出成一个 .json 文件，存到你选的位置。换手机或误删后，可以导入恢复。", 13, Ui.SUB, false), Ui.lp(this, 6));
        LinearLayout br = Ui.row(this);
        br.addView(Ui.button(this, "导出备份", Ui.BLUE, v -> doExport()));
        br.addView(Ui.button(this, "导入恢复", Ui.ROSE, v -> doImport()), bp);
        bc.addView(br, Ui.lp(this, 12));
        body.addView(bc, Ui.lp(this, 12));
    }

    private EditText labeledEdit(LinearLayout parent, String label, String value) {
        parent.addView(Ui.text(this, label, 13, Ui.SUB, false), Ui.lp(this, 10));
        EditText e = Ui.edit(this, "", value, true);
        parent.addView(e, Ui.lp(this, 4));
        return e;
    }

    // ================= 备份 =================
    private void doExport() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "focusbuddy-backup-" +
                new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".json");
        startActivityForResult(i, REQ_EXPORT);
    }

    private void doImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();

        if (req == REQ_EXPORT) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                os.write(TodoStore.exportJson(this).getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "备份成功", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "备份失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (req == REQ_IMPORT) {
            new AlertDialog.Builder(this)
                    .setMessage("导入会覆盖现有的所有待办，确定继续吗？")
                    .setPositiveButton("导入", (d, w) -> {
                        try (InputStream is = getContentResolver().openInputStream(uri)) {
                            ByteArrayOutputStream bo = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
                            String json = new String(bo.toByteArray(), StandardCharsets.UTF_8);
                            if (TodoStore.importJson(this, json)) {
                                Toast.makeText(this, "已恢复", Toast.LENGTH_SHORT).show();
                                tab = 0;
                                render();
                            } else {
                                Toast.makeText(this, "文件格式不对，没有导入", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "读取失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("取消", null).show();
        }
    }

    // ================= 悬浮计时 =================
    private void startFloating(String title, int minutes) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先允许“显示在其他应用的上层”，然后回来再点一次", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        Intent i = new Intent(this, FloatingTimerService.class);
        i.putExtra(FloatingTimerService.EXTRA_TITLE, title);
        i.putExtra(FloatingTimerService.EXTRA_TOTAL, minutes * 60_000L);
        startForegroundService(i);
    }

    // ================= 时段提醒 =================
    private int num(EditText et, int def, int min, int max) {
        try {
            int v = Integer.parseInt(et.getText().toString().trim());
            return Math.max(min, Math.min(max, v));
        } catch (Exception e) {
            return def;
        }
    }

    private void enable() {
        ReminderScheduler.prefs(this).edit()
                .putInt("sh", num(etStart, 14, 0, 23))
                .putInt("eh", num(etEnd, 17, 0, 23))
                .putInt("iv", num(etIv, 30, 5, 600))
                .putBoolean("on", true).apply();

        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            }
        }
        refreshStatus(ReminderScheduler.scheduleNext(this, true));
    }

    private void disable() {
        ReminderScheduler.prefs(this).edit().putBoolean("on", false).apply();
        ReminderScheduler.cancel(this);
        refreshStatus(-1);
    }

    private void refreshStatus(long at) {
        if (tvStatus == null) return;
        if (!ReminderScheduler.enabled(this)) {
            tvStatus.setText("提醒：已关闭");
        } else if (at > 0) {
            tvStatus.setText("提醒：已开启，下一次 " +
                    new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(at)));
        } else {
            tvStatus.setText("提醒：已开启");
        }
    }
}
