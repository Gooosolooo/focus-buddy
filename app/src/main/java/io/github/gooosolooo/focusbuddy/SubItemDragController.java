package io.github.gooosolooo.focusbuddy;

import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;

/** 长按手柄后移动原始子项目视图，只允许在相同完成分组内竖直排序。 */
final class SubItemDragController implements View.OnTouchListener {
    /** 拖动视图提升的高度，单位 dp。 */
    private static final int DRAG_ELEVATION_DP = 4;
    /** 手指进入滚动区域边缘后触发自动滚动的距离，单位 dp。 */
    private static final int SCROLL_EDGE_DP = 48;
    /** 自动滚动每帧移动的距离，单位 dp。 */
    private static final int SCROLL_STEP_DP = 8;
    /** 相邻项目让位动画的时长，单位毫秒。 */
    private static final int SHIFT_DURATION_MS = 120;

    interface OrderListener {
        void onMove(Todo.Sub target, boolean after);
    }

    private final LinearLayout parent;
    private final LinearLayout item;
    private final Todo.Sub sub;
    private final ImageButton handle;
    private final OrderListener listener;
    private final List<View> peers = new ArrayList<>();
    private final List<View> hiddenDividers = new ArrayList<>();
    private final List<Float> originalDividerAlphas = new ArrayList<>();
    private final Runnable dragFrame = this::onDragFrame;
    private final int[] parentLocation = new int[2];
    private final int[] scrollLocation = new int[2];
    private int pointerId = -1;
    private int originalIndex;
    private int targetIndex;
    private float[] targetCenters;
    private int initialParentScreenY;
    private float initialRawY;
    private float lastRawY;
    private float originalElevation;
    private Drawable originalBackground;
    private ScrollView scrollView;
    private boolean dragging;

