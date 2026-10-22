package io.github.gooosolooo.focusbuddy;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 验证组件在删除、归档及子项目完成后禁用启动入口。 */
public class WidgetSupportTest {
    @Test
    public void deletedOrArchivedTodoCannotStartTimer() {
        assertFalse(WidgetSupport.isActive(null));
        Todo archived = new Todo();
        archived.completedAt = 1L;
        assertFalse(WidgetSupport.isActive(archived));
    }

    @Test
    public void completedSubitemsOverrideStaleParentProgress() {
        Todo todo = new Todo();
        Todo.Sub sub = new Todo.Sub();
        sub.progress = 100;
        todo.subs.add(sub);
        assertFalse(WidgetSupport.isActive(todo));
        sub.progress = 90;
        todo.progress = 100;
        assertTrue(WidgetSupport.isActive(todo));
    }
}
