package io.github.gooosolooo.focusbuddy;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Paint;
import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_EXPORT = 11, REQ_IMPORT = 12;
    /** 开启弹窗提醒前申请悬浮窗权限的请求码。 */
    private static final int REQ_REMINDER_OVERLAY = 13;
    /**
     * 父卡片文字、进度条和统计区域之间的间距，单位 dp。
     */
    private static final int CARD_CONTENT_GAP_DP = 8;
    /**
     * 父卡片上下内边距，单位 dp；不影响子项目之间的间距。
     */
    private static final int CARD_SECTION_GAP_DP = 16;
    /**
     * 父卡片文字行最小高度，单位 dp。
     */
    private static final int CARD_TEXT_ROW_HEIGHT_DP = 24;
    /**
     * 父卡片进度条区域高度，单位 dp。
     */
    private static final int CARD_PROGRESS_HEIGHT_DP = 20;
    /**
     * 子项目的水平内边距，单位 dp；只影响子项目，不影响父卡片。
     */
    private static final int SUB_ITEM_HORIZONTAL_PADDING_DP = 0;
    /**
     * 子项目的上下内边距，单位 dp。
     */
    private static final int SUB_ITEM_VERTICAL_PADDING_DP = 6;
    /**
     * 相邻子项目及子项目与统计区域的间距，单位 dp。
     */
    private static final int SUB_ITEM_GAP_DP = 6;
    /**
     * 子项目文字行最小高度，单位 dp；左侧手柄仍保留 48dp 点击区域。
     */
    private static final int SUB_ITEM_TEXT_ROW_HEIGHT_DP = 24;
    /**
     * 子项目进度条区域高度，单位 dp。
     */
    private static final int SUB_ITEM_PROGRESS_HEIGHT_DP = 20;
    /**
     * 子项目文字区域与进度条区域的间距，单位 dp。
     */
    private static final int SUB_ITEM_CONTENT_GAP_DP = 6;
    /**
     * 子项目上方分隔线与文字区域的间距，单位 dp。
     */
    private static final int SUB_ITEM_TOP_DIVIDER_GAP_DP = 12;
    /**
     * 分组末项进度条区域与下方分隔线的间距，单位 dp；与相邻子项目的双侧内边距一致。
     */
    private static final int SUB_ITEM_BOTTOM_DIVIDER_GAP_DP = 12;
    /**
     * 相邻子项目之间的浅灰分隔线厚度，单位 dp。
     */
    private static final int SUB_ITEM_DIVIDER_HEIGHT_DP = 1;
    /**
     * 设置页时间数值框高度，单位 dp。
     */
    private static final int REMINDER_VALUE_HEIGHT_DP = 32;
    /**
     * 设置页时间数值框的浅灰底色。
     */
    private static final int REMINDER_VALUE_BACKGROUND = 0xFFF0F0F0;
    /**
     * 分钟选择列表的步长，单位为分钟。
     */
    private static final int TIMER_MINUTES_PICKER_STEP_SIZE = 10;
    /**
     * 小时选择列表的步长，单位为小时。
     */
    private static final int REMINDER_HOUR_PICKER_STEP_SIZE = 1;

    private int tab = 0;                                   // 0 待办 / 1 已完成 / 2 设置
    private boolean review = false;                        // 是否处于"开工核对"模式
    private final Set<String> expanded = new HashSet<>();  // 当前展开的待办
    private final Set<String> completedSubsExpanded = new HashSet<>();
    private LinearLayout body;
    private final Button[] tabBtns = new Button[3];

    private int reminderStartHour;
    private int reminderEndHour;
    private int reminderIntervalMinutes;
    private TextView tvStatus;
    private LinearLayout timerTodoSelector;
    private TextView timerTodoLabel;
    private ImageButton timerTodoExpand;
    private Button timerMinutes;
    private int timerMinutesValue = 60;
    private Button timerStartButton;
    private String timerTodoId;
    private final List<Todo> timerTodos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);

        LinearLayout toolbar = Ui.row(this);
        toolbar.setPadding(Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 12), 0);
        TextView title = Ui.text(this, "FocusBuddy", 24, Ui.INK, true);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        toolbar.addView(Ui.iconButton(this, R.drawable.ic_info, Ui.DEEP,
            "操作指南", view -> OperationGuideView.show(this)));
        root.addView(toolbar);

        LinearLayout tabs = Ui.row(this);
        tabs.setPadding(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 8));
        String[] names = {"待办", "已完成", "设置"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            Button b = Ui.button(this, names[i], Ui.SAND, v -> {
                tab = idx;
                render();
            });
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
        root.addView(createTimerBar());
        setContentView(root);

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        applyReviewIntent(getIntent());
        render();
        openWidgetTodo(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (applyReviewIntent(intent)) {
            render();
        }
        openWidgetTodo(intent);
    }

    /** 无论应用是否已经打开，确认开工核对后均进入待办页并展开未归档项目。 */
    private boolean applyReviewIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(ReminderScheduler.EXTRA_REVIEW, false)) {
            return false;
        }
        tab = 0;
        review = true;
        for (Todo todo : TodoStore.all(this)) {
            if (todo.completedAt == 0) {
                expanded.add(todo.id);
            }
        }
        return true;
    }

    /** 从桌面组件进入时展开对应待办，并将其滚动到可见区域。 */
    private void openWidgetTodo(Intent intent) {
        String todoId = intent.getStringExtra(WidgetSupport.EXTRA_TODO_ID);
        if (todoId == null) {
            return;
        }
        intent.removeExtra(WidgetSupport.EXTRA_TODO_ID);
        Todo todo = WidgetSupport.findTodo(this, todoId);
        if (todo == null) {
            return;
        }
        tab = todo.completedAt > 0 ? 1 : 0;
        expanded.add(todoId);
        render();
        body.post(() -> {
            for (int index = 0; index < body.getChildCount(); index++) {
                View child = body.getChildAt(index);
                if (todoId.equals(child.getTag())) {
                    ((ScrollView) body.getParent()).smoothScrollTo(0, child.getTop());
                    break;
                }
            }
        });
    }

    // ================= 总渲染 =================
    private void render() {
        int doneN = 0;
        for (Todo t : TodoStore.all(this)) {
            if (t.completedAt > 0) {
                doneN++;
            }
        }
        tabBtns[1].setText(doneN > 0 ? getString(R.string.completed_tab_count, doneN)
            : getString(R.string.completed_tab));
        for (int i = 0; i < 3; i++) {
            Ui.setFill(tabBtns[i], i == tab ? Ui.SAGE : Ui.SAND);
        }

        body.removeAllViews();
        if (tab == 0) {
            renderTodos();
        } else if (tab == 1) {
            renderDone();
        } else {
            renderSettings();
        }
        refreshTimerTodos();
    }

    private View createTimerBar() {
        timerMinutesValue = WidgetSupport.lastMinutes(this);
        timerTodoId = WidgetSupport.lastTodoId(this);
        LinearLayout bar = Ui.row(this);
        bar.setBaselineAligned(false);
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        bar.setBackgroundColor(Ui.CARD);
        timerTodoSelector = Ui.row(this);
        GradientDrawable selectorBackground = Ui.round(Ui.SELECT_BG, Ui.dp(this, 12));
        selectorBackground.setStroke(Ui.dp(this, 1), Ui.SAGE);
        timerTodoSelector.setBackground(selectorBackground);
        timerTodoSelector.setPadding(0, 0, Ui.dp(this, 8), 0);
        timerTodoExpand = Ui.iconButton(this, R.drawable.ic_expand_down, Ui.DEEP,
            "选择计时待办", view -> showTimerTodoChooser());
        timerTodoSelector.addView(timerTodoExpand);
        timerTodoLabel = Ui.text(this, "暂无待办", 14, Ui.TITLE, false);
        timerTodoLabel.setSingleLine(true);
        timerTodoLabel.setEllipsize(TextUtils.TruncateAt.END);
        timerTodoSelector.addView(timerTodoLabel, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        timerTodoSelector.setOnClickListener(view -> showTimerTodoChooser());
        bar.addView(timerTodoSelector, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f));
        timerMinutes = Ui.button(this, getString(R.string.timer_minutes_value, timerMinutesValue),
            REMINDER_VALUE_BACKGROUND,
            view -> showTimerMinutesPicker());
        timerMinutes.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        timerMinutes.setContentDescription(getString(R.string.time_dropdown_description));
        LinearLayout durationGroup = Ui.row(this);
        durationGroup.setBaselineAligned(false);
        durationGroup.addView(timerMinutes, new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 48)));
        TextView unit = Ui.text(this, "分钟", 13, Ui.SUB, false);
        unit.setPadding(Ui.dp(this, 4), 0, 0, 0);
        durationGroup.addView(unit);
        LinearLayout.LayoutParams durationParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int componentSpacing = Ui.dp(this, 8);
        durationParams.setMargins(componentSpacing, 0, componentSpacing, 0);
        bar.addView(durationGroup, durationParams);
        timerStartButton = Ui.button(this, "悬浮计时开始", Ui.BLUE, view -> {
            for (Todo todo : timerTodos) {
                if (todo.id.equals(timerTodoId)) {
                    startFloating(todo.title, timerMinutesValue);
                    return;
                }
            }
        });
        timerStartButton.setTextSize(13);
        bar.addView(timerStartButton);
        return bar;
    }

    private void refreshTimerTodos() {
        timerTodos.clear();
        int selected = 0;
        for (Todo todo : TodoStore.all(this)) {
            if (todo.completedAt == 0) {
                if (todo.id.equals(timerTodoId)) {
                    selected = timerTodos.size();
                }
                timerTodos.add(todo);
            }
        }
        boolean available = !timerTodos.isEmpty();
        timerTodoId = available ? timerTodos.get(selected).id : null;
        timerTodoLabel.setText(available ? timerTodos.get(selected).title : "暂无待办");
        timerTodoSelector.setEnabled(available);
        timerTodoExpand.setEnabled(available);
        timerStartButton.setEnabled(available);
        timerStartButton.setAlpha(available ? 1f : 0.5f);
    }

    private void showTimerTodoChooser() {
        if (timerTodos.isEmpty()) {
            return;
        }
        List<Todo> choices = new ArrayList<>(timerTodos);
        String[] titles = new String[choices.size()];
        int selected = 0;
        for (int index = 0; index < choices.size(); index++) {
            Todo todo = choices.get(index);
            titles[index] = todo.title;
            if (todo.id.equals(timerTodoId)) {
                selected = index;
            }
        }
        ListView list = new ListView(this);
        list.setDivider(null);
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView label = (TextView) super.getView(position, convertView, parent);
                label.setTextSize(14);
                label.setTextColor(Ui.TITLE);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setMinHeight(Ui.dp(MainActivity.this, 48));
                return label;
            }
        });
        android.graphics.Rect visibleFrame = new android.graphics.Rect();
        timerTodoSelector.getWindowVisibleDisplayFrame(visibleFrame);
        int[] location = new int[2];
        timerTodoSelector.getLocationOnScreen(location);
        int popupHeight = Math.min(Ui.dp(this, 48) * Math.min(choices.size(), 5),
            Math.max(Ui.dp(this, 48), location[1] - visibleFrame.top));
        int popupWidth = Math.min(visibleFrame.width(),
            Math.max(Ui.dp(this, 200), timerTodoSelector.getWidth()));
        PopupWindow popup = new PopupWindow(list, popupWidth, popupHeight, true);
        popup.setBackgroundDrawable(Ui.round(Ui.CARD, Ui.dp(this, Ui.CARD_CORNER_RADIUS_DP)));
        popup.setElevation(Ui.dp(this, 8));
        popup.setOutsideTouchable(true);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Todo todo = choices.get(position);
            timerTodoId = todo.id;
            WidgetSupport.rememberTodo(this, todo.id);
            timerTodoLabel.setText(todo.title);
            popup.dismiss();
        });
        popup.showAsDropDown(timerTodoSelector, 0, -timerTodoSelector.getHeight() - popupHeight);
        list.setSelection(selected);
    }

    private void showTimerMinutesPicker() {
        // 悬浮计时最多2h
        showTimeDropdown(timerMinutes, timerMinutesValue, TIMER_MINUTES_PICKER_STEP_SIZE, 2 * 60,
            TIMER_MINUTES_PICKER_STEP_SIZE, true, value -> {
            timerMinutesValue = value;
            WidgetSupport.rememberMinutes(this, value);
            timerMinutes.setText(getString(R.string.timer_minutes_value, value));
        });
    }

    // ================= 待办页 =================
    private void renderTodos() {
        if (review) {
            LinearLayout rc = Ui.card(this);
            rc.addView(Ui.text(this, "开工核对", 17, Ui.INK, true));
            rc.addView(
                Ui.text(this, "拖一拖各个待办（或子项目）的进度条，对齐到现在的真实进度。已经替你存好了今天的起点快照。", 13,
                    Ui.SUB, false), Ui.lp(this, 4));
            rc.addView(Ui.button(this, "核对完毕", Ui.SAGE, v -> {
                review = false;
                render();
            }), Ui.lp(this, 10));
            body.addView(rc, Ui.lp(this, 0));
        }

        LinearLayout add = Ui.card(this);
        add.setOrientation(LinearLayout.HORIZONTAL);
        add.setGravity(Gravity.CENTER_VERTICAL);
        EditText et = Ui.edit(this, "新待办，例如：简历完善", "", false);
        add.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button ab = Ui.button(this, "添加", Ui.SAGE, v -> {
            String s = et.getText().toString().trim();
            if (s.isEmpty()) {
                return;
            }
            Todo t = new Todo();
            t.title = s;
            TodoStore.all(this).add(0, t);
            TodoStore.save(this);
            render();
        });
        LinearLayout.LayoutParams ap =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.leftMargin = Ui.dp(this, 10);
        add.addView(ab, ap);
        body.addView(add, Ui.lp(this, review ? 12 : 0));

        boolean any = false;
        for (Todo t : new ArrayList<>(TodoStore.all(this))) {
            if (t.completedAt > 0) {
                continue;
            }
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
        c.setTag(t.id);
        c.setPadding(Ui.dp(this, 16), Ui.dp(this, CARD_SECTION_GAP_DP),
            Ui.dp(this, 16), Ui.dp(this, CARD_SECTION_GAP_DP));
        c.setOnLongClickListener(v -> {
            confirmDeleteTodo(t);
            return true;
        });   // 长按：删除

        LinearLayout head = Ui.row(this);
        head.setBaselineAligned(false);
        head.setMinimumHeight(Ui.dp(this, CARD_TEXT_ROW_HEIGHT_DP));
        TextView title = Ui.text(this, t.title, 17, Ui.TITLE, true);
        title.setIncludeFontPadding(false);
        title.setOnClickListener(v -> editText("修改待办", t.title, str -> {   // 单击标题：编辑
            t.title = str;
            TodoStore.save(this);
            render();
        }));
        title.setOnLongClickListener(v -> {
            confirmDeleteTodo(t);
            return true;
        });
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView pct = Ui.text(this, getString(R.string.progress_percent, t.progress()), 15, Ui.DEEP, true);
        pct.setIncludeFontPadding(false);
        head.addView(pct);
        ImageButton addSubButton = Ui.iconButton(this, R.drawable.ic_add, Ui.ACTION_BLUE,
            "添加子项目", view -> showAddSubDialog(t));
        LinearLayout.LayoutParams addSubParams = new LinearLayout.LayoutParams(
            Ui.dp(this, 48), Ui.dp(this, 48));
        addSubParams.leftMargin = Ui.dp(this, 8);
        head.addView(addSubButton, addSubParams);
        ImageButton arrow = Ui.iconButton(this,
            open ? R.drawable.ic_expand_down : R.drawable.ic_expand_right, Ui.DEEP,
            open ? "收起待办详情" : "展开待办详情", view -> toggle(t));
        arrow.setOnLongClickListener(view -> {
            confirmDeleteTodo(t);
            return true;
        });
        head.addView(arrow);
        c.addView(head);

        FrameLayout progressArea = new FrameLayout(this);
        if (t.subs.isEmpty()) {
            // 没有子项目时直接编辑进度，10% 一档。
            progressArea.addView(seek(t.progress(), pct, val -> setTodoProgress(t, val)),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    Ui.dp(this, CARD_PROGRESS_HEIGHT_DP), Gravity.CENTER_VERTICAL));
        } else {
            // 有子项目时显示汇总进度。
            ProgressBar pb = Ui.bar(this, Ui.SAGE);
            pb.setProgress(t.progress());
            progressArea.addView(pb, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 8), Gravity.CENTER_VERTICAL));
        }
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, CARD_PROGRESS_HEIGHT_DP));
        progressParams.topMargin = Ui.dp(this, CARD_CONTENT_GAP_DP);
        c.addView(progressArea, progressParams);

        boolean doneOpen = completedSubsExpanded.contains(t.id);
        String meta = t.subs.isEmpty() ? "" : getString(R.string.completed_subitems,
            doneOpen ? "▾" : "▸", t.doneCount(), t.subs.size());
        Integer base = TodoStore.todayBase(this, t.id);
        if (base != null && t.progress() > base) {
            int progressToday = t.progress() - base;
            meta = meta.isEmpty() ? getString(R.string.progress_today, progressToday)
                : getString(R.string.completed_subitems_today, meta, progressToday);
        }
        if (!meta.isEmpty()) {
            TextView metaTv = Ui.text(this, meta, 12, Ui.SUB, false);
            metaTv.setIncludeFontPadding(false);
            metaTv.setMinHeight(Ui.dp(this, CARD_TEXT_ROW_HEIGHT_DP));
            metaTv.setGravity(Gravity.CENTER_VERTICAL);
            if (!t.subs.isEmpty()) {
                metaTv.setOnClickListener(view -> {
                    if (!completedSubsExpanded.add(t.id)) {
                        completedSubsExpanded.remove(t.id);
                    }
                    render();
                });
            }
            c.addView(metaTv, Ui.lp(this, CARD_CONTENT_GAP_DP));
        }

        if (doneOpen) {
            for (Todo.Sub sub : new ArrayList<>(t.subs)) {
                if (sub.progress >= 100) {
                    addSubRow(c, t, sub);
                }
            }
        }

        if (open) {
            addExpanded(c, t);
        }
        return c;
    }

    private void addExpanded(LinearLayout c, Todo t) {
        if (t.subs.isEmpty()) {
            TextView hint = Ui.text(this, "暂无子项目，单击+创建子项目", 13, Ui.SUB, false);
            hint.setIncludeFontPadding(false);
            c.addView(hint, Ui.lp(this, CARD_CONTENT_GAP_DP));
            return;
        }
        for (Todo.Sub s : new ArrayList<>(t.subs)) {
            if (s.progress < 100) {
                addSubRow(c, t, s);
            }
        }
    }

    private void showAddSubDialog(Todo todo) {
        editText("添加子项目", "", text -> {
            Todo.Sub sub = new Todo.Sub();
            sub.text = text;
            todo.subs.add(sub);
            expanded.add(todo.id);
            TodoStore.save(this);
            render();
        });
    }

    private void addSubRow(LinearLayout c, Todo t, Todo.Sub s) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setTag(s);
        item.setPadding(Ui.dp(this, SUB_ITEM_HORIZONTAL_PADDING_DP),
            Ui.dp(this, SUB_ITEM_VERTICAL_PADDING_DP), Ui.dp(this, SUB_ITEM_HORIZONTAL_PADDING_DP),
            Ui.dp(this, SUB_ITEM_VERTICAL_PADDING_DP));
        View divider = new View(this);
        divider.setBackgroundColor(Ui.TRACK);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, SUB_ITEM_DIVIDER_HEIGHT_DP));
        dividerParams.bottomMargin = Ui.dp(this, SUB_ITEM_TOP_DIVIDER_GAP_DP);
        item.addView(divider, dividerParams);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout top = Ui.row(this);
        top.setBaselineAligned(false);
        top.setMinimumHeight(Ui.dp(this, SUB_ITEM_TEXT_ROW_HEIGHT_DP));
        TextView tv = Ui.text(this, s.text, 15, s.progress >= 100 ? Ui.SUB : Ui.INK, false);
        tv.setIncludeFontPadding(false);
        if (s.progress >= 100) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        top.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView sp = Ui.text(this, getString(R.string.progress_percent, s.progress), 13, Ui.DEEP, true);
        sp.setIncludeFontPadding(false);
        top.addView(sp);
        top.setOnClickListener(v -> editText("修改子项目", s.text, str -> {
            s.text = str;
            TodoStore.save(this);
            render();
        }));
        top.setOnLongClickListener(v -> {
            confirmDeleteSub(t, s);
            return true;
        });
        content.addView(top);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, SUB_ITEM_PROGRESS_HEIGHT_DP));
        progressParams.topMargin = Ui.dp(this, SUB_ITEM_CONTENT_GAP_DP);
        content.addView(seek(s.progress, sp, val -> setSubProgress(t, s, val)), progressParams);
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.TOP);
        row.setBaselineAligned(false);
        ImageButton handle = Ui.iconButton(this, R.drawable.ic_drag_handle, Ui.SUB,
            getString(R.string.subitem_drag_description), null);
        handle.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 24));
        row.addView(handle);
        row.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        item.addView(row);
        View bottomDivider = new View(this);
        bottomDivider.setBackgroundColor(Ui.TRACK);
        LinearLayout.LayoutParams bottomDividerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, SUB_ITEM_DIVIDER_HEIGHT_DP));
        bottomDividerParams.topMargin = Ui.dp(this, SUB_ITEM_BOTTOM_DIVIDER_GAP_DP);
        item.addView(bottomDivider, bottomDividerParams);
        new SubItemDragController(c, item, s, handle, (target, after) -> {
            if (t.moveSubWithinGroup(s, target, after)) {
                TodoStore.save(this);
                body.post(this::render);
            }
        });
        c.addView(item, Ui.lp(this, SUB_ITEM_GAP_DP));
        updateSubItemDividers(c);
    }

    /**
     * 每个分组首尾及相邻子项目之间展示分隔线，避免相邻项出现双线。
     */
    private void updateSubItemDividers(LinearLayout parent) {
        Todo.Sub previous = null;
        for (int index = 0; index < parent.getChildCount(); index++) {
            View child = parent.getChildAt(index);
            if (!(child.getTag() instanceof Todo.Sub)) {
                previous = null;
                continue;
            }
            Todo.Sub sub = (Todo.Sub) child.getTag();
            boolean sameGroup = previous != null && (previous.progress >= 100) == (sub.progress >= 100);
            LinearLayout item = (LinearLayout) child;
            item.getChildAt(0).setVisibility(View.VISIBLE);
            View next = index + 1 < parent.getChildCount() ? parent.getChildAt(index + 1) : null;
            boolean nextInGroup = next != null && next.getTag() instanceof Todo.Sub
                && (((Todo.Sub) next.getTag()).progress >= 100) == (sub.progress >= 100);
            item.getChildAt(item.getChildCount() - 1).setVisibility(nextInGroup ? View.GONE : View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) item.getLayoutParams();
            params.topMargin = sameGroup ? 0 : Ui.dp(this, SUB_ITEM_GAP_DP);
            item.setLayoutParams(params);
            previous = sub;
        }
    }

    private void toggle(Todo t) {
        if (!expanded.add(t.id)) {
            expanded.remove(t.id);
        }
        render();
    }

    /**
     * 弹出输入框修改文字
     */
    private void editText(String title, String current, Consumer<String> onSave) {
        EditText et = Ui.edit(this, "", current, false);
        et.setSelection(et.getText().length());
        new AppDialog.Builder(this)
            .setTitle(title)
            .setView(et)
            .setPositiveButton("保存", (d, w) -> {
                String txt = et.getText().toString().trim();
                if (!txt.isEmpty()) {
                    onSave.accept(txt);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void confirmDeleteTodo(Todo t) {
        new AppDialog.Builder(this)
            .setMessage(getString(R.string.confirm_delete_todo, t.title))
            .setPositiveButtonColor(Ui.ROSE)
            .setPositiveButton("删除", (d, w) -> {
                TodoStore.all(this).remove(t);
                TodoStore.save(this);
                render();
            })
            .setNegativeButton("取消", null).show();
    }

    private void confirmDeleteSub(Todo t, Todo.Sub sub) {
        new AppDialog.Builder(this)
            .setMessage(getString(R.string.confirm_delete_subitem, sub.text))
            .setPositiveButtonColor(Ui.ROSE)
            .setPositiveButton("删除", (d, w) -> removeSub(t, sub))
            .setNegativeButton("取消", null).show();
    }

    /**
     * 10% 一档的滑块：拖动中只更新数字，松手才提交
     */
    @SuppressLint("ClickableViewAccessibility")
    private SeekBar seek(int pct, TextView label, IntConsumer onCommit) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(10);
        sb.setProgress(pct / 10);
        sb.setProgressTintList(ColorStateList.valueOf(Ui.DEEP));
        sb.setThumbTintList(ColorStateList.valueOf(Ui.DEEP));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(Ui.TRACK));

        // 只有按住"圆点"才能拖动：点轨道、从别处划过都不会改进度
        final int grabRange = Ui.dp(this, 24);
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        final boolean[] grabbed = {false};
        final boolean[] moved = {false};
        final float[] downX = {0f};
        sb.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    android.graphics.Rect r = sb.getThumb().getBounds();
                    float thumbCx = sb.getPaddingLeft() - sb.getThumbOffset() + r.centerX();
                    grabbed[0] = Math.abs(e.getX() - thumbCx) <= grabRange;
                    moved[0] = false;
                    downX[0] = e.getX();
                    if (grabbed[0] && v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return !grabbed[0];          // 没按到圆点：吞掉这次触摸，不改进度
                }
                case MotionEvent.ACTION_MOVE:
                    if (!grabbed[0]) {
                        return true;
                    }
                    if (Math.abs(e.getX() - downX[0]) > slop) {
                        moved[0] = true;
                    }
                    return false;                // 按住圆点：交给 SeekBar 正常拖动
                case MotionEvent.ACTION_UP:
                    if (!grabbed[0]) {
                        return true;
                    }
                    if (!moved[0]) {             // 只是轻点圆点、没拖动：当作取消，避免跳档
                        MotionEvent cancel = MotionEvent.obtain(e);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        v.onTouchEvent(cancel);
                        cancel.recycle();
                        return true;
                    }
                    return false;
                default:
                    return !grabbed[0];
            }
        });
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && label != null) {
                    label.setText(getString(R.string.progress_percent, p * 10));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                onCommit.accept(s.getProgress() * 10);
            }
        });
        return sb;
    }

    private void setTodoProgress(Todo t, int val) {
        int prev = t.progress;
        if (val == prev) {
            return;
        }
        t.progress = val;
        afterChange(t, () -> t.progress = prev);
    }

    private void setSubProgress(Todo t, Todo.Sub s, int val) {
        int prev = s.progress;
        if (val == prev) {
            return;
        }
        s.progress = val;
        afterChange(t, () -> s.progress = prev);
    }

    private void removeSub(Todo t, Todo.Sub s) {
        int idx = t.subs.indexOf(s);
        t.subs.remove(s);
        afterChange(t, () -> t.subs.add(Math.min(idx, t.subs.size()), s));
    }

    /**
     * 进度变化后统一处理：保存；到 100% 就归档，并给几秒钟的"撤销"
     */
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
        for (Todo t : TodoStore.all(this)) {
            if (t.completedAt > 0) {
                list.add(t);
            }
        }
        list.sort((a, b) -> Long.compare(b.completedAt, a.completedAt));

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
            String completedTime = f.format(new Date(t.completedAt));
            String completedDetails = t.subs.isEmpty() ? getString(R.string.completed_at, completedTime)
                : getString(R.string.completed_at_with_subitems, completedTime, t.subs.size());
            c.addView(Ui.text(this, completedDetails, 12, Ui.SUB, false),
                Ui.lp(this, 4));

            LinearLayout r = Ui.row(this);
            r.addView(Ui.button(this, "恢复到待办", Ui.SAGE, v -> {
                t.completedAt = 0;
                if (t.subs.isEmpty()) {
                    t.progress = 0;
                } else {
                    for (Todo.Sub s : t.subs) {
                        s.progress = 0;   // 恢复时进度清零
                    }
                }
                TodoStore.save(this);
                render();
            }));
            Button del = Ui.button(this, "删除", Ui.ROSE, v ->
                new AppDialog.Builder(this)
                    .setMessage(getString(R.string.confirm_delete_completed_todo, t.title))
                    .setPositiveButtonColor(Ui.ROSE)
                    .setPositiveButton("删除", (d, w) -> {
                        TodoStore.all(this).remove(t);
                        TodoStore.save(this);
                        render();
                    })
                    .setNegativeButton("取消", null).show());
            LinearLayout.LayoutParams dlp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        rc.addView(Ui.text(this, "每天时段开始的那一刻，会自动记录各待办的进度快照，并提醒你核对。", 13, Ui.SUB, false),
            Ui.lp(this, 4));
        SharedPreferences p = ReminderScheduler.prefs(this);
        reminderStartHour = p.getInt("sh", ReminderScheduler.DEFAULT_START_HOUR);
        reminderEndHour = p.getInt("eh", ReminderScheduler.DEFAULT_END_HOUR);
        reminderIntervalMinutes = p.getInt("iv", ReminderScheduler.DEFAULT_INTERVAL_MINUTES);
        addReminderSetting(rc, R.string.reminder_start_time, R.string.reminder_hour_unit,
            R.string.reminder_start_time, reminderStartHour, 0, 23, REMINDER_HOUR_PICKER_STEP_SIZE,
            value -> reminderStartHour = value);
        addReminderSetting(rc, R.string.reminder_end_time, R.string.reminder_hour_unit,
            R.string.reminder_end_time, reminderEndHour, 0, 23, REMINDER_HOUR_PICKER_STEP_SIZE,
            value -> reminderEndHour = value);
        addReminderSetting(rc, R.string.reminder_every, R.string.reminder_minutes_suffix,
            R.string.reminder_interval_title, reminderIntervalMinutes, ReminderScheduler.MIN_INTERVAL_MINUTES,
            ReminderScheduler.MAX_INTERVAL_MINUTES, TIMER_MINUTES_PICKER_STEP_SIZE,
            value -> reminderIntervalMinutes = value);

        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.leftMargin = Ui.dp(this, 8);
        LinearLayout row1 = Ui.row(this);
        row1.addView(Ui.button(this, "保存并开启", Ui.SAGE, v -> enable()));
        row1.addView(Ui.button(this, "关闭", Ui.SAND, v -> disable()), bp);
        rc.addView(row1, Ui.lp(this, 14));

        LinearLayout row2 = Ui.row(this);
        row2.addView(Ui.button(this, getString(R.string.reminder_test_rest), Ui.MAUVE,
            v -> ReminderScheduler.notifyNow(this)));
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
        bc.addView(Ui.text(this,
            "把所有待办（含子项目、已完成、每日快照）导出成一个 .json 文件，存到你选的位置。换手机或误删后，可以导入恢复。", 13,
            Ui.SUB, false), Ui.lp(this, 6));
        LinearLayout br = Ui.row(this);
        br.addView(Ui.button(this, "导出备份", Ui.BLUE, v -> doExport()));
        br.addView(Ui.button(this, "导入恢复", Ui.ROSE, v -> doImport()), bp);
        bc.addView(br, Ui.lp(this, 12));
        body.addView(bc, Ui.lp(this, 12));
    }

    /** 标题列固定为四个汉字宽，数值框紧接标题列并保持右边缘对齐。 */
    private void addReminderSetting(LinearLayout parent, int labelResource, int suffixResource,
        int titleResource, int currentValue, int minimum, int maximum, int stepSize, IntConsumer onConfirm) {
        LinearLayout row = Ui.row(this);
        row.setBaselineAligned(false);
        TextView label = Ui.text(this, getString(labelResource), 14, Ui.INK, false);
        label.setSingleLine(true);
        int labelWidth = (int) Math.ceil(label.getPaint().measureText(getString(R.string.reminder_start_time))) + 20;
        row.addView(label,
            new LinearLayout.LayoutParams(labelWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        Button valueButton = Ui.button(this, getString(R.string.reminder_setting_value, currentValue),
            REMINDER_VALUE_BACKGROUND, null);
        valueButton.setPadding(0, 0, 0, 0);
        valueButton.setIncludeFontPadding(false);
        valueButton.setGravity(Gravity.CENTER);
        valueButton.setContentDescription(getString(titleResource));
        int[] selectedValue = {currentValue};
        valueButton.setOnClickListener(view -> showTimeDropdown(valueButton,
            selectedValue[0], minimum, maximum, stepSize, false, value -> {
                selectedValue[0] = value;
                valueButton.setText(getString(R.string.reminder_setting_value, value));
                onConfirm.accept(value);
            }));
        row.addView(valueButton, new LinearLayout.LayoutParams(0,
            Ui.dp(this, REMINDER_VALUE_HEIGHT_DP), 1f));
        TextView suffix = Ui.text(this, getString(suffixResource), 14, Ui.SUB, false);
        suffix.setSingleLine(true);
        suffix.setPadding(Ui.dp(this, 8), 0, 0, 0);
        row.addView(suffix, new LinearLayout.LayoutParams(Ui.dp(this, 88), ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(row, Ui.lp(this, 12));
    }

    /** 在数值控件旁展开列表；底栏向上展开，输入数字后按键盘完成即可应用。 */
    private void showTimeDropdown(View anchor, int value, int minimum, int maximum, int stepSize, boolean above,
        IntConsumer onConfirm) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setFocusableInTouchMode(true);
        EditText input = Ui.edit(this, getString(R.string.time_input_hint),
            getString(R.string.reminder_setting_value, value), true);
        input.setGravity(Gravity.CENTER);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setSelectAllOnFocus(true);
        container.addView(input, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48)));
        List<Integer> selectableValues = new ArrayList<>();
        List<String> values = new ArrayList<>();
        int selectedPosition = 0;
        long nearestDistance = Long.MAX_VALUE;
        for (int number = minimum; number <= maximum; number += stepSize) {
            selectableValues.add(number);
            values.add(getString(R.string.reminder_setting_value, number));
            long distance = Math.abs((long) number - value);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                selectedPosition = selectableValues.size() - 1;
            }
        }
        ListView list = new ListView(this);
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, values) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView label = (TextView) super.getView(position, convertView, parent);
                label.setGravity(Gravity.CENTER);
                label.setTextColor(Ui.INK);
                label.setBackgroundColor(selectableValues.get(position) == value
                    ? REMINDER_VALUE_BACKGROUND : Ui.CARD);
                return label;
            }
        });
        container.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        android.graphics.Rect frame = new android.graphics.Rect();
        anchor.getWindowVisibleDisplayFrame(frame);
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int available = above ? location[1] - frame.top : frame.bottom - location[1] - anchor.getHeight();
        boolean openAbove = above || available < Ui.dp(this, 144);
        if (openAbove) {
            available = location[1] - frame.top;
        }
        int height = Math.min(Ui.dp(this, 280), Math.max(Ui.dp(this, 96), available));
        int width = Math.min(frame.width(), Math.max(anchor.getWidth(), Ui.dp(this, 144)));
        PopupWindow popup = new PopupWindow(container, width, height, true);
        popup.setBackgroundDrawable(Ui.round(Ui.CARD, Ui.dp(this, Ui.CARD_CORNER_RADIUS_DP)));
        popup.setElevation(Ui.dp(this, 4));
        popup.setOutsideTouchable(true);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        popup.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        popup.setOnDismissListener(() -> {
            android.view.inputmethod.InputMethodManager keyboard =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) {
                keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            onConfirm.accept(selectableValues.get(position));
            popup.dismiss();
        });
        input.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            try {
                int entered = Integer.parseInt(input.getText().toString().trim());
                if (entered >= minimum && entered <= maximum) {
                    onConfirm.accept(entered);
                    popup.dismiss();
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // 空值或超出整数范围时，保留输入并提示允许范围。
            }
            input.setError(getString(R.string.time_input_range, minimum, maximum));
            return true;
        });
        container.requestFocus();
        popup.showAsDropDown(anchor, 0, openAbove ? -anchor.getHeight() - height : 0);
        list.setSelection(selectedPosition);
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
        if (req == REQ_REMINDER_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                SharedPreferences settings = ReminderScheduler.prefs(this);
                reminderStartHour = settings.getInt("sh", ReminderScheduler.DEFAULT_START_HOUR);
                reminderEndHour = settings.getInt("eh", ReminderScheduler.DEFAULT_END_HOUR);
                reminderIntervalMinutes = settings.getInt("iv", ReminderScheduler.DEFAULT_INTERVAL_MINUTES);
                enable();
            } else {
                Toast.makeText(this, R.string.reminder_overlay_required, Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (res != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();

        if (req == REQ_EXPORT) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                assert os != null;
                os.write(TodoStore.exportJson(this).getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "备份成功", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.backup_failed, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        } else if (req == REQ_IMPORT) {
            new AppDialog.Builder(this)
                .setMessage("导入会覆盖现有的所有待办，确定继续吗？")
                .setPositiveButton("导入", (d, w) -> {
                    try (InputStream is = getContentResolver().openInputStream(uri)) {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while (true) {
                            assert is != null;
                            if (!((n = is.read(buf)) > 0))
                                break;
                            bo.write(buf, 0, n);
                        }
                        String json = bo.toString(StandardCharsets.UTF_8);
                        if (TodoStore.importJson(this, json)) {
                            Toast.makeText(this, "已恢复", Toast.LENGTH_SHORT).show();
                            tab = 0;
                            render();
                        } else {
                            Toast.makeText(this, "文件格式不对，没有导入", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.import_read_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
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
    private void enable() {
        ReminderScheduler.prefs(this).edit()
            .putInt("sh", reminderStartHour)
            .putInt("eh", reminderEndHour)
            .putInt("iv", reminderIntervalMinutes).apply();
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.reminder_overlay_required, Toast.LENGTH_LONG).show();
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())), REQ_REMINDER_OVERLAY);
            return;
        }
        ReminderScheduler.prefs(this).edit()
            .putBoolean("on", true).apply();

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (!am.canScheduleExactAlarms()) {
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getPackageName())));
        }
        refreshStatus(ReminderScheduler.scheduleNext(this, true));
    }

    private void disable() {
        ReminderScheduler.prefs(this).edit().putBoolean("on", false).apply();
        ReminderScheduler.cancel(this);
        refreshStatus(-1);
    }

    private void refreshStatus(long at) {
        if (tvStatus == null) {
            return;
        }
        if (!ReminderScheduler.enabled(this)) {
            tvStatus.setText(R.string.reminder_disabled);
        } else if (at > 0) {
            tvStatus.setText(getString(R.string.reminder_next,
                new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(at))));
        } else {
            tvStatus.setText(R.string.reminder_enabled);
        }
    }
}