    SubItemDragController(LinearLayout parent, LinearLayout item, Todo.Sub sub,
        ImageButton handle, OrderListener listener) {
        this.parent = parent;
        this.item = item;
        this.sub = sub;
        this.handle = handle;
        this.listener = listener;
        handle.setOnTouchListener(this);
        handle.setOnLongClickListener(view -> {
            startDrag();
            return true;
        });
        handle.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {}

            @Override
            public void onViewDetachedFromWindow(View view) {
                finishDrag(false);
            }
        });
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pointerId = event.getPointerId(0);
                initialRawY = event.getRawY();
                lastRawY = initialRawY;
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) {
                    return false;
                }
                int pointerIndex = event.findPointerIndex(pointerId);
                if (pointerIndex < 0) {
                    finishDrag(false);
                    return true;
                }
                lastRawY = event.getRawY(pointerIndex);
                updatePosition();
                return true;
            case MotionEvent.ACTION_UP:
                boolean wasDragging = dragging;
                finishDrag(true);
                pointerId = -1;
                view.setPressed(false);
                if (!wasDragging) {
                    view.performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                boolean cancelledDrag = dragging;
                finishDrag(false);
                pointerId = -1;
                return cancelledDrag;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == pointerId) {
                    boolean activeDrag = dragging;
                    finishDrag(false);
                    pointerId = -1;
                    return activeDrag;
                }
                return dragging;
            default:
                return dragging;
        }
    }

    private void startDrag() {
        if (pointerId < 0 || dragging) {
            return;
        }
        peers.clear();
        for (int index = 0; index < parent.getChildCount(); index++) {
            View child = parent.getChildAt(index);
            if (child.getTag() instanceof Todo.Sub
                && (((Todo.Sub) child.getTag()).progress >= 100) == (sub.progress >= 100)) {
                peers.add(child);
            }
        }
        if (peers.size() < 2 || !peers.contains(item)) {
            return;
        }
        originalIndex = peers.indexOf(item);
        targetIndex = originalIndex;
        targetCenters = new float[peers.size()];
        for (int index = 0; index < peers.size(); index++) {
            View peer = peers.get(index);
            // 高度不同的项目也按拖动行的实际落位中心计算，首尾位置均可到达。
            targetCenters[index] = index <= originalIndex
                ? peer.getTop() + item.getHeight() / 2f : peer.getBottom() - item.getHeight() / 2f;
        }
        parent.getLocationOnScreen(parentLocation);
        initialParentScreenY = parentLocation[1];
        originalElevation = item.getElevation();
        originalBackground = item.getBackground();
        item.setBackgroundColor(Ui.CARD);
        item.setElevation(originalElevation + Ui.dp(item.getContext(), DRAG_ELEVATION_DP));
        updateDragDividers();
        scrollView = findScrollView();
        dragging = true;
        handle.getParent().requestDisallowInterceptTouchEvent(true);
        handle.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        handle.postOnAnimation(dragFrame);
    }

    private void updatePosition() {
        parent.getLocationOnScreen(parentLocation);
        float requested = lastRawY - initialRawY + initialParentScreenY - parentLocation[1];
        float minimum = peers.get(0).getTop() - item.getTop();
        float maximum = peers.get(peers.size() - 1).getBottom() - item.getBottom();
        item.setTranslationX(0f);
        item.setTranslationY(Math.max(minimum, Math.min(maximum, requested)));
        float center = item.getTop() + item.getTranslationY() + item.getHeight() / 2f;
        int nextIndex = targetIndexForCenter(targetCenters, originalIndex, center);
        if (nextIndex == targetIndex) {
            return;
        }
        targetIndex = nextIndex;
        updateDragDividers();
        ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) item.getLayoutParams();
        float distance = item.getHeight() + margins.topMargin + margins.bottomMargin;
        for (int index = 0; index < peers.size(); index++) {
            View peer = peers.get(index);
            if (peer == item) {
                continue;
            }
            float offset = 0f;
            if (originalIndex < index && index <= targetIndex) {
                offset = -distance;
            } else if (targetIndex <= index && index < originalIndex) {
                offset = distance;
            }
            peer.animate().translationY(offset).setDuration(SHIFT_DURATION_MS).start();
        }
    }

    /** 隐藏拖动项及起始、目标位置相邻项的边界线，保留原布局占位。 */
    private void updateDragDividers() {
        restoreDividers();
        for (int index = 0; index < peers.size(); index++) {
            if (Math.abs(index - originalIndex) <= 1 || Math.abs(index - targetIndex) <= 1) {
                LinearLayout peer = (LinearLayout) peers.get(index);
                hideDivider(peer.getChildAt(0));
                hideDivider(peer.getChildAt(peer.getChildCount() - 1));
            }
        }
    }

    private void hideDivider(View divider) {
        hiddenDividers.add(divider);
        originalDividerAlphas.add(divider.getAlpha());
        divider.setAlpha(0f);
    }

    /** 松手、取消或视图移除时恢复分隔线，不改变原有可见性和间距。 */
    private void restoreDividers() {
        for (int index = 0; index < hiddenDividers.size(); index++) {
            hiddenDividers.get(index).setAlpha(originalDividerAlphas.get(index));
        }
        hiddenDividers.clear();
        originalDividerAlphas.clear();
    }

    /** 根据实际落位中心计算位置，包含首尾边界，不依赖相邻行正在播放的动画。 */
    static int targetIndexForCenter(float[] centers, int originalIndex, float center) {
        int result = originalIndex;
        for (int index = 0; index < centers.length; index++) {
            if (index < originalIndex && center <= centers[index]) {
                return index;
            }
            if (index > originalIndex && center >= centers[index]) {
                result = index;
            }
        }
        return result;
    }

    private void onDragFrame() {
        if (!dragging) {
            return;
        }
        if (scrollView != null) {
            scrollView.getLocationOnScreen(scrollLocation);
            int edge = Ui.dp(item.getContext(), SCROLL_EDGE_DP);
            int step = Ui.dp(item.getContext(), SCROLL_STEP_DP);
            if (lastRawY < scrollLocation[1] + edge) {
                scrollView.scrollBy(0, -step);
            } else if (lastRawY > scrollLocation[1] + scrollView.getHeight() - edge) {
                scrollView.scrollBy(0, step);
            }
        }
        updatePosition();
        handle.postOnAnimation(dragFrame);
    }

    private ScrollView findScrollView() {
        ViewParent ancestor = parent.getParent();
        while (ancestor != null) {
            if (ancestor instanceof ScrollView) {
                return (ScrollView) ancestor;
            }
            ancestor = ancestor.getParent();
        }
        return null;
    }

    private void finishDrag(boolean commit) {
        if (!dragging) {
            return;
        }
        dragging = false;
        handle.removeCallbacks(dragFrame);
        handle.setPressed(false);
        handle.getParent().requestDisallowInterceptTouchEvent(false);
        for (View peer : peers) {
            peer.animate().cancel();
            peer.setTranslationX(0f);
            peer.setTranslationY(0f);
        }
        item.setElevation(originalElevation);
        item.setBackground(originalBackground);
        restoreDividers();
        if (commit && targetIndex != originalIndex) {
            Todo.Sub target = (Todo.Sub) peers.get(targetIndex).getTag();
            listener.onMove(target, targetIndex > originalIndex);
        }
        peers.clear();
    }
}
