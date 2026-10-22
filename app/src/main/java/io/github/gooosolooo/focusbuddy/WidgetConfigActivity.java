package io.github.gooosolooo.focusbuddy;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

/** 添加或重新配置桌面组件；只有保存后才向桌面返回成功。 */
public class WidgetConfigActivity extends AppCompatActivity {
    private int widgetId;
    private Spinner todoSelector;
    private Button saveButton;
    private TextView emptyHint;
    private String selectedTodoId;
    private final List<Todo> choices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID);
        if (WidgetSupport.isInvalidWidget(this, widgetId)) {
            finish();
            return;
        }
        selectedTodoId = state == null ? WidgetSupport.todoId(this, widgetId) : state.getString("selected_todo");
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int padding = Ui.dp(this, 24);
        page.setPadding(padding, padding, padding, padding);
        page.setBackgroundColor(Ui.BG);
        page.addView(Ui.text(this, getString(R.string.widget_progress_name),
            22, Ui.TITLE, true));
        page.addView(Ui.text(this, getString(R.string.widget_config_todo), 15, Ui.INK, false), Ui.lp(this, 24));
        todoSelector = new Spinner(this);
        page.addView(todoSelector, Ui.lp(this, 8));
        emptyHint = Ui.text(this, getString(R.string.widget_no_todos), 14, Ui.SUB, false);
        page.addView(emptyHint, Ui.lp(this, 8));
        page.addView(Ui.button(this, getString(R.string.widget_manage_todos), Ui.SAND,
            view -> startActivity(new Intent(this, MainActivity.class))), Ui.lp(this, 12));
        saveButton = Ui.button(this, getString(R.string.widget_save), Ui.SAGE, view -> saveConfiguration());
        page.addView(saveButton, Ui.lp(this, 24));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        scroll.addView(page);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(view);
            if (controller != null) {
                controller.setAppearanceLightStatusBars(true);
                controller.setAppearanceLightNavigationBars(true);
            }
            return insets;
        });
        setContentView(scroll);
        ViewCompat.requestApplyInsets(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (todoSelector == null) {
            return;
        }
        String selectedId = choices.isEmpty() ? selectedTodoId
            : choices.get(Math.max(0, todoSelector.getSelectedItemPosition())).id;
        choices.clear();
        List<String> titles = new ArrayList<>();
        int selected = 0;
        for (Todo todo : TodoStore.all(this)) {
            if (WidgetSupport.isActive(todo)) {
                if (todo.id.equals(selectedId)) {
                    selected = choices.size();
                }
                choices.add(todo);
                titles.add(todo.title);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        todoSelector.setAdapter(adapter);
        todoSelector.setSelection(selected);
        todoSelector.setEnabled(!choices.isEmpty());
        emptyHint.setVisibility(choices.isEmpty() ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!choices.isEmpty());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        int selected = todoSelector == null ? -1 : todoSelector.getSelectedItemPosition();
        state.putString("selected_todo", selected >= 0 && selected < choices.size()
            ? choices.get(selected).id : selectedTodoId);
    }

    private void saveConfiguration() {
        int selected = todoSelector.getSelectedItemPosition();
        if (selected < 0 || selected >= choices.size()) {
            return;
        }
        WidgetSupport.save(this, widgetId, choices.get(selected).id);
        WidgetSupport.update(this, widgetId);
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
        finish();
    }
}
