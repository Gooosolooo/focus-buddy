package io.github.gooosolooo.focusbuddy;

import android.Manifest;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.PowerManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/** 使用临时宿主验证进度组件的独立绑定及刷新，不修改桌面布局或待办文件。 */
@RunWith(AndroidJUnit4.class)
public class DesktopWidgetTest {
    /** 本测试专用的宿主编号，不使用系统桌面的宿主。 */
    private static final int TEST_HOST_ID = 17986;

    @Test
    public void progressWidgetsKeepIndependentBindingsAndRefresh() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        KeyguardManager keyguard = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assumeTrue("锁屏时系统会遮蔽组件内容，需在手机解锁且亮屏时执行。",
            power.isInteractive() && !keyguard.isDeviceLocked());
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        AppWidgetHost host = new AppWidgetHost(context, TEST_HOST_ID);
        Todo first = new Todo();
        first.title = "组件测试待办一";
        first.progress = 40;
        Todo second = new Todo();
        second.title = "组件测试待办二";
        second.progress = 20;
        int progressId = host.allocateAppWidgetId();
        int otherId = host.allocateAppWidgetId();
        instrumentation.getUiAutomation().adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET);
        try {
            ComponentName provider = new ComponentName(context, ProgressWidgetProvider.class);
            assertTrue(manager.bindAppWidgetIdIfAllowed(progressId, provider));
            assertTrue(manager.bindAppWidgetIdIfAllowed(otherId, provider));
            assertEquals(2, manager.getAppWidgetInfo(progressId).targetCellWidth);
            assertEquals(1, manager.getAppWidgetInfo(progressId).targetCellHeight);
            TodoStore.all(context).add(first);
            TodoStore.all(context).add(second);
            WidgetSupport.save(context, progressId, first.id);
            WidgetSupport.save(context, otherId, second.id);
            WidgetSupport.updateAll(context);
            instrumentation.runOnMainSync(() -> {
                AppWidgetHostView view = host.createView(context, progressId, manager.getAppWidgetInfo(progressId));
                assertEquals(first.title, ((TextView) view.findViewById(R.id.widget_title)).getText().toString());
                assertEquals(40, ((ProgressBar) view.findViewById(R.id.widget_progress_bar)).getProgress());
            });
            first.progress = 70;
            WidgetSupport.updateAll(context);
            instrumentation.runOnMainSync(() -> {
                AppWidgetHostView firstView = host.createView(context, progressId, manager.getAppWidgetInfo(progressId));
                AppWidgetHostView otherView = host.createView(context, otherId, manager.getAppWidgetInfo(otherId));
                assertEquals(70, ((ProgressBar) firstView.findViewById(R.id.widget_progress_bar)).getProgress());
                assertEquals(20, ((ProgressBar) otherView.findViewById(R.id.widget_progress_bar)).getProgress());
            });
            TodoStore.all(context).remove(first);
            WidgetSupport.update(context, progressId);
            instrumentation.runOnMainSync(() -> {
                AppWidgetHostView view = host.createView(context, progressId, manager.getAppWidgetInfo(progressId));
                assertEquals(context.getString(R.string.widget_choose_todo),
                    ((TextView) view.findViewById(R.id.widget_title)).getText().toString());
            });
        } finally {
            TodoStore.all(context).remove(first);
            TodoStore.all(context).remove(second);
            WidgetSupport.delete(context, progressId);
            WidgetSupport.delete(context, otherId);
            host.deleteHost();
            instrumentation.getUiAutomation().dropShellPermissionIdentity();
        }
    }
}
