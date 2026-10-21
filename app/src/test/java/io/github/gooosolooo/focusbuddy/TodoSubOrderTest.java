package io.github.gooosolooo.focusbuddy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * 验证排序只影响当前完成分组，不改变进度和另一分组的位置。
 */
public class TodoSubOrderTest {
    @Test
    public void movingUnfinishedPreservesCompletedSlotsAndProgress() {
        Todo todo = new Todo();
        Todo.Sub first = sub(20);
        Todo.Sub completed = sub(100);
        Todo.Sub second = sub(50);
        todo.subs.addAll(Arrays.asList(first, completed, second));
        int progress = todo.progress();
        assertTrue(todo.moveSubWithinGroup(first, second, true));
        assertEquals(Arrays.asList(second, completed, first), todo.subs);
        assertEquals(progress, todo.progress());
        assertEquals(1, todo.doneCount());
        assertTrue(todo.moveSubWithinGroup(first, second, false));
        assertEquals(Arrays.asList(first, completed, second), todo.subs);
    }

    @Test
    public void movingCompletedPreservesUnfinishedSlots() {
        Todo todo = new Todo();
        Todo.Sub first = sub(100);
        Todo.Sub unfinished = sub(30);
        Todo.Sub second = sub(100);
        todo.subs.addAll(Arrays.asList(first, unfinished, second));
        assertTrue(todo.moveSubWithinGroup(second, first, false));
        assertEquals(Arrays.asList(second, unfinished, first), todo.subs);
    }

    @Test
    public void crossingGroupsOrMovingUnknownItemsDoesNotChangeOrder() {
        Todo todo = new Todo();
        Todo.Sub unfinished = sub(40);
        Todo.Sub completed = sub(100);
        todo.subs.addAll(Arrays.asList(unfinished, completed));
        assertFalse(todo.moveSubWithinGroup(unfinished, completed, true));
        assertFalse(todo.moveSubWithinGroup(sub(40), unfinished, false));
        assertFalse(todo.moveSubWithinGroup(unfinished, unfinished, true));
        assertEquals(Arrays.asList(unfinished, completed), todo.subs);
    }

    private static Todo.Sub sub(int progress) {
        Todo.Sub sub = new Todo.Sub();
        sub.progress = progress;
        return sub;
    }
}
