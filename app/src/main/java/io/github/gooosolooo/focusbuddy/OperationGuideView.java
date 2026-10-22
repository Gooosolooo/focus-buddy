package io.github.gooosolooo.focusbuddy;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * 按需打开的操作指南面板，操作名称与说明分列显示。
 */
final class OperationGuideView extends LinearLayout {
    private OperationGuideView(Context context, Runnable onClose) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(Ui.dp(context, 20), Ui.dp(context, 12), Ui.dp(context, 20), Ui.dp(context, 24));
        setBackground(Ui.round(Ui.CARD, Ui.dp(context, 24)));

        View handle = new View(context);
        handle.setBackground(Ui.round(Ui.TRACK, Ui.dp(context, 2)));
        LayoutParams handleParams = new LayoutParams(Ui.dp(context, 32), Ui.dp(context, 4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(handle, handleParams);

        LinearLayout header = Ui.row(context);
        header.addView(Ui.text(context, "操作指南", 20, Ui.TITLE, true),
            new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Ui.button(context, "关闭", Ui.SAND, view -> onClose.run()));
        addView(header, Ui.lp(context, 16));

        ScrollView scroll = new ScrollView(context);
        LinearLayout entries = new LinearLayout(context);
        entries.setOrientation(VERTICAL);
        String[][] instructions = {
            {"修改名称", "单击待办或子项目标题。"},
            {"添加子项目", "单击标题行的 +，添加后自动展开。"},
            {"展开/收起", "单击标题行最右侧箭头，查看或收起详情。"},
            {"调整进度", "按住进度条圆点拖动，每档 10%；无子项目的待办可直接调整。"},
            {"查看已完成", "单击卡片统计行“已完成 数量/总数”，独立展开或收起已完成子项目。"},
            {"恢复未完成", "将已完成子项目的进度调至 100% 以下。"},
            {"悬浮计时", "点击底部左侧箭头或名称，在上方列表选择待办；点击分钟数后滚动选择或输入时长（1～1440，默认60），再点击“悬浮计时开始”。"},
            {"删除项目", "长按待办或子项目标题，确认后删除。"},
            {"调整顺序", context.getString(R.string.guide_subitem_reorder)},
            {"时段提醒", context.getString(R.string.guide_reminder_settings)},
            {"桌面组件", context.getString(R.string.guide_desktop_widgets)},
            {"自动归档", "待办进度达到 100% 后移入“已完成”页。"}
        };
        for (String[] instruction : instructions) {
            LinearLayout row = Ui.row(context);
            row.setGravity(Gravity.TOP);
            row.setPadding(0, Ui.dp(context, 12), 0, Ui.dp(context, 12));
            TextView action = Ui.text(context, instruction[0], 14, Ui.TITLE, true);
            row.addView(action, new LayoutParams(Ui.dp(context, 96), ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView description = Ui.text(context, instruction[1], 14, Ui.INK, false);
            row.addView(description, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            entries.addView(row);
        }
        scroll.addView(entries);
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    static void show(Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        OperationGuideView guide = new OperationGuideView(context, dialog::dismiss);
        // 留出顶部空间，长内容在面板内滚动。
        int availableHeight = context.getResources().getDisplayMetrics().heightPixels;
        int panelHeight = Math.min(Ui.dp(context, 600), availableHeight * 3 / 4);
        dialog.setContentView(guide, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, panelHeight));
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.show();
    }
}
