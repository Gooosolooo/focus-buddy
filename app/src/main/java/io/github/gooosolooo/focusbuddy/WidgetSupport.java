package io.github.gooosolooo.focusbuddy;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

/** 单项进度组件的绑定、刷新，以及应用内计时选择的持久化。 */
final class WidgetSupport {
    /** 从桌面进度组件打开的待办 ID。 */
    static final String EXTRA_TODO_ID = "widget_todo_id";
    /** 应用内悬浮计时的默认时长，单位分钟。 */
    static final int DEFAULT_MINUTES = 60;
    private static final String PREFS = "desktop_widgets";

    private WidgetSupport() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean isOwned(Context context, int widgetId) {
        AppWidgetProviderInfo info = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId);
        return info != null && info.provider.equals(new ComponentName(context, ProgressWidgetProvider.class));
    }

    static String todoId(Context context, int widgetId) {
        return prefs(context).getString("todo_" + widgetId, "");
    }

    static String lastTodoId(Context context) {
        return prefs(context).getString("last_todo_id", "");
    }

    static int lastMinutes(Context context) {
        return prefs(context).getInt("last_minutes", DEFAULT_MINUTES);
    }

    static void rememberTodo(Context context, String todoId) {
        prefs(context).edit().putString("last_todo_id", todoId).apply();
    }

    static void rememberMinutes(Context context, int minutes) {
        prefs(context).edit().putInt("last_minutes", minutes).apply();
    }

    static void save(Context context, int widgetId, String todoId) {
        prefs(context).edit().putString("todo_" + widgetId, todoId).apply();
    }

    static void delete(Context context, int widgetId) {
        prefs(context).edit().remove("todo_" + widgetId).remove("minutes_" + widgetId).apply();
    }

    static Todo findTodo(Context context, String id) {
        for (Todo todo : TodoStore.all(context)) {
            if (todo.id.equals(id)) {
                return todo;
            }
        }
        return null;
    }

    static boolean isActive(Todo todo) {
        return todo != null && todo.completedAt == 0 && todo.progress() < 100;
    }

    static Intent configIntent(Context context, int widgetId) {
        return widgetIntent(context, WidgetConfigActivity.class, widgetId, "configure");
    }

    private static Intent widgetIntent(Context context, Class<?> activity, int widgetId, String action) {
        return new Intent(context, activity)
            .setData(Uri.parse("focusbuddy://widget/" + widgetId + "/" + action))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
    }

    private static PendingIntent click(Context context, Intent intent, int widgetId) {
        return PendingIntent.getActivity(context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** 数据保存后主动刷新所有进度组件实例，不使用周期轮询。 */
    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int id : manager.getAppWidgetIds(new ComponentName(context, ProgressWidgetProvider.class))) {
            update(context, id);
        }
    }

    static void update(Context context, int widgetId) {
        if (!isOwned(context, widgetId)) {
            return;
        }
        Todo todo = findTodo(context, todoId(context, widgetId));
        boolean active = isActive(todo);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_progress);
        String title = todo == null ? context.getString(R.string.widget_choose_todo) : todo.title;
        if (todo != null && !active) {
            title = context.getString(R.string.widget_completed_title, todo.title);
        }
        views.setTextViewText(R.id.widget_title, title);
        int progress = todo == null ? 0 : todo.progress();
        views.setTextViewText(R.id.widget_percent, context.getString(R.string.progress_percent, progress));
        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false);
        Intent open = widgetIntent(context, MainActivity.class, widgetId, "open")
            .putExtra(EXTRA_TODO_ID, todo == null ? "" : todo.id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        views.setOnClickPendingIntent(R.id.widget_root,
            click(context, active ? open : configIntent(context, widgetId), widgetId));
        AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views);
    }
}
