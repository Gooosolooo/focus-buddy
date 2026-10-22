package io.github.gooosolooo.focusbuddy;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

/** 处理桌面组件更新、删除和系统备份恢复。 */
abstract class FocusWidgetProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            WidgetSupport.updateAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        for (int id : widgetIds) {
            WidgetSupport.update(context, id);
        }
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        for (int id : widgetIds) {
            WidgetSupport.delete(context, id);
        }
    }

    @Override
    public void onRestored(Context context, int[] oldIds, int[] newIds) {
        for (int index = 0; index < oldIds.length; index++) {
            WidgetSupport.save(context, newIds[index], WidgetSupport.todoId(context, oldIds[index]));
        }
        for (int id : newIds) {
            WidgetSupport.update(context, id);
        }
    }
}
